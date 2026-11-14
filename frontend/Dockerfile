FROM node:24-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --include=dev

COPY next.config.ts postcss.config.mjs tsconfig.json ./
COPY src ./src
COPY public ./public
# next/font/google precisa acessar o Google Fonts durante o build.
RUN npm run build

FROM node:24-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production \
    HOSTNAME=0.0.0.0 \
    PORT=3000

COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public
RUN mkdir -p .next/cache && chown node:node .next/cache

USER node
EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s \
  CMD ["node", "-e", "fetch('http://127.0.0.1:3000/api/health').then(r => { if (!r.ok) process.exitCode = 1 }).catch(() => { process.exitCode = 1 })"]

CMD ["node", "server.js"]
