package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.repository.IEditorProfileRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EditorDirectoryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IEditorProfileRepository editorProfileRepository;

    @BeforeEach
    void clean(){
        editorProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void editorPublicaPerfilComPortfolioEApareceNaVitrine() throws Exception {
        String token = registrarEditor("joao@teste.com", "João Silva");

        upsertPerfil(token, """
                {
                  "headline": "Cortes de Reels que retêm",
                  "bio": "5 anos editando short-form.",
                  "niches": ["REELS", "TIKTOK"],
                  "tools": ["PREMIERE", "CAPCUT"],
                  "priceMinCents": 8000,
                  "priceMaxCents": 40000,
                  "turnaroundDays": 2,
                  "available": true
                }
                """);

        String perfilBody = mockMvc.perform(post("/v1/me/editor-profile/portfolio")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Reel de lançamento", "videoUrl": "https://youtu.be/abc123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.portfolio[0].provider").value("YOUTUBE"))
                .andReturn().getResponse().getContentAsString();

        String slug = JsonPath.read(perfilBody, "$.slug");

        // Vitrine pública (sem token) mostra o editor recém-publicado.
        mockMvc.perform(get("/v1/editors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].displayName").value("João Silva"))
                .andExpect(jsonPath("$.content[0].coverVideoUrl").value("https://youtu.be/abc123"));

        // Detalhe público por slug.
        mockMvc.perform(get("/v1/editors/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Cortes de Reels que retêm"))
                .andExpect(jsonPath("$.portfolio.length()").value(1));
    }

    @Test
    void filtroPorNichoRespeitaAsTags() throws Exception {
        String token = registrarEditor("ana@teste.com", "Ana");
        upsertPerfil(token, """
                {"niches": ["YOUTUBE"], "tools": ["DAVINCI_RESOLVE"]}
                """);

        mockMvc.perform(get("/v1/editors").param("niche", "REELS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/v1/editors").param("niche", "YOUTUBE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void clienteViraEditorAoCriarOPerfil() throws Exception {
        // A conta é uma só: quem entrou pra contratar se torna editor no ato de
        // publicar o perfil, sem cadastro separado.
        String token = registrar("cliente@teste.com", "Cliente", "CLIENT");

        mockMvc.perform(get("/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").value("CLIENT"));

        upsertPerfil(token, """
                {"niches": ["REELS", "TIKTOK"], "tools": ["PREMIERE"]}
                """);

        mockMvc.perform(get("/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItems("CLIENT", "EDITOR")));

        // E já aparece na vitrine pública.
        mockMvc.perform(get("/v1/editors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void portfolioSoAceitaYoutubeOuVimeo() throws Exception {
        String token = registrarEditor("bug@teste.com", "Bug");
        upsertPerfil(token, """
                {"niches": ["REELS", "TIKTOK"], "tools": ["PREMIERE"]}
                """);

        mockMvc.perform(post("/v1/me/editor-profile/portfolio")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Link solto", "videoUrl": "https://exemplo.com/video.mp4"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void vitrineExigeSlugValido() throws Exception {
        mockMvc.perform(get("/v1/editors/nao-existe"))
                .andExpect(status().isNotFound());
    }

    // ---------- Helpers ----------

    private String registrarEditor(String email, String displayName) throws Exception {
        return registrar(email, displayName, "EDITOR");
    }

    private String registrar(String email, String displayName, String role) throws Exception {
        String body = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "senhaforte123",
                                  "displayName": "%s",
                                  "roles": ["%s"]
                                }
                                """.formatted(email, displayName, role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.accessToken");
    }

    private void upsertPerfil(String token, String json) throws Exception {
        mockMvc.perform(put("/v1/me/editor-profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }
}
