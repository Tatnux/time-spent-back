package com.collet.timetracker.service.activity;

import com.collet.timetracker.models.api.activity.Activity;
import com.collet.timetracker.models.api.activity.ActivityIssue;
import com.collet.timetracker.models.api.activity.MergeRequest;
import com.collet.timetracker.models.api.timelogs.Issue;
import com.collet.timetracker.service.ReferenceQueryService;
import com.collet.timetracker.service.activity.reference.IssueFullPathIidReference;
import com.collet.timetracker.service.activity.reference.IssueIdIidReference;
import com.collet.timetracker.service.activity.reference.MergeRequestBranchReference;
import com.collet.timetracker.service.activity.reference.MergeRequestIidReference;
import com.collet.timetracker.service.activity.reference.ReferenceCollection;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
public class ActivityResolver {
    
    private static final String ISSUE = "Issue";
    private static final String MERGE_REQUEST = "MergeRequest";
    private static final String BRANCH = "branch";
    private static final String DEVELOP = "develop";

    private final ReferenceCollection<Issue> issuesReferences = new ReferenceCollection<>();
    private final ReferenceCollection<MergeRequest> mergeRequestReferences = new ReferenceCollection<>();

    private final List<ActivityIssue> result = new ArrayList<>();

    private final ReferenceQueryService referenceQueryService;


    public List<ActivityIssue> resolve(List<Activity> activities) {
        activities.forEach(this::resolveActivity);

        this.referenceQueryService.resolveReferences(this.mergeRequestReferences, MergeRequest.class);
        this.referenceQueryService.resolveReferences(this.issuesReferences, Issue.class);

        return result;
    }

    private void resolveActivity(Activity activity) {
        // Issue Ref
        if(ISSUE.equals(activity.targetType())) {
            this.issuesReferences.add(new IssueIdIidReference(activity.projectId(), activity.targetIid()),
                    issue -> this.resolveIssue(activity, issue));
            return;
        }

        // Merge Request Ref
        if(MERGE_REQUEST.equals(activity.targetType())) {
            this.mergeRequestReferences.add(new MergeRequestIidReference(activity.projectId(), activity.targetIid()),
                    mr -> this.resolveMergeRequest(activity, mr));
            return;
        }

        // Comment on Issue
        if(activity.note() != null && ISSUE.equals(activity.note().noteableType())) {
            this.issuesReferences.add(new IssueIdIidReference(activity.note().projectId(), activity.note().noteableIid()),
                    issue -> this.resolveIssue(activity, issue));
            return;
        }

        // Comment on Merge Request
        if(activity.note() != null && MERGE_REQUEST.equals(activity.note().noteableType())) {
            this.mergeRequestReferences.add(new MergeRequestIidReference(activity.note().projectId(), activity.note().noteableIid()),
                    mr -> this.resolveMergeRequest(activity, mr));
            return;
        }

        // Push on branch
        if(activity.pushData() != null && BRANCH.equals(activity.pushData().refType()) && !DEVELOP.equals(activity.pushData().ref())){
            this.mergeRequestReferences.add(new MergeRequestBranchReference(activity.projectId(), activity.pushData().ref()),
                    mr -> this.resolveMergeRequest(activity, mr));
        }
    }

    private ActivityIssue resolveIssue(Activity activity, Issue issue) {
        final ActivityIssue activityIssue = this.result.stream()
                .filter(ai -> ai.issue().id().equals(issue.id()))
                .findFirst()
                .orElseGet(() -> {
                    ActivityIssue newIssue = new ActivityIssue();
                    newIssue.issue(issue);
                    this.result.add(newIssue);
                    return newIssue;
                });

        if(activityIssue.activities().stream().noneMatch(a -> a.id() == activity.id())) {
            activityIssue.activities().add(activity);
            return activityIssue;
        }
        return null;
    }

    private void resolveMergeRequest(Activity activity, MergeRequest mergeRequest) {

        String description = mergeRequest.description();

        // Issues URL links
        Pattern urlPattern = Pattern.compile(
                "https?://[^\\s)]+?/(?<projectPath>.+?)/-+/issues/(?<iid>\\d+)(?:\\b|/)?",
                Pattern.CASE_INSENSITIVE
        );
        Matcher urlMatcher = urlPattern.matcher(description);
        while (urlMatcher.find()) {
            String projectFullPath = urlMatcher.group("projectPath");
            long iid = Long.parseLong(urlMatcher.group("iid"));
            this.issuesReferences.add(new IssueFullPathIidReference(projectFullPath, iid),
                    issue -> this.resolveIssueAndMergeRequest(activity, issue, mergeRequest));
        }

        // #123 pattern parsing
        Pattern shortPattern = Pattern.compile("#(\\d+)");
        Matcher shortMatcher = shortPattern.matcher(description);
        while (shortMatcher.find()) {
            long iid = Long.parseLong(shortMatcher.group(1));
            this.issuesReferences.add(new IssueIdIidReference(mergeRequest.projectId(), iid),
                    issue -> this.resolveIssueAndMergeRequest(activity, issue, mergeRequest));
        }
    }

    private void resolveIssueAndMergeRequest(Activity activity, Issue issue, MergeRequest mergeRequest) {
        ActivityIssue activityIssue = this.resolveIssue(activity, issue);
        if (activityIssue != null && activityIssue.mergeRequest().stream()
                .noneMatch(mr -> mr.id().equals(mergeRequest.id()))) {
            activityIssue.mergeRequest().add(mergeRequest);
        }
    }

}
