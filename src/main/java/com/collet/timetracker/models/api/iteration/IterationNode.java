package com.collet.timetracker.models.api.iteration;

public record IterationNode(String id, String dueDate, String startDate, IterationState state, IterationCadence iterationCadence) {
}
