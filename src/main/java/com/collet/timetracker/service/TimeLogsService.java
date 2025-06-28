package com.collet.timetracker.service;

import com.collet.timetracker.models.api.timelogs.TimeLogNode;
import com.collet.timetracker.service.graphql.GitLabGraphQLService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TimeLogsService {

    private static final String TIME_LOGS = GitLabGraphQLService.loadGraphQLQuery("TimeLogs");
    private static final String TIME_LOGS_CREATE = GitLabGraphQLService.loadGraphQLQuery("TimeLogsCreate");

    private final GitLabGraphQLService graphQLService;

    public List<TimeLogNode> getTimeSpentNotes(String username, String startTime, String endTime) {
        return graphQLService.graphQLQuery(TimeLogNode.class, TIME_LOGS, "timelogs",
                this.graphQLService.getGroupMap(Map.of(
                        "startTime", startTime,
                        "endTime", endTime,
                        "username", username
                )));
    }

    public String createTimeSpent(String issueId, String timeSpent, String spentAt) {
        return graphQLService.graphQLQuery(TIME_LOGS_CREATE, Map.of(
                "issueId", issueId,
                "timeSpent", timeSpent,
                "spentAt", spentAt
        ));
    }


}
