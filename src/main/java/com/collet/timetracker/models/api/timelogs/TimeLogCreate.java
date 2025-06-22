package com.collet.timetracker.models.api.timelogs;

public record TimeLogCreate(String issueId, String timeSpent, String spentAt) {
}
