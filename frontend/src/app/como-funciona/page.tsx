import type { Metadata } from "next";
import Link from "next/link";
import { NicheIcon } from "@/components/niche-tag";
import type { EditorNiche } from "@/lib/types";

export const metadata: Metadata = {
  title: "Como funciona — TramposEditor",
  description:
    "Como o TramposEditor conecta quem precisa de vídeo editado a editores de Reels, TikTok e YouTube: sem lance, sem assinatura, sem taxa pra conversar.",
  alternates: { canonical: "/como-funciona" },
};

const PLATAFORMAS: EditorNiche[] = ["REELS", "TIKTOK", "YOUTUBE"];

const PASSOS_CLIENTE = [
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

const PASSOS_EDITOR = [
  {
    titulo: "Crie sua conta",
    texto:
      "De graça, sem plano e sem análise de cadastro. É a mesma conta de quem contrata — não existe cadastro separado de editor.",
  },
  {
    titulo: "Publique seu perfil",
    texto:
      "Cole os links dos seus melhores vídeos (YouTube ou Vimeo), marque as plataformas que você atende, seu prazo e sua faixa de preço. Publicar é o que registra você como editor.",
  },
  {
    titulo: "Receba contato de quem já quer contratar",
    texto:
      "Quem chega na vitrine está procurando editor — não é feed frio. Marque-se como ocupado quando encher a agenda.",
  },
];

export default function ComoFuncionaPage() {
  return (
    <div className="mx-auto max-w-6xl px-6">
      <section className="pt-14 pb-4">
        <h1 className="max-w-[18ch] text-balance text-4xl font-extrabold leading-[1.04] tracking-tight sm:text-5xl">
          Achar um editor bom não devia ser <span className="text-accent">burocrático</span>.
        </h1>
        <p className="mt-3.5 max-w-[56ch] text-lg text-muted">
          Aqui não tem proposta, lance nem assinatura. Você navega, vê o trabalho pronto e fala
          direto com quem edita.
        </p>

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
      </section>

      <Passos
        id="para-clientes"
        etiqueta="Pra quem precisa de vídeo editado"
        titulo="Do primeiro clique ao vídeo entregue"
        passos={PASSOS_CLIENTE}
        cta={{ href: "/", label: "Ver editores disponíveis" }}
      />

      <Passos
        id="para-editores"
        etiqueta="Pra quem edita"
        titulo="Seu portfólio na frente de quem contrata"
        passos={PASSOS_EDITOR}
        cta={{ href: "/criar-conta", label: "Criar minha conta de editor" }}
      />

      <section className="border-t border-line py-12">
        <h2 className="text-2xl font-extrabold tracking-tight">Perguntas diretas</h2>
        <div className="mt-6 grid gap-5 md:grid-cols-2">
          <Faq pergunta="Quanto custa?">
            Hoje, nada — dos dois lados. Não cobramos pra você aparecer na vitrine nem pra falar com
            um editor. Se um dia existir algo pago, vai ser um destaque opcional, avulso, nunca uma
            assinatura obrigatória.
          </Faq>
          <Faq pergunta="Vocês ficam com uma porcentagem?">
            Não. O combinado de preço e pagamento é entre você e o editor, direto. Não entramos no
            meio da transação.
          </Faq>
          <Faq pergunta="Preciso descrever meu projeto num formulário?">
            Não. Isso é o modelo de lance dos sites de freela, e é justamente o que a gente evitou.
            Você olha portfólio e chama quem gostou.
          </Faq>
          <Faq pergunta="Sou editor e cliente ao mesmo tempo. Dá?">
            Dá, e é o padrão: a conta é uma só. Você entra podendo contratar, e no dia que quiser
            receber trabalhos é só publicar seu perfil de editor no painel — sem cadastro separado.
          </Faq>
        </div>
      </section>

      <section className="mb-16 rounded-[18px] border border-line bg-card p-8 text-center">
        <h2 className="text-2xl font-extrabold tracking-tight">Pronto pra começar?</h2>
        <p className="mx-auto mt-2 max-w-[46ch] text-muted">
          Crie sua conta de graça ou dê uma olhada nos editores antes — do seu jeito.
        </p>
        <div className="mt-6 flex flex-wrap justify-center gap-3">
          <Link
            href="/criar-conta"
            className="rounded-full bg-accent px-5 py-2.5 text-sm font-semibold text-accent-contrast hover:bg-accent-strong"
          >
            Criar conta
          </Link>
          <Link
            href="/"
            className="rounded-full border border-line px-5 py-2.5 text-sm font-semibold hover:border-accent hover:text-accent"
          >
            Ver editores
          </Link>
        </div>
      </section>
    </div>
  );
}

function Passos({
  id,
  etiqueta,
  titulo,
  passos,
  cta,
}: {
  id: string;
  etiqueta: string;
  titulo: string;
  passos: { titulo: string; texto: string }[];
  cta: { href: string; label: string };
}) {
  return (
    // scroll-mt compensa a nav fixa quando se chega pelo link com âncora.
    <section id={id} className="scroll-mt-24 border-t border-line py-12">
      <p className="text-xs font-bold uppercase tracking-[0.09em] text-accent">{etiqueta}</p>
      <h2 className="mt-2 text-2xl font-extrabold tracking-tight">{titulo}</h2>

      <ol className="mt-6 grid gap-5 md:grid-cols-3">
        {passos.map((passo, i) => (
          <li key={passo.titulo} className="rounded-[18px] border border-line bg-card p-6">
            <span className="grid h-9 w-9 place-items-center rounded-full bg-accent-soft text-sm font-bold text-accent-strong">
              {i + 1}
            </span>
            <h3 className="mt-3.5 font-bold tracking-tight">{passo.titulo}</h3>
            <p className="mt-1.5 text-sm text-muted">{passo.texto}</p>
          </li>
        ))}
      </ol>

      <Link
        href={cta.href}
        className="mt-6 inline-block rounded-full border border-line px-5 py-2.5 text-sm font-semibold hover:border-accent hover:text-accent"
      >
        {cta.label}
      </Link>
    </section>
  );
}

function Faq({ pergunta, children }: { pergunta: string; children: React.ReactNode }) {
  return (
    <div className="rounded-[18px] border border-line bg-card p-5">
      <h3 className="font-bold tracking-tight">{pergunta}</h3>
      <p className="mt-1.5 text-sm text-muted">{children}</p>
    </div>
  );
}
