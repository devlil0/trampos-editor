# Aceite, recorrência e concorrência — T19/T20

## Pedido e contratação

Cada pedido permite escolher um editor. O primeiro aceite cria a conversa
e o trampo, marca a candidatura como aceita e fecha o pedido. Fechar um
pedido mantém seu histórico; impede novas candidaturas e novos aceites,
inclusive de candidatos que chegaram antes do fechamento. Não conclui nem
cancela o trampo já registrado.

Repetir o aceite da mesma candidatura retorna o pedido existente, sem criar
outro trampo, conversa ou aviso. Isso vale depois de DONE e CANCELLED.
Aceitar outra candidatura de um pedido já preenchido retorna 409. Somente
o titular do pedido pode aceitar ou fechar. As alterações do mesmo pedido
são serializadas por bloqueio de linha, sem migração.

## Novo trabalho legítimo

Quando não há trampo OPEN na conversa, o cliente pode registrar outro
trabalho explicitamente. Esse gesto cria um registro distinto; repetir o
aceite antigo nunca serve para recontratar. Também é possível publicar um
novo pedido e aceitar nova candidatura do mesmo editor. O histórico conserva
os registros e cada link “Abrir conversa” aponta para sua conversationId.

Na T19, o cliente continua concluindo/cancelando e os dois participantes
podem avaliar um trampo DONE. Confirmação bilateral, conversa nova por
recontratação e mediação são alterações separadas da T27.

## Contato e avisos

A T19 preserva o contato por e-mail após aceite como alternativa existente,
enquanto a substituição pelo fluxo de chat/mediação é integrada na T27.
Esse contato era intencional e não foi classificado como vazamento anônimo.
Os avisos existentes de candidatura, aceite e conclusão continuam usando
a estrutura de notificações. Replay não solicita outro aviso de aceite.
Novos textos/destinatários de confirmação são alinhados na entrega separada
com Felipe, evitando dois avisos ao editor pelo mesmo evento.

## Dados anteriores e limites

Não há saneamento automático de pedidos antigos com vários aceites ou
trampos duplicados. Os registros e avaliações existentes são preservados;
eventual correção de dados exige decisão da equipe. A T20 verifica as
demais corridas de abertura/encerramento, avaliações e agregação da nota.
O teste de dois aceites simultâneos da T19 não cobre essas outras operações.

## Concorrência — T20

As operações usam transações e bloqueios de linha no PostgreSQL:

- Registrar um novo trampo bloqueia a conversa antes de verificar se existe
  OPEN. Requisições duplicadas resultam em uma criação e um 409.
- Concluir, cancelar e avaliar bloqueiam o trampo antes de ler seu estado e
  suas avaliações. Na disputa de encerramento, só uma transição é aceita;
  a outra recebe 409. Os avisos de conclusão só são solicitados pela transição
  vencedora DONE. Cada participante continua podendo avaliar uma vez.
- A avaliação é enviada ao banco por flush antes do recálculo. O perfil do
  destinatário é bloqueado antes da consulta do agregado, serializando notas
  recebidas em trabalhos distintos. Média e contagem são recalculadas das
  linhas de reviews, preservando o arredondamento existente.

O índice de um OPEN por conversa e a unicidade por autor/trampo continuam
como proteção adicional. Conflitos mantêm 409 com JSON error da T07, sem
alteração do handler, esquema ou estados públicos. Mediação não foi adicionada.

A reputação continua sendo da conta: inclui avaliações recebidas como cliente
e como editor quando essa conta possui perfil. Não foi adotado filtro por
papel; essa é uma decisão de produto futura, não um defeito presumido.

Na reprodução anterior à correção, duas transições terminais retornaram 200
e duas notas concorrentes deixaram rating_count em 1 apesar de duas reviews
commitadas. A existência dessa falha foi demonstrada agora por teste controlado,
não presumida da auditoria original. Abertura/review duplicadas já eram
protegidas pelas constraints e pela T07.

Os testes concorrentes usam requisições independentes, barreiras de leitura e
PostgreSQL descartável; comparam o cache com a consulta das avaliações após os
commits. Não representam prova de ausência de toda corrida possível. Futuras
remoções de reviews ou rotinas que recalcularem reputação devem respeitar a
mesma serialização. Não há saneamento ou recálculo em massa de dados antigos.

## Roteiro manual com duas contas

1. Pela vitrine: cliente contata editor; cliente registra, conclui e avalia
   um trampo. Na mesma conversa, registra outro e conclui/cancela. O editor
   não vê formulário de registro nem controles de conclusão.
2. Por pedido: editor se candidata e cliente aceita. Pedido fica fechado,
   sai do feed e gera um único trampo. Repetir aceite antigo, inclusive
   depois do encerramento, não cria outro registro nem aviso de aceite.
3. Com dois candidatos: aceitar um impede aceitar o outro. Com pedido
   fechado manualmente, nenhuma candidatura pendente pode ser aceita.
4. No histórico, “Abrir conversa” abre a conversa daquele registro.
5. Com outra aba desatualizada ou API indisponível, aceite/fechamento exibe
   erro recuperável. Um terceiro não pode aceitar, abrir ou concluir
   registros alheios. Testar com sessões distintas.
