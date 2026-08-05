import { cookies } from "next/headers";
import { ORIGIN_COOKIE } from "@/proxy";

/** Espelha o SignupOriginDTO do backend. */
export interface SignupOrigin {
  referrer: string;
  utmSource: string;
  landing: string;
}

/**
 * Lê o cookie de primeira visita que o `proxy.ts` gravou (ESBOCO §3.1).
 *
 * Devolve `undefined` quando não há nada útil — e "nada útil" inclui o cookie existir com
 * os três campos vazios, que é o caso de quem digitou o endereço direto. Mandar um objeto
 * de vazios pro backend gravaria três strings em branco no lugar de três nulos, e "não
 * sei" ficaria indistinguível de "sei que é vazio" na hora de contar.
 *
 * Só roda no servidor, e só em dois lugares: o cadastro por senha e a troca do código
 * social. Nunca em navegação.
 */
export async function readSignupOrigin(): Promise<SignupOrigin | undefined> {
  const raw = (await cookies()).get(ORIGIN_COOKIE)?.value;
  if (!raw) return undefined;

  try {
    const parsed = JSON.parse(raw) as Partial<SignupOrigin>;

    const origem: SignupOrigin = {
      referrer: parsed.referrer ?? "",
      utmSource: parsed.utmSource ?? "",
      landing: parsed.landing ?? "",
    };

    return origem.referrer || origem.utmSource || origem.landing ? origem : undefined;
  } catch {
    // Cookie adulterado ou de uma versão antiga do formato. Perder a atribuição de uma
    // visita é irrelevante; deixar isso derrubar um cadastro, não.
    return undefined;
  }
}
