package com.devlil0.tramposeditor;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Postgres real nos testes — o mesmo da produção.
 * Com ddl-auto=validate, qualquer divergência entre as migrations e as entidades
 * quebra o contexto aqui em vez de quebrar no deploy.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer(){
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
    }
}
