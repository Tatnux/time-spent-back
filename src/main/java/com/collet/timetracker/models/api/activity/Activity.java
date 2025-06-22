package com.collet.timetracker.models.api.activity;

import com.fasterxml.jackson.annotation.JsonAlias;

public record Activity(long id,
                       @JsonAlias("project_id") long projectId,
                       @JsonAlias("action_name") String actionName,
                       @JsonAlias("target_iid") long targetIid,
                       @JsonAlias("target_type") String targetType,
                       @JsonAlias("push_data") PushData pushData,
                       NoteData note) {
}
