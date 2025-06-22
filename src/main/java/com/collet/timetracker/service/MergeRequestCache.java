package com.collet.timetracker.service;

import com.collet.timetracker.models.api.activity.MergeRequest;
import com.collet.timetracker.models.api.timelogs.Issue;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MergeRequestCache {

    private static final String SEPARATOR = "§";
    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.provider.gitlab.api}")
    private String gitlabApi;

    LoadingCache<String, MergeRequest> iidToMergeRequestCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(this::getMergeRequestFromIid);

    LoadingCache<String, MergeRequest> branchToMergeRequestCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(this::getMergeRequestFromBranch);

    LoadingCache<String, Issue> idToIssueCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(this::getIssueFromId);

    private MergeRequest getMergeRequestFromIid(String key) {
        String[] split = key.split(SEPARATOR);
        String url = "%s/projects/%s/merge_requests/%s".formatted(gitlabApi, split[0], split[1]);
        try {
            MergeRequest mr = restTemplate.getForEntity(url, MergeRequest.class).getBody();
            if(mr != null) {
                this.branchToMergeRequestCache.put(mr.projectId() + SEPARATOR + mr.sourceBranch(), mr);
            }
            return mr;
        } catch (Exception e) {
            return null;
        }
    }

    private MergeRequest getMergeRequestFromBranch(String key) {
        String[] split = key.split(SEPARATOR);
        String url = "%s/projects/%s/merge_requests?source_branch=%s".formatted(gitlabApi, split[0], split[1]);
        try {
            Optional<MergeRequest> mergeRequest = Objects.requireNonNull(restTemplate.exchange(url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<List<MergeRequest>>() {
                            })
                    .getBody()).stream().findFirst();
            mergeRequest.ifPresent(mr -> {
                this.iidToMergeRequestCache.put(mr.projectId() + SEPARATOR + mr.iid(), mr);
            });
            return mergeRequest.orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Issue getIssueFromId(String key) {
        String[] split = key.split(SEPARATOR);
        String url = "%s/projects/%s/issues/%s".formatted(gitlabApi, split[0], split[1]);
        try {
            return restTemplate.getForEntity(url, Issue.class).getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public Optional<MergeRequest> mergeRequestFromIid(long projectId, long iid) {
        return Optional.ofNullable(this.iidToMergeRequestCache.get(projectId + SEPARATOR + iid));
    }

    public Optional<MergeRequest> mergeRequestFromBranch(long projectId, String branch) {
        return Optional.ofNullable(this.branchToMergeRequestCache.get(projectId + SEPARATOR + branch));
    }

    public Optional<Issue> issueFromId(long projectId, long iid) {
        return Optional.ofNullable(this.idToIssueCache.get(projectId + SEPARATOR + iid));
    }

}
