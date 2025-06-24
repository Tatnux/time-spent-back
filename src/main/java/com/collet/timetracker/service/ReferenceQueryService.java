package com.collet.timetracker.service;

import com.collet.timetracker.service.activity.reference.Reference;
import com.collet.timetracker.service.activity.reference.ReferenceCollection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
        List<Reference<T>> list = new ArrayList<>(collection.getMap().keySet());

        // Split list to reduce complexity for GraphQL request
        for (List<Reference<T>> referenceList : splitList(list, 15)) {
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
            this.processResult(collection, referenceList, clazz, result);
        }

    }

    private <T> void processResult(ReferenceCollection<T> collection, List<Reference<T>> references,
                                   Class<T> clazz, JsonNode result) {
        JsonNode data = result.get("data");
        for (int i = 0; i < references.size(); i++) {
            Reference<T> reference = references.get(i);
            JsonNode value = data.findValue("i" + i);
            if (value != null) {
                JsonNode jsonNode = reference.processJson(value);
                if(jsonNode != null) {
                    try {
                        T nodeResult = this.objectMapper.treeToValue(this.graphQLService.flattenNodesRecursively(jsonNode), clazz);
                        collection.getMap().get(reference).forEach(consumer -> consumer.accept(nodeResult));
                    } catch (JsonProcessingException e) {
                        LOG.log(Level.SEVERE, "An error occurred while parsing %s".formatted(clazz.getSimpleName()), e);
                    }
                }
            }
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
            String id = reference.projectId();
            map.computeIfAbsent(id == null || id.startsWith("gid") ? id : "gid://gitlab/Project/" + id,
                    _ -> new ArrayList<>()).add(indexedReference);
        }
        return map;
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
