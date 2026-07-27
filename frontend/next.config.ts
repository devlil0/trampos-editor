import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    // Thumbnails do YouTube (capa dos vídeos do portfólio).
    remotePatterns: [
      { protocol: "https", hostname: "i.ytimg.com" },
      { protocol: "https", hostname: "img.youtube.com" },
    ],
  },
};

export default nextConfig;
