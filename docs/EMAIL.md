# E-mail de autenticação — T01

## Configuração

O cadastro por senha depende de código enviado por e-mail. A conta e a sessão só
nascem depois da confirmação. SMTP aceitar a mensagem não garante entrega na caixa
de entrada: recebimento e pasta de spam precisam de teste autorizado.

O perfil prod exige SMTP e rejeita configuração crítica ausente ou
inválida, sem mostrar valores nos erros. O modo log está disponível
somente fora de produção.

O docker-compose.prod.yml repassa ao serviço app as variáveis do .env:
- NOTIFICATIONS_TRANSPORT: smtp em produção.
- MAIL_HOST: servidor SMTP do provedor.
- MAIL_PORT: porta, padrão 587, com STARTTLS e autenticação.
- MAIL_USERNAME e MAIL_PASSWORD: credenciais SMTP, nunca versionadas.
- NOTIFICATIONS_FROM: um remetente autorizado pelo provedor.
- FRONTEND_BASE_URL: origem pública HTTPS do frontend, sem caminho, query ou fragmento.

Copie o .env.example e preencha por meio seguro. O arquivo .env é usado pelo Compose
para substituição; as variáveis precisam estar explicitamente repassadas ao container.
A validação de inicialização é local: não conecta ao provedor nem comprova credenciais.
Debug de protocolo de e-mail não deve ser habilitado em produção.
Não registre corpos, assuntos, códigos, tokens ou exceções cruas do provedor.

Os logs operacionais de envio registram apenas os campos descritos na T11.

### Capturador SMTP local

O modo `log` simula o envio e registra apenas o resultado operacional.
Para testar o cadastro, reset ou verificação pelo navegador, use o
Mailpit com endereços fictícios, como `usuario@example.test`.
Inicie o capturador em um terminal separado.

```powershell
docker run --rm --name trampos-mailpit -p 127.0.0.1:1025:1025 -p 127.0.0.1:8025:8025 axllent/mailpit:v1.27.4
```

Mantenha o banco e o frontend locais. Pare o backend de desenvolvimento
em execução com Ctrl+C e inicie-o no mesmo terminal, na raiz do projeto.

```powershell
$env:NOTIFICATIONS_TRANSPORT = 'smtp'
$env:SPRING_MAIL_HOST = '127.0.0.1'
$env:SPRING_MAIL_PORT = '1025'
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH = 'false'
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE = 'false'
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
```

A API local envia as mensagens fictícias ao capturador. Abra
`http://127.0.0.1:8025` para consultar o código ou link. Esse container
usa portas de loopback, armazenamento temporário e o SMTP padrão do
Mailpit sem encaminhamento externo. Interrompa o container com Ctrl+C
ao terminar. [Instalação com Docker](https://mailpit.axllent.org/docs/install/docker/)

## Contrato para Marcelino — T16

Mantidos os corpos e status de sucesso existentes:

| Rota POST | Sucesso | Falha detectada no envio |
|---|---|---|
| /v1/auth/register | 202, com email e expiresIn; sem sessão | 503 com error |
| /v1/auth/register/resend | 204, também sem pendência ou com pendência expirada | 503 com error quando um envio é tentado e falha |
| /v1/users/me/email-verification/resend | 204, também se já verificado | 503 com error |
| /v1/auth/password-reset/request | 204, corpo vazio | Mesmo 204, corpo vazio |

Corpo do 503:
{"error":"Não foi possível enviar o e-mail agora. Tente novamente mais tarde."}

Em 503 o frontend deve permitir nova tentativa e não afirmar envio.
Pedidos de cadastro, reenvio e recuperação também podem receber `429` com
`Retry-After` quando bloqueados pela [T05](LIMITES-AUTH.md).
Erros de validação, autorização e negócio existentes permanecem.

O reset é a exceção pública deliberada: a resposta confirma o recebimento do pedido,
sem afirmar entrega. Texto sugerido: "Se houver uma conta elegível, tentaremos enviar
as instruções. Se não receber, tente novamente mais tarde."
Esse mesmo texto deve ser usado para qualquer resposta 204; diferenciar falha somente
para conta conhecida revelaria sua existência. O backend registra a falha sem segredos.
A T01 preserva esse contrato de resposta; não promete equalizar tempos de resposta.

Envios obrigatórios de identidade propagam falha. O serviço transacional reverte
pendências/tokens alterados na tentativa malsucedida, preservando o estado anterior.
No reset, a exceção é suprimida somente no controller, depois dessa reversão.
Avisos do marketplace continuam de melhor esforço: falhar o e-mail não desfaz a ação.
A seleção dos transportes e as assinaturas públicas usadas pelo marketplace permanecem.

## Avisos e operação — T11

- Conversa, candidatura, aceite e conclusão de contratação registram o
  aviso durante a transação. Só o commit coloca dados já preparados na
  fila em memória. O executor não recebe entidades JPA nem consulta o
  banco para produzir a mensagem. O aviso de conversa mantém o filtro
  de uma notificação por ciclo de mensagens não lidas.
- Por instância, os padrões são 2 threads dedicadas e 32 avisos
  enfileirados. As propriedades `tramposeditor.notifications.notice-workers`,
  `tramposeditor.notifications.notice-queue-capacity` e
  `tramposeditor.notifications.notice-shutdown-ms` controlam esses limites.
  Fila cheia ou executor já fechado descarta o aviso e registra a
  categoria. A requisição não envia SMTP em caso de saturação.
- No encerramento, o executor recusa novos avisos e tenta escoar a fila
  por até 5 segundos. Ao esgotar a espera, descarta os avisos ainda na
  fila e solicita a interrupção das threads em execução. Interromper
  uma thread não garante cancelar I/O SMTP já iniciado. Os timeouts de
  socket e o fim do processo continuam relevantes.
- Os logs registram um UUID gerado por aviso ou envio obrigatório, tipo,
  categoria e duração em milissegundos. No envio, a duração mede a
  chamada ao transporte. Em descartes, mede o tempo desde a preparação.
  A categoria `aceito_pelo_transporte` significa que o adaptador
  retornou com sucesso. `simulado` identifica o modo log. Nenhum desses
  resultados comprova chegada à caixa de entrada.
- A fila pode perder avisos em reinício, saturação ou encerramento
  forçado. Há uma tentativa por envio, sem retentativa automática nem
  promessa de entrega única. Repetir um envio após uma resposta SMTP
  incerta pode duplicar a mensagem.
- Identidade continua síncrona e segura os bloqueios descritos no
  [fluxo de cadastro](CADASTRO-E-CODIGOS.md). Esta entrega retira o SMTP
  da requisição e da transação de marketplace. SMTP obrigatório
  continua sob o bloqueio necessário para preservar o código ou link
  anterior em caso de falha.
- Conexão, leitura e escrita têm limites separados de 5 segundos. O
  perfil de produção rejeita valores efetivos ausentes, inválidos ou
  não positivos. Sucessivas operações de rede, DNS e espera no banco
  podem alongar a requisição. Os 5 segundos não são um limite total.

## Validação isolada

Usar JDK 21 e Docker com Testcontainers. Os comandos abaixo não carregam o perfil
local privado: selecionam apenas application.yaml do classpath, perfil test, transporte
log e uma chave fictícia. O Testcontainers fornece banco descartável; testes de falha
substituem EmailSender por simulação, e o teste do adaptador SMTP intercepta send.

No PowerShell:

    .\mvnw.cmd '-Dtest=AuthControllerTest,EmailVerificationTest,PasswordResetTest,NotificationTest,RequiredEmailTest,EmailTransportTest' '-Dspring.config.location=classpath:/application.yaml' '-Dspring.profiles.active=test' '-Dtramposeditor.notifications.transport=log' '-Dtramposeditor.security.jwt.secret=teste-isolado-chave-sem-uso-externo-123456789' test

O seletor -Dtest escolhe as suítes. O comando test compila e executa os testes.
Para inspecionar códigos fictícios, use o capturador local. O modo log
simula sem imprimir as mensagens.

### Medições locais da T11

O baseline da T11 passou com 55 testes. Após a alteração, passaram
os 27 testes focados e a suíte completa do backend (`clean verify`),
com 163 testes, zero falhas, erros ou ignorados. O JAR passou na
inspeção dos 23 recursos esperados.

Com um servidor SMTP descartável em loopback e timeouts de teste de
400 ms, a leitura expirou em 404 e 414 ms nas duas execuções, quando
o servidor não enviou a saudação. A escrita expirou em 447 e 463 ms
quando o servidor aceitou o comando DATA e parou de ler. A conexão em
loopback foi estabelecida normalmente. Para a conexão, o teste de
configuração confirma o valor aplicado em produção.

Com PostgreSQL descartável e o transporte simulado, a conversa foi
gravada e a requisição terminou enquanto o worker ainda aguardava
liberação. Após a liberação, a falha simulada não desfez a gravação. A
reversão da transação não produziu aviso. Os testes de fila mantiveram
o worker ocupado de forma controlada para verificar saturação,
escoamento e interrupção no encerramento.

Na revisão final, o JAR no perfil dev foi executado com o Mailpit
v1.27.4 e PostgreSQL descartáveis, em portas de loopback. O capturador
recebeu o código de cadastro e o link de reset. A API respondeu 202 no
início do cadastro, 201 na confirmação e 204 no pedido de reset.
Destinatário e token fictícios ficaram fora da saída da API.

## Teste real pendente

Exige autorização para usar provedor, remetente/domínio e caixa de entrada de teste.
Configurar SPF/DKIM conforme instruções do provedor apenas com acesso e autorização.
Nenhum fornecedor específico é obrigatório.

Em ambiente isolado autorizado, verificar: código recebido, conta ausente antes da
confirmação, confirmação válida, links HTTPS de reset/verificação, pasta de spam e
falha com SMTP indisponível. Consultar registros de entrega sem copiar códigos/tokens.
Não alterar DNS ou produção como experimento. Credenciais não entram no Git.

Veja também os limites da T05, a atomicidade da T06 e a proteção
de artefatos da T02.
