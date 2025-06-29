package com.collet.timetracker.service.activity.reference;

import com.collet.timetracker.models.api.activity.MergeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@AllArgsConstructor
@EqualsAndHashCode
public class MergeRequestFullPathIidReference implements Reference<MergeRequest> {

    private final String fullPath;
    private final long iid;

    @Override
    public String graphQLQuery() {
        return """
                project(fullPath: "%s") {
                    mergeRequest(iid: "%s") {
                        id
                        iid
                        projectId
                        webUrl
                        title
                        description,
                        sourceBranch
                        assignees {
                            nodes {
                                id
                                name
                                username
                            }
                        }
                    }
                }
               """.formatted(this.fullPath, this.iid);
    }

    @Override
    public JsonNode processJson(JsonNode node) {
        return node.get("mergeRequest");
    }

    @Override
    public String projectId() {
        return null;
    }
}
