# Demonstração do TramposEditor (T26)

## Versão e ambiente

O ensaio local usa o código da revisão f6120e9db208f4dfa5088bfc1cc304d22ad25fef
da develop. O script constrói a API e o frontend a partir dessa revisão,
usando os Dockerfiles do produto. Separe as imagens testadas no computador
da apresentação e confirme a revisão com a equipe antes de congelar a versão
do dia 25/11. Se aprovar outra revisão, atualize o script e repita o ensaio.

Pré-requisitos: Git com a revisão disponível, tar, Node 24 e Docker local com
Compose 2.24.4 ou superior. Reserve 62935 (site), 62936 (Mailpit) e 62937
(API) no loopback. Banco e SMTP são internos ao projeto Compose
trampos-demo-local-t26. Os dados desse banco em memória são descartáveis.

Instale as dependências e o Chromium uma vez em frontend.

~~~sh
npm ci
npx playwright install chromium --only-shell
~~~

No Linux, inclua --with-deps na instalação do Chromium se faltarem bibliotecas.

Na raiz do repo, prepare o ambiente. A preparação inicial usa internet para
as imagens-base, dependências e Google Fonts.

~~~sh
node frontend/demo/run.mjs prepare
~~~

A fixture usa a interface real para cadastrar e confirmar as duas contas no
Mailpit, publicar o perfil e abrir um pedido. Para mostrar um portfólio em
vídeo, passe como segundo argumento uma URL pública do YouTube ou Vimeo
que a equipe tenha autorização para apresentar. Use o mesmo link em cada
reset e confirme a abertura antes do evento. Sem material autorizado, ensaie
com o perfil e sem vídeo. Todas as contas, o pedido, as mensagens e as
avaliações da fixture representam situações fictícias.

| Papel | Nome | E-mail fictício |
| --- | --- | --- |
| Cliente | DEMO Cliente | cliente@trampos-demo.test |
| Editor | DEMO Editor | editor@trampos-demo.test |

Senha pública das duas contas, exclusiva da fixture: Trampos-demo-local-2026!.
Abra http://localhost:62935 em um perfil normal para o cliente e em uma janela
anônima para o editor, ou use dois perfis de navegador. Duas janelas anônimas do
mesmo perfil podem compartilhar sessão. Use localhost, pois o cookie Secure
pode ser descartado ao acessar por IP via HTTP.

## Ordem da apresentação

1. Apresente os dois papéis e avise que os dados são fictícios. Para mostrar
   a confirmação de e-mail, cadastre uma terceira conta fictícia antes de
   entrar com os usuários do roteiro e use o Mailpit local. Ele captura a
   mensagem no ambiente de ensaio.
2. Mostre o perfil publicado do editor. Como cliente, busque por **DEMO Editor**
   em **Editores**, abra o perfil e clique em **Solicitar um Trampo**.
   Se houver amostra autorizada, mostre o item do portfólio.
3. Na conversa, envie um briefing como cliente, abra a mesma conversa no
   painel do editor e responda. Atualize as mensagens como cliente. Registre
   um trampo, entre em **Trampos fechados**, conclua e avalie. Abra a conversa
   de novo, registre outro trampo e encerre com **Não vingou**.
4. Mostre o pedido **DEMO Pedido de edição de Reels** em **Meus pedidos**.
   Como editor, abra o feed, candidate-se com uma mensagem fictícia e, como
   cliente, aceite. Abra a conversa do pedido em **Trampos fechados** e troque
   uma mensagem. Conclua como cliente e avalie como editor.
5. Termine em **Trampos fechados** para mostrar os estados finais e as
   avaliações. O cliente recebe a opção de avaliar após a conclusão.

## Repetição e recuperação

Na raiz do repo, rode o ensaio automatizado com duas sessões Chromium
isoladas. O navegador automatizado restringe as requisições ao site e à API
locais. O ensaio consome o cenário, então use reset entre execuções.

~~~sh
node frontend/demo/run.mjs rehearse
node frontend/demo/run.mjs reset
node frontend/demo/run.mjs rehearse
~~~

O reset recria exclusivamente a stack local da T26 com as imagens já
armazenadas, cadastra as duas contas novamente e repõe o pedido aberto.
Feche as sessões antigas antes de reiniciar a apresentação. Reabra o site
e entre nas contas de demonstração. Rode reset uma última vez antes de uma
apresentação manual. Para encerrar e descartar os dados locais, rode
node frontend/demo/run.mjs stop.

Se a rede externa falhar, use a stack local previamente preparada e mostre
o fluxo por senha e os e-mails no Mailpit. Se o vídeo autorizado precisar de
internet, pule a reprodução ou use um trecho local que a equipe também
tenha autorização para exibir. Grave com antecedência o percurso das duas
sessões, sem exibir códigos, tokens ou dados reais. Se a stack local falhar,
identifique a gravação como ensaio e continue a partir dela. Antes da
apresentação, teste o plano com a rede externa desconectada.

Se a equipe disponibilizar um candidato HTTPS, ensaie separadamente nesse
destino conforme [o runbook de deploy](DEPLOY.md). Use somente contas de
demonstração autorizadas pelas pessoas responsáveis pelo ambiente. O script
deste roteiro mantém os dados e as migrations no Docker local descartável.
A comprovação de HTTPS, SMTP externo e login social exige seus próprios
testes. O roteiro principal usa senha.
