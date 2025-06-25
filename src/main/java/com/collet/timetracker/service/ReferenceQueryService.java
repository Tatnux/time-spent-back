package com.collet.timetracker.service;

import com.collet.timetracker.service.activity.reference.Reference;
import com.collet.timetracker.service.activity.reference.ReferenceCollection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class ReferenceQueryService {

    private static final Logger LOG = Logger.getLogger(ReferenceQueryService.class.getName());

    @Value("${app.group}")
    private String group;

    @Value("${spring.security.oauth2.client.provider.gitlab.graphql}")
    private String graphQLUrl;

    private final GitLabGraphQLService graphQLService;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public <T> void resolveReferences(ReferenceCollection<T> collection, Class<T> clazz) {
        long start = System.currentTimeMillis();
        if(collection.getMap().isEmpty()) {
            return;
        }

        final Map<Reference<T>, List<Consumer<T>>> map = new HashMap<>(collection.getMap());
        collection.clear();

        List<Reference<T>> list = new ArrayList<>(map.keySet());
        list.sort(Comparator.comparing(ref -> {
            String projectId = getProjectId(ref.projectId());
            return projectId == null ? "" : projectId;
        }));

        // Split list to reduce complexity for each GraphQL request
        List<List<Reference<T>>> batches = splitList(list, 5);

        try(ExecutorService rawExecutor = Executors.newFixedThreadPool(Math.min(batches.size(), 10))) {
            ExecutorService executor = new DelegatingSecurityContextExecutorService(rawExecutor);
            List<Future<Void>> futures = new ArrayList<>();

            for (List<Reference<T>> referenceList : batches) {
                Callable<Void> task = () -> {
                    buildAndExecuteQuery(map, clazz, referenceList);
                    return null;
                };
                futures.add(executor.submit(task));
            }

            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Erreur lors de l'exécution parallèle des requêtes GraphQL", e);
                }
            }

            executor.shutdown();
        }

        Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);

        long seconds = duration.getSeconds();
        long millis = duration.toMillisPart(); // Java 9+

        LOG.info(() -> "Time : %d.%03d seconds".formatted(seconds, millis));

    }

    private <T> void buildAndExecuteQuery(Map<Reference<T>, List<Consumer<T>>> map, Class<T> clazz, List<Reference<T>> referenceList) throws JsonProcessingException {
        Map<String, List<IndexedReference<T>>> projectMap = toProjectMap(referenceList);

        StringBuilder query = new StringBuilder("{\n");
        StringBuilder groupQuery = new StringBuilder("group(fullPath: \"" + this.group + "\") {\n");

        AtomicInteger mapIndex = new AtomicInteger();
        projectMap.forEach((projectId, indexedReferences) -> {

            // Building the query
            StringBuilder referencesQuery = new StringBuilder();
            indexedReferences.forEach(ref ->
                    referencesQuery.append(ref.index()).append(":").append(ref.reference().graphQLQuery()));

            // Adding it to the global query
            if (projectId == null) {
                query.append(referencesQuery);
            } else {
                groupQuery.append("p").append(mapIndex.getAndIncrement()).append(":")
                        .append(projectBlock(projectId, referencesQuery.toString()));
            }
        });

        groupQuery.append("}");
        if (groupQuery.length() > 23 + this.group.length()) {
            // Adding the group query only if it has been updated
            query.append(groupQuery);
        }
        query.append("}");

        JsonNode result = this.objectMapper.readTree(this.graphQLService.graphQLQuery(query.toString(), Map.of()));
        this.processResult(map, referenceList, clazz, result);
    }

    private <T> void processResult(Map<Reference<T>, List<Consumer<T>>> map, List<Reference<T>> references,
                                   Class<T> clazz, JsonNode result) {
        JsonNode errors = result.get("errors");
        if(errors != null) {
            LOG.severe(() -> "An error occured on GitLab side : %s".formatted(errors));
            return;
        }
        JsonNode data = result.get("data");
        for (int i = 0; i < references.size(); i++) {
            Reference<T> reference = references.get(i);
            JsonNode rawValue = data.findValue("i" + i);

            T parsedResult = parseNodeValue(reference, rawValue, clazz);
            List<Consumer<T>> consumers = map.get(reference);

            if (consumers != null) {
                consumers.forEach(consumer -> consumer.accept(parsedResult));
            } else {
                LOG.fine(() -> "No consumer found for reference: " + reference);
            }
        }
    }

    private <T> T parseNodeValue(Reference<T> reference, JsonNode rawValue, Class<T> clazz) {
        if (rawValue == null) {
            return null;
        }

        JsonNode processed = reference.processJson(rawValue);
        if (processed == null) {
            return null;
        }

        try {
            JsonNode flattened = graphQLService.flattenNodesRecursively(processed);
            return objectMapper.treeToValue(flattened, clazz);
        } catch (JsonProcessingException e) {
            LOG.log(Level.SEVERE, "Error parsing JSON to %s".formatted(clazz.getSimpleName()), e);
            return null;
        }
    }

    /**
     * Split the provided list of references to a project -> map
     * @param references The list of references
     */
    private static <T> Map<String, List<IndexedReference<T>>> toProjectMap(List<Reference<T>> references) {
        Map<String, List<IndexedReference<T>>> map = new HashMap<>();

        for (int i = 0; i < references.size(); i++) {
            Reference<T> reference = references.get(i);
            IndexedReference<T> indexedReference = new IndexedReference<>("i" + i, reference);
            map.computeIfAbsent(getProjectId(reference.projectId()),
                    _ -> new ArrayList<>()).add(indexedReference);
        }
        return map;
    }

    private static String getProjectId(String id) {
        return id == null || id.startsWith("gid") ? id : "gid://gitlab/Project/" + id;
    }

    public static <T> List<List<T>> splitList(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }

    private static String projectBlock(String id, String query) {
        return  """
                projects(ids: ["%s"], includeSubgroups: true, first: 1) {
                    nodes {
                        %s
                    }
                }
                """.formatted(id, query);
    }

    record IndexedReference<T>(String index, Reference<T> reference) {
    }

}
