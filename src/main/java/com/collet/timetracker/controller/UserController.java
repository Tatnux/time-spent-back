package com.collet.timetracker.controller;

import com.collet.timetracker.models.api.user.GitlabUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final OAuth2AuthorizedClientService clientService;
    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.provider.gitlab.api}")
    private String gitlabApi;

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