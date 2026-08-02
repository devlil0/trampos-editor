import type { Metadata } from "next";
import { CtaFinal, Faq, GuiaHero, Passos, Perguntas, type Passo } from "@/components/guia";
import { NicheIcon } from "@/components/niche-tag";
import type { EditorNiche } from "@/lib/types";

export const metadata: Metadata = {
  title: "Para clientes — como contratar um editor de vídeo | TramposEditor",
  description:
    "Como contratar um editor de vídeo no TramposEditor: veja portfólio, prazo e preço antes de falar com alguém. Sem lance, sem assinatura e sem comissão sobre o combinado.",
  alternates: { canonical: "/para-clientes" },
};

const PLATAFORMAS: EditorNiche[] = ["REELS", "TIKTOK", "YOUTUBE"];

const PASSOS: Passo[] = [
  {
    titulo: "Filtre por onde você publica",
    texto:
      "Reels, TikTok, YouTube ou cortes de podcast. Dá pra estreitar por ferramenta, preço máximo e quem está disponível agora.",
  },
  {
    titulo: "Compare portfólio, prazo e preço",
    texto:
      "Cada perfil mostra os vídeos que a pessoa já entregou, o prazo médio e a faixa de preço. O trabalho fala por si — nada de currículo.",
  },
  {
    titulo: "Fale direto e combine",
    texto:
      "Sem proposta, sem leilão, sem intermediário. Você fala com o editor e fecha do jeito de vocês.",
  },
];

export default function ParaClientesPage() {
  return (
    <div className="mx-auto max-w-6xl px-6">
      <GuiaHero
        etiqueta="PRA QUEM PRECISA DE VÍDEO EDITADO"
        titulo={
          <>
            Achar um editor bom não devia ser tão <span className="text-accent">burocrático</span>.
          </>
        }
      >
        Aqui não tem proposta, lance nem assinatura. Você navega, vê o trabalho pronto e fala
        direto com quem edita.
      </GuiaHero>

      <div className="mt-6 flex flex-wrap items-center gap-2.5 text-sm text-muted">
        <span>Editores para</span>
        {PLATAFORMAS.map((p) => (
          <span
            key={p}
            className="inline-flex items-center gap-1.5 rounded-full border border-line bg-card px-3 py-1.5 font-semibold text-fg"
          >
            <NicheIcon niche={p} size={15} />
            {p === "REELS" ? "Reels" : p === "TIKTOK" ? "TikTok" : "YouTube"}
          </span>
        ))}
      </div>

      <Passos
        titulo="Do primeiro clique ao vídeo entregue"
        passos={PASSOS}
        cta={{ href: "/editores", label: "Ver editores disponíveis" }}
      />

      <Perguntas>
        <Faq pergunta="Quanto custa?">
          Pra você, nada. Não cobramos pra procurar, pra abrir um perfil nem pra falar com um
          editor. O que você paga é o combinado com ele, direto.
        </Faq>
        <Faq pergunta="Vocês ficam com uma porcentagem?">
          Não. O preço e o pagamento são entre você e o editor. Não entramos no meio da transação
          e não cobramos comissão sobre o que vocês fecharem.
        </Faq>
        <Faq pergunta="Preciso descrever meu projeto num formulário?">
          Não. Isso é o modelo de lance dos sites de freela, e é justamente o que a gente evitou.
          Você olha portfólio e chama quem gostou. Se preferir o contrário — descrever e deixar os
          editores virem até você — dá pra postar um pedido, mas é opcional.
        </Faq>
        {/* Sem garantia de propósito: pagamento protegido é Fase 3 do FUNDACAO e não
            existe no produto. Prometer o que não há custaria mais caro que a objeção. */}
        <Faq pergunta="E se eu não gostar do resultado?">
          Você escolhe vendo trabalho pronto, não promessa — o portfólio está lá antes de você
          chamar. E como o combinado é direto, dá pra dividir a entrega em partes e alinhar no
          caminho. Pagamento protegido pela plataforma ainda não existe; quando existir, vai ser
          opcional.
        </Faq>
        <Faq pergunta="Preciso criar conta pra usar?">
          Só pra falar com alguém ou publicar um pedido. Ver a vitrine, abrir perfis e assistir aos
          portfólios não pede cadastro nenhum.
        </Faq>
        <Faq pergunta="Como sei quem consegue pegar meu vídeo agora?">
          Quem está com a agenda livre aparece marcado como disponível, e a vitrine já ordena essas
          pessoas primeiro. O prazo médio de entrega fica no perfil, do lado do preço.
        </Faq>
      </Perguntas>

      <CtaFinal
        titulo="Pronto pra achar seu editor?"
        texto="A vitrine é pública — dá pra olhar tudo antes de criar conta."
        principal={{ href: "/editores", label: "Ver editores" }}
        secundario={{ href: "/para-editores", label: "Na verdade, eu edito" }}
      />
    </div>
  );
}
