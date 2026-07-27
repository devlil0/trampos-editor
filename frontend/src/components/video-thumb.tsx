import Image from "next/image";
import { coverThumbnail } from "@/lib/format";

const GRADIENTS = [
  "linear-gradient(135deg, #2b2450, #191627)",
  "linear-gradient(135deg, #24304f, #161a27)",
  "linear-gradient(135deg, #3a2748, #201626)",
  "linear-gradient(135deg, #1f3a44, #14202a)",
  "linear-gradient(135deg, #402a3a, #241620)",
  "linear-gradient(135deg, #2a2f52, #16192b)",
];

function gradientFor(seed: string): string {
  let hash = 0;
  for (let i = 0; i < seed.length; i++) hash = (hash * 31 + seed.charCodeAt(i)) | 0;
  return GRADIENTS[Math.abs(hash) % GRADIENTS.length];
}

const PlayGlyph = (
  <span className="grid h-13 w-13 place-items-center rounded-full border border-white/35 bg-white/15 text-white backdrop-blur-sm transition-transform group-hover:scale-110 group-hover:border-accent group-hover:bg-accent">
    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M8 5v14l11-7z" />
    </svg>
  </span>
);

interface VideoThumbProps {
  url: string | null | undefined;
  alt: string;
  seed: string;
  children?: React.ReactNode;
}

export function VideoThumb({ url, alt, seed, children }: VideoThumbProps) {
  const thumb = coverThumbnail(url);
  return (
    <div
      className="relative grid aspect-[16/10] place-items-center overflow-hidden"
      style={thumb ? undefined : { background: gradientFor(seed) }}
    >
      {thumb && (
        <Image
          src={thumb}
          alt={alt}
          fill
          sizes="(max-width: 640px) 100vw, 320px"
          className="object-cover"
        />
      )}
      {thumb && <span className="absolute inset-0 bg-black/15" aria-hidden="true" />}
      <span className="relative">{PlayGlyph}</span>
      {children}
    </div>
  );
}
