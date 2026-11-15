# E-mail de autenticação — T01

## Configuração

O cadastro por senha depende de código enviado por e-mail. A conta e a sessão só
nascem depois da confirmação. SMTP aceitar a mensagem não garante entrega na caixa
de entrada: recebimento e pasta de spam precisam de teste autorizado.

No desenvolvimento, NOTIFICATIONS_TRANSPORT=log simula envio no console.
Nunca use esse transporte em produção. O perfil prod exige smtp e rejeita
configuração crítica ausente ou inválida, sem mostrar valores nos erros.

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

Produção desabilita o adaptador que imprime mensagens. Falhas de envio registram
somente uma mensagem operacional genérica. Não exponha conteúdo de logs privados
em relatórios ou PRs.

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
Nenhum Retry-After ou limite de frequência é introduzido pela T01 (T05).
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

## Validação isolada

Usar JDK 21 e Docker com Testcontainers. Os comandos abaixo não carregam o perfil
local privado: selecionam apenas application.yaml do classpath, perfil test, transporte
log e uma chave fictícia. O Testcontainers fornece banco descartável; testes de falha
substituem EmailSender por simulação, e o teste do adaptador SMTP intercepta send.

No PowerShell:

    .\mvnw.cmd '-Dtest=AuthControllerTest,EmailVerificationTest,PasswordResetTest,NotificationTest,RequiredEmailTest,EmailTransportTest' '-Dspring.config.location=classpath:/application.yaml' '-Dspring.profiles.active=test' '-Dtramposeditor.notifications.transport=log' '-Dtramposeditor.security.jwt.secret=teste-isolado-chave-sem-uso-externo-123456789' test

O seletor -Dtest escolhe as suítes; test compila e executa os testes. O modo log
pode mostrar códigos fictícios no ambiente de teste; em prod esse adaptador é proibido.
Não execute o Compose de produção para testar esta alteração.

## Teste real pendente

Exige autorização para usar provedor, remetente/domínio e caixa de entrada de teste.
Configurar SPF/DKIM conforme instruções do provedor apenas com acesso e autorização.
Nenhum fornecedor específico é obrigatório.

Em ambiente isolado autorizado, verificar: código recebido, conta ausente antes da
confirmação, confirmação válida, links HTTPS de reset/verificação, pasta de spam e
falha com SMTP indisponível. Consultar registros de entrega sem copiar códigos/tokens.
Não alterar DNS ou produção como experimento. Credenciais não entram no Git.

Timeouts, pós-commit e retentativas amplas pertencem à T11; limites à T05;
atomicidade concorrente à T06; proteção de artefatos à T02.