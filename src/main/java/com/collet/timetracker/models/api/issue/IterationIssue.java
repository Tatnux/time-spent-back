package com.collet.timetracker.models.api.issue;

import com.collet.timetracker.models.api.user.GitlabUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class IterationIssue extends Issue {
    private String closedAt;
    private List<Tags> labels;
    private List<TimeLog> timelogs;
    private int timeEstimate;

    public record TimeLog(String id, String spentAt, int timeSpent, GitlabUser user){
    }
}
