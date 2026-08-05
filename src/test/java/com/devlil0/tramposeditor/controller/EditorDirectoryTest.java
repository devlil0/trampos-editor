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
    void filtroPorEstiloDeEdicaoRespeitaAsTags() throws Exception {
        String token = registrarEditor("dark@teste.com", "Editor Dark");
        upsertPerfil(token, """
                {"niches": ["YOUTUBE"], "tools": ["PREMIERE"], "styles": ["CLEAN", "STORYTELLING"]}
                """);

        mockMvc.perform(get("/v1/editors").param("style", "CLEAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].styles", hasItems("CLEAN", "STORYTELLING")));

        mockMvc.perform(get("/v1/editors").param("style", "MEME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void perfilSemEstiloContinuaValido() throws Exception {
        // styles é opcional — perfil criado antes da dimensão existir não pode quebrar.
        String token = registrarEditor("semestilo@teste.com", "Sem Estilo");
        upsertPerfil(token, """
                {"niches": ["REELS"], "tools": ["CAPCUT"]}
                """);

        mockMvc.perform(get("/v1/editors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].styles.length()").value(0));
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

    @Test
    void disponivelERecorteDaVitrineENaoOutraSuperficie() throws Exception {
        String livre = registrarEditor("livre@teste.com", "Editor Livre");
        upsertPerfil(livre, """
                {"niches": ["REELS"], "tools": ["PREMIERE"], "available": true}
                """);

        String ocupado = registrarEditor("ocupado@teste.com", "Editor Ocupado");
        upsertPerfil(ocupado, """
                {"niches": ["REELS"], "tools": ["PREMIERE"], "available": false}
                """);

        // O `/v1/feed/editors` saiu em 2026-08-04: mostrava esta mesma lista com um filtro
        // a menos. "Quem está livre agora" é valor de filtro, não superfície.
        mockMvc.perform(get("/v1/editors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // Sem login, como o resto da vitrine: o recorte não é área logada.
        mockMvc.perform(get("/v1/editors").param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].displayName").value("Editor Livre"))
                .andExpect(jsonPath("$.content[0].available").value(true));

        // E combina com os outros filtros — o que o feed antigo não fazia.
        mockMvc.perform(get("/v1/editors").param("available", "true").param("niche", "PODCAST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
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
                                  "username": "u-%s",
                                  "roles": ["%s"]
                                }
                                """.formatted(email, displayName, email.split("@")[0], role)))
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
