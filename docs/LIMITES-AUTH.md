# Limites de autenticação na T05

A API conta pedidos válidos antes de consultar a conta, verificar senha ou
enviar e-mail. Nas operações com cota por conta, o pedido precisa caber
na cota do e-mail normalizado e na cota da origem. As duas reservas são feitas juntas.

| Operação | Por conta | Por origem | Janela | Intervalo entre pedidos de envio |
| --- | ---: | ---: | --- | --- |
| Login por senha | 8 | 80 | 5 minutos | — |
| Início e reenvio de cadastro, somados | 5 | 60 | 10 minutos | 60 segundos por conta |
| Confirmação de cadastro | — | 40 | 5 minutos | — |
| Pedido de recuperação de senha | 5 | 30 | 15 minutos | 60 segundos por conta |

Os valores são configuráveis em `tramposeditor.auth-limits`, com `per-account`,
`per-origin`, `window` e `cooldown` nas políticas `login`, `signup` e
`password-reset`. A janela começa na primeira tentativa admitida de cada
conta ou origem. O intervalo entre envios continua valendo quando a janela
vira. O pedido admitido ocupa a cota e o intervalo, inclusive quando não há
conta, falta uma pendência ou ocorre falha de envio.

Ao bloquear um pedido, a API responde com `429`, `Retry-After` em segundos
arredondados para cima e `Cache-Control: no-store`. O JSON tem apenas
`{"error":"Muitas tentativas. Aguarde e tente novamente."}`. Na recuperação,
contas ativas, inativas e inexistentes recebem o mesmo `204` enquanto a
tentativa é admitida e a mesma regra de `429` quando bloqueada. O controle
não busca a conta antes de decidir. A regra existente de e-mail duplicado
no início do cadastro permanece.

A confirmação usa `tramposeditor.auth-limits.signup-confirm.per-origin`
e `window`. A cota é independente de início e reenvio, que continuam
compartilhando seus limites. A API valida o formato do pedido e consulta
a cota antes de chamar o serviço. Um pedido admitido ocupa uma vaga da
origem mesmo quando a pendência não existe. Um `429` de volume não abre
a transação de confirmação, não consulta o banco e não altera a pendência.
Só códigos incorretos admitidos contam para os cinco erros que encerram a
pendência. Depois da janela, um código vigente ainda pode ser confirmado.

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

## Contrato proposto para T09 e T16

1. **Ingresso.** Descartar o `X-Trampos-Client-IP` que veio de fora e
   escrever um único IP literal, sem porta, obtido do par da conexão.
   Quando houver CDN ou balanceador antes do ingresso, aceitar metadados
   de origem somente pelos pares autorizados desse serviço e restringir
   o acesso por esse caminho. Remover `Forwarded` e `X-Real-IP` recebidos
   e substituir `X-Forwarded-For` pelo IP verificado. Definir `Host` e
   `X-Forwarded-Host` pelo host público aceito e `X-Forwarded-Proto`
   pelo esquema externo efetivo. Preservar `Origin` para a verificação
   das Server Actions. [NGINX Real IP](https://nginx.org/en/docs/http/ngx_http_realip_module.html)
2. **Next.** A porta do frontend deve receber tráfego somente do ingresso
   autorizado. Em `frontend/src/app/actions/auth.ts`, ler no servidor
   somente o `X-Trampos-Client-IP` emitido pelo ingresso e reenviar um
   único IP válido ao construir o `fetch` para a `API_URL` interna.
   Omitir o header quando o valor faltar ou for inválido. Aplicar a login,
   início, reenvio, confirmação e pedido de recuperação. Para `429`, ler
   `Retry-After` em segundos e manter a confirmação no passo do código.
   O bloqueio por volume deve indicar espera e preservar o formulário.
3. **API.** Manter a reescrita genérica desativada e identificar o emissor
   com `request.getRemoteAddr()`. Medir em pedidos vindos do Next e, se
   houver repasse direto, também em pedidos vindos do ingresso. Adicionar
   somente os IPs exatos medidos de emissores exclusivos. Um gateway
   compartilhado ou um endereço reutilizável sem controle não serve para
   identificar o emissor. Até isolar o caminho, manter a lista vazia.

No ensaio local, uma rede Docker dedicada a Next e API pode reservar
o IP do Next via IPAM. A rede do banco deve ficar separada e as portas
diretas devem ficar inacessíveis a clientes externos. Usar endereços do
projeto descartável apenas nesse ensaio. Recriar os serviços, conferir o
par observado e testar duas origens, headers inválidos e o `Retry-After`
na tela. Se os endereços forem dinâmicos, coordenar a atualização da lista
e retirar endereços antigos antes da reutilização.
[Redes no Docker Compose](https://docs.docker.com/compose/how-tos/networking/)

O ambiente candidato deve confirmar as redes, firewall, pares IPv4/IPv6 e
eventual NAT ou CDN. Conferir os consumidores da API pública antes de
propor qualquer restrição de rota e preservar os fluxos acordados, como
OAuth e leitura de avatar. A validação com MockMvc simula par e headers.
O ensaio completo com ingresso e Next depende da integração frontend da
T16. Até lá, os pedidos do Next compartilham a cota da origem observada.

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
