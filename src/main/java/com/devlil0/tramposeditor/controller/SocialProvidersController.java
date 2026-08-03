package com.devlil0.tramposeditor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Quais provedores de login estão ligados neste ambiente.
 * <p>
 * O frontend pergunta pra decidir se desenha os botões: botão de "Entrar com Google" que
 * leva a uma tela de erro é pior que botão nenhum. Sem credencial configurada, devolve
 * lista vazia e as telas de login ficam só com e-mail e senha.
 */
@RestController
@RequestMapping("v1/auth/social")
@RequiredArgsConstructor
public class SocialProvidersController {

    private final ObjectProvider<ClientRegistrationRepository> registrations;

    @GetMapping("/providers")
    @ResponseStatus(HttpStatus.OK)
    public List<String> enabled(){
        ClientRegistrationRepository repository = registrations.getIfAvailable();

        // Só o repositório em memória é iterável. É o que a autoconfiguração cria a
        // partir das variáveis de ambiente, então cobre o caso real; qualquer outro
        // cai no vazio em vez de arriscar um cast que estoura.
        if (!(repository instanceof InMemoryClientRegistrationRepository inMemory)) {
            return List.of();
        }

        return StreamSupport.stream(inMemory.spliterator(), false)
                .map(ClientRegistration::getRegistrationId)
                .toList();
    }
}
