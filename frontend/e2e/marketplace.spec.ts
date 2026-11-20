import { expect, test, type Page } from "@playwright/test";
import { compose, mail, site } from "./stack.cjs";

const settings = "/painel/meu-perfil/configuracoes";
const deals = "/painel/trampos-fechados";
const password = "Trampos-fixture-only-2026!";
const field = (page: Page, name: string) => page.locator('[name="' + name + '"]');
const button = (page: Page, name: string | RegExp) => page.getByRole("button", { name, exact: typeof name === "string" });
const account = (name: string) => ({ name, email: name + "@example.test" });

async function message(email: string, pattern: RegExp): Promise<string> {
  let match: string | undefined;
  const url = mail + "/view/latest.txt?query=" + encodeURIComponent("to:" + email);
  await expect.poll(async () => {
    const res = await fetch(url);
    match = res.ok ? (await res.text()).match(pattern)?.[1] : undefined;
    return Boolean(match);
  }, { message: "Mensagem esperada no Mailpit local" }).toBe(true);
  return match!;
}

async function signup(page: Page, user: ReturnType<typeof account>) {
  await page.goto("/criar-conta");
  await field(page, "displayName").fill(user.name);
  await field(page, "username").fill(user.name);
  await field(page, "email").fill(user.email);
  await field(page, "password").fill(password);
  await field(page, "passwordConfirm").fill(password);
  await button(page, "Criar conta").click();
  await expect(field(page, "code")).toBeVisible();
  const code = await message(user.email, /\b(\d{6})\b/);
  await field(page, "code").fill(code);
  await button(page, "Confirmar e entrar").click();
  await expect(page).toHaveURL(/\/painel\//);
}

async function login(page: Page, email: string, value: string) {
  await page.goto("/entrar");
  await field(page, "email").fill(email);
  await field(page, "password").fill(value);
  await button(page, "Entrar").click();
}

async function openDeal(page: Page, title: string) {
  await field(page, "title").fill(title);
  await button(page, "Registrar trampo").click();
  await expect(page.getByText(title, { exact: true })).toBeVisible();
}

function deal(page: Page, title: string) {
  return page.locator("article").filter({ has: page.getByRole("heading", { name: title, exact: true }) });
}

async function send(page: Page, body: string) {
  await field(page, "body").fill(body);
  await button(page, "Enviar").click();
  await expect(page.getByText(body, { exact: true })).toBeVisible();
}

test.beforeAll(() => {
  if (!/^trampos-e2e-[a-f0-9]{12}$/.test(process.env.TRAMPOS_E2E_PROJECT ?? "")) {
    throw new Error("Use npm run test:e2e para criar a stack descartável.");
  }
});

test("cliente e editor percorrem vitrine, pedido e recorrência", async ({ browser }) => {
  const client = await browser.newContext({ baseURL: site });
  const editor = await browser.newContext({ baseURL: site });
  const outsider = await browser.newContext({ baseURL: site });
  try {
    const c = await client.newPage();
    const e = await editor.newPage();
    const other = await outsider.newPage();
    await Promise.all([
      signup(c, account("cliente-e2e")),
      signup(e, account("editor-e2e")),
      signup(other, account("terceiro-e2e")),
    ]);

    await expect(c.getByText(/Pra se candidatar você precisa ter um perfil publicado/)).toBeVisible();
    await e.goto(settings);
    await field(e, "headline").fill("Edição demonstrativa T25");
    await e.locator('[name="niches"][value="REELS"]').check();
    await e.locator('[name="tools"][value="PREMIERE"]').check();
    await button(e, "Publicar meu perfil").click();
    await expect(field(e, "videoUrl")).toBeVisible();
    await field(e, "title").fill("Portfólio demonstrativo");
    await field(e, "videoUrl").fill("https://youtu.be/dQw4w9WgXcQ");
    await button(e, "Adicionar").click();
    await expect(e.getByText("Portfólio demonstrativo", { exact: true })).toBeVisible();

    await c.goto("/editores");
    await c.getByRole("searchbox", { name: /buscar editores/i }).filter({ visible: true }).fill("editor-e2e");
    await c.getByRole("searchbox", { name: /buscar editores/i }).filter({ visible: true }).press("Enter");
    await expect(c).toHaveURL(/q=editor-e2e/);
    await c.getByRole("link", { name: /editor-e2e/ }).click();
    await expect(c.getByText("Portfólio demonstrativo", { exact: true })).toBeVisible();
    await button(c, "Solicitar um Trampo").click();
    await expect(c).toHaveURL(/\/painel\/conversas\/[\w-]+$/);
    const direct = c.url();
    await other.goto(direct);
    await expect(other.getByRole("heading", { name: "Página não encontrada" })).toBeVisible();

    await send(c, "Briefing fictício de edição");
    await e.goto(direct);
    await expect(e.getByText("Briefing fictício de edição")).toBeVisible();
    await send(e, "Briefing recebido pelo editor");
    await button(c, "Atualizar mensagens").click();
    await expect(c.getByText("Briefing recebido pelo editor")).toBeVisible();
    await expect(button(e, "Registrar trampo")).toHaveCount(0);

    await openDeal(c, "Trampo pela vitrine");
    await c.goto(deals);
    const first = deal(c, "Trampo pela vitrine");
    await first.getByRole("button", { name: /conclu/i }).click();
    await expect(first.getByText("Concluído", { exact: true })).toBeVisible();
    await first.getByRole("radio", { name: "5 de 5" }).click();
    await first.getByRole("button", { name: "Enviar avaliação" }).click();
    await expect(first.getByText("Você já avaliou este trampo.")).toBeVisible();
    await e.goto(deals);
    await expect(deal(e, "Trampo pela vitrine").getByText("O que disseram de você", { exact: true })).toBeVisible();

    await first.getByRole("link", { name: "Abrir conversa" }).click();
    await expect(c).toHaveURL(direct);
    await openDeal(c, "Trampo recorrente");
    await c.goto(deals);
    const recurring = deal(c, "Trampo recorrente");
    await recurring.getByRole("button", { name: "Não vingou" }).click();
    await expect(recurring.getByText("Não vingou", { exact: true })).toBeVisible();

    await c.goto("/painel/pedidos/novo");
    await field(c, "title").fill("Pedido aberto T25");
    await c.locator('textarea[name="description"]').fill("Edição de três cortes para uma demonstração fictícia.");
    await c.locator('[name="niche"][value="REELS"]').check();
    await button(c, "Publicar pedido").click();
    await expect(c).toHaveURL(/\/painel\/pedidos$/);
    await other.goto("/painel/feed");
    const clientListing = other.locator("article").filter({ hasText: "Pedido aberto T25" });
    await expect(clientListing).toBeVisible();
    await expect(clientListing.getByRole("button", { name: "Tenho interesse" })).toHaveCount(0);
    await e.goto("/painel/feed");
    const listing = e.locator("article").filter({ hasText: "Pedido aberto T25" });
    await listing.locator('[name="message"]').fill("Candidatura fictícia com experiência em Reels.");
    await listing.getByRole("button", { name: "Tenho interesse" }).click();
    await expect(e.getByText(/Interesse enviado/).first()).toBeVisible();
    await c.reload();
    await expect(c.getByText("Candidatura fictícia com experiência em Reels.")).toBeVisible();
    await button(c, "Aceitar editor").click();
    await expect(c.getByText("Fechado", { exact: true })).toBeVisible();
    await c.goto(deals);
    const accepted = deal(c, "Pedido aberto T25");
    await accepted.getByRole("link", { name: "Abrir conversa" }).click();
    await expect(c).toHaveURL(/\/painel\/conversas\/[\w-]+$/);
    await expect(c).not.toHaveURL(direct);
    await send(c, "Início do trabalho pelo pedido");
    await e.goto(c.url());
    await expect(e.getByText("Início do trabalho pelo pedido", { exact: true })).toBeVisible();
    await c.goto(deals);
    const fromRequest = deal(c, "Pedido aberto T25");
    await fromRequest.getByRole("button", { name: /conclu/i }).click();
    await expect(fromRequest.getByText("Concluído", { exact: true })).toBeVisible();
    await e.goto(deals);
    const finished = deal(e, "Pedido aberto T25");
    await expect(finished.getByText("Concluído", { exact: true })).toBeVisible();
    await finished.getByRole("radio", { name: "4 de 5" }).click();
    await finished.getByRole("button", { name: "Enviar avaliação" }).click();
    await expect(finished.getByText("Você já avaliou este trampo.")).toBeVisible();
  } finally {
    await Promise.all([client.close(), editor.close(), outsider.close()]);
  }
});

test("recupera erro de formulário, logout, reset, sessão expirada e API indisponível", async ({ browser }) => {
  const context = await browser.newContext({ baseURL: site });
  try {
    const page = await context.newPage();
    const user = account("sessao-e2e");
    await signup(page, user);
    await page.goto(settings);
    await button(page, "Sair da conta").click();
    await expect(page).toHaveURL(site + "/");
    await page.goto("/painel/pedidos");
    await expect(page).toHaveURL(/\/entrar$/);
    await login(page, user.email, "Senha-invalida-2026!");
    await expect(page.getByRole("alert").filter({ hasText: /incorret/i })).toBeVisible();
    await login(page, user.email, password);
    await expect(page).toHaveURL(/\/painel\//);

    // Recuperação em um contexto público, enquanto a sessão anterior fica no painel.
    const recovery = await browser.newContext({ baseURL: site });
    const newPassword = "Trampos-fixture-reset-2026!";
    try {
      const reset = await recovery.newPage();
      await reset.goto("/esqueci-senha");
      await field(reset, "email").fill(user.email);
      await button(reset, "Mandar o link").click();
      await expect(reset.getByText(/Se houver uma conta elegível/)).toBeVisible();
      const path = await message(user.email, /http:\/\/localhost:62925(\/redefinir-senha\?token=[\w-]+)/);
      // Mantém o link de recuperação fora de logs e verificações que imprimem URLs.
      await reset.goto(path).catch(() => { throw new Error("Falha ao abrir a recuperação local."); });
      await field(reset, "password").fill(newPassword);
      await field(reset, "confirm").fill(newPassword);
      await button(reset, "Trocar a senha").click();
      await expect(reset.getByText("Senha trocada.")).toBeVisible();
    } finally {
      await recovery.close();
    }

    await page.goto("/painel/pedidos");
    await expect(page).toHaveURL(/\/entrar\?motivo=sessao/);
    await login(page, user.email, newPassword);
    await expect(page).toHaveURL(/\/painel\//);
    const session = (await context.cookies()).find((cookie) => cookie.name === "te_session");
    if (!session) throw new Error("Sessão local ausente após login.");

    const claims = JSON.parse(Buffer.from(session.value.split(".")[1], "base64url").toString());
    expect(claims.exp - claims.iat, "TTL exclusivo da stack de teste").toBe(60);
    await expect.poll(async () => {
      const response = await fetch("http://localhost:62927/v1/users/me", {
        headers: { Authorization: "Bearer " + session.value },
        signal: AbortSignal.timeout(3000),
      });
      await response.body?.cancel();
      return response.status;
    }, { timeout: 135_000, intervals: [5000] }).toBe(401);

    // Simula uma aba que ainda possui o cookie de uma sessão expirada.
    await context.addCookies([{ ...session, expires: -1 }]);
    await page.goto("/painel/pedidos");
    await expect(page).toHaveURL(/\/entrar\?motivo=sessao/);
    await expect(page.getByText(/Sua sessão não está mais válida/)).toBeVisible();

    await login(page, user.email, newPassword);
    await expect(page).toHaveURL(/\/painel\//);
    const project = process.env.TRAMPOS_E2E_PROJECT;
    compose(project, "stop", "api");
    try {
      await page.goto("/painel/pedidos");
      await expect(page).toHaveURL(/\/painel\/pedidos$/);
      await expect(page.getByText("Sessão indisponível")).toBeVisible();
      await expect(button(page, "Tentar de novo")).toBeVisible();
    } finally {
      compose(project, "up", "-d", "--wait", "--wait-timeout", "90", "api");
    }
    await button(page, "Tentar de novo").click();
    await expect(page.getByRole("heading", { name: "Meus pedidos" })).toBeVisible();
  } finally {
    await context.close();
  }
});

