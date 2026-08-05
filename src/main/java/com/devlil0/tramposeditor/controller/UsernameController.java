package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.dto.UsernameAvailabilityDTO;
import com.devlil0.tramposeditor.service.UsernameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Esse nome está livre?" — a checagem que roda enquanto a pessoa digita.
 * <p>
 * Público, e não sob {@code /v1/auth}, porque o cadastro é só um dos dois usos: as
 * configurações chamam o mesmo endpoint pra trocar. E precisa ser público porque no
 * cadastro ainda não existe token — perguntar se um nome está livre é justamente o que se
 * faz antes de ter conta.
 * <p>
 * Não vaza nada: username é público por construção, é o endereço da pessoa em
 * {@code /{username}}. Quem quisesse enumerar já podia fazê-lo pedindo as páginas.
 * <p>
 * A resposta é sempre 200, inclusive pra nome inválido — "está livre?" é uma pergunta que
 * foi respondida. 400 aqui faria o campo piscar erro de rede enquanto a pessoa ainda está
 * na terceira letra do nome.
 */
@RestController
@RequestMapping("v1/usernames")
@RequiredArgsConstructor
public class UsernameController {

    private final UsernameService usernameService;

    @GetMapping("/availability")
    @ResponseStatus(HttpStatus.OK)
    public UsernameAvailabilityDTO availability(@RequestParam String username){
        String problema = usernameService.validar(usernameService.normalizar(username), null);

        return new UsernameAvailabilityDTO(problema == null, problema);
    }
}
