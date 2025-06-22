package com.collet.timetracker.service;

import com.collet.timetracker.models.api.activity.Activity;
import com.collet.timetracker.models.api.activity.ActivityIssue;
import com.collet.timetracker.models.api.activity.MergeRequest;
import com.collet.timetracker.models.api.activity.ReferencedIssue;
import com.collet.timetracker.models.api.activity.ReferencedMergeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GitLabActivityService {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RestTemplate restTemplate;
    private final MergeRequestCache mergeRequestCache;

    @Value("${spring.security.oauth2.client.provider.gitlab.api}")
    private String gitlabApi;

    public Collection<ActivityIssue> getIssues(String userId, String inputDate) {
        final Map<ReferencedIssue, ActivityIssue> issueActivityMap = new HashMap<>();

        List<Activity> activityList = this.getActivity(userId, inputDate);
        activityList.forEach(activity ->
                this.findIssueForActivity(activity).ifPresent(referencedMergeRequest ->
                {
                    ActivityIssue activityIssue = issueActivityMap.computeIfAbsent(referencedMergeRequest.issue(),
                            _ -> new ActivityIssue());
                    activityIssue.activities().add(activity);
                    if(activityIssue.mergeRequest() == null) {
                        activityIssue.mergeRequest(referencedMergeRequest.mergeRequest());
                    }
                }));

        issueActivityMap.forEach((referencedIssue, activities) ->
                this.mergeRequestCache.issueFromId(referencedIssue.projectId(), referencedIssue.iid())
                        .ifPresent(activities::issue));

        return issueActivityMap.values();
    }

    public List<Activity> getActivity(String userId, String inputDate) {
        LocalDate date = LocalDate.parse(inputDate, formatter);

        LocalDate after = date.minusDays(1);
        LocalDate before = date.plusDays(1);

        String url = "%s/users/%s/events?sort=asc&per_page=100&after=%s&before=%s".formatted(this.gitlabApi, userId, formatter.format(after), formatter.format(before));
        ResponseEntity<List<Activity>> exchange = this.restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
        });
        return exchange.getBody();
    }

    private Optional<ReferencedMergeRequest> findIssueForActivity(Activity activity) {
        if("Issue".equals(activity.targetType())) {
            return Optional.of(new ReferencedMergeRequest(new ReferencedIssue(activity.projectId(), activity.targetIid()), null));
        }

        if(activity.note() != null && "Issue".equals(activity.note().noteableType())) {
            return Optional.of(new ReferencedMergeRequest(new ReferencedIssue(activity.note().projectId(), activity.note().noteableIid()), null));
        }

        if("MergeRequest".equals(activity.targetType())) {
            return Optional.of(activity.targetIid())
                    .flatMap(iid -> this.mergeRequestCache.mergeRequestFromIid(activity.projectId(), iid))
                    .flatMap(mergeRequest -> findIssueForMergeRequest(mergeRequest).map(issue -> new ReferencedMergeRequest(issue, mergeRequest)));
        }

        if(activity.note() != null && "MergeRequest".equals(activity.note().noteableType())) {
            return Optional.of(activity.note().noteableIid())
                    .flatMap(iid -> this.mergeRequestCache.mergeRequestFromIid(activity.note().projectId(), iid))
                    .flatMap(mergeRequest -> findIssueForMergeRequest(mergeRequest).map(issue -> new ReferencedMergeRequest(issue, mergeRequest)));
        }

        if(activity.pushData() != null && "branch".equals(activity.pushData().refType()) && !"develop".equals(activity.pushData().ref())){
            return Optional.of(activity.projectId())
                    .flatMap(projectId -> this.mergeRequestCache.mergeRequestFromBranch(projectId, activity.pushData().ref()))
                    .flatMap(mergeRequest -> findIssueForMergeRequest(mergeRequest).map(issue -> new ReferencedMergeRequest(issue, mergeRequest)));
        }

        return Optional.empty();
    }


    private Optional<ReferencedIssue> findIssueForMergeRequest(MergeRequest mergeRequest) {
        Pattern shortPattern = Pattern.compile("(?:closes|link to issue|to close issue)\\s+#(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher shortMatcher = shortPattern.matcher(mergeRequest.description());
        if (shortMatcher.find()) {
            int issueNumber = Integer.parseInt(shortMatcher.group(1));
            return Optional.of(new ReferencedIssue(mergeRequest.projectId(), issueNumber));
        }

        return Optional.empty();
    }

}
