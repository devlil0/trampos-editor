# Upgrade de uma cópia legada na V11

A V12 pode parar em um empate de `created_at` ou quando o username com
sufixo coincide com outro candidato. A V13 também trata qualquer
username `tmp-*` como intermediário. O histórico da falha sem preparo
continua no [diagnóstico da T10](CI.md#falha-reproduzida-na-v12).

## Decisão

O utilitário manual da V12 usa o Flyway da aplicação com dois ganchos
explícitos na conexão da migração. Antes da V12, prepara as linhas que
afetam a geração de usernames. Logo após a V13, grava os usernames
planejados e restaura os nomes de exibição e slugs originais. A execução
agrupa as migrações pendentes na mesma transação PostgreSQL. Os ganchos
exigem uma conexão transacional e verificam que a preparação e a
restauração usam a mesma conexão. Essa garantia foi ensaiada com o Flyway
11.14.1 usado pelo projeto. Os arquivos publicados rodam normalmente
e o Flyway registra seus checksums.

Uma preparação manual em transações separadas exigiria guardar os nomes
originais e recuperar um estado intermediário após interrupções. Inserir
uma V11.x na sequência padrão ampliaria o impacto aos bancos que já
passaram dela. Uma migração posterior só pode atuar depois que V12 e V13
terminarem. O utilitário fica em `src/test/java`, fora do JAR, e
só é instalado na sessão do Flyway criada pelo comando explícito.
[Configuração de transação do Flyway](https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-group-setting)

| Estado da base | Procedimento |
| --- | --- |
| Instalação nova | Executar as migrações normais desde a base vazia. |
| V12 ou superior | Usar o fluxo normal de upgrade e preservar os usernames já publicados. Se a base estiver parada por uma falha na V13, investigar em separado a partir de uma cópia. |
| V11 | Diagnosticar uma restauração isolada. Usar o fluxo normal se o ensaio até a última migração passar e preservar o resultado esperado da V13. Para dados afetados, usar o preparo explícito apenas na cópia descartável. |

## Diagnóstico e plano

O diagnóstico usa uma transação de leitura e imprime somente contagens:
contas, grupos duplicados projetados na V12, contas nesses grupos, contas
que receberiam sufixo no plano, candidatos com prefixo `tmp-` e contas que
seriam preparadas. Zero colisões na V12 ainda exige conferir a V13 e o
resultado final. Nenhum nome, slug, username, e-mail, UUID ou mapa
individual aparece no resumo.

O plano reserva os slugs existentes. O editor conserva seu slug quando
ele seria aproveitável na V12. Nos demais empates pelo mesmo candidato,
vence a menor dupla `(created_at, UUID)`. Os candidatos principais são
reservados antes de alocar os sufixos. Se o sufixo curto colidir, o plano
tenta extensões numéricas, em ordem estável, com ajuste para o limite de
30 caracteres. Só as linhas cujo candidato precisa mudar ou que seriam
tratadas como `tmp-*` recebem um alias intermediário exclusivo. O
utilitário restaura os campos legados antes de concluir a transação.

## Execução em cópia descartável

Restaure uma cópia descartável na V11, em PostgreSQL local, com a API e
qualquer outro escritor desligados. O utilitário aceita apenas uma URL
JDBC literal em `127.0.0.1`, porta explícita, banco com prefixo
`v12_disposable_` e esquema `public`. Use credenciais locais
descartáveis pelas variáveis `V12_DB_USER` e `V12_DB_PASSWORD`, e
`V12_JDBC_URL=jdbc:postgresql://127.0.0.1:<porta>/v12_disposable_<nome>`.
A restrição da URL reduz erros de destino. Use somente uma instância que
você confirmou ser descartável, sem túnel ou encaminhamento para outra
base.

Exemplo em PowerShell, com as três variáveis definidas localmente:

```powershell
.\mvnw.cmd -B test-compile 'org.codehaus.mojo:exec-maven-plugin:3.5.0:java' `
  '-Dexec.classpathScope=test' `
  '-Dexec.mainClass=com.devlil0.tramposeditor.database.V12LegacyUpgrade' `
  '-Dexec.args=diagnose'
```

O primeiro uso pode baixar o plugin Maven indicado. Confira as contagens
e, em outra invocação do mesmo comando, use
`-Dexec.args=apply-disposable`. O modo de aplicação valida a V11 antes
de iniciar o Flyway e novamente sob bloqueio de `users` e
`editor_profiles`. O plano é recalculado sob esse bloqueio. Os dois
modos recusam histórico diferente da sequência V1 a V11. O marcador de
criação do esquema pelo Flyway é aceito quando presente. A presença de
`users.username` também bloqueia o procedimento. A aplicação também valida o histórico
pelo Flyway, compara os dados ao plano e confere a ausência de pendências.

## Integridade e recuperação

- Faça backup da cópia antes de começar e preserve-o até validar o ensaio.
  Use o histórico do Flyway para decidir o próximo passo após interrupção.
- Se voltou à V11, confirme que a coluna `username` segue ausente e
  compare o diagnóstico e os dados com o backup antes de repetir.
- Se o histórico chegou à última versão, rode `validate` com o Flyway
  normal e confira contagens, unicidade, nomes de exibição, slugs e
  redirecionamentos. A recusa em reaplicar é esperada. Se houver um estado
  intermediário ou os dados divergirem, pare e restaure outra cópia do
  backup para investigar.
- O procedimento carrega o plano em memória e bloqueia as duas tabelas
  durante o upgrade. Mede-se a viabilidade em uma cópia com volume
  representativo antes de qualquer avaliação operacional. A aprovação
  aqui é para a cópia descartável local. Mudanças na versão do Flyway ou
  no conjunto de migrações exigem repetir os testes de rollback.
