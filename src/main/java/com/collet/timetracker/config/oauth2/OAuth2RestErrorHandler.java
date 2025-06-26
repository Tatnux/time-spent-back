package com.collet.timetracker.config.oauth2;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;

public class OAuth2RestErrorHandler extends DefaultResponseErrorHandler {

    @Override
    public void handleError(URI uri, HttpMethod method, ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();

        // Propager directement certaines erreurs au frontend
        if (status == HttpStatus.UNAUTHORIZED) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "GitLab: Unauthorized access (invalid or expired token)"
            );
        } else if (status == HttpStatus.FORBIDDEN) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "GitLab: Forbidden access"
            );
        }

        super.handleError(uri, method, response);
    }

}
