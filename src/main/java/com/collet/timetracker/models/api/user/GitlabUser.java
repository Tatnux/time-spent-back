package com.collet.timetracker.models.api.user;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public final class GitlabUser {
    private String id;
    private String name;
    private String username;
    @JsonAlias("public_email")
    private String publicEmail;
    @JsonAlias("avatar_url")
    private String avatarUrl;
    private LocalDate lastActivityOn;
    private String gpcRole;
}
