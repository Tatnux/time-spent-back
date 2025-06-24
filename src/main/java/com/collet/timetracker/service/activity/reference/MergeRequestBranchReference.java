package com.collet.timetracker.service.activity.reference;

import com.collet.timetracker.models.api.activity.MergeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@AllArgsConstructor
@EqualsAndHashCode
public class MergeRequestBranchReference implements Reference<MergeRequest> {

    private final long projectId;
    private final String branchName;

    @Override
    public String graphQLQuery() {
        return """
                mergeRequests(sourceBranches: "%s", first: 1) {
                    nodes {
                        id
                        iid
                        projectId
                        webUrl
                        title
                        description,
                        sourceBranch
                    }
                }
               """.formatted(this.branchName);
    }

    @Override
    public String projectId() {
        return String.valueOf(this.projectId);
    }

    @Override
    public JsonNode processJson(JsonNode node) {
        return node.get("nodes").get(0);
    }
}
