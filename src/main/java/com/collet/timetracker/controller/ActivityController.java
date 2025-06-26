package com.collet.timetracker.controller;

import com.collet.timetracker.models.api.activity.ActivityIssue;
import com.collet.timetracker.service.activity.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityService service;

    @GetMapping
    public Collection<ActivityIssue> getTimeSpent(@RequestParam String userId, @RequestParam String day) {
        return this.service.getActivityIssues(userId, day);
    }

}
