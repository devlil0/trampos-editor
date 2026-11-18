package com.devlil0.tramposeditor.config;

import com.devlil0.tramposeditor.service.BrowserLoginAttempt;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.time.LocalDateTime;

final class BrowserOAuthAuthorizationRequests implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private static final String ATTEMPT = BrowserOAuthAuthorizationRequests.class.getName() + ".attempt";
    private static final String EXPIRES_AT = ATTEMPT + ".expiresAt";
    private final HttpSessionOAuth2AuthorizationRequestRepository delegate =
            new HttpSessionOAuth2AuthorizationRequestRepository();

    static BrowserLoginAttempt attemptFrom(HttpServletRequest request) {
        return (BrowserLoginAttempt) request.getAttribute(ATTEMPT);
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return delegate.loadAuthorizationRequest(request);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorization,
                                         HttpServletRequest request, HttpServletResponse response) {
        String challenge = request.getParameter("browser_challenge");
        if (authorization != null && challenge != null && challenge.matches(BrowserLoginAttempt.PROOF_FORMAT)) {
            authorization = OAuth2AuthorizationRequest.from(authorization)
                    .attributes(attributes -> {
                        attributes.put(ATTEMPT, challenge);
                        attributes.put(EXPIRES_AT, LocalDateTime.now().plusMinutes(5));
                    }).build();
        }
        delegate.saveAuthorizationRequest(authorization, request, response);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                HttpServletResponse response) {
        OAuth2AuthorizationRequest authorization = delegate.removeAuthorizationRequest(request, response);
        if (authorization != null && authorization.getAttribute(ATTEMPT) instanceof String challenge) {
            request.setAttribute(ATTEMPT, new BrowserLoginAttempt(challenge, authorization.getAttribute(EXPIRES_AT)));
        }
        return authorization;
    }
}
