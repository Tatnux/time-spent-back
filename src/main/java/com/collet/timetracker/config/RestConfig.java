package com.collet.timetracker.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class RestConfig {

    private final OAuth2AuthorizedClientService authorizedClientService;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .interceptors((request, body, execution) -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                    if (auth instanceof OAuth2AuthenticationToken oauth2Auth) {
                        String registrationId = oauth2Auth.getAuthorizedClientRegistrationId();
                        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(registrationId, oauth2Auth.getName());

                        if (client != null) {
                            request.getHeaders().setBearerAuth(client.getAccessToken().getTokenValue());
                            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        }
                    }

                    return execution.execute(request, body);
                })
                .errorHandler(new RestErrorHandler())
                .build();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer forceFieldAccess() {
        return builder -> builder
                .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

}
