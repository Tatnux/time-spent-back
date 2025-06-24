package com.collet.timetracker.models.api.activity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ReferencedIssueUrl(String projectFullName, long iid) {

    private static final Pattern ISSUE_URL_PATTERN = Pattern.compile("^https?://[^/]+/(?<projectPath>.+?)/-+/issues/(?<iid>\\\\d+)(?:/.*)?$");

    public static ReferencedIssueUrl parse(String url) {
        Matcher matcher = ISSUE_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("URL d’issue GitLab non reconnue : " + url);
        }

        String projectFullName = matcher.group("projectPath");
        long iid = Long.parseLong(matcher.group("iid"));

        return new ReferencedIssueUrl(projectFullName, iid);
    }

}
