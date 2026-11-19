package com.devlil0.tramposeditor.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthLimitProperties.class)
public class AuthLimitsConfiguration {
    @Bean
    AuthClientOrigin authClientOrigin(AuthLimitProperties limits, Environment environment) {
        var binder = Binder.get(environment);
        if (!"none".equalsIgnoreCase(binder.bind("server.forward-headers-strategy", String.class).orElse(""))
                || hasText(binder, "server.tomcat.remoteip.remote-ip-header")
                || hasText(binder, "server.tomcat.remoteip.protocol-header")) {
            throw new IllegalStateException("A origem da autenticação exige forward-headers-strategy=none "
                    + "e reescrita genérica de IP desativada no Tomcat");
        }
        return new AuthClientOrigin(limits.trustedProxies());
    }

    @Bean
    Clock authRateLimitClock() {
        return Clock.systemUTC();
    }

    private boolean hasText(Binder binder, String property) {
        return !binder.bind(property, String.class).orElse("").isBlank();
    }
}
