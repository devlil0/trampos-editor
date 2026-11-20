"use client";

import { useCallback, useEffect, useLayoutEffect, useRef, useState } from "react";

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
 *
 * <h3>Por que o arrasto não passa pelo estado do React</h3>
 *
 * A primeira versão movia a imagem com `left`/`top` e chamava `setState` a cada
 * `pointermove`. Isso dava um arrasto travado, e por dois motivos somados: `left`/`top` são
 * propriedades de <b>layout</b>, então cada pixel de movimento remontava a caixa e
 * repintava; e cada evento do mouse — que chega mais rápido que o quadro da tela —
 * disparava um ciclo inteiro de reconciliação.
 *
 * Agora a geometria ao vivo mora em {@link geo}, um ref, e o arrasto escreve
 * `transform: translate3d(...) scale(...)` direto no nó, agendado por
 * `requestAnimationFrame`. `transform` é resolvido pelo compositor, sem layout nem repaint,
 * e o rAF garante <b>uma</b> escrita por quadro em vez de uma por evento. Durante o arrasto
 * o React não re-renderiza nenhuma vez.
 *
 * E não há `transition` no arrasto, de propósito: interpolar o que o dedo está movendo
 * deixaria a imagem atrasada em relação ao cursor. Manipulação direta tem que ser 1:1 —
 * a suavidade vem de entregar todo quadro, não de amaciar o movimento. A transição existe
 * só no teclado, onde o passo é discreto e um salto seco pareceria falha.
 */

/** Lado do recorte no sistema de coordenadas interno. */
const BOX = 264;

/**
 * Lado do JPEG gerado. Maior que os 256 finais do backend de propósito: ele ainda reduz, e
 * reduzir de 512 dá uma imagem melhor do que mandar exatamente o tamanho final e torcer pra
 * nenhuma etapa reamostrar de novo.
 */
const OUT = 512;

const ZOOM_MIN = 1;
const ZOOM_MAX = 4;

/** Só pros passos discretos do teclado. Curto o bastante pra não parecer atraso. */
const TRANSICAO = "transform 130ms cubic-bezier(0.22, 1, 0.36, 1)";

interface Geometria {
  x: number;
  y: number;
  zoom: number;
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
  /** Espelho do zoom só pra barra refletir o valor; quem manda é o {@link geo}. */
  const [zoomUi, setZoomUi] = useState(1);

  const boxRef = useRef<HTMLDivElement>(null);
  const imgRef = useRef<HTMLImageElement>(null);

  /** A geometria ao vivo. É a fonte da verdade — o estado do React não acompanha o arrasto. */
  const geo = useRef<Geometria>({ x: 0, y: 0, zoom: 1 });
  const arrasto = useRef<{ px: number; py: number; ox: number; oy: number } | null>(null);
  const quadro = useRef<number | null>(null);

  /** Escala em que a imagem *cobre* o quadrado. É o zoom 1 — nunca sobra buraco. */
  const escalaBase = img ? BOX / Math.min(img.naturalWidth, img.naturalHeight) : 1;

  /**
   * Prende a imagem nas bordas: o quadrado nunca pode mostrar vazio. Sem isto dá pra
   * arrastar a foto pra fora e enviar um avatar meio branco.
   */
  const prender = useCallback((x: number, y: number, z: number, imagem: HTMLImageElement) => {
    const s = (BOX / Math.min(imagem.naturalWidth, imagem.naturalHeight)) * z;
    const largura = imagem.naturalWidth * s;
    const altura = imagem.naturalHeight * s;
    return {
      x: Math.min(0, Math.max(BOX - largura, x)),
      y: Math.min(0, Math.max(BOX - altura, y)),
    };
  }, []);

  /** Escreve a geometria no nó. Uma vez por quadro, nunca uma vez por evento. */
  const pintar = useCallback(() => {
    if (quadro.current != null) return;
    quadro.current = requestAnimationFrame(() => {
      quadro.current = null;
      const el = imgRef.current;
      if (!el) return;
      const { x, y, zoom } = geo.current;
      const width = el.naturalWidth;
      const height = el.naturalHeight;
      if (!width || !height) return;
      const base = BOX / Math.min(width, height);
      el.style.transform = `translate3d(${100 * x / (width * base)}%, ${100 * y / (height * base)}%, 0) scale(${zoom})`;
    });
  }, []);

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
      geo.current = {
        x: (BOX - imagem.naturalWidth * s) / 2,
        y: (BOX - imagem.naturalHeight * s) / 2,
        zoom: 1,
      };
      setImg(imagem);
      setZoomUi(1);
    };
    // O navegador precisa conseguir abrir o arquivo para convertê-lo em JPEG.
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

  // O primeiro enquadramento tem que estar no nó antes da tela pintar, senão a foto aparece
  // no canto e salta pro centro. `useLayoutEffect` roda antes disso; o rAF do `pintar` não.
  useLayoutEffect(() => {
    const el = imgRef.current;
    if (!img || !el) return;
    const { x, y, zoom } = geo.current;
    el.style.transform = `translate3d(${x}px, ${y}px, 0) scale(${zoom})`;
    boxRef.current?.focus();
  }, [img]);

  useEffect(() => {
    return () => {
      if (quadro.current != null) cancelAnimationFrame(quadro.current);
    };
  }, []);

  /** Aproxima mantendo parado o ponto que está no centro do quadrado. */
  const aplicarZoom = useCallback(
    (bruto: number, imagem: HTMLImageElement) => {
      const z = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, bruto));
      const atual = geo.current;
      const meio = BOX / 2;
      const fator = z / atual.zoom;

      const preso = prender(
        meio - (meio - atual.x) * fator,
        meio - (meio - atual.y) * fator,
        z,
        imagem,
      );

      geo.current = { ...preso, zoom: z };
      pintar();
      setZoomUi(z);
    },
    [prender, pintar],
  );

  // A roda do mouse precisa de listener nativo com `passive: false`: o React registra
  // `wheel` como passivo, e sem o preventDefault a página rola atrás do recorte.
  useEffect(() => {
    const box = boxRef.current;
    if (!box || !img) return;

    const aoRolar = (e: WheelEvent) => {
      e.preventDefault();
      if (imgRef.current) imgRef.current.style.transition = "";
      aplicarZoom(geo.current.zoom - e.deltaY * 0.002, img);
    };

    box.addEventListener("wheel", aoRolar, { passive: false });
    return () => box.removeEventListener("wheel", aoRolar);
  }, [aplicarZoom, img]);

  function aoPressionar(e: React.PointerEvent) {
    if (!img) return;
    e.currentTarget.setPointerCapture(e.pointerId);
    // Nada de transição enquanto o dedo está na tela: o movimento tem que ser 1:1.
    if (imgRef.current) imgRef.current.style.transition = "";
    arrasto.current = { px: e.clientX, py: e.clientY, ox: geo.current.x, oy: geo.current.y };
  }

  function aoMover(e: React.PointerEvent<HTMLDivElement>) {
    const inicio = arrasto.current;
    if (!inicio || !img) return;

    const fator = BOX / e.currentTarget.clientWidth;
    const preso = prender(
      inicio.ox + (e.clientX - inicio.px) * fator,
      inicio.oy + (e.clientY - inicio.py) * fator,
      geo.current.zoom,
      img,
    );

    geo.current = { ...preso, zoom: geo.current.zoom };
    pintar();
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
    const mapa: Record<string, { x: number; y: number }> = {
      ArrowLeft: { x: -passo, y: 0 },
      ArrowRight: { x: passo, y: 0 },
      ArrowUp: { x: 0, y: -passo },
      ArrowDown: { x: 0, y: passo },
    };

    const delta = mapa[e.key];
    if (delta) {
      e.preventDefault();
      // Aqui a transição entra: o passo é discreto, e sem ela a imagem pisca de posição.
      if (imgRef.current) imgRef.current.style.transition = TRANSICAO;
      const preso = prender(
        geo.current.x + delta.x,
        geo.current.y + delta.y,
        geo.current.zoom,
        img,
      );
      geo.current = { ...preso, zoom: geo.current.zoom };
      pintar();
      return;
    }

    if (e.key === "+" || e.key === "=") {
      e.preventDefault();
      if (imgRef.current) imgRef.current.style.transition = TRANSICAO;
      aplicarZoom(geo.current.zoom + 0.2, img);
    }
    if (e.key === "-" || e.key === "_") {
      e.preventDefault();
      if (imgRef.current) imgRef.current.style.transition = TRANSICAO;
      aplicarZoom(geo.current.zoom - 0.2, img);
    }
  }

  function confirmar() {
    if (!img) return;

    const canvas = document.createElement("canvas");
    canvas.width = OUT;
    canvas.height = OUT;
    const ctx = canvas.getContext("2d");
    if (!ctx) {
      setErro(true);
      return;
    }

    // Fundo branco pelo mesmo motivo do backend: JPEG não tem canal alfa, e PNG
    // transparente sem fundo sai com as áreas vazias pretas.
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, OUT, OUT);
    ctx.imageSmoothingEnabled = true;
    ctx.imageSmoothingQuality = "high";

    // O que a janela mostra, em coordenadas da imagem original — o inverso exato da
    // transformação usada na tela. Lê do ref, que é sempre o valor mais recente.
    const { x, y, zoom } = geo.current;
    const s = escalaBase * zoom;
    ctx.drawImage(img, -x / s, -y / s, BOX / s, BOX / s, 0, 0, OUT, OUT);

    canvas.toBlob((blob) => {
      if (blob) onConfirm(blob);
      else setErro(true);
    }, "image/jpeg", 0.92);
  }

  if (erro) {
    return (
      <div className="mt-4 rounded-[18px] border border-busy/40 bg-busy/10 p-4">
        <p role="alert" className="text-sm text-busy">
          Não consegui abrir essa imagem. Tente outra — JPG, PNG ou WebP.
        </p>
        <button type="button" autoFocus onClick={onCancel} className="btn btn-ghost btn-sm mt-3">
          Escolher outra
        </button>
      </div>
    );
  }

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
        style={{ width: BOX }}
        className="relative mt-4 aspect-square max-w-full cursor-grab touch-none overflow-hidden rounded-[14px] bg-black/40 active:cursor-grabbing focus:outline-none focus-visible:ring-2 focus-visible:ring-accent"
      >
        {img && (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            ref={imgRef}
            src={img.src}
            alt=""
            draggable={false}
            style={{
              position: "absolute",
              left: 0,
              top: 0,
              // Tamanho do zoom 1, fixo. O que muda é só o `transform`, e é por isso que
              // mexer aqui não custa layout nenhum.
              width: `${100 * img.naturalWidth * escalaBase / BOX}%`,
              height: `${100 * img.naturalHeight * escalaBase / BOX}%`,
              maxWidth: "none",
              transformOrigin: "0 0",
              // Avisa o compositor pra promover a imagem a camada própria antes do
              // primeiro arrasto, em vez de no meio dele.
              willChange: "transform",
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
          value={zoomUi}
          onChange={(e) => {
            if (!img) return;
            if (imgRef.current) imgRef.current.style.transition = "";
            aplicarZoom(Number(e.target.value), img);
          }}
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
