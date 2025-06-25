package com.collet.timetracker.service.activity.reference;

import com.collet.timetracker.models.api.activity.MergeRequest;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@AllArgsConstructor
public class MergeRequestIidReference implements Reference<MergeRequest> {

    private final String projectId;
    private final long iid;

    public MergeRequestIidReference(long projectId, long iid) {
        this.projectId = String.valueOf(projectId);
        this.iid = iid;
    }

    @Override
    public String graphQLQuery() {
        return """
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
               """.formatted(this.iid);
    }

    @Override
    public String projectId() {
        return this.projectId;
    }
}
