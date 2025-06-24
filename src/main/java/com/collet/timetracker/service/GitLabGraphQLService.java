package com.collet.timetracker.service;

import com.collet.timetracker.models.api.iteration.IterationNode;
import com.collet.timetracker.models.api.timelogs.TimeLogNode;
import com.collet.timetracker.models.api.user.GitlabUser;
import com.collet.timetracker.models.api.user.GroupMemeber;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GitLabGraphQLService {

    private static final String ITERATION_CLOSED = loadGraphQLQuery("iteration-closed");
    private static final String ITERATION_CURRENT = loadGraphQLQuery("iteration-current");
    private static final String TIME_LOGS = loadGraphQLQuery("time-logs");
    private static final String TIME_LOGS_CREATE = loadGraphQLQuery("time-logs-create");
    private static final String USERS = loadGraphQLQuery("users");
    
    private static final String GROUP = "group";
    private static final String NODES = "nodes";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.group}")
    private String group;

    @Value("${spring.security.oauth2.client.provider.gitlab.graphql}")
    private String graphQL;

    public List<GitlabUser> getUsers() {
        final LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        return graphQLQuery(GroupMemeber.class, USERS, "groupMembers", Map.of(GROUP, this.group)).stream()
                .map(GroupMemeber::user)
                .filter(gitlabUser -> gitlabUser.lastActivityOn() != null && gitlabUser.lastActivityOn().isAfter(sixMonthsAgo))
                .toList();
    }

    public List<IterationNode> getClosedIteration(int first) {
        return graphQLQuery(IterationNode.class, ITERATION_CLOSED, "iterations", Map.of(GROUP, this.group,
                "first", first));
    }

    public List<IterationNode> getCurrentIteration() {
        return graphQLQuery(IterationNode.class, ITERATION_CURRENT, "iterations", Map.of(GROUP, this.group));
    }

    public List<TimeLogNode> getTimeSpentNotes(String username, String startTime, String endTime) {
        return graphQLQuery(TimeLogNode.class, TIME_LOGS, "timelogs", Map.of(GROUP, this.group,
                "startTime", startTime,
                "endTime", endTime,
                "username", username
        ));
    }

    public String createTimeSpent(String issueId, String timeSpent, String spentAt) {
        return graphQLQuery(TIME_LOGS_CREATE, Map.of(
                "issueId", "gid://gitlab/Issue/" + issueId,
                "timeSpent", timeSpent,
                "spentAt", spentAt
        ));
    }

    public String graphQLQuery(String query, Map<String, Object> variables) {
        Map<String, Object> requestPayload = Map.of(
                "query", query,
                "variables", variables);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestPayload);
        ResponseEntity<String> response = restTemplate.postForEntity(
                this.graphQL,
                request,
                String.class);
        return response.getBody();
    }

    @SneakyThrows
    private <T> List<T> graphQLQuery(Class<T> clazz, String query, String rootField, Map<String, Object> variables) {
        JsonNode root = objectMapper.readTree(this.graphQLQuery(query, variables));
        JsonNode nodes = root.path("data").path(GROUP).path(rootField).path(NODES);

        if (nodes.isMissingNode() || !nodes.isArray()) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>();
        for (JsonNode node : nodes) {
            result.add(objectMapper.treeToValue(flattenNodesRecursively(node), clazz));
        }

        return result;
    }

    public JsonNode flattenNodesRecursively(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = node.deepCopy();
            Iterator<String> fieldNames = node.fieldNames();

            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode child = node.get(fieldName);

                if (child.isObject() && child.has(NODES) && child.get(NODES).isArray()) {
                    objectNode.set(fieldName, flattenNodesRecursively(child.get(NODES)));
                } else if (child.isContainerNode()) {
                    objectNode.set(fieldName, flattenNodesRecursively(child));
                }
            }
            return objectNode;
        }

        if (node.isArray()) {
            ArrayNode arrayNode = node.deepCopy();
            for (int i = 0; i < arrayNode.size(); i++) {
                arrayNode.set(i, flattenNodesRecursively(arrayNode.get(i)));
            }
            return arrayNode;
        }

        return node;
    }

    @SneakyThrows
    private static String loadGraphQLQuery(String fileName) {
        ClassPathResource resource = new ClassPathResource("graphql/%s.graphql".formatted(fileName));
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
