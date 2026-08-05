package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.repository.IEditorProfileRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O N+1 da vitrine (ESBOCO §7 item 7).
 * <p>
 * Contar consulta em teste parece exagero até lembrar que a vitrine é a página indexada:
 * é ela que o Google visita e é nela que a latência custa cliente. Um teste que só
 * verificasse o JSON passaria feliz com 80 consultas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class VitrineQueryCountTest {

    /** Página de 10 editores: 1 pela página + coleções em lote + user. Sem batch daria ~40. */
    private static final int TETO_DE_CONSULTAS = 12;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private IEditorProfileRepository editorProfileRepository;

    @Autowired
    private IUserRepository userRepository;

    @BeforeEach
    void limpar(){
        editorProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void custoDaVitrineNaoCresceComONumeroDeEditores() throws Exception {
        for (int i = 0; i < 10; i++) {
            publicarEditor("editor" + i + "@teste.com", "Editor " + i);
        }

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        mockMvc.perform(get("/v1/editors?size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                // As coleções precisam vir preenchidas: um teto de consultas alcançado
                // por devolver campo vazio não valeria nada.
                .andExpect(jsonPath("$.content[0].niches.length()").value(2))
                .andExpect(jsonPath("$.content[0].tools.length()").value(2));

        assertThat(stats.getPrepareStatementCount())
                .as("vitrine de 10 editores em no máximo %d consultas", TETO_DE_CONSULTAS)
                .isLessThanOrEqualTo(TETO_DE_CONSULTAS);
    }

    private void publicarEditor(String email, String displayName) throws Exception {
        String body = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "senhaforte123",
                                  "displayName": "%s",
                                  "username": "u-%s",
                                  "roles": ["EDITOR"]
                                }
                                """.formatted(email, displayName, email.split("@")[0])))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(body, "$.accessToken");

        mockMvc.perform(put("/v1/me/editor-profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "headline": "Cortes que retêm",
                                  "niches": ["REELS", "TIKTOK"],
                                  "tools": ["PREMIERE", "CAPCUT"],
                                  "styles": ["DINAMICA"],
                                  "available": true
                                }
                                """))
                .andExpect(status().isOk());
    }
}
