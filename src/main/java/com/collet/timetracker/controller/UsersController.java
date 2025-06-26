package com.collet.timetracker.controller;

import com.collet.timetracker.models.api.user.GitlabUser;
import com.collet.timetracker.service.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UsersController {

    private final UsersService service;
    private final OAuth2AuthorizedClientService clientService;

    @GetMapping
    public List<GitlabUser> getUsers() {
        return this.service.getUsers();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getGitlabUserInfo(OAuth2AuthenticationToken auth) {
        return ResponseEntity.ok(Map.of(
                "id", Objects.requireNonNull(auth.getPrincipal().getAttribute("id")),
                "username", Objects.requireNonNull(auth.getPrincipal().getAttribute("username"))
        ));
    }

    @GetMapping("/token")
    public String getToken(OAuth2AuthenticationToken auth) {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                auth.getAuthorizedClientRegistrationId(),
                auth.getName()
        );

        return client.getAccessToken().getTokenValue();
    }

}
