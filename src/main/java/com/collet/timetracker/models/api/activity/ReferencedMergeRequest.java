package com.collet.timetracker.models.api.activity;

public record ReferencedMergeRequest(ReferencedIssue issue, MergeRequest mergeRequest) {
}
