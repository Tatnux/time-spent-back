package com.collet.timetracker.service.activity.reference;

import com.collet.timetracker.models.api.activity.MergeRequest;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@AllArgsConstructor
@EqualsAndHashCode
public class MergeRequestIidReference implements Reference<MergeRequest> {

    private final long projectId;
    private final long iid;

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
                }
               """.formatted(this.iid);
    }

    @Override
    public String projectId() {
        return String.valueOf(this.projectId);
    }
}
