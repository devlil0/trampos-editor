package com.devlil0.tramposeditor.config;

import com.devlil0.tramposeditor.notification.NotificationProperties;
import com.devlil0.tramposeditor.service.SocialLoginService;
import com.devlil0.tramposeditor.service.BrowserLoginAttempt;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

class OAuth2SuccessHandlerTest {
    @Test
    void googleSoRepassaEmailVerificadoDoIdToken() throws Exception {
        var service = mock(SocialLoginService.class);
        var handler = handler(service);
        for (boolean verified : new boolean[]{false, true}) {
            handler.onAuthenticationSuccess(callback(), new MockHttpServletResponse(),
                    google(verified));
        }
        verify(service).completeLogin(eq("google"), eq("id-teste"), isNull(), isNull(), eq("example.com"), any(BrowserLoginAttempt.class));
        verify(service).completeLogin(eq("google"), eq("id-teste"), eq("pessoa@example.com"), isNull(), eq("example.com"), any(BrowserLoginAttempt.class));
    }

    @Test
    void falhaInternaUsaSomenteCodigoEMensagemDoCatalogo() throws Exception {
        var service = mock(SocialLoginService.class);
        when(service.completeLogin(eq("google"), eq("id-teste"), eq("pessoa@example.com"), isNull(), eq("example.com"), any(BrowserLoginAttempt.class)))
                .thenThrow(new IllegalStateException("detalhe-interno-ficticio"));
        var response = new MockHttpServletResponse();
        handler(service).onAuthenticationSuccess(callback(), response, google(true));
        assertThat(response.getRedirectedUrl()).contains("/entrar/callback?oauth_error=failed&attempt=")
                .doesNotContain("detalhe-interno-ficticio");
    }

    private MockHttpServletRequest callback() {
        var repository = new BrowserOAuthAuthorizationRequests();
        var start = new MockHttpServletRequest();
        start.setParameter("browser_challenge", "a".repeat(64));
        var authorization = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://oauth.example.invalid/authorize").clientId("teste")
                .state("simulado").build();
        var response = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(authorization, start, response);
        start.setParameter("state", "simulado");
        repository.removeAuthorizationRequest(start, response);
        return start;
    }

    private OAuth2SuccessHandler handler(SocialLoginService service) {
        var properties = mock(NotificationProperties.class);
        when(properties.link(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(call -> "https://frontend.example.invalid" + call.getArgument(0));
        return new OAuth2SuccessHandler(service, properties);
    }

    private OAuth2AuthenticationToken google(boolean verified) {
        var user = mock(OidcUser.class);
        var now = Instant.now();
        when(user.getIdToken()).thenReturn(new OidcIdToken("simulado", now, now.plusSeconds(60), Map.of(
                "sub", "id-teste", "email", "pessoa@example.com", "email_verified", verified, "hd", "example.com")));
        return new OAuth2AuthenticationToken(user, List.of(new SimpleGrantedAuthority("OIDC_USER")), "google");
    }
}
