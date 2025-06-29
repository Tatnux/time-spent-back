package com.collet.timetracker.service;

import com.collet.timetracker.models.api.issue.IterationIssue;
import com.collet.timetracker.models.api.issue.Tags;
import com.collet.timetracker.models.api.iteration.IterationNode;
import com.collet.timetracker.models.api.iteration.IterationSort;
import com.collet.timetracker.models.api.iteration.IterationState;
import com.collet.timetracker.models.api.issue.Issue;
import com.collet.timetracker.service.graphql.GitLabGraphQLService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IterationService {

    private static final String ITERATION_QUERY = GitLabGraphQLService.loadGraphQLQuery("Iteration");
    private static final String ITERATION_ISSUES_QUERY = GitLabGraphQLService.loadGraphQLQuery("IterationIssues");
    private static final String TAGS_QUERY = GitLabGraphQLService.loadGraphQLQuery("Tags");

    private final GitLabGraphQLService graphQLService;

    public List<IterationNode> getIteration(IterationState state, IterationSort sort, int first) {
        return this.graphQLService.graphQLQuery(IterationNode.class, ITERATION_QUERY, "iterations",
                this.graphQLService.getGroupMap(Map.of(
                        "state", state.toString(),
                        "sort", sort.toString(),
                        "first", first)));
    }

    public List<IterationIssue> getIterationIssues(String iterationId) {
        return this.graphQLService.graphQLQuery(IterationIssue.class, ITERATION_ISSUES_QUERY, "issues",
                this.graphQLService.getGroupMap(Map.of("iteration", iterationId)));
    }

    public List<Tags> getGroupTags() {
        return this.graphQLService.graphQLQuery(Tags.class, TAGS_QUERY, "labels", this.graphQLService.getGroupMap());
    }


}
