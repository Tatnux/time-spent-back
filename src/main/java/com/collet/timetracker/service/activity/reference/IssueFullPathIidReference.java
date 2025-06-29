package com.collet.timetracker.service.activity.reference;

import com.collet.timetracker.models.api.issue.Issue;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@AllArgsConstructor
@EqualsAndHashCode
public class IssueFullPathIidReference implements Reference<Issue> {

    private final String fullPath;
    private final long iid;

    @Override
    public String graphQLQuery() {
        return """
                project(fullPath: "%s") {
                    issue(iid: "%s") {
                        id
                        iid
                        webUrl
                        state
                        projectId
                        title
                        moved
                        movedTo {
                            id
                        }
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
        return node.get("issue");
    }

    @Override
    public String projectId() {
        return null;
    }
}
