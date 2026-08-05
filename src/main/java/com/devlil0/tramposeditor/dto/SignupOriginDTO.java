package com.devlil0.tramposeditor.dto;

/**
 * De onde a pessoa veio na primeira visita (ESBOCO §3.1, V16).
 * <p>
 * Vem do cookie que o {@code proxy.ts} do frontend grava no primeiro acesso, e chega aqui
 * pelo cadastro ou pela troca do código do login social. Todos os campos são opcionais:
 * acesso direto não tem referrer, e visita sem campanha não tem {@code utm_source}. Ausência
 * é resposta, não erro.
 * <p>
 * <b>Sem anotação de validação de propósito.</b> Valor comprido é truncado no serviço, não
 * recusado — medição não pode custar um cadastro. Um {@code @Length} aqui faria uma URL de
 * campanha grande derrubar a conta inteira com 400, que é exatamente o instrumento
 * atrapalhando o que ele deveria só observar.
 */
public record SignupOriginDTO(
        String referrer,
        String utmSource,
        String landing
) {
}
