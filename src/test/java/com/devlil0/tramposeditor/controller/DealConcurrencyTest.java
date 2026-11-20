package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.repository.*;
import com.devlil0.tramposeditor.dto.RatingAggregateDTO;
import com.devlil0.tramposeditor.notification.NotificationService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Requisições concorrentes, transações independentes e PostgreSQL real. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DealConcurrencyTest {
    @Autowired private MockMvc mvc;
    @MockitoSpyBean private IDealRepository deals;
    @MockitoSpyBean private IReviewRepository reviews;
    @MockitoSpyBean private NotificationService notifications;
    @Autowired private IConversationRepository conversations;
    @Autowired private IJobRequestRepository jobs;
    @Autowired private IUserRepository users;
    @Autowired private IEditorProfileRepository profiles;

    private Account client;
    private Account editor;
    private String conversation;

    @BeforeEach
    void fixture() throws Exception {
        deals.deleteAll();
        conversations.deleteAll();
        jobs.deleteAll();
        users.deleteAll();
        client = account("cliente");
        editor = account("editor");
        conversation = conversation(client, editor);
    }

    @Test
    void aberturaSimultaneaMantemUmOpenERetornaConflito() throws Exception {
        var gate = new CountDownLatch(2);
        var delegate = mockingDetails(deals).getMockCreationSettings().getDefaultAnswer();
        doAnswer(inv -> {
            Object result = delegate.answer(inv);
            rendezvous(gate);
            return result;
        }).when(deals).findByConversationIdAndStatus(eq(UUID.fromString(conversation)), any());

        assertThat(together(
                () -> postStatus("/from-conversation/" + conversation, client, "{\"title\":\"Primeiro\"}"),
                () -> postStatus("/from-conversation/" + conversation, client, "{\"title\":\"Segundo\"}")))
                .containsExactlyInAnyOrder(201, 409);
        assertThat(deals.count()).isEqualTo(1);
        assertThat(deals.findAll().getFirst().getStatus().name()).isEqualTo("OPEN");
    }

    @RepeatedTest(3)
    void concluirECancelarDisputamUmaUnicaTransicaoTerminal() throws Exception {
        String deal = open(client, conversation);
        alignOldDealReads(deal);
        clearInvocations(notifications);
        List<Integer> results = together(
                () -> postStatus("/" + deal + "/done", client, null),
                () -> postStatus("/" + deal + "/cancel", client, null));
        assertThat(results).containsExactlyInAnyOrder(200, 409);
        var persisted = deals.findById(UUID.fromString(deal)).orElseThrow();
        assertThat(persisted.getStatus().name()).isEqualTo(results.get(0) == 200 ? "DONE" : "CANCELLED");
        assertThat(persisted.getClosedAt()).isNotNull();
        verify(notifications, times(results.get(0) == 200 ? 2 : 0)).dealClosed(any(), any(), anyString());
    }

    @Test
    void duasConclusoesNaoRepetemAvisos() throws Exception {
        String deal = open(client, conversation);
        alignOldDealReads(deal);
        clearInvocations(notifications);
        assertThat(together(
                () -> postStatus("/" + deal + "/done", client, null),
                () -> postStatus("/" + deal + "/done", client, null)))
                .containsExactlyInAnyOrder(200, 409);
        verify(notifications, times(2)).dealClosed(any(), any(), anyString());
    }

    @Test
    void reviewDuplicadaRetorna409SemDuplicarLinhaOuAgregado() throws Exception {
        String deal = completed(client, conversation);
        alignOldDealReads(deal);
        assertThat(together(
                () -> review(deal, client, 4),
                () -> review(deal, client, 4))).containsExactlyInAnyOrder(201, 409);
        assertThat(reviews.count()).isEqualTo(1);
        assertRating(editor);
    }

    @RepeatedTest(3)
    void reviewsDeDealsDiferentesRecalculamMesmoPerfilSemPerderNota() throws Exception {
        String first = completed(client, conversation);
        String second = completed(client, conversation);
        var gate = new CountDownLatch(2);
        var delegate = mockingDetails(reviews).getMockCreationSettings().getDefaultAnswer();
        // Antes da correção, força os dois agregados a ler sem enxergar o commit alheio.
        // Se a implementação serializar o perfil, a primeira leitura pode seguir sozinha.
        doAnswer(inv -> {
            Object result = delegate.answer(inv);
            rendezvous(gate);
            return result;
        }).when(reviews).aggregateFor(editor.id());
        assertThat(together(
                () -> review(first, client, 1),
                () -> review(second, client, 5))).containsExactly(201, 201);
        assertThat(reviews.count()).isEqualTo(2);
        assertRating(editor);
    }

    @Test
    void participantesAvaliamSimultaneamenteSemPerderNenhumAutor() throws Exception {
        String deal = completed(client, conversation);
        alignOldDealReads(deal);
        assertThat(together(
                () -> review(deal, client, 4),
                () -> review(deal, editor, 5))).containsExactly(201, 201);
        assertThat(reviews.count()).isEqualTo(2);
        assertRating(editor);
        assertRating(client);
    }

    @Test
    void reviewDisputandoComConclusaoRespeitaEstadoCommitado() throws Exception {
        String deal = open(client, conversation);
        List<Integer> results = together(
                () -> postStatus("/" + deal + "/done", client, null),
                () -> review(deal, client, 5));
        assertThat(results.get(0)).isEqualTo(200);
        assertThat(results.get(1)).isIn(201, 409);
        assertThat(deals.findById(UUID.fromString(deal)).orElseThrow().getStatus().name()).isEqualTo("DONE");
        assertThat(reviews.count()).isEqualTo(results.get(1) == 201 ? 1 : 0);
        assertRating(editor);
    }

    @Test
    void reputacaoPorContaContinuaIncluindoAvaliacoesNosDoisPapeis() throws Exception {
        String first = completed(client, conversation);
        assertThat(review(first, editor, 1)).isEqualTo(201); // cliente recebe 1
        String reverse = conversation(editor, client);
        String second = completed(editor, reverse);
        assertThat(review(second, editor, 5)).isEqualTo(201); // mesma conta como editor recebe 5
        assertRating(client);
        var profile = profiles.findByUserId(client.id()).orElseThrow();
        assertThat(profile.getRatingAvg()).isEqualByComparingTo("3.0");
        assertThat(profile.getRatingCount()).isEqualTo(2);
    }

    private void alignOldDealReads(String id) {
        var gate = new CountDownLatch(2);
        var delegate = mockingDetails(deals).getMockCreationSettings().getDefaultAnswer();
        doAnswer(inv -> {
            Object result = delegate.answer(inv);
            rendezvous(gate);
            return result;
        }).when(deals).findById(UUID.fromString(id));
    }

    private static void rendezvous(CountDownLatch gate) throws InterruptedException {
        gate.countDown();
        // Limite para não criar deadlock artificial quando a correção serializa leituras.
        gate.await(2, TimeUnit.SECONDS);
    }

    private List<Integer> together(Callable<Integer> first, Callable<Integer> second) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = List.of(first, second).stream().map(call -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Sem início simultâneo");
                return call.call();
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
        } finally {
            start.countDown();
        }
    }

    private void assertRating(Account target) {
        // Fora das transações dos requests: lê somente o estado já commitado.
        RatingAggregateDTO source = reviews.aggregateFor(target.id());
        var profile = profiles.findByUserId(target.id()).orElseThrow();
        assertThat(profile.getRatingCount()).isEqualTo(source.count().intValue());
        assertThat(profile.getRatingAvg()).isEqualByComparingTo(
                BigDecimal.valueOf(source.average()).setScale(1, RoundingMode.HALF_UP));
    }

    private int review(String deal, Account author, int rating) throws Exception {
        return postStatus("/" + deal + "/review", author, "{\"rating\":" + rating + "}");
    }

    private int postStatus(String path, Account who, String body) throws Exception {
        var request = post("/v1/me/deals" + path).header(HttpHeaders.AUTHORIZATION, "Bearer " + who.token());
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        var response = mvc.perform(request).andReturn().getResponse();
        if (response.getStatus() == 409) {
            assertThat((Object) JsonPath.read(response.getContentAsString(), "$.error")).isInstanceOf(String.class);
        }
        return response.getStatus();
    }

    private String open(Account who, String conversationId) throws Exception {
        String body = mvc.perform(post("/v1/me/deals/from-conversation/" + conversationId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + who.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Trampo concorrente\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String completed(Account who, String conversationId) throws Exception {
        String id = open(who, conversationId);
        assertThat(postStatus("/" + id + "/done", who, null)).isEqualTo(200);
        return id;
    }

    private String conversation(Account who, Account target) throws Exception {
        String body = mvc.perform(post("/v1/me/conversations/with-editor/" + target.slug())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + who.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Ensaio concorrente\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private Account account(String name) throws Exception {
        String email = name + "@teste.com";
        mvc.perform(post("/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"senhaforte123\",\"displayName\":\"" + name
                        + "\",\"username\":\"" + name + "\",\"roles\":[\"CLIENT\"]}"))
                .andExpect(status().isAccepted());
        String body = mvc.perform(post("/v1/auth/register/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"code\":\"" + TestcontainersConfiguration.CODIGO_DE_CADASTRO + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");
        String profile = mvc.perform(put("/v1/me/editor-profile").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"niches\":[\"REELS\"],\"tools\":[\"PREMIERE\"]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Account(UUID.fromString(JsonPath.read(body, "$.user.id")), token, JsonPath.read(profile, "$.slug"));
    }

    private record Account(UUID id, String token, String slug) { }
}
