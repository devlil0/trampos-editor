import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    // Thumbnails do YouTube (capa dos vídeos do portfólio).
    remotePatterns: [
      { protocol: "https", hostname: "i.ytimg.com" },
      { protocol: "https", hostname: "img.youtube.com" },
    ],
  },

  /**
   * `/como-funciona` virou duas páginas por público. O redirect é permanente (308)
   * pra que o buscador transfira o que a URL antiga já tinha acumulado em vez de
   * tratar as novas como páginas do zero — e pra não quebrar link já compartilhado.
   *
   * Limitação aceita: a âncora `#para-editores` não chega ao servidor — o navegador
   * nunca a envia — então links antigos com ela caem em `/para-clientes` como qualquer
   * outro. Não dá pra desviar por âncora aqui, e não vale um redirect no cliente só
   * por isso: o destino genérico é o certo pra maioria, que chega querendo contratar.
   */
  async redirects() {
    return [
      { source: "/como-funciona", destination: "/para-clientes", permanent: true },
      // "Trabalhos" virou "Trampos" — a palavra do produto. A rota antiga não era
      // indexada (o painel inteiro é noindex), então isto serve só pra favorito
      // salvo antes da troca.
      //
      // Aponta direto pro destino final: `/painel/trampos` também virou redirecionamento
      // em 2026-08-06 (o feed unificou), e encadear 308 em 308 é um salto de rede a mais
      // pra entregar a mesma tela.
      { source: "/painel/trabalhos", destination: "/painel/feed", permanent: true },
    ];
  },
};

export default nextConfig;
