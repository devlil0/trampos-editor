import { chromium, expect } from "@playwright/test";

const site = "http://localhost:62935";
const mail = "http://localhost:62936";
const browserOrigins = [site, "http://localhost:62937"];
const settings = "/painel/meu-perfil/configuracoes";
const deals = "/painel/trampos-fechados";
const password = "Trampos-demo-local-2026!";
const client = { name: "DEMO Cliente", username: "demo-cliente", email: "cliente@trampos-demo.test" };
const editor = { name: "DEMO Editor", username: "demo-editor", email: "editor@trampos-demo.test" };
const requestTitle = "DEMO Pedido de edição de Reels";
const directTitle = "DEMO Trampo pela vitrine";
const recurringTitle = "DEMO Segundo trampo pela vitrine";

const field = (page, name) => page.locator('[name="' + name + '"]');
const button = (page, name) => page.getByRole("button", { name, exact: typeof name === "string" });
const deal = (page, title) => page.locator("article").filter({
  has: page.getByRole("heading", { name: title, exact: true }),
});

export function validatePortfolio(value) {
  if (!value) return;
  let url;
  try {
    url = new URL(value);
  } catch {
    throw new Error("Informe uma URL HTTPS pública de YouTube ou Vimeo autorizada para a demo.");
  }
  const allowed = ["youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be",
    "vimeo.com", "www.vimeo.com", "player.vimeo.com"];
  if (url.protocol !== "https:" || !allowed.includes(url.hostname) || url.username || url.password || url.port) {
    throw new Error("Informe uma URL HTTPS pública de YouTube ou Vimeo autorizada para a demo.");
  }
}

async function withSessions(task) {
  const browser = await chromium.launch();
  try {
    const clientContext = await browser.newContext({ baseURL: site });
    const editorContext = await browser.newContext({ baseURL: site });
    for (const context of [clientContext, editorContext]) {
      await context.route("**/*", (route) => {
        const origin = new URL(route.request().url()).origin;
        return browserOrigins.includes(origin) ? route.continue() : route.abort();
      });
    }
    await task(await clientContext.newPage(), await editorContext.newPage());
  } finally {
    await browser.close();
  }
}

async function signup(page, user) {
  await page.goto("/criar-conta");
  await field(page, "displayName").fill(user.name);
  await field(page, "username").fill(user.username);
  await field(page, "email").fill(user.email);
  await field(page, "password").fill(password);
  await field(page, "passwordConfirm").fill(password);
  await button(page, "Criar conta").click();
  await expect(field(page, "code")).toBeVisible();
  let code;
  const url = new URL("/view/latest.txt", mail);
  url.searchParams.set("query", "to:" + user.email);
  await expect.poll(async () => {
    const response = await fetch(url, { signal: AbortSignal.timeout(3_000) });
    code = response.ok ? (await response.text()).match(/\b(\d{6})\b/)?.[1] : undefined;
    return Boolean(code);
  }, { message: "Confirmação esperada no Mailpit local", timeout: 15_000 }).toBe(true);
  try {
    await field(page, "code").fill(code);
    await button(page, "Confirmar e entrar").click();
    await expect(page).toHaveURL(/\/painel\//);
  } catch {
    throw new Error("Falha ao confirmar o cadastro local de " + user.name + ".");
  }
}

export async function fixture(portfolioUrl) {
  await withSessions(async (c, e) => {
    await signup(c, client);
    await signup(e, editor);
    await e.goto(settings);
    await field(e, "headline").fill("DEMO Edição de vídeos curtos");
    await e.locator('[name="niches"][value="REELS"]').check();
    await e.locator('[name="tools"][value="PREMIERE"]').check();
    await button(e, "Publicar meu perfil").click();
    await expect(field(e, "videoUrl")).toBeVisible();
    if (portfolioUrl) {
      await field(e, "title").fill("DEMO Amostra autorizada");
      await field(e, "videoUrl").fill(portfolioUrl);
      await button(e, "Adicionar").click();
      await expect(e.getByText("DEMO Amostra autorizada", { exact: true })).toBeVisible();
    }

    await c.goto("/painel/pedidos/novo");
    await field(c, "title").fill(requestTitle);
    await c.locator('textarea[name="description"]').fill("Cenário fictício: editar três cortes verticais para uma apresentação.");
    await c.locator('[name="niche"][value="REELS"]').check();
    await button(c, "Publicar pedido").click();
    await expect(c).toHaveURL(/\/painel\/pedidos$/);
    await expect(c.getByText(requestTitle, { exact: true })).toBeVisible();
    console.log(portfolioUrl ? "Fixture criada com a amostra informada." : "Fixture criada sem vídeo externo.");
  });
}

async function login(page, user) {
  await page.goto("/entrar");
  await field(page, "email").fill(user.email);
  await field(page, "password").fill(password);
  await button(page, "Entrar").click();
  await expect(page).toHaveURL(/\/painel\//);
}

async function openDeal(page, title) {
  await field(page, "title").fill(title);
  await button(page, "Registrar trampo").click();
  await expect(page.getByText(title, { exact: true })).toBeVisible();
}

async function send(page, body) {
  await field(page, "body").fill(body);
  await button(page, "Enviar").click();
  await expect(page.getByText(body, { exact: true })).toBeVisible();
}

export async function rehearse() {
  await withSessions(async (c, e) => {
    await Promise.all([login(c, client), login(e, editor)]);
    await c.goto(deals);
    await expect(deal(c, directTitle)).toHaveCount(0);
    await expect(deal(c, recurringTitle)).toHaveCount(0);
    await expect(deal(c, requestTitle)).toHaveCount(0);

    await c.goto("/editores");
    const search = c.getByRole("searchbox", { name: /buscar editores/i }).filter({ visible: true });
    await search.fill(editor.name);
    await search.press("Enter");
    await expect(c).toHaveURL((url) => url.searchParams.get("q") === editor.name);
    await c.getByRole("link", { name: /DEMO Editor/i }).click();
    await button(c, "Solicitar um Trampo").click();
    await expect(c).toHaveURL(/\/painel\/conversas\/[\w-]+$/);
    const direct = c.url();
    const briefing = "DEMO Briefing do cliente";
    await send(c, briefing);
    await e.goto(direct);
    await expect(e.getByText(briefing, { exact: true })).toBeVisible();
    await expect(button(e, "Registrar trampo")).toHaveCount(0);
    const reply = "DEMO Briefing recebido pelo editor";
    await send(e, reply);
    await button(c, "Atualizar mensagens").click();
    await expect(c.getByText(reply, { exact: true })).toBeVisible();

    await openDeal(c, directTitle);
    await c.goto(deals);
    const first = deal(c, directTitle);
    await first.getByRole("button", { name: /conclu/i }).click();
    await expect(first.getByText("Concluído", { exact: true })).toBeVisible();
    await first.getByRole("radio", { name: "5 de 5" }).click();
    await first.getByRole("button", { name: "Enviar avaliação" }).click();
    await expect(first.getByText("Você já avaliou este trampo.")).toBeVisible();
    await e.goto(deals);
    await expect(deal(e, directTitle).getByText("O que disseram de você", { exact: true })).toBeVisible();
    await first.getByRole("link", { name: "Abrir conversa" }).click();
    await expect(c).toHaveURL(direct);
    await openDeal(c, recurringTitle);
    await c.goto(deals);
    const recurring = deal(c, recurringTitle);
    await recurring.getByRole("button", { name: "Não vingou" }).click();
    await expect(recurring.getByText("Não vingou", { exact: true })).toBeVisible();
    console.log("Vitrine, conversa dos dois papéis, conclusão, avaliação e recontratação com cancelamento aprovadas.");

    await e.goto("/painel/feed");
    const listing = e.locator("article").filter({ hasText: requestTitle });
    await expect(listing).toHaveCount(1);
    await listing.locator('[name="message"]').fill("DEMO Candidatura para edição de Reels.");
    await listing.getByRole("button", { name: "Tenho interesse" }).click();
    // Navegar ao pedido só após a confirmação da candidatura.
    await expect(e.getByText(/Interesse enviado/).first()).toBeVisible();
    await c.goto("/painel/pedidos");
    await expect(c.getByText("DEMO Candidatura para edição de Reels.")).toBeVisible();
    await button(c, "Aceitar editor").click();
    await expect(c.getByText("Fechado", { exact: true })).toBeVisible();
    await c.goto(deals);
    await deal(c, requestTitle).getByRole("link", { name: "Abrir conversa" }).click();
    await expect(c).toHaveURL(/\/painel\/conversas\/[\w-]+$/);
    await expect(c).not.toHaveURL(direct);
    const start = "DEMO Vamos iniciar o trabalho do pedido";
    await send(c, start);
    await e.goto(c.url());
    await expect(e.getByText(start, { exact: true })).toBeVisible();
    await c.goto(deals);
    await deal(c, requestTitle).getByRole("button", { name: /conclu/i }).click();
    await expect(deal(c, requestTitle).getByText("Concluído", { exact: true })).toBeVisible();
    await e.goto(deals);
    const finished = deal(e, requestTitle);
    await expect(finished.getByText("Concluído", { exact: true })).toBeVisible();
    await finished.getByRole("radio", { name: "4 de 5" }).click();
    await finished.getByRole("button", { name: "Enviar avaliação" }).click();
    await expect(finished.getByText("Você já avaliou este trampo.")).toBeVisible();
    console.log("Candidatura, aceite, conversa do pedido, conclusão e avaliação aprovados.");
  });
}
