package com.collet.timetracker.models.api.timelogs;

import java.util.Date;

public record TimeLogNode(String id, Issue issue, Date spentAt, int timeSpent, String summary, String note) {
}
