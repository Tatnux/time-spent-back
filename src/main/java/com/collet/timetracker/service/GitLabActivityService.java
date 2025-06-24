package com.collet.timetracker.service;

import com.collet.timetracker.models.api.activity.Activity;
import com.collet.timetracker.models.api.activity.ActivityIssue;
import com.collet.timetracker.service.activity.ActivityResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GitLabActivityService {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RestTemplate restTemplate;
    private final ReferenceQueryService referenceQueryService;

    @Value("${spring.security.oauth2.client.provider.gitlab.api}")
    private String gitlabApi;

    public Collection<ActivityIssue> getIssues(String userId, String inputDate) {
        final ActivityResolver activityAssembler = new ActivityResolver(referenceQueryService);

        List<Activity> activityList = this.getActivity(userId, inputDate);
        return activityAssembler.resolve(activityList);
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

}
