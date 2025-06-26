package com.collet.timetracker.service;

import com.collet.timetracker.models.api.user.GitlabUser;
import com.collet.timetracker.models.api.user.GroupMemeber;
import com.collet.timetracker.service.graphql.GitLabGraphQLService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UsersService {

    private static final String USERS = GitLabGraphQLService.loadGraphQLQuery("users");

    private final GitLabGraphQLService graphQLService;

    public List<GitlabUser> getUsers() {
        final LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        return this.graphQLService.graphQLQuery(GroupMemeber.class, USERS, "groupMembers", this.graphQLService.getGroupMap(Map.of())).stream()
                .map(GroupMemeber::user)
                .filter(gitlabUser -> gitlabUser.lastActivityOn() != null && gitlabUser.lastActivityOn().isAfter(sixMonthsAgo))
                .toList();
    }

}
