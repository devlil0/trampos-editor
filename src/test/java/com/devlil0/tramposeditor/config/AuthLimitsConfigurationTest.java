package com.devlil0.tramposeditor.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthLimitsConfigurationTest {
    @Test
    void rejeitaConfiguracoesQueReescrevemIpAntesDaValidacao() {
        var policy = new AuthLimitProperties.Policy(1, 1, Duration.ofMinutes(1), Duration.ZERO);
        var properties = new AuthLimitProperties(10, List.of("192.0.2.1"), policy, policy, policy, policy,
                new AuthLimitProperties.OriginPolicy(1, Duration.ofMinutes(1)));
        var configuration = new AuthLimitsConfiguration();
        for (String strategy : List.of("native", "framework")) {
            var environment = new MockEnvironment().withProperty("server.forward-headers-strategy", strategy);
            assertThatThrownBy(() -> configuration.authClientOrigin(properties, environment))
                    .isInstanceOf(IllegalStateException.class);
        }
        for (String property : List.of("remote-ip-header", "protocol-header")) {
            var environment = new MockEnvironment().withProperty("server.forward-headers-strategy", "none")
                    .withProperty("server.tomcat.remoteip." + property, "X-Forwarded-For");
            assertThatThrownBy(() -> configuration.authClientOrigin(properties, environment))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void proxyAceitaApenasUmIpLiteralEmFormatoCanonico() {
        var resolver = new AuthClientOrigin(List.of("192.0.2.1"));
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.1");
        request.addHeader(AuthClientOrigin.CLIENT_IP_HEADER, "::ffff:192.0.2.2");
        assertThat(resolver.resolve(request)).isEqualTo("192.0.2.2");
        request.addHeader(AuthClientOrigin.CLIENT_IP_HEADER, "192.0.2.3");
        assertThat(resolver.resolve(request)).isEqualTo("192.0.2.1");
        request.removeHeader(AuthClientOrigin.CLIENT_IP_HEADER);
        for (String invalid : List.of("192.0.2.2, 192.0.2.3", "localhost", "[::1]:8765", "192.0.02.1")) {
            request.addHeader(AuthClientOrigin.CLIENT_IP_HEADER, invalid);
            assertThat(resolver.resolve(request)).isEqualTo("192.0.2.1");
            request.removeHeader(AuthClientOrigin.CLIENT_IP_HEADER);
        }
    }
}
