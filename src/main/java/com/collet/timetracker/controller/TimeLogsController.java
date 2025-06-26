package com.collet.timetracker.controller;

import com.collet.timetracker.models.api.timelogs.TimeLogCreate;
import com.collet.timetracker.models.api.timelogs.TimeLogNode;
import com.collet.timetracker.service.TimeLogsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timespent")
public class TimeLogsController {

    private final TimeLogsService service;

    @GetMapping
    public List<TimeLogNode> getTimeSpent(@RequestParam String username, @RequestParam String startTime, @RequestParam String endTime) {
        return this.service.getTimeSpentNotes(username, startTime, endTime);
    }

    @PostMapping("/create")
    public String createTimeSpent(@RequestBody TimeLogCreate body) {
        return this.service.createTimeSpent(body.issueId(), body.timeSpent(), body.spentAt());
    }
}
