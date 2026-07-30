import Image from "next/image";

/**
 * O Instagram de alguém, com a logo da marca.
 *
 * Reaproveita a arte de /plataformas/reels.png: é o mesmo glifo do Instagram que já
 * marca o nicho Reels no resto do site — outra cópia do mesmo PNG seria peso e uma
 * segunda verdade pra manter.
 *
 * Sem hooks: usado tanto em Server Component (perfil público, painel) quanto dentro
 * de formulário no cliente.
 */
export function InstagramLink({
  username,
  size = 16,
}: {
  username: string;
  size?: number;
}) {
  return (
    <a
      href={`https://instagram.com/${username}`}
      target="_blank"
      // noreferrer junto porque o link sai pra fora do produto.
      rel="noopener noreferrer"
      className="inline-flex items-center gap-1.5 font-semibold hover:text-accent"
    >
      <InstagramGlyph size={size} />
      @{username}
    </a>
  );
}

/** Só a logo — pra quando o texto ao lado já diz do que se trata (label de formulário). */
export function InstagramGlyph({ size = 16 }: { size?: number }) {
  return (
    <Image
      src="/plataformas/reels.png"
      alt=""
      width={size}
      height={size}
      className="object-contain"
      style={{ width: size, height: size }}
    />
  );
}
