"use client";

import { useCallback, useEffect, useRef, useState } from "react";

/**
 * Enquadramento da foto de perfil — arrastar e aproximar antes de enviar (2026-08-06).
 *
 * Antes disto o backend recortava o centro e pronto: quem tivesse o rosto fora do meio da
 * foto — que é a maioria das fotos de celular — ficava com um avatar de ombro ou de testa,
 * sem nada a fazer além de tentar outra imagem. A decisão de enquadramento é de quem está
 * na foto, e agora ela é feita aqui.
 *
 * <b>O recorte acontece no browser e o backend não muda.</b> O que sobe é um JPEG já
 * quadrado, então o recorte central do `AvatarService` vira operação neutra — e todas as
 * garantias dele continuam de pé, porque ele segue decodificando e reencodando o que
 * chega: arquivo que não é imagem continua sendo recusado, e nada embutido sobrevive.
 * Fazer o corte no cliente é sugestão de enquadramento, nunca dispensa de validação.
 *
 * Efeito colateral bem-vindo: o canvas do browser respeita a orientação EXIF, que o
 * `ImageIO` do Java ignora. Foto tirada em pé, que antes era gravada deitada, agora sobe
 * na posição em que a pessoa a vê aqui.
 *
 * Sem biblioteca de recorte: o produto tem `next` e `react` como dependências de runtime e
 * só. A matemática toda é uma escala e um deslocamento, e nada disso justifica trazer uma
 * árvore de dependências pra dentro por causa de uma tela.
 */

/** Lado do viewport de recorte, em px de tela. */
const BOX = 264;

/**
 * Lado do JPEG gerado. Maior que os 256 finais do backend de propósito: ele ainda reduz, e
 * reduzir de 512 dá uma imagem melhor do que mandar exatamente o tamanho final e torcer pra
 * nenhuma etapa reamostrar de novo.
 */
const OUT = 512;

const ZOOM_MIN = 1;
const ZOOM_MAX = 4;

interface Offset {
  x: number;
  y: number;
}

export function AvatarCropper({
  file,
  pending,
  onCancel,
  onConfirm,
}: {
  file: File;
  pending: boolean;
  onCancel: () => void;
  onConfirm: (blob: Blob) => void;
}) {
  const [img, setImg] = useState<HTMLImageElement | null>(null);
  const [erro, setErro] = useState(false);
  const [zoom, setZoom] = useState(1);
  const [offset, setOffset] = useState<Offset>({ x: 0, y: 0 });

  const boxRef = useRef<HTMLDivElement>(null);
  const arrasto = useRef<{ px: number; py: number; ox: number; oy: number } | null>(null);

  /** Escala em que a imagem *cobre* o quadrado. É o zoom 1 — nunca sobra buraco. */
  const escalaBase = img ? BOX / Math.min(img.naturalWidth, img.naturalHeight) : 1;

  /**
   * Prende a imagem nas bordas: o quadrado nunca pode mostrar vazio. Sem isto dá pra
   * arrastar a foto pra fora e enviar um avatar meio branco.
   */
  const prender = useCallback(
    (bruto: Offset, z: number, imagem: HTMLImageElement): Offset => {
      const s = (BOX / Math.min(imagem.naturalWidth, imagem.naturalHeight)) * z;
      const largura = imagem.naturalWidth * s;
      const altura = imagem.naturalHeight * s;
      return {
        x: Math.min(0, Math.max(BOX - largura, bruto.x)),
        y: Math.min(0, Math.max(BOX - altura, bruto.y)),
      };
    },
    [],
  );

  // Carrega o arquivo escolhido e centraliza o enquadramento inicial.
  //
  // O `vivo` não é zelo abstrato — sem ele esta tela acusa "formato inválido" em toda foto
  // válida durante o desenvolvimento. Em StrictMode o efeito roda duas vezes (monta, limpa,
  // monta), e a limpeza da primeira passada revoga o object URL dela. Revogar aborta o
  // carregamento em curso, o que dispara `onerror` da primeira imagem — e esse handler
  // escreve no estado do **mesmo** componente que a segunda passada acabou de carregar com
  // sucesso. Resultado: erro na tela e imagem perfeita por trás dele. A trava é ignorar
  // tudo que chega de uma passada já descartada.
  useEffect(() => {
    const url = URL.createObjectURL(file);
    const imagem = new Image();
    let vivo = true;

    imagem.onload = () => {
      if (!vivo) return;
      const s = BOX / Math.min(imagem.naturalWidth, imagem.naturalHeight);
      setImg(imagem);
      setZoom(1);
      setOffset({
        x: (BOX - imagem.naturalWidth * s) / 2,
        y: (BOX - imagem.naturalHeight * s) / 2,
      });
    };
    // Arquivo corrompido ou formato que o browser não abre: o backend também recusaria,
    // mas dizer aqui evita uma ida ao servidor pra receber a mesma resposta.
    imagem.onerror = () => {
      if (!vivo) return;
      setErro(true);
    };
    imagem.src = url;

    return () => {
      vivo = false;
      URL.revokeObjectURL(url);
    };
  }, [file]);

  /** Aproxima mantendo parado o ponto que está no centro do quadrado. */
  const aplicarZoom = useCallback(
    (novo: number) => {
      if (!img) return;
      const z = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, novo));
      setOffset((atual) => {
        const meio = BOX / 2;
        const fator = z / zoom;
        return prender(
          { x: meio - (meio - atual.x) * fator, y: meio - (meio - atual.y) * fator },
          z,
          img,
        );
      });
      setZoom(z);
    },
    [img, zoom, prender],
  );

  // A roda do mouse precisa de listener nativo com `passive: false`: o React registra
  // `wheel` como passivo, e sem o preventDefault a página rola atrás do recorte.
  useEffect(() => {
    const box = boxRef.current;
    if (!box) return;

    const aoRolar = (e: WheelEvent) => {
      e.preventDefault();
      aplicarZoom(zoom - e.deltaY * 0.002);
    };

    box.addEventListener("wheel", aoRolar, { passive: false });
    return () => box.removeEventListener("wheel", aoRolar);
  }, [aplicarZoom, zoom]);

  function aoPressionar(e: React.PointerEvent) {
    if (!img) return;
    e.currentTarget.setPointerCapture(e.pointerId);
    arrasto.current = { px: e.clientX, py: e.clientY, ox: offset.x, oy: offset.y };
  }

  function aoMover(e: React.PointerEvent) {
    const inicio = arrasto.current;
    if (!inicio || !img) return;
    setOffset(
      prender(
        { x: inicio.ox + (e.clientX - inicio.px), y: inicio.oy + (e.clientY - inicio.py) },
        zoom,
        img,
      ),
    );
  }

  function aoSoltar(e: React.PointerEvent) {
    arrasto.current = null;
    if (e.currentTarget.hasPointerCapture(e.pointerId)) {
      e.currentTarget.releasePointerCapture(e.pointerId);
    }
  }

  /** Teclado: quem não usa mouse também precisa enquadrar. */
  function aoTeclar(e: React.KeyboardEvent) {
    if (!img) return;
    const passo = e.shiftKey ? 20 : 6;
    const mapa: Record<string, Offset> = {
      ArrowLeft: { x: -passo, y: 0 },
      ArrowRight: { x: passo, y: 0 },
      ArrowUp: { x: 0, y: -passo },
      ArrowDown: { x: 0, y: passo },
    };
    const delta = mapa[e.key];
    if (delta) {
      e.preventDefault();
      setOffset((atual) => prender({ x: atual.x + delta.x, y: atual.y + delta.y }, zoom, img));
      return;
    }
    if (e.key === "+" || e.key === "=") {
      e.preventDefault();
      aplicarZoom(zoom + 0.2);
    }
    if (e.key === "-" || e.key === "_") {
      e.preventDefault();
      aplicarZoom(zoom - 0.2);
    }
  }

  function confirmar() {
    if (!img) return;

    const canvas = document.createElement("canvas");
    canvas.width = OUT;
    canvas.height = OUT;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    // Fundo branco pelo mesmo motivo do backend: JPEG não tem canal alfa, e PNG
    // transparente sem fundo sai com as áreas vazias pretas.
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, OUT, OUT);
    ctx.imageSmoothingEnabled = true;
    ctx.imageSmoothingQuality = "high";

    // O que a janela mostra, em coordenadas da imagem original — o inverso exato da
    // transformação usada na tela.
    const s = escalaBase * zoom;
    ctx.drawImage(img, -offset.x / s, -offset.y / s, BOX / s, BOX / s, 0, 0, OUT, OUT);

    canvas.toBlob((blob) => blob && onConfirm(blob), "image/jpeg", 0.92);
  }

  if (erro) {
    return (
      <div className="mt-4 rounded-[18px] border border-busy/40 bg-busy/10 p-4">
        <p role="alert" className="text-sm text-busy">
          Não consegui abrir essa imagem. Tente outra — JPG, PNG ou WebP.
        </p>
        <button type="button" onClick={onCancel} className="btn btn-ghost btn-sm mt-3">
          Escolher outra
        </button>
      </div>
    );
  }

  const largura = img ? img.naturalWidth * escalaBase * zoom : 0;
  const altura = img ? img.naturalHeight * escalaBase * zoom : 0;

  return (
    <div className="mt-4 rounded-[18px] border border-line bg-panel p-5">
      <h3 className="text-sm font-bold tracking-tight">Enquadre sua foto</h3>
      <p className="mt-1 text-sm text-muted">
        Arraste pra posicionar e use a barra pra aproximar. O que estiver dentro do círculo é
        o que vai aparecer.
      </p>

      <div
        ref={boxRef}
        role="application"
        aria-label="Área de enquadramento: arraste para posicionar, setas para ajustar, mais e menos para aproximar"
        tabIndex={0}
        onPointerDown={aoPressionar}
        onPointerMove={aoMover}
        onPointerUp={aoSoltar}
        onPointerCancel={aoSoltar}
        onKeyDown={aoTeclar}
        style={{ width: BOX, height: BOX }}
        className="relative mt-4 max-w-full cursor-grab touch-none overflow-hidden rounded-[14px] bg-black/40 active:cursor-grabbing focus:outline-none focus-visible:ring-2 focus-visible:ring-accent"
      >
        {img && (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={img.src}
            alt=""
            draggable={false}
            style={{
              position: "absolute",
              left: offset.x,
              top: offset.y,
              width: largura,
              height: altura,
              maxWidth: "none",
            }}
          />
        )}

        {/* A sombra gigante escurece tudo que está fora do círculo — é a máscara, e ela
            mostra o recorte redondo real em vez de prometer um quadrado. */}
        <div className="pointer-events-none absolute inset-0 rounded-full shadow-[0_0_0_9999px_rgba(0,0,0,0.55)]" />
      </div>

      <label className="mt-4 flex items-center gap-3">
        <span className="text-xs font-bold uppercase tracking-[0.08em] text-muted">Zoom</span>
        <input
          type="range"
          min={ZOOM_MIN}
          max={ZOOM_MAX}
          step={0.01}
          value={zoom}
          onChange={(e) => aplicarZoom(Number(e.target.value))}
          className="h-1.5 max-w-[264px] flex-1 cursor-pointer appearance-none rounded-full bg-line accent-[var(--accent)]"
        />
      </label>

      <div className="mt-4 flex flex-wrap items-center gap-2.5">
        <button
          type="button"
          onClick={confirmar}
          disabled={!img || pending}
          className="btn btn-primary btn-sm disabled:opacity-60"
        >
          {pending ? "Enviando…" : "Usar esta foto"}
        </button>
        <button
          type="button"
          onClick={onCancel}
          disabled={pending}
          className="text-sm font-semibold text-muted hover:text-fg disabled:opacity-60"
        >
          Cancelar
        </button>
      </div>
    </div>
  );
}
