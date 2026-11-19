# Operação do TramposEditor

## Versões e configuração

O Compose de produção usa PostgreSQL 16, API Java 21 e frontend Next.js
standalone. Use Docker Compose 2.24 ou posterior para o arquivo de
ambiente OAuth opcional. Confirme que T14 e T15 estão na revisão da release.
O build do frontend instala as dependências do lockfile e precisa acessar
o Google Fonts. Registre o commit da release e as referências imutáveis
das três imagens. Publique API e frontend juntos para manter o callback
social da T04 compatível.

Use um nome de projeto Compose por ambiente, como `trampos-prod`.
Banco, rede e volume são criados com esse prefixo. API e frontend
publicam somente em `127.0.0.1`. Reserve portas de host diferentes para
instalações no mesmo servidor. O banco de produção fica sem porta
publicada. O Compose de desenvolvimento publica PostgreSQL apenas em
`127.0.0.1`, com porta ajustável por `DEV_DB_HOST_PORT`.

Copie `.env.example` para um arquivo privado, restrinja suas permissões
e mantenha os valores fora do Git, das imagens e dos logs. A configuração
usa as seguintes variáveis.

| Variáveis | Configuração |
| --- | --- |
| `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Banco exclusivo do ambiente. O Compose monta `DB_URL` com o host interno `postgres`. Em volume existente, alterar as variáveis não altera a senha ou o nome do banco já inicializado. |
| `JWT_SECRET`, `JWT_TTL` | Chave de pelo menos 32 bytes e duração ISO-8601. Preserve a chave nas trocas de imagem quando a intenção for manter sessões. |
| `API_URL` | URL interna usada só pelo Next. O padrão do Compose é `http://app:8080`. |
| `PUBLIC_API_URL` | Origem HTTPS pela qual o navegador alcança a API, usada em avatares e OAuth. |
| `SITE_URL` | Origem HTTPS pública do Next para HTML, robots e sitemap. É passada no build e em execução. Reconstrua a imagem ao mudar essa origem. |
| `FRONTEND_BASE_URL`, `CORS_ALLOWED_ORIGINS` | Origem HTTPS pública do Next. Esta é a base dos links de e-mail e retornos OAuth. CORS aceita a lista explícita de origens separadas por vírgula. |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `NOTIFICATIONS_FROM` | SMTP autenticado com STARTTLS. O perfil `prod` exige `smtp`. O remetente precisa ser autorizado. A T01 descreve a configuração e os limites do envio. |
| `API_HOST_PORT`, `FRONTEND_HOST_PORT` | Portas locais do proxy de host. Padrão 8080 e 3000. |
| `API_IMAGE`, `FRONTEND_IMAGE`, `POSTGRES_IMAGE` | Tags imutáveis ou digests da release. As tags locais padrão das imagens de aplicação usam o nome do projeto. |
| `DB_CPUS`, `API_CPUS`, `FRONTEND_CPUS` | Limites iniciais de 1, 1,5 e 1 CPU, respectivamente. |
| `DB_MEMORY`, `API_MEMORY`, `FRONTEND_MEMORY` | Limites iniciais de 512 MiB, 768 MiB e 512 MiB. Ajuste com base nas medições e confira os valores efetivos com `docker inspect`. |

Use `docker compose --env-file .env -p trampos-prod -f docker-compose.prod.yml config --quiet`
para validar a configuração sem imprimir valores. Em uma release com imagens
já disponíveis, use o mesmo prefixo com `up -d --no-build --wait`. Para o
ensaio local, valide primeiro e use `build` antes de subir. Confirme `ps`,
a saúde dos serviços e os testes de navegação. Evite publicar a saída de
`docker compose config` ou de inspeções que mostrem o ambiente.

## OAuth, proxy e HTTPS

OAuth fica desabilitado se o arquivo `.env.oauth` estiver ausente. Defina
`OAUTH_ENV_FILE` para usar outro caminho privado. Coloque nesse arquivo
somente a configuração dos provedores habilitados. As variáveis abaixo
mostram os nomes; substitua os marcadores somente no arquivo privado.

```dotenv
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=<id-autorizado>
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=<segredo-autorizado>
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_REDIRECT_URI=https://api.exemplo.com/login/oauth2/code/google

SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_ID=<id-autorizado>
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_SECRET=<segredo-autorizado>
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_REDIRECT_URI=https://api.exemplo.com/login/oauth2/code/github
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_SCOPE=read:user,user:email
```

Cadastre exatamente o callback HTTPS no aplicativo de teste de cada
provedor. O callback do provedor vai para a API. O redirecionamento
posterior vai para `FRONTEND_BASE_URL`. Enquanto o OAuth real estiver
pendente, mantenha o login por senha como caminho funcional.

Configure o proxy de host com dois hosts HTTPS explícitos. Encaminhe o
host do site para `127.0.0.1:3000` e o da API para `127.0.0.1:8080`,
ajustando as portas configuradas. Redirecione o HTTP público para HTTPS.
Rejeite hosts que não pertencem ao ambiente. Preserve o host público nos
pedidos ao Next para que o `Origin` das ações continue coerente com o
host encaminhado. Em um Nginx diretamente exposto, use estes ajustes nos
blocos `location` de ambos os hosts.

```nginx
proxy_set_header Host $http_host;
proxy_set_header X-Forwarded-Host $http_host;
proxy_set_header X-Forwarded-Proto https;
proxy_set_header X-Forwarded-For $remote_addr;
proxy_set_header Forwarded "";
proxy_set_header X-Trampos-Client-IP $remote_addr;
client_max_body_size 6m;
```

Se houver outro ingresso antes do Nginx, determine o IP com a cadeia
confiável desse ingresso antes de escrever o header dedicado. Não utilize
valores encaminhados livremente pelo cliente. Preserve
`server.forward-headers-strategy=none` na API. Os callbacks explícitos
evitam depender da interpretação de headers de encaminhamento pelo Spring.

Na T05, a lista de proxies confiáveis começa vazia. Identifique o par TCP
que a API observa e só inclua endereços exatos controlados pelo ingresso.
Na topologia com proxy no host, a API pode observar o gateway da rede
Docker. Se esse par também representar origens que podem chamar a API
diretamente, mantenha a lista vazia até isolar a cadeia. A API só lê
`X-Trampos-Client-IP` desses pares autorizados. A T16 precisa encaminhar
a origem pelo Next sem copiar headers arbitrários. Até lá, as chamadas
Next → API compartilham a cota por origem observada pela API. Validar o
proxy em uma chamada direta para a API não comprova o repasse pela T16.

Os cookies de sessão e de prova OAuth do Next usam `Secure`,
`HttpOnly` e `SameSite=Lax`. O perfil `prod` também define esses
atributos na sessão OAuth do Spring. Verifique o caminho do cookie de
prova e sua limpeza no callback com provedor simulado ou real autorizado.
Exclua queries dos logs de acesso e nunca registre cookies, Authorization,
corpos de autenticação ou valores de tokens. Para uploads rejeitados no
proxy, o frontend precisa aceitar `413` mesmo sem JSON.

## Saúde, backup e rollback

- Verifique o banco com `pg_isready`. A API usa
  `/actuator/health/readiness` com estado de prontidão e banco. A rota
  `/actuator/health/liveness` mede a vida da aplicação sem consultar o
  banco ou SMTP. Não use o estado agregado para reiniciar a API quando
  apenas o provedor de e-mail estiver indisponível.
- No Next, `/api/health` indica que o processo responde. Valide também
  uma página dinâmica que consulta a API, como a vitrine.
- Após a subida, confira redirecionamentos, sitemap, robots, avatares e
  links de e-mail com os hosts públicos. Teste o login por senha antes de
  habilitar OAuth. Confira se portas de banco e serviços de aplicação
  estão limitadas aos endereços esperados.

Gere backup antes de uma release com migrações. Em um shell Linux, mantenha
o arquivo em local de acesso restrito e use o formato customizado do
PostgreSQL. Cada execução cria um diretório novo para preservar as cópias
anteriores. Use o arquivo somente se o comando terminar com sucesso.

```sh
umask 077
backup_dir=$(mktemp -d ./tramposeditor-backup.XXXXXXXX) || exit 1
docker compose --env-file .env -p trampos-prod -f docker-compose.prod.yml \
  exec -T postgres sh -c 'exec pg_dump -Fc -U "$POSTGRES_USER" "$POSTGRES_DB"' \
  > "$backup_dir/tramposeditor.dump"
```

Registre o caminho da cópia, commit, horário e versão do PostgreSQL. Envie cópias criptografadas
para um destino autorizado e aplique a política de retenção combinada.
Ensaie a recuperação em um banco vazio separado, com nome de projeto e
volume próprios. Nesse projeto, use o mesmo prefixo Compose e passe o
arquivo pelo stdin para `pg_restore`. Defina `backup_dir` com o caminho
da cópia escolhida se estiver em outro shell.

```sh
docker compose --env-file restauracao.env -p trampos-restauracao -f docker-compose.prod.yml \
  exec -T postgres sh -c 'exec pg_restore --exit-on-error --no-owner --no-privileges -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < "$backup_dir/tramposeditor.dump"
```

Prepare papéis e concessões do ambiente restaurado separadamente quando
necessário. Confira o histórico do Flyway, o volume, as contagens e dados
representativos. Só então inicie a versão compatível da aplicação no
ambiente de recuperação. A inicialização da API pode aplicar migrações
pendentes, por isso a primeira validação da restauração deve conferir o
banco antes de subir a aplicação.

**Rollback de código.** Aponte `API_IMAGE` e `FRONTEND_IMAGE` para a
release anterior registrada e repita a subida sem build. Faça isso somente
se as imagens são compatíveis com o esquema já aplicado.

**Recuperação do banco.** Se for necessário voltar o esquema ou os dados,
restaure o backup em outro volume e valide com a release compatível antes
de qualquer troca de tráfego autorizada. Preserve o volume anterior.
Edite somente migrações futuras ainda não aplicadas. Uma restauração pode
perder alterações feitas depois do backup, então combine o ponto de
recuperação e a aprovação antes de executá-la em ambiente compartilhado.

## Ensaio isolado

Use outro nome de projeto, portas de loopback livres e um arquivo privado
com valores fictícios. Para TLS local, use hosts reservados como
`site.exemplo.test` e `api.exemplo.test`, resolvidos para o loopback
apenas no cliente do ensaio. Confie no certificado local só nesse
cliente e na JVM descartável quando ele também assinar o TLS do SMTP.

Os ensaios locais usam nomes, portas, volumes e credenciais fictícios.
Use um capturador SMTP com STARTTLS em rede interna, sem relay externo.
O SMTP de produção exige STARTTLS. Para verificar a topologia local,
confirme o recebimento no capturador e simule a indisponibilidade de SMTP
e banco separadamente. Teste o cookie de sessão com login no navegador.
O início e o cancelamento OAuth podem ser simulados com credenciais
fictícias, sem seguir o redirecionamento para o provedor. TLS e OAuth reais,
entrega SMTP, verificação do domínio/remetente e retenção de backups
dependem do ambiente autorizado.
