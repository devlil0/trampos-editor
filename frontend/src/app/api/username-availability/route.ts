import { fetchWithTimeout } from "@/lib/api-fetch";
import type { NextRequest } from "next/server";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

/**
 * "Esse nome está livre?" — a ponte entre o campo que a pessoa está digitando e o backend.
 *
 * Existe porque `API_URL` é variável de servidor (§11.6): o browser não fala com o Spring,
 * fala com o Next. Server Action não serve aqui — ela é POST e não cacheia, e isto é uma
 * pergunta de leitura que dispara a cada pausa na digitação.
 *
 * Quem decide continua sendo o `UsernameService` do backend, inclusive a frase do erro.
 * Este arquivo não conhece regra nenhuma de username de propósito: duplicar "mínimo 3
 * caracteres" aqui criaria uma segunda cópia pra manter, e ela ficaria desatualizada na
 * primeira vez que a lista de nomes reservados crescesse.
 */
export async function GET(request: NextRequest) {
  const username = request.nextUrl.searchParams.get("username") ?? "";

  if (!username.trim()) {
    return Response.json({ available: false, message: null });
  }

  try {
    const res = await fetchWithTimeout(
      `${API_URL}/v1/usernames/availability?username=${encodeURIComponent(username)}`,
      { cache: "no-store" },
    );
    if (!res.ok) {
      return Response.json({ available: false, message: null });
    }
    return Response.json(await res.json());
  } catch {
    // Backend fora do ar não pode pintar o campo de vermelho como se o nome fosse
    // inválido. Silêncio aqui, e o submit dá a mensagem de verdade.
    return Response.json({ available: false, message: null });
  }
}
