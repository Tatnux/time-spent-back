package com.collet.timetracker.models.api.activity;

import com.collet.timetracker.models.api.issue.Issue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public final class ActivityIssue {
    private String id;
    private Issue issue;
    private List<Activity> activities = new ArrayList<>();
    private List<MergeRequest> mergeRequest = new ArrayList<>();
}
