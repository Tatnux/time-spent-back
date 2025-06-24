package com.collet.timetracker.models.api.timelogs;

import com.collet.timetracker.models.api.user.GitlabUser;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record Issue(String id,
                    int iid,
                    String title,
                    String state,
                    @JsonAlias("web_url") String webUrl,
                    @JsonAlias("project_id") long projectId,
                    List<GitlabUser> assignees,
                    @JsonAlias("moved_to_id") long movedToId) {
}
