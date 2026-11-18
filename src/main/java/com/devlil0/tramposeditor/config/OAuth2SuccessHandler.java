package com.devlil0.tramposeditor.config;

import com.devlil0.tramposeditor.exception.SocialLoginFailure;
import com.devlil0.tramposeditor.exception.SocialLoginFailure.Reason;
import com.devlil0.tramposeditor.notification.NotificationProperties;
import com.devlil0.tramposeditor.service.BrowserLoginAttempt;
import com.devlil0.tramposeditor.service.SocialLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Fim do login social: resolve a conta e devolve a pessoa pro frontend com um código de
 * uso único.
 * <p>
 * O código vai na query string em vez do JWT de propósito — JWT em URL fica em log de
 * servidor, histórico do browser e cabeçalho Referer. O código vale dois minutos, serve
 * uma vez, e só o servidor do Next consegue trocá-lo por sessão.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final SocialLoginService socialLoginService;
    private final NotificationProperties properties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String provider = token.getAuthorizedClientRegistrationId();
        OAuth2User user = token.getPrincipal();
        BrowserLoginAttempt attempt = BrowserOAuthAuthorizationRequests.attemptFrom(request);

        String code;
        try {
            OidcUser google = "google".equals(provider) && user instanceof OidcUser oidc ? oidc : null;
            String verifiedEmail = verifiedEmail(provider, user, google);
            code = socialLoginService.completeLogin(
                    provider,
                    subjectOf(provider, user, google),
                    verifiedEmail,
                    displayNameOf(user),
                    google == null ? null : google.getIdToken().getClaimAsString("hd"),
                    attempt);
        } catch (SocialLoginFailure failure) {
            redirectFailure(request, response, failure.reason());
            return;
        } catch (RuntimeException e) {
            redirectFailure(request, response, Reason.FAILED);
            return;
        }
        redirect(response, "/entrar/callback?code=" + encode(code) + "&attempt=" + encode(attempt.challenge()));
    }

    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException failure) throws IOException {
        Reason reason = Reason.FAILED;
        if (failure instanceof OAuth2AuthenticationException oauthFailure) {
            reason = switch (oauthFailure.getError().getErrorCode()) {
                case "access_denied" -> Reason.CANCELLED;
                case "authorization_request_not_found", "invalid_state_parameter" -> Reason.EXPIRED;
                default -> Reason.FAILED;
            };
        }
        redirectFailure(request, response, reason);
    }

    private void redirectFailure(HttpServletRequest request, HttpServletResponse response, Reason reason) throws IOException {
        String code = reason.name().toLowerCase(Locale.ROOT);
        log.warn("Login social não concluído: {}", code);
        BrowserLoginAttempt attempt = BrowserOAuthAuthorizationRequests.attemptFrom(request);
        String path = attempt == null ? "/entrar?oauth_error=" + code
                : "/entrar/callback?oauth_error=" + code + "&attempt=" + encode(attempt.challenge());
        redirect(response, path);
    }

    private String verifiedEmail(String provider, OAuth2User user, OidcUser google) {
        if ("github".equals(provider)) {
            return user.getAttribute(GitHubEmailOAuth2UserService.VERIFIED_EMAIL_ATTRIBUTE);
        }
        return google != null && Boolean.TRUE.equals(google.getIdToken().getEmailVerified())
                ? google.getIdToken().getEmail() : null;
    }

    /**
     * O identificador estável da pessoa no provedor. Google usa {@code sub} (OIDC);
     * GitHub devolve um {@code id} numérico.
     */
    private String subjectOf(String provider, OAuth2User user, OidcUser google){
        if ("github".equals(provider)) {
            Object id = user.getAttribute("id");
            return id instanceof Number || id instanceof String ? id.toString() : null;
        }
        return google == null ? null : google.getIdToken().getSubject();
    }

    /** GitHub pode ter `name` vazio; aí o login (`login`) é o melhor que existe. */
    private String displayNameOf(OAuth2User user){
        String name = user.getAttribute("name");
        return name != null && !name.isBlank() ? name : user.getAttribute("login");
    }

    private void redirect(HttpServletResponse response, String path) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.sendRedirect(properties.link(path));
    }

    private String encode(String value){
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
