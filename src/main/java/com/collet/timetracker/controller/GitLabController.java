package com.collet.timetracker.controller;

import com.collet.timetracker.models.api.activity.ActivityIssue;
import com.collet.timetracker.models.api.iteration.IterationNode;
import com.collet.timetracker.models.api.iteration.IterationSort;
import com.collet.timetracker.models.api.iteration.IterationState;
import com.collet.timetracker.models.api.timelogs.TimeLogCreate;
import com.collet.timetracker.models.api.timelogs.TimeLogNode;
import com.collet.timetracker.models.api.user.GitlabUser;
import com.collet.timetracker.service.GitLabActivityService;
import com.collet.timetracker.service.GitLabGraphQLService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class GitLabController {

    private final GitLabGraphQLService service;
    private final GitLabActivityService activityService;

    @GetMapping("/users")
    public List<GitlabUser> getUsers() {
        return this.service.getUsers();
    }

    @GetMapping("/timespent")
    public List<TimeLogNode> getTimeSpent(@RequestParam String username, @RequestParam String startTime, @RequestParam String endTime) {
        return this.service.getTimeSpentNotes(username, startTime, endTime);
    }

    @PostMapping("/timespent/create")
    public String createTimeSpent(@RequestBody TimeLogCreate body) {
        return this.service.createTimeSpent(body.issueId(), body.timeSpent(), body.spentAt());
    }

    @GetMapping("/activity")
    public Collection<ActivityIssue> getTimeSpent(@RequestParam String userId, @RequestParam String day) {
        return this.activityService.getIssues(userId, day);
    }

    @GetMapping("/iteration/current")
    public List<IterationNode> getCurrentIteration(@RequestParam(defaultValue = "1") int first) {
        return this.service.getIteration(IterationState.current, IterationSort.CADENCE_AND_DUE_DATE_DESC, first);
    }

    @GetMapping("/iteration/next")
    public List<IterationNode> getUpComingIteration(@RequestParam(defaultValue = "1") int first) {
        return this.service.getIteration(IterationState.upcoming, IterationSort.CADENCE_AND_DUE_DATE_ASC, first);
    }

    @GetMapping("/iteration/closed")
    public List<IterationNode> getClosedIteration(@RequestParam(defaultValue = "10") int first) {
        return this.service.getIteration(IterationState.closed, IterationSort.CADENCE_AND_DUE_DATE_DESC, first);
    }
}