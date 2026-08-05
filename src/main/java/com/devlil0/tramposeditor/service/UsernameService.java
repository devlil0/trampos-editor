package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Gera e valida o username — a identidade da pessoa e o endereço dela em {@code /{username}}.
 * <p>
 * A regra que manda aqui vem de uma decisão de rota: a <b>raiz virou namespace de
 * usuário</b>. Isso é o que Instagram e Twitter fazem, e o preço é o mesmo — toda rota do
 * produto vira nome reservado, pra sempre. Um usuário chamado "entrar" tornaria a tela de
 * login inalcançável.
 */
@Service
@RequiredArgsConstructor
public class UsernameService {

    public static final int MAX_LENGTH = 30;
    private static final int MIN_LENGTH = 3;

    /**
     * Quanto tempo entre uma troca de username e a próxima.
     * <p>
     * Existe porque o endereço da pessoa é público: quem já salvou {@code /fulano} não tem
     * como saber que virou outra coisa — a troca <b>não</b> deixa redirecionamento pra trás
     * (decisão de 2026-08-05, mesma do Instagram). Catorze dias é o suficiente pra que a
     * troca seja uma decisão e não um hábito, sem parecer castigo.
     */
    public static final int COOLDOWN_DIAS = 14;

    private static final int MINUTOS_POR_DIA = 24 * 60;

    /**
     * Nomes que ninguém pode tomar.
     * <p>
     * Inclui as rotas que existem hoje, as tags da vitrine (que moram sob
     * {@code /editores/} mas podem subir), e um punhado de palavras que a plataforma vai
     * querer depois — {@code admin}, {@code api}, {@code suporte}. Reservar antes é
     * barato; tomar de volta de alguém que já usa, não.
     */
    private static final Set<String> RESERVED = Set.of(
            // rotas atuais
            "editores", "entrar", "criar-conta", "esqueci-senha", "redefinir-senha",
            "painel", "para-clientes", "para-editores", "configuracoes", "conversas",
            "pedidos", "trampos", "trampos-fechados", "meu-perfil",
            // convenções de arquivo e infraestrutura
            "api", "sitemap", "sitemap.xml", "robots", "robots.txt", "favicon.ico",
            "_next", "static", "public", "assets",
            // reservados pro produto
            "admin", "suporte", "ajuda", "sobre", "contato", "blog", "termos",
            "privacidade", "seguranca", "conta", "settings", "login", "logout",
            "signup", "me", "eu", "novo", "new", "escolher-username",
            // tags da vitrine
            "tiktok", "reels", "youtube", "podcast", "canal-dark",
            "premiere", "capcut", "after-effects", "davinci-resolve", "final-cut-pro");

    private final IUserRepository userRepository;

    public boolean isReserved(String username){
        return username != null && RESERVED.contains(username.toLowerCase(Locale.ROOT));
    }

    /**
     * Valida um username escolhido pela pessoa. Devolve a mensagem do problema, ou
     * {@code null} se estiver bom.
     */
    public String validar(String username, UUID donoAtual){
        if (username == null || username.isBlank()) {
            return "Escolha um nome de usuário.";
        }

        String limpo = username.trim().toLowerCase(Locale.ROOT);

        if (limpo.length() < MIN_LENGTH) {
            return "O nome de usuário precisa de pelo menos " + MIN_LENGTH + " caracteres.";
        }
        if (limpo.length() > MAX_LENGTH) {
            return "O nome de usuário pode ter no máximo " + MAX_LENGTH + " caracteres.";
        }
        // Hífen nas pontas geraria URL feia e ambígua ao lado das rotas do produto.
        if (!limpo.matches("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")) {
            return "Use só letras, números e hífen — sem espaço, acento ou símbolo.";
        }
        if (isReserved(limpo)) {
            return "Esse nome é reservado pela plataforma. Escolha outro.";
        }

        return userRepository.findByUsername(limpo)
                .filter(dono -> !dono.getId().equals(donoAtual))
                .map(dono -> "Esse nome de usuário já está em uso.")
                .orElse(null);
    }

    /** Forma canônica do que a pessoa digitou — é isto que vai pro banco. */
    public String normalizar(String username){
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Quantos dias faltam até a pessoa poder trocar de novo. Zero quer dizer "pode agora",
     * e é o que devolve pra quem nunca trocou ({@code ultimaTroca} nulo).
     */
    public long diasAteProximaTroca(LocalDateTime ultimaTroca){
        if (ultimaTroca == null) {
            return 0;
        }

        LocalDateTime liberaEm = ultimaTroca.plusDays(COOLDOWN_DIAS);
        LocalDateTime agora = LocalDateTime.now();
        if (!agora.isBefore(liberaEm)) {
            return 0;
        }

        // Arredonda pra cima: faltando 3h ainda é "amanhã", nunca "hoje" — dizer 0 dia e
        // recusar mesmo assim seria mentir na mensagem.
        return (long) Math.ceil(Duration.between(agora, liberaEm).toMinutes() / (double) MINUTOS_POR_DIA);
    }

    /**
     * Username automático pra conta nova, derivado do nome.
     * <p>
     * Sobrou <b>um</b> caminho depois que o cadastro por senha passou a pedir o nome no
     * formulário (V15): o login social, onde a conta nasce no callback do provedor e não há
     * onde perguntar. O nome que sai daqui é provisório por contrato — a conta vem com
     * {@code usernameChosen = false} e a pessoa escolhe o dela antes de navegar.
     * <p>
     * Em colisão, sufixo curto aleatório em vez de contador: evita uma segunda consulta de
     * sondagem e não vaza quantos homônimos existem, mesma escolha do slug do editor.
     */
    public String gerarPara(String displayName){
        String base = slugify(displayName);
        if (base.length() < MIN_LENGTH) {
            base = "editor";
        }

        String candidato = base;
        while (isReserved(candidato) || userRepository.existsByUsername(candidato)) {
            String sufixo = UUID.randomUUID().toString().substring(0, 6);
            candidato = truncate(base, MAX_LENGTH - sufixo.length() - 1) + "-" + sufixo;
        }
        return candidato;
    }

    private String slugify(String value){
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")               // remove acentos
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")           // não-alfanumérico vira hífen
                .replaceAll("(^-+)|(-+$)", "");          // apara hífens das pontas
        return truncate(normalized, MAX_LENGTH);
    }

    private String truncate(String value, int max){
        return value.length() <= max ? value : value.substring(0, max);
    }
}
