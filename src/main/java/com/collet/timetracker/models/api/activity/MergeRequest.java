package com.collet.timetracker.models.api.activity;

import com.fasterxml.jackson.annotation.JsonAlias;

public record MergeRequest(int id,
                           int iid,
                           @JsonAlias("project_id") long projectId,
                           @JsonAlias("web_url") String webUrl,
                           String title,
                           String description,
                           @JsonAlias("source_branch")
                           String sourceBranch) {
}
