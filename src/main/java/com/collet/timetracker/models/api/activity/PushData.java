package com.collet.timetracker.models.api.activity;

import com.fasterxml.jackson.annotation.JsonAlias;

public record PushData(@JsonAlias("commit_count") int commitCount,
                       @JsonAlias("ref_type") String refType,
                       String ref) {
}
