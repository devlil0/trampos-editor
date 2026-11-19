# Limites de autenticação na T05

A API conta pedidos válidos antes de consultar a conta, verificar senha ou
enviar e-mail. Cada pedido precisa caber tanto na cota do e-mail normalizado
quanto na cota da origem. As duas reservas são feitas juntas.

| Operação | Por conta | Por origem | Janela | Intervalo entre pedidos de envio |
| --- | ---: | ---: | --- | --- |
| Login por senha | 8 | 80 | 5 minutos | — |
| Início e reenvio de cadastro, somados | 5 | 60 | 10 minutos | 60 segundos por conta |
| Pedido de recuperação de senha | 5 | 30 | 15 minutos | 60 segundos por conta |

Os valores são configuráveis em `tramposeditor.auth-limits`, com `per-account`,
`per-origin`, `window` e `cooldown` nas políticas `login`, `signup` e
`password-reset`. A janela começa na primeira tentativa admitida de cada
conta ou origem. O intervalo entre envios continua valendo quando a janela
vira. O pedido admitido ocupa a cota e o intervalo, inclusive quando não há
conta, falta uma pendência ou ocorre falha de envio. Confirmação do cadastro
continua usando o limite existente de cinco erros por código.

Ao bloquear um pedido, a API responde com `429`, `Retry-After` em segundos
arredondados para cima e `Cache-Control: no-store`. O JSON tem apenas
`{"error":"Muitas tentativas. Aguarde e tente novamente."}`. Na recuperação,
contas ativas, inativas e inexistentes recebem o mesmo `204` enquanto a
tentativa é admitida e a mesma regra de `429` quando bloqueada. O controle
não busca a conta antes de decidir. A regra existente de e-mail duplicado
no início do cadastro permanece.

## Origem e cadeia de confiança

- Por padrão, a lista `tramposeditor.auth-limits.trusted-proxies` está vazia.
  A API usa o IP do par TCP observado pelo servidor. `Forwarded` e
  `X-Forwarded-For` não participam do controle.
- Com um proxy habilitado, a API só aceita `X-Trampos-Client-IP` se o par
  TCP estiver na lista de endereços IP exatos. O header precisa conter um
  único IP literal. Header ausente, repetido ou inválido usa o IP do par.
  A lista não aceita nomes DNS nem redes amplas.
- O componente confiável deve descartar valores desse header vindos do
  cliente e escrever apenas o IP que ele próprio determinou pela cadeia de
  rede confiável. Restrinja o acesso direto ao proxy e ao Next quando eles
  forem usados como intermediários confiáveis. Só habilite a lista após
  confirmar os IPs que a API realmente observa na topologia candidata.
- `server.forward-headers-strategy` deve ser `none`. A API recusa iniciar
  com `native` ou `framework`, ou com `server.tomcat.remoteip.remote-ip-header`
  ou `protocol-header` configurado. Isso evita que o IP seja reescrito antes
  da checagem. Preserve essa fronteira ao configurar proxy e OAuth na T09.

Nos testes, o MockMvc simula separadamente o par TCP e os headers. A cota
direta permanece a mesma quando o cliente muda o header, e um proxy
permitido mantém cotas distintas para dois clientes encaminhados.

## Contrato proposto para a T16

O ponto de integração é `frontend/src/app/actions/auth.ts`. As quatro
ações devem receber a origem somente do ingresso confiável e encaminhá-la
no header dedicado, por uma rota de rede que a API possa identificar como
par confiável. O Next não deve copiar headers arbitrários do navegador.
Quando a API devolver `429`, a ação pode ler `Retry-After` e expor apenas
o número de segundos e a mensagem genérica no estado do formulário. Os
componentes envolvidos em exibir esse estado são
`frontend/src/components/auth-forms.tsx` e
`frontend/src/components/password-forms.tsx`.

Sem essa integração, o limite por conta funciona, mas pedidos enviados
pelo Next compartilham a cota por origem observada pela API. A integração
e a validação da topologia real ficam pendentes de T16/T09.

## Capacidade e operação

O controle guarda até 10.000 entradas em memória por processo, configuráveis
em `max-entries`. Entradas vencidas são removidas na próxima verificação.
Ao atingir o teto, uma tentativa que necessite de chave nova recebe
`429` até abrir espaço, preservando os contadores ainda válidos. Nenhum
e-mail em texto puro é guardado como chave do limitador.

Os contadores reiniciam com o processo e não são compartilhados entre
réplicas. Uma rede compartilhada pode atingir a cota por IP, e a janela
fixa pode admitir rajadas próximas de seus limites. Esses valores são
iniciais para uma instância e devem ser ajustados após observar o uso.
