package com.collet.timetracker.service;

import com.collet.timetracker.models.api.iteration.IterationNode;
import com.collet.timetracker.models.api.iteration.IterationSort;
import com.collet.timetracker.models.api.iteration.IterationState;
import com.collet.timetracker.service.graphql.GitLabGraphQLService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IterationService {

    private static final String ITERATION_QUERY = GitLabGraphQLService.loadGraphQLQuery("Iteration");

    private final GitLabGraphQLService graphQLService;

    public List<IterationNode> getIteration(IterationState state, IterationSort sort, int first) {
        return this.graphQLService.graphQLQuery(IterationNode.class, ITERATION_QUERY, "iterations",
                this.graphQLService.getGroupMap(Map.of(
                        "state", state.toString(),
                        "sort", sort.toString(),
                        "first", first)));
    }


}
