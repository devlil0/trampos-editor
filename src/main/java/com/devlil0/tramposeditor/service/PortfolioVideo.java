package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.enums.PortfolioProvider;
import com.devlil0.tramposeditor.exception.BusinessException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/** Mantém as formas aceitas em sincronia com frontend/src/lib/format.ts. */
record PortfolioVideo(PortfolioProvider provider, String url) {
    private static final Pattern YOUTUBE_ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final Pattern YOUTUBE_SHORT_PATH = Pattern.compile("/([A-Za-z0-9_-]{11})/?");
    private static final Pattern YOUTUBE_PATH = Pattern.compile("/(?:shorts|embed|live)/([A-Za-z0-9_-]{11})/?");
    private static final Pattern VIMEO_PATH = Pattern.compile("/([0-9]+)(?:/([A-Za-z0-9]+))?/?");
    private static final Pattern VIMEO_PLAYER_PATH = Pattern.compile("/video/([0-9]+)/?");
    private static final Pattern VIMEO_HASH = Pattern.compile("[A-Za-z0-9]+");

    static PortfolioVideo parse(String value) {
        URI uri;
        try {
            uri = new URI(value.trim());
        } catch (URISyntaxException e) {
            throw invalidLink();
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (host == null || !("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) {
            throw invalidLink();
        }
        host = host.toLowerCase(Locale.ROOT);
        String authority = uri.getRawAuthority().toLowerCase(Locale.ROOT);
        String defaultPort = "https".equalsIgnoreCase(scheme) ? ":443" : ":80";
        if (!authority.equals(host) && !authority.equals(host + defaultPort)) {
            throw invalidLink();
        }

        return switch (host) {
            case "youtu.be" -> youtube(pathId(YOUTUBE_SHORT_PATH, uri));
            case "youtube.com", "www.youtube.com", "m.youtube.com" -> youtube(
                    "/watch".equals(uri.getRawPath()) ? queryParam(uri, "v") : pathId(YOUTUBE_PATH, uri));
            case "vimeo.com", "www.vimeo.com" -> {
                var match = VIMEO_PATH.matcher(uri.getRawPath());
                if (!match.matches()) {
                    throw invalidLink();
                }
                yield vimeo(match.group(1), match.group(2));
            }
            case "player.vimeo.com" -> vimeo(pathId(VIMEO_PLAYER_PATH, uri), queryParam(uri, "h"));
            default -> throw invalidLink();
        };
    }

    private static PortfolioVideo youtube(String id) {
        if (id == null || !YOUTUBE_ID.matcher(id).matches()) {
            throw invalidLink();
        }
        return new PortfolioVideo(PortfolioProvider.YOUTUBE, "https://www.youtube.com/watch?v=" + id);
    }

    private static PortfolioVideo vimeo(String id, String hash) {
        if (hash != null && !VIMEO_HASH.matcher(hash).matches()) {
            throw invalidLink();
        }
        // O hash de links não listados é necessário para abrir o vídeo.
        String suffix = hash == null ? "" : "/" + hash;
        return new PortfolioVideo(PortfolioProvider.VIMEO, "https://vimeo.com/" + id + suffix);
    }

    private static String pathId(Pattern pattern, URI uri) {
        var match = pattern.matcher(uri.getRawPath());
        if (!match.matches()) {
            throw invalidLink();
        }
        return match.group(1);
    }

    private static String queryParam(URI uri, String name) {
        String query = uri.getRawQuery();
        if (query == null) {
            return null;
        }
        String result = null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (name.equals(URLDecoder.decode(pair[0], StandardCharsets.UTF_8))) {
                if (result != null) {
                    throw invalidLink();
                }
                result = pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
            }
        }
        return result;
    }

    private static BusinessException invalidLink() {
        // T07: mapear a validação de URL para o contrato comum de erro de entrada.
        return new BusinessException("Informe um link válido de vídeo do YouTube ou Vimeo.");
    }
}
