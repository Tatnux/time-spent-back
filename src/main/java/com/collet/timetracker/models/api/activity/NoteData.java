package com.collet.timetracker.models.api.activity;

import com.fasterxml.jackson.annotation.JsonAlias;

public record NoteData(String body,
                       @JsonAlias("project_id") long projectId,
                       @JsonAlias("noteable_id") long noteableId,
                       @JsonAlias("noteable_iid") int noteableIid,
                       @JsonAlias("noteable_type") String noteableType) {
}
