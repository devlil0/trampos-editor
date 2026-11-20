# Testes E2E dos dois papéis (T25)

Execute na pasta `frontend`, com Node 24 e Docker Compose v2 ativos. As
portas locais 62925, 62926 e 62927 precisam estar livres.

```sh
npm ci
npx playwright install chromium --only-shell
npm run test:e2e
```

Em um runner Linux sem as bibliotecas do Chromium, use
`npx playwright install --with-deps chromium --only-shell`.

O comando constrói os Dockerfiles de produção e inicia um projeto Compose
próprio por execução. Usa PostgreSQL em memória, com as migrações do clone,
e Mailpit sem encaminhamento. Site, Mailpit e API ficam no loopback. Banco e SMTP são acessados pela rede dos containers. A execução exige internet
na construção das imagens, inclusive para as fontes do frontend. No fim,
o runner encerra o projeto que criou e descarta seus dados. Execute uma
instância por máquina por vez, pois as portas locais são fixas.

Os testes usam contas `example.test` e contextos separados do Chromium.
Códigos, sessões e URL de reset ficam em memória. Traces, vídeos e
screenshots automáticos ficam desativados porque podem capturar dados de
autenticação. O teste de expiração usa TTL reduzido no backend local e
aguarda a rejeição real, incluindo a tolerância de relógio do JWT.

## O que a suíte exercita

- Cadastro com confirmação no Mailpit, publicação de editor e portfólio,
  busca, conversa pela vitrine, contratação, conclusão, avaliação e novo
  trabalho cancelado.
- Publicação de pedido, candidatura do editor, aceite, conversa vinculada
  ao pedido, conclusão e avaliação. Uma conta cliente sem perfil pode
  ver o pedido e fica sem a ação de candidatura. Um terceiro recebe
  página não encontrada ao abrir a conversa alheia.
- Logout, feedback de senha incorreta, login, reset por e-mail, revogação
  da sessão anterior, expiração real e recuperação após parada da API.

Para investigar apenas um cenário, passe um filtro após `--`:

```sh
npm run test:e2e -- --grep "vitrine"
```

## Integração na CI

O job pode executar os três comandos acima em runner Linux com Docker e
Node 24, usando `frontend` como diretório dos passos de npm. Reserve tempo
para construir as imagens e aguardar a expiração da sessão. A inclusão
desse job no workflow é combinada com Felipe (T10). OAuth real depende
do ensaio autorizado com os provedores e as URLs do ambiente candidato.

