package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, OAuthApiIsolationTest.FakeProvider.class})
class OAuthApiIsolationTest {
    private static final String CHALLENGE = "a".repeat(64);
    @Autowired
    private MockMvc mockMvc;

    @Test
    void inicioOAuthGuardaEstadoNaSessaoSemContatarProvedor() throws Exception {
        var result = mockMvc.perform(get("/oauth2/authorization/github").param("browser_challenge", CHALLENGE))
                .andExpect(status().isFound())
                .andReturn();
        var location = UriComponentsBuilder.fromUriString(result.getResponse().getRedirectedUrl()).build();
        assertThat(location.getHost()).isEqualTo("oauth.example.invalid");
        assertThat(location.getQueryParams()).doesNotContainKey("browser_challenge");
        String state = location.getQueryParams().getFirst("state");
        assertThat(state).isNotBlank();

        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        var callback = get("/login/oauth2/code/github")
                .param("state", UriUtils.decode(state, StandardCharsets.UTF_8))
                .session(session).buildRequest(result.getRequest().getServletContext());
        var repository = new HttpSessionOAuth2AuthorizationRequestRepository();
        assertThat(repository.loadAuthorizationRequest(callback)).isNotNull();
    }

    @Test
    void sessaoOAuthNaoAutenticaRequisicaoDaApiSemBearer() throws Exception {
        var authorities = List.of(new SimpleGrantedAuthority("OAUTH2_USER"));
        var principal = new DefaultOAuth2User(authorities, Map.of("id", "fake-user"), "id");
        var authentication = new OAuth2AuthenticationToken(principal, authorities, "github");
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));

        mockMvc.perform(get("/v1/users/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }

    @Test
    void cancelamentoNoProvedorVoltaAoFrontendSemDetalheDaExcecao() throws Exception {
        var start = mockMvc.perform(get("/oauth2/authorization/github").param("browser_challenge", CHALLENGE))
                .andReturn();
        var redirect = UriComponentsBuilder.fromUriString(start.getResponse().getRedirectedUrl()).build();
        String state = UriUtils.decode(redirect.getQueryParams().getFirst("state"), StandardCharsets.UTF_8);
        var session = (MockHttpSession) start.getRequest().getSession(false);

        var response = mockMvc.perform(get("/login/oauth2/code/github").session(session)
                        .param("state", state).param("error", "access_denied")
                        .param("error_description", "detalhe-interno-ficticio"))
                .andExpect(status().isFound()).andReturn().getResponse();
        assertThat(response.getRedirectedUrl()).contains("/entrar/callback?oauth_error=cancelled&attempt=" + CHALLENGE)
                .doesNotContain("detalhe-interno-ficticio");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeProvider {
        @Bean
        ClientRegistrationRepository testClientRegistrations() {
            var registration = ClientRegistration.withRegistrationId("github")
                    .clientId("test-client")
                    .clientSecret("test-only")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .authorizationUri("https://oauth.example.invalid/authorize")
                    .tokenUri("https://oauth.example.invalid/token")
                    .userInfoUri("https://oauth.example.invalid/user")
                    .userNameAttributeName("id")
                    .clientName("GitHub de teste")
                    .build();
            return new InMemoryClientRegistrationRepository(registration);
        }
    }
}
