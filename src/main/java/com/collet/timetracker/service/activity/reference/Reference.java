package com.collet.timetracker.service.activity.reference;

import com.fasterxml.jackson.databind.JsonNode;

public interface Reference<T> {

    String projectId();

    String graphQLQuery();

    default  JsonNode processJson(JsonNode node) {
        return node;
    }
}
