package com.collet.timetracker.service.activity;

import com.collet.timetracker.models.api.activity.Activity;
import com.collet.timetracker.models.api.activity.ActivityIssue;
import com.collet.timetracker.models.api.activity.MergeRequest;
import com.collet.timetracker.models.api.timelogs.Issue;
import com.collet.timetracker.service.graphql.ReferenceQueryService;
import com.collet.timetracker.service.activity.reference.IssueFullPathIidReference;
import com.collet.timetracker.service.activity.reference.IssueIdIidReference;
import com.collet.timetracker.service.activity.reference.MergeRequestBranchReference;
import com.collet.timetracker.service.activity.reference.MergeRequestFullPathIidReference;
import com.collet.timetracker.service.activity.reference.MergeRequestIidReference;
import com.collet.timetracker.service.activity.reference.ReferenceCollection;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
public class ActivityResolver {

    private static final String ISSUE_LINK_REGEX = "https?://[^\\s)]+?/(?<projectPath>.+?)/-+/issues/(?<iid>\\d+)(?:\\b|/)?";
    private static final String MERGE_REQUEST_LINK_REGEX = "https?://[^\\s)]+?/(?<projectPath>.+?)/-+/merge_requests/(?<iid>\\d+)(?:\\b|/)?";
    private static final String ISSUE_LINK_PATTERN_REGEX = "#(\\d+)";
    private static final String MERGE_REQUEST_LINK_PATTERN_REGEX = "!(\\d+)";

    private static final String ISSUE = "Issue";
    private static final String MERGE_REQUEST = "MergeRequest";
    private static final String BRANCH = "branch";
    private static final String DEVELOP = "develop";
    private static final int MR_DEPTH = 2;

    private final ReferenceCollection<Issue> issuesReferences = new ReferenceCollection<>();
    private final ReferenceCollection<MergeRequest> mergeRequestReferences = new ReferenceCollection<>();

    private final List<ActivityIssue> result = new ArrayList<>();

    private final ReferenceQueryService referenceQueryService;


    public List<ActivityIssue> resolve(List<Activity> activities) {
        activities.forEach(this::resolveActivity);

        for (int i = 0; i < MR_DEPTH && !this.mergeRequestReferences.getMap().isEmpty(); i++) {
            this.referenceQueryService.resolveReferences(this.mergeRequestReferences, MergeRequest.class);
        }
        this.referenceQueryService.resolveReferences(this.issuesReferences, Issue.class);

        return result;
    }

    private void resolveActivity(Activity activity) {
        // Issue Ref
        if (ISSUE.equals(activity.targetType())) {
            this.issuesReferences.add(new IssueIdIidReference(activity.projectId(), activity.targetIid()),
                    issue -> this.resolveIssue(activity, issue));
            return;
        }

        // Merge Request Ref
        if (MERGE_REQUEST.equals(activity.targetType())) {
            this.mergeRequestReferences.add(new MergeRequestIidReference(activity.projectId(), activity.targetIid()),
                    mr -> this.resolveMergeRequest(activity, mr));
            return;
        }

        // Comment on Issue
        if (activity.note() != null && ISSUE.equals(activity.note().noteableType())) {
            this.issuesReferences.add(new IssueIdIidReference(activity.note().projectId(), activity.note().noteableIid()),
                    issue -> this.resolveIssue(activity, issue));
            return;
        }

        // Comment on Merge Request
        if (activity.note() != null && MERGE_REQUEST.equals(activity.note().noteableType())) {
            this.mergeRequestReferences.add(new MergeRequestIidReference(activity.note().projectId(), activity.note().noteableIid()),
                    mr -> this.resolveMergeRequest(activity, mr));
            return;
        }

        // Push on branch
        if (activity.pushData() != null && BRANCH.equals(activity.pushData().refType()) && !DEVELOP.equals(activity.pushData().ref())) {
            this.mergeRequestReferences.add(new MergeRequestBranchReference(activity.projectId(), activity.pushData().ref()),
                    mr -> this.resolveMergeRequest(activity, mr));
        }
    }

    private ActivityIssue resolveIssue(Activity activity, Issue issue) {
        if (issue == null) {
            return null;
        }

        ActivityIssue activityIssue = this.getAndAddActivityIssue(activity, issue.id());
        if(activityIssue != null) {
            activityIssue.issue(issue);
        }

        return activityIssue;
    }

    private ActivityIssue getAndAddActivityIssue(Activity activity, String id) {
        final ActivityIssue activityIssue = this.result.stream()
                .filter(ai -> ai.id().equals(id))
                .findFirst()
                .orElseGet(() -> {
                    synchronized (this.result) {
                        ActivityIssue newIssue = new ActivityIssue();
                        newIssue.id(id);
                        this.result.add(newIssue);
                        return newIssue;
                    }
                });

        if (activityIssue.activities().stream().noneMatch(a -> a.id() == activity.id())) {
            activityIssue.activities().add(activity);
            return activityIssue;
        }
        return null;
    }

    private void resolveMergeRequest(Activity activity, MergeRequest mergeRequest) {
        this.resolveMergeRequest(activity, mergeRequest, new ArrayList<>());
    }

    private void resolveMergeRequest(Activity activity, MergeRequest mergeRequest, List<MergeRequest> mergeRequests) {
        if (mergeRequest == null) {
            this.resolveIssueAndMergeRequest(activity, null, mergeRequests);
            return;
        }
        mergeRequests.add(mergeRequest);

        String description = mergeRequest.description();
        AtomicBoolean hasMatched = new AtomicBoolean();

        // Issues Link
        extractAndHandleMatches(
                description,
                ISSUE_LINK_REGEX,
                match -> {
                    String projectFullPath = match.group("projectPath");
                    long iid = Long.parseLong(match.group("iid"));
                    issuesReferences.add(new IssueFullPathIidReference(projectFullPath, iid),
                            issue -> resolveIssueAndMergeRequest(activity, issue, mergeRequests));
                    hasMatched.set(true);
                });

        // Merge Request Link
        extractAndHandleMatches(
                description,
                MERGE_REQUEST_LINK_REGEX,
                match -> {
                    String projectFullPath = match.group("projectPath");
                    long iid = Long.parseLong(match.group("iid"));
                    mergeRequestReferences.add(new MergeRequestFullPathIidReference(projectFullPath, iid),
                            mr -> resolveMergeRequest(activity, mr, mergeRequests));
                    hasMatched.set(true);
                });

        // Issue Reference (#1)
        extractAndHandleMatches(
                description,
                ISSUE_LINK_PATTERN_REGEX,
                match -> {
                    long iid = Long.parseLong(match.group(1));
                    issuesReferences.add(new IssueIdIidReference(mergeRequest.projectId(), iid),
                            issue -> resolveIssueAndMergeRequest(activity, issue, mergeRequests));
                    hasMatched.set(true);
                });

        // Merge Request Reference (!5)
        extractAndHandleMatches(
                description,
                MERGE_REQUEST_LINK_PATTERN_REGEX,
                match -> {
                    long iid = Long.parseLong(match.group(1));
                    mergeRequestReferences.add(new MergeRequestIidReference(mergeRequest.projectId(), iid),
                            mr -> resolveMergeRequest(activity, mr, mergeRequests));
                    hasMatched.set(true);
                });

        if(!hasMatched.get()) {
            this.resolveIssueAndMergeRequest(activity, null, mergeRequests);
        }
    }

    private void extractAndHandleMatches(String text, String regex, Consumer<Matcher> matchHandler) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            matchHandler.accept(matcher);
        }
    }

    private void resolveIssueAndMergeRequest(Activity activity, Issue issue, List<MergeRequest> mergeRequests) {
        mergeRequests.forEach(mergeRequest -> {
            ActivityIssue activityIssue = issue == null ? this.getAndAddActivityIssue(activity, mergeRequest.id()) : this.resolveIssue(activity, issue);
            if (activityIssue != null && activityIssue.mergeRequest().stream()
                    .noneMatch(mr -> mr.id().equals(mergeRequest.id()))) {
                activityIssue.mergeRequest().add(mergeRequest);
            }
        });
    }

}
