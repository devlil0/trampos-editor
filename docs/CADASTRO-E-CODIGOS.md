# Cadastro e códigos na T06

## Contrato para a T16

- O início do cadastro retorna `202`. A conta é criada quando a confirmação
  retorna `201`. O código vale dez minutos e a quinta tentativa incorreta
  encerra a pendência.
- Ao receber o `409` genérico de código inválido ou expirado, a tela pode
  oferecer **Reenviar código** e **Voltar ao cadastro**. Use o código mais
  recente. Se a pendência tiver expirado ou sido encerrada pela quinta
  tentativa, é preciso iniciar o cadastro novamente.
- O reenvio retorna `204` também quando já não existe pendência. A tela pode
  dizer que, se houver uma pendência válida, um novo código será enviado.
  Se o e-mail não chegar, mantenha a opção de voltar ao formulário. A resposta
  genérica evita revelar se um endereço tem cadastro pendente.
- Para `429`, respeite o `Retry-After` da T05. Para `503` de início ou reenvio,
  indique falha no envio e permita uma tentativa posterior. Em caso de falha no
  reenvio, o código anterior continua válido pelo prazo e pelas tentativas
  que ainda tinha.
- A disponibilidade continua retornando `200` com `available` e `message`.
  Se o nome estiver reservado por outro cadastro vigente, mostre a mensagem e
  deixe a pessoa escolher outro. Se ela estiver retomando o próprio cadastro,
  pode manter o nome: o início do cadastro identifica sua pendência pelo
  e-mail, enquanto a consulta pública recebe somente o nome. Na retomada,
  deixe a requisição de início confirmar se a pessoa pode manter o nome.
- Links de reset e verificação só podem ser usados uma vez. Quando um link
  falha por ter expirado ou já ter sido usado, direcione para pedir outro.
  A solicitação de reset mantém a resposta genérica da T05.

## Transações e envio

Cadastro e confirmação obtêm o bloqueio do nome antes de bloquear a
pendência. Reenvio bloqueia somente a pendência. A limpeza de vencidos
processa até 100 linhas em uma transação separada e pula as que estiverem
bloqueadas. A disponibilidade consulta contas e reservas vigentes em uma
única leitura do PostgreSQL, inclusive na transição de pendência para conta.
As operações de escrita revalidam o nome no banco. Emissão e consumo dos links de identidade
bloqueiam a conta antes de alterar o token.

Se a confirmação concluir primeiro, remove a pendência e o reenvio termina
sem mandar outro e-mail. Se o reenvio concluir primeiro, o código anterior
deixa de valer. Enquanto a transação do reenvio está aberta, a confirmação
aguarda o resultado.

Os envios obrigatórios permanecem síncronos dentro da transação. O início
do cadastro segura o nome reservado durante o envio. O reenvio segura a
pendência. A emissão dos links de reset e verificação segura a conta.
Pedidos que dependem desses mesmos bloqueios podem aguardar o SMTP.
Se o envio falhar, a transação reverte e preserva o código ou link anterior
quando houver um. A limpeza geral libera suas linhas antes de enviar.

A configuração de SMTP define 5 segundos para conexão, 5 para leitura e 5
para escrita. Cada valor se aplica a uma operação de rede. A requisição
pode demorar mais, somando espera pelo banco e várias operações no SMTP.

Na T11, medir o impacto da espera e avaliar como reduzir o tempo de
bloqueio, mantendo a resposta de falha e a validade do código ou link
anterior quando o envio falhar.
