package com.devlil0.tramposeditor.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A página de uma pessoa em {@code /{username}} — os dois papéis na mesma forma.
 * <p>
 * Existe porque o produto passou a ter <b>uma identidade por conta</b>, e não uma página
 * só pra quem edita: quem contrata também é alguém no marketplace, e o que ele fez
 * (trampos fechados, avaliações que deixou) é o que dá lastro pro editor decidir se
 * responde.
 * <p>
 * {@code editor} vem nulo pra quem nunca publicou perfil — é o que distingue os dois
 * papéis sem precisar de um campo de tipo.
 */
public record PublicProfileDTO(
        String username,
        String displayName,
        /** Versão da foto; nulo quando não há. Entra na URL da imagem. */
        UUID avatarVersion,
        /** Username do Instagram, sem "@". */
        String instagram,
        LocalDateTime memberSince,
        /** Nulo quando a pessoa não publicou perfil de editor. */
        EditorProfileDTO editor,
        /** Trampos que a pessoa fechou, pelos dois lados. */
        long dealsClosed,
        /** Avaliações que a pessoa escreveu — mede participação, não reputação. */
        long reviewsWritten,
        /** Nota recebida. Só faz sentido pra quem foi avaliado; zero quando ninguém avaliou. */
        double ratingAvg,
        long ratingCount
) {
}
