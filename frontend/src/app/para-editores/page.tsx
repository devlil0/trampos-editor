import type { Metadata } from "next";
import { CtaFinal, Faq, GuiaHero, Passos, Perguntas, type Passo } from "@/components/guia";

export const metadata: Metadata = {
  title: "Para editores — receba trabalhos de edição de vídeo | TramposEditor",
  description:
    "Publique seu portfólio de edição de vídeo e apareça pra quem já está procurando editor de Reels, TikTok e YouTube. De graça, sem plano e sem comissão sobre o seu trabalho.",
  alternates: { canonical: "/para-editores" },
};

const PASSOS: Passo[] = [
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
    // Descrição fiel ao que existe hoje: aparecer na vitrine e responder ao feed de
    // pedidos. O contato direto a partir do perfil ainda está sendo construído — dizer
    // que ele já funciona seria vender o que não há.
    titulo: "Apareça na vitrine e responda aos pedidos",
    texto:
      "Quem chega na vitrine está procurando editor — não é feed frio. No painel você também vê os pedidos abertos e diz quais topa pegar. Marque-se como ocupado quando encher a agenda.",
  },
];

export default function ParaEditoresPage() {
  return (
    <div className="mx-auto max-w-6xl px-6">
      <GuiaHero
        etiqueta="PRA QUEM EDITA"
        titulo={
          <>
            Seu portfólio na frente de quem já quer <span className="text-accent">contratar</span>.
          </>
        }
      >
        Sem dar lance, sem baixar seu preço pra ganhar de outro e sem mensalidade pra existir na
        plataforma. Você mostra o trabalho — o cliente escolhe olhando.
      </GuiaHero>

      <Passos
        titulo="De conta criada a primeiro contato"
        passos={PASSOS}
        cta={{ href: "/criar-conta", label: "Criar minha conta de editor" }}
      />

      <Perguntas>
        <Faq pergunta="Quanto custa pra aparecer?">
          Nada. Sem mensalidade, sem plano e sem taxa pra publicar seu perfil. Se um dia existir
          algo pago, vai ser um destaque opcional e avulso — nunca um pedágio pra você existir na
          vitrine.
        </Faq>
        <Faq pergunta="Vocês ficam com uma porcentagem do meu trabalho?">
          Não. O preço é seu e o pagamento é direto com o cliente. A plataforma não entra na
          transação nem tira percentual do que vocês combinarem.
        </Faq>
        {/* O anti-leilão é o diferencial mais forte pro lado da oferta (FUNDACAO §5.1):
            é o que separa daqui dos sites onde o freelancer compete derrubando preço. */}
        <Faq pergunta="Preciso dar lance ou baixar meu preço pra ganhar o trabalho?">
          Não existe leilão aqui. Você não disputa um trabalho derrubando preço contra outros
          editores — você publica sua faixa e o cliente escolhe pelo portfólio. Quem contrata
          olhando trabalho pronto não contrata pelo menor lance.
        </Faq>
        <Faq pergunta="Preciso passar por aprovação pra entrar?">
          Não. Publicar o perfil é o que registra você como editor — não tem análise de cadastro
          nem espera. Capriche no portfólio: é ele que decide, não o cadastro.
        </Faq>
        <Faq pergunta="Sou editor e cliente ao mesmo tempo. Dá?">
          Dá, e é o padrão: a conta é uma só. Você entra podendo contratar, e no dia que quiser
          receber trabalhos é só publicar seu perfil de editor no painel — sem cadastro separado.
        </Faq>
        <Faq pergunta="Onde meu portfólio fica hospedado?">
          Nos seus links do YouTube ou Vimeo — a gente só embute. Você não sobe arquivo, não perde
          qualidade e continua dono dos vídeos onde eles já estão.
        </Faq>
      </Perguntas>

      <CtaFinal
        titulo="Pronto pra aparecer?"
        texto="Criar conta e publicar o perfil leva alguns minutos, e é de graça."
        principal={{ href: "/criar-conta", label: "Criar minha conta" }}
        secundario={{ href: "/para-clientes", label: "Na verdade, quero contratar" }}
      />
    </div>
  );
}
