/**
 * Gera os assets de marca a partir das artes originais em `media/` (LogoFaster).
 *
 *   node scripts/gen-brand-assets.mjs
 *
 * As artes originais têm muito respiro (a claquete ocupa ~48% da largura), o que fica
 * ilegível em favicon/nav. Aqui a arte é re-enquadrada em cima do glifo e exportada nos
 * formatos que o Next consome por convenção de arquivo (icon, apple-icon, opengraph-image).
 *
 * Os arquivos gerados são versionados — este script é a fonte da verdade de como
 * refazê-los, não uma etapa de build.
 */
import { fileURLToPath } from "node:url";
import path from "node:path";
import fs from "node:fs/promises";
import sharp from "sharp";

const here = path.dirname(fileURLToPath(import.meta.url));
const frontend = path.resolve(here, "..");
const media = path.resolve(frontend, "..", "media");
const appDir = path.join(frontend, "src", "app");
const publicDir = path.join(frontend, "public");

const SOURCE = path.join(media, "icon-square-format.png");

/** Arte circular — é a que vira o ícone da aba do navegador. */
const CIRCLE_SOURCE = path.join(media, "icon-circle-format.png");

/** Bbox do glifo branco (a claquete) dentro da arte 675x675 — medido, não chutado. */
const GLYPH = { x0: 169, y0: 199, x1: 495, y1: 506 };

/** Cores da marca extraídas da própria arte. */
const BRAND = {
  plum: "#2f1140", // fundo do badge
  ink: "#0e0d14", // fundo do site (dark é o tema de origem)
  fg: "#eceaf2",
  accent: "#a78bfa",
  muted: "#9a96a8",
};

/**
 * Recorta um quadrado centrado no glifo, de modo que ele ocupe `fill` da aresta.
 * `fill` menor = mais respiro (necessário quando o badge é redondo e o glifo é diagonal).
 */
function reframe(fill) {
  const cx = (GLYPH.x0 + GLYPH.x1) / 2;
  const cy = (GLYPH.y0 + GLYPH.y1) / 2;
  const side = Math.round(Math.max(GLYPH.x1 - GLYPH.x0, GLYPH.y1 - GLYPH.y0) / fill);
  const half = Math.round(side / 2);
  return {
    left: Math.max(0, Math.round(cx) - half),
    top: Math.max(0, Math.round(cy) - half),
    width: side,
    height: side,
  };
}

function badge(fill, size, source = SOURCE) {
  return sharp(source)
    .extract(reframe(fill))
    .resize(size, size, { fit: "fill" })
    .flatten({ background: BRAND.plum }); // sem alpha: a máscara redonda vem depois, se for o caso
}

/**
 * Badge recortado em círculo. Necessário mesmo partindo da arte já circular: o
 * reenquadramento em cima do glifo cai todo dentro do disco original, então o recorte
 * volta a ser um quadrado cheio — o círculo tem que ser redesenhado no tamanho novo.
 */
async function roundBadge(fill, size, source = SOURCE) {
  const square = await badge(fill, size, source).png().toBuffer();
  const mask = Buffer.from(
    `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}">
       <circle cx="${size / 2}" cy="${size / 2}" r="${size / 2}" fill="#fff"/>
     </svg>`,
  );
  return sharp(square)
    .composite([{ input: mask, blend: "dest-in" }])
    .png()
    .toBuffer();
}

/**
 * Empacota PNGs num .ico. O formato aceita PNG embutido desde o Vista, então
 * não é preciso gerar bitmap DIB — só o cabeçalho e o diretório de entradas.
 */
function buildIco(pngs) {
  const header = Buffer.alloc(6);
  header.writeUInt16LE(0, 0); // reservado
  header.writeUInt16LE(1, 2); // tipo: 1 = ícone
  header.writeUInt16LE(pngs.length, 4);

  let offset = 6 + pngs.length * 16;
  const entries = pngs.map(({ size, data }) => {
    const e = Buffer.alloc(16);
    e.writeUInt8(size >= 256 ? 0 : size, 0); // 0 significa 256
    e.writeUInt8(size >= 256 ? 0 : size, 1);
    e.writeUInt8(0, 2); // paleta
    e.writeUInt8(0, 3); // reservado
    e.writeUInt16LE(1, 4); // planes
    e.writeUInt16LE(32, 6); // bits por pixel
    e.writeUInt32LE(data.length, 8);
    e.writeUInt32LE(offset, 12);
    offset += data.length;
    return e;
  });

  return Buffer.concat([header, ...entries, ...pngs.map((p) => p.data)]);
}

function escapeXml(s) {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

/** Card 1200x630 pro preview de link (TikTok/IG/Discord são o canal de aquisição). */
async function shareCard() {
  const W = 1200;
  const H = 630;
  const badgeSize = 210;
  const badgeX = 96;
  const badgeY = Math.round((H - badgeSize) / 2);
  const textX = badgeX + badgeSize + 52;
  const font = "Segoe UI, Inter, Helvetica, Arial, sans-serif";

  const wordmark = escapeXml("Trampos");
  const wordmarkAccent = escapeXml("Editor");
  const tagline = escapeXml("Editores de vídeo pra Reels, TikTok e Shorts");
  const kicker = escapeXml("SEM BUROCRACIA · FALE DIRETO COM O EDITOR");

  const bg = Buffer.from(
    `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}">
       <defs>
         <radialGradient id="glow" cx="22%" cy="50%" r="62%">
           <stop offset="0%" stop-color="${BRAND.accent}" stop-opacity="0.22"/>
           <stop offset="100%" stop-color="${BRAND.ink}" stop-opacity="0"/>
         </radialGradient>
       </defs>
       <rect width="${W}" height="${H}" fill="${BRAND.ink}"/>
       <rect width="${W}" height="${H}" fill="url(#glow)"/>
       <rect y="${H - 8}" width="${W}" height="8" fill="${BRAND.accent}"/>
       <text x="${textX}" y="${badgeY + 92}" font-family="${font}" font-size="74" font-weight="700" letter-spacing="-1.5">
         <tspan fill="${BRAND.fg}">${wordmark}</tspan><tspan fill="${BRAND.accent}">${wordmarkAccent}</tspan>
       </text>
       <text x="${textX}" y="${badgeY + 146}" font-family="${font}" font-size="31" fill="${BRAND.muted}">${tagline}</text>
       <text x="${textX}" y="${badgeY + 200}" font-family="${font}" font-size="19" font-weight="600" letter-spacing="1.6" fill="${BRAND.accent}">${kicker}</text>
     </svg>`,
  );

  const mark = await roundBadge(0.64, badgeSize);
  return sharp(bg)
    .composite([{ input: mark, left: badgeX, top: badgeY }])
    .png()
    .toBuffer();
}

/**
 * Ícones das plataformas (tags de nicho). As artes vêm em tamanhos e proporções
 * diferentes (o do YouTube é 640x443, os outros são quadrados de 4-5k), então cada uma
 * é normalizada num quadrado transparente de 64px — assim todas ocupam a mesma caixa
 * de 16px na tag, com peso óptico parecido.
 */
async function platformIcons() {
  const dir = path.join(publicDir, "plataformas");
  await fs.mkdir(dir, { recursive: true });

  // nome do arquivo de saída = valor do enum EditorNiche em minúsculo
  const map = [
    ["tiktok", "tiktok-icon.png"],
    ["reels", "instagram-icon.png"], // Reels é o formato; a marca é o Instagram
    ["youtube", "youtube-icon.png"],
  ];

  for (const [niche, file] of map) {
    await sharp(path.join(media, "enterprises", file))
      .trim({ threshold: 1 }) // tira a margem transparente antes de normalizar
      .resize(64, 64, { fit: "contain", background: { r: 0, g: 0, b: 0, alpha: 0 } })
      .png({ compressionLevel: 9 })
      .toFile(path.join(dir, `${niche}.png`));
  }
  return map.map(([niche]) => `public/plataformas/${niche}.png`);
}

async function main() {
  await fs.mkdir(publicDir, { recursive: true });

  // Ícone da aba do navegador: arte CIRCULAR, com o glifo reenquadrado pra sobreviver
  // a 16px. Fill menor que o do quadrado porque a claquete é diagonal e o disco corta
  // os cantos do seu retângulo envolvente.
  const TAB_FILL = 0.62;
  await fs.writeFile(
    path.join(appDir, "icon.png"),
    await roundBadge(TAB_FILL, 512, CIRCLE_SOURCE),
  );

  // O decoder de .ico do Next exige PNG em RGBA — a máscara redonda já garante o alpha.
  const icoSizes = [16, 32, 48];
  const icoPngs = await Promise.all(
    icoSizes.map(async (size) => ({
      size,
      data: await sharp(await roundBadge(TAB_FILL, size, CIRCLE_SOURCE))
        .ensureAlpha()
        .png({ palette: false })
        .toBuffer(),
    })),
  );
  await fs.writeFile(path.join(appDir, "favicon.ico"), buildIco(icoPngs));

  // apple-icon segue QUADRADO e opaco de propósito: o iOS aplica a máscara dele por
  // cima e trata transparência como preto — um círculo aqui viraria círculo dentro de
  // quadrado preto na tela inicial.
  await badge(0.72, 180).png().toFile(path.join(appDir, "apple-icon.png"));

  // Marca da nav: renderizada a 36px com bordas arredondadas via CSS, daí 4x aqui.
  await badge(0.66, 144).png().toFile(path.join(publicDir, "logo-mark.png"));

  const card = await shareCard();
  await fs.writeFile(path.join(appDir, "opengraph-image.png"), card);
  await fs.writeFile(path.join(appDir, "twitter-image.png"), card);

  const icones = await platformIcons();

  console.log("assets gerados a partir de", path.relative(frontend, SOURCE));
  for (const f of [
    "src/app/icon.png",
    "src/app/apple-icon.png",
    "src/app/favicon.ico",
    "src/app/opengraph-image.png",
    "src/app/twitter-image.png",
    "public/logo-mark.png",
    ...icones,
  ]) {
    const { size } = await fs.stat(path.join(frontend, f));
    console.log(`  ${f} — ${(size / 1024).toFixed(1)} KB`);
  }
}

await main();
