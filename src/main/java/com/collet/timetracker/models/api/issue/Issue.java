package com.collet.timetracker.models.api.issue;

import com.collet.timetracker.models.api.user.GitlabUser;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@NoArgsConstructor
@Accessors(fluent = true)
public class Issue {
    private String id;
    private int iid;
    private String title;
    private String state;
    private String webUrl;
    private long projectId;
    private List<GitlabUser> assignees;
    private boolean moved;
    private Issue movedTo;

}
