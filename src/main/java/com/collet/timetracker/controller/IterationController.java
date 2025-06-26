package com.collet.timetracker.controller;

import com.collet.timetracker.models.api.iteration.IterationNode;
import com.collet.timetracker.models.api.iteration.IterationSort;
import com.collet.timetracker.models.api.iteration.IterationState;
import com.collet.timetracker.service.IterationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/iteration")
public class IterationController {

    private final IterationService service;

    @GetMapping("/current")
    public List<IterationNode> getCurrentIteration(@RequestParam(defaultValue = "1") int first) {
        return this.service.getIteration(IterationState.current, IterationSort.CADENCE_AND_DUE_DATE_DESC, first);
    }

    @GetMapping("/next")
    public List<IterationNode> getUpComingIteration(@RequestParam(defaultValue = "1") int first) {
        return this.service.getIteration(IterationState.upcoming, IterationSort.CADENCE_AND_DUE_DATE_ASC, first);
    }

    @GetMapping("/closed")
    public List<IterationNode> getClosedIteration(@RequestParam(defaultValue = "10") int first) {
        return this.service.getIteration(IterationState.closed, IterationSort.CADENCE_AND_DUE_DATE_DESC, first);
    }

}
