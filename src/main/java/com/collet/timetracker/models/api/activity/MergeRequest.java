package com.collet.timetracker.models.api.activity;

import com.collet.timetracker.models.api.user.GitlabUser;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record MergeRequest(String id,
                           int iid,
                           @JsonAlias("project_id") String projectId,
                           @JsonAlias("web_url") String webUrl,
                           String title,
                           String description,
                           List<GitlabUser> assignees,
                           @JsonAlias("source_branch")
                           String sourceBranch) {
}
