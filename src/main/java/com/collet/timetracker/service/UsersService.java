package com.collet.timetracker.service;

import com.collet.timetracker.models.api.user.GitlabUser;
import com.collet.timetracker.models.api.user.GroupMemeber;
import com.collet.timetracker.service.graphql.GitLabGraphQLService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.Yaml;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class UsersService {

    private static final Logger LOG = Logger.getLogger(UsersService.class.getName());

    private static final String GPC_MEMBERS_CONF = "/projects/27222/repository/files/members.yml/raw?ref=master";
    private static final String USERS = GitLabGraphQLService.loadGraphQLQuery("Users");

    @Value("${spring.security.oauth2.client.provider.gitlab.api}")
    private String gitlabApi;

    private final RestTemplate restTemplate;
    private final GitLabGraphQLService graphQLService;

    private Map<String, String> membersRoles = new HashMap<>();

    public List<GitlabUser> getUsers() {
        if(membersRoles.isEmpty()) {
            this.readGpcMembers();
        }

        final LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        return this.graphQLService.graphQLQuery(GroupMemeber.class, USERS, "groupMembers", this.graphQLService.getGroupMap(Map.of())).stream()
                .map(GroupMemeber::user)
                .filter(gitlabUser -> gitlabUser.lastActivityOn() != null && gitlabUser.lastActivityOn().isAfter(sixMonthsAgo) && !gitlabUser.username().contains(".cicd"))
                .filter(gitlabUser -> membersRoles.isEmpty() || membersRoles.containsKey(gitlabUser.username()))
                .map(this::assignGpcRole)
                .toList();
    }

    private GitlabUser assignGpcRole(GitlabUser user) {
        user.gpcRole(this.membersRoles.get(user.username()));
        return user;
    }

    private void readGpcMembers() {
        try {
            final Yaml yaml = new Yaml();
            final List<String> profiles = Arrays.asList("gael_owners", "gael_maintainers", "gael_developers", "gael_support");

            ResponseEntity<String> response = this.restTemplate.getForEntity(this.gitlabApi + GPC_MEMBERS_CONF, String.class);
            GpcConfig gpcConfig = yaml.loadAs(response.getBody(), GpcConfig.class);

            gpcConfig.member_profiles.stream()
                    .filter(profile -> profiles.contains(profile.name))
                    .forEach(profile ->
                            profile.members.forEach(user -> this.membersRoles.put(user, profile.name)));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e, () -> "Cannot load GPC config");
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class GpcConfig {
        private List<MemberProfiles> member_profiles;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class MemberProfiles {
        private String name;
        private String role;
        private List<String> members;
    }

}
