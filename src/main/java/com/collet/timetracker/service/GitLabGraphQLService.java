package com.collet.timetracker.service;

import com.collet.timetracker.models.api.iteration.IterationNode;
import com.collet.timetracker.models.api.timelogs.Issue;
import com.collet.timetracker.models.api.timelogs.TimeLogNode;
import com.collet.timetracker.models.api.user.GitlabUser;
import com.collet.timetracker.models.api.user.GroupMemeber;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private static final String ACTIVITY = loadGraphQLQuery("current-user-activity");
    private static final String ISSUE = loadGraphQLQuery("issue");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.security.oauth2.client.provider.gitlab.graphql}")
    private String graphQL;

    @Value("${app.group}")
    private String group;

    public List<GitlabUser> getUsers() {
        final LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        return graphQLQuery(GroupMemeber.class, USERS, "groupMembers", Map.of(
                "group", this.group
        )).stream()
                .map(GroupMemeber::user)
                .filter(gitlabUser -> gitlabUser.lastActivityOn() != null && gitlabUser.lastActivityOn().isAfter(sixMonthsAgo))
                .toList();
    }

    public String getActivity() {
        return graphQLQuery(ACTIVITY, Collections.emptyMap());
    }

    public List<Issue> getIssues(String... ids) {
        return graphQLQuery(Issue.class, ISSUE, "issues", Map.of(
                "group", this.group,
                "iids", ids
        ));
    }

    public List<IterationNode> getClosedIteration() {
        return graphQLQuery(IterationNode.class, ITERATION_CLOSED, "iterations", Map.of(
                "group", this.group
        ));
    }

    public List<IterationNode> getCurrentIteration() {
        return graphQLQuery(IterationNode.class, ITERATION_CURRENT, "iterations", Map.of(
                "group", this.group
        ));
    }

    public List<TimeLogNode> getTimeSpentNotes(String username, String startTime, String endTime) {
        return graphQLQuery(TimeLogNode.class, TIME_LOGS, "timelogs", Map.of(
                "group", this.group,
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

    private String graphQLQuery(String query, Map<String, Object> variables) {
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
        JsonNode nodes = root.path("data").path("group").path(rootField).path("nodes");

        if (nodes.isMissingNode() || !nodes.isArray()) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>();
        for (JsonNode node : nodes) {
            result.add(objectMapper.treeToValue(node, clazz));
        }

        return result;
    }

    @SneakyThrows
    private static String loadGraphQLQuery(String fileName) {
        ClassPathResource resource = new ClassPathResource("graphql/%s.graphql".formatted(fileName));
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
