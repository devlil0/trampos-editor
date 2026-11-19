package com.devlil0.tramposeditor.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubEmailOAuth2UserServiceTest {
    @Test
    void emailPublicoNaoSubstituiProvaEPreferePrincipalVerificado() {
        assertThat(verifiedEmail(withSuccess("""
                [
                  {"email":"publico@example.com","verified":false,"primary":false},
                  {"email":"secundario@example.com","verified":true,"primary":false},
                  {"email":"principal@example.com","verified":true,"primary":true}
                ]
                """, MediaType.APPLICATION_JSON))).isEqualTo("principal@example.com");
    }

    @Test
    void emailPublicoNaoViraProvaQuandoInvalidoOuConsultaFalha() {
        assertThat(verifiedEmail(withSuccess("""
                [{"email":"publico@example.com","verified":false,"primary":true}]
                """, MediaType.APPLICATION_JSON))).isNull();
        assertThat(verifiedEmail(withStatus(HttpStatus.FORBIDDEN))).isNull();
    }

    private String verifiedEmail(ResponseCreator reply) {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.github.com/user/emails")).andRespond(reply);
        var attributes = Map.<String, Object>of("id", 42, "email", "publico@example.com",
                GitHubEmailOAuth2UserService.VERIFIED_EMAIL_ATTRIBUTE, "atributo-nao-confiavel");
        var principal = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")), attributes, "id");
        var registration = ClientRegistration.withRegistrationId("github")
                .clientId("teste").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://oauth.example.invalid/authorize")
                .tokenUri("https://oauth.example.invalid/token")
                .userInfoUri("https://oauth.example.invalid/user").userNameAttributeName("id").build();
        var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "simulado", null, null);
        var service = new GitHubEmailOAuth2UserService(request -> principal, builder.build());

        var user = service.loadUser(new OAuth2UserRequest(registration, token));
        server.verify();
        assertThat(user.getName()).isEqualTo("42");
        return user.getAttribute(GitHubEmailOAuth2UserService.VERIFIED_EMAIL_ATTRIBUTE);
    }
}
