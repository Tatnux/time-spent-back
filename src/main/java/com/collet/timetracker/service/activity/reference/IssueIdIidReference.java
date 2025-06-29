package com.collet.timetracker.service.activity.reference;

import com.collet.timetracker.models.api.issue.Issue;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@AllArgsConstructor
public class IssueIdIidReference implements Reference<Issue> {

    private final String projectId;
    private final long iid;

    public IssueIdIidReference(long projectId, long iid) {
        this(String.valueOf(projectId), iid);
    }

    @Override
    public String graphQLQuery() {
        return """
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
               """.formatted(this.iid);
    }

    @Override
    public String projectId() {
        return this.projectId;
    }
}
