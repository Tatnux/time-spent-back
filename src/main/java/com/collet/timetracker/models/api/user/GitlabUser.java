package com.collet.timetracker.models.api.user;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.LocalDate;

public record GitlabUser(String id, String name, String username, @JsonAlias("public_email") String publicEmail, @JsonAlias("avatar_url") String avatarUrl, LocalDate lastActivityOn) {
}
