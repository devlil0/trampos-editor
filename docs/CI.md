# CI e validação da T10

A CI roda em pull requests com destino a `develop`. Os checks
`Backend (Java 21)` e `Frontend (Node 24)` são independentes, têm nomes
estáveis e executam em runners Linux hospedados pelo GitHub. O workflow
usa permissões de leitura, não mantém a credencial do checkout no Git
e não usa credenciais de produção nem o evento `pull_request_target`.

## Backend

Com Java 21, Docker disponível e Python 3, execute em uma cópia limpa.
No PowerShell, use `.\mvnw.cmd` e passe as opções na mesma linha.

```sh
bash ./mvnw -B clean verify \
  -Dspring.config.location=classpath:/application.yaml \
  -Dspring.profiles.active=test \
  -Dtramposeditor.notifications.transport=log \
  -Dtramposeditor.security.jwt.secret=ci-chave-ficticia-sem-uso-externo-para-testes
python scripts/verify_backend_jar.py target/TramposEditor-0.0.1-SNAPSHOT.jar
```

O Testcontainers fornece o PostgreSQL descartável. O comando usa apenas
a configuração compartilhada do classpath, com transporte simulado.
Não precisa de banco, SMTP ou OAuth externos. A CI falha se o Docker
estiver indisponível, se os testes falharem, se nenhum teste rodar ou se
algum for ignorado. O artefato `backend-surefire` fica disponível por 14
dias e contém só o JSON com os totais de testes, ignorados, falhas e
erros. A CI tenta gerar o resumo também em execuções que falham, a
partir dos resultados que o Maven tiver gravado. XMLs brutos,
propriedades da JVM, mensagens de teste e logs não fazem parte do artefato.

`FlywayMigrationTest` cobre a instalação em banco vazio e a atualização
de V11 até a última migração, com contas, papéis, perfil e vínculo social
sintéticos. Verifica o histórico do Flyway, a resolução de colisões de
username, preservação de vínculos e os valores de verificação de e-mail
e versão da sessão. As fixtures estão em `src/test/resources/migration`.

### Falha reproduzida na V12

Dois usuários da V11 chamados `Ana Café`, com o mesmo `created_at`,
geram `ana-cafe`. A V12 só acrescenta um sufixo se houver outro
registro com data estritamente anterior. Com o empate, ela tenta criar
`uk_users_username` com valores duplicados e o PostgreSQL retorna
`23505`. O rollback foi verificado. O banco fica na V11, com as cinco
contas da fixture e sem a coluna parcialmente adicionada.

Para reproduzir em PostgreSQL descartável, adicione
`-Dtest=FlywayMigrationTest#documentaLimiteConhecidoDaV12ComHomonimosNaMesmaData`
ao comando Maven.

O workflow executa esse teste no `clean verify`. O teste só passa se o
Flyway falhar com `23505` na restrição esperada e o banco permanecer
intacto na V11. Um check verde confirma que o defeito foi reproduzido
e que houve rollback. **O upgrade dos dados afetados continua bloqueado.**
O outro teste chega à V19 com a fixture de datas distintas. A CI usa
bancos descartáveis e não avalia a base que será atualizada.

#### Pré-verificação em cópia restaurada

Se a V12 estiver pendente, restaure um backup em ambiente isolado e leve
a **cópia** até a V11, sem iniciar a API. A V12 usa o slug de editor com
até 30 caracteres ou deriva o nome de exibição. Ela substitui nomes
vazios por um identificador curto e acrescenta sufixo apenas quando há
um candidato igual com data anterior. A consulta abaixo projeta o
resultado dessas etapas e conta os usernames que ainda estariam duplicados.
Ela também encontra colisões que envolvam o sufixo, sem precisar listar
nomes, endereços de e-mail ou identificadores de contas.

Salve a consulta como `preflight-v12.sql` para executar com `psql` no
mesmo esquema usado pelo Flyway na cópia da V11.

```sql
WITH sementes AS (
    SELECT u.id, u.created_at,
           COALESCE(
               e.slug,
               LEFT(TRIM(BOTH '-' FROM REGEXP_REPLACE(
                   LOWER(TRANSLATE(u.display_name,
                       'áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ',
                       'aaaaaeeeeiiiiooooouuuucnAAAAAEEEEIIIIOOOOOUUUUCN')),
                   '[^a-z0-9]+', '-', 'g')), 30)
           ) AS semente
    FROM users u
    LEFT JOIN editor_profiles e ON e.user_id = u.id AND LENGTH(e.slug) <= 30
),
candidatos AS (
    SELECT id, created_at,
           CASE WHEN semente IS NULL OR semente = '' THEN 'u-' || LEFT(id::TEXT, 8)
                ELSE semente
           END AS candidato
    FROM sementes
),
ordenados AS (
    SELECT id, created_at, candidato,
           MIN(created_at) OVER (PARTITION BY candidato) AS primeira_data
    FROM candidatos
),
projetados AS (
    SELECT CASE WHEN created_at > primeira_data
                THEN LEFT(candidato, 21) || '-' || LEFT(id::TEXT, 8)
                ELSE candidato
           END AS username_previsto
    FROM ordenados
),
duplicados AS (
    SELECT COUNT(*) AS contas
    FROM projetados
    GROUP BY username_previsto
    HAVING COUNT(*) > 1
)
SELECT COUNT(*) AS grupos_duplicados, COALESCE(SUM(contas), 0) AS contas_afetadas
FROM duplicados;
```

Validei a consulta em três esquemas V11 descartáveis e conferi o
resultado executando a V12 em cada um. A fixture compatível retornou
`0|0` e migrou. O empate de datas e uma colisão com um username já
terminado no sufixo retornaram `1|2` e foram recusados pela V12.

Configure o serviço de conexão `restauracao_v11` para apontar somente
para a cópia, com uma conta de leitura e o esquema correto. Use o
resultado como condição para avançar o procedimento de upgrade.

```sh
if ! resumo=$(PGSERVICE=restauracao_v11 PGOPTIONS='-c default_transaction_read_only=on' \
    psql -X -v ON_ERROR_STOP=1 -At -f preflight-v12.sql); then
  echo 'Pré-verificação falhou. Upgrade bloqueado.' >&2
  exit 1
fi
if [ "$resumo" != '0|0' ]; then
  echo "Upgrade bloqueado. Grupos duplicados | contas afetadas: $resumo" >&2
  exit 1
fi
```

Mantenha a nova versão da API parada enquanto o upgrade não estiver
liberado, pois ela executa o Flyway na inicialização. Qualquer colisão,
erro de conexão/consulta ou resultado inesperado bloqueia o avanço.
Com `0|0`, ensaie o upgrade completo em outra cópia do mesmo backup.
Se o banco de origem mudar após o backup, produza e valide uma cópia
atualizada dentro da janela de manutenção antes de autorizar o upgrade.

Se houver colisões, mantenha o bloqueio até aprovar e testar um passo
de preparação de dados anterior à V12, com desempate determinístico e
revisão do impacto nos endereços públicos. Uma migração posterior à V19
não alcança um banco que falha na V12. O SQL e o checksum das migrações
publicadas foram preservados e nenhum dado compartilhado foi alterado.


## Frontend

Execute na pasta `frontend` em um clone limpo, com Node 24 e as URLs
fictícias configuradas no workflow. O build precisa de acesso ao registro
npm e ao Google Fonts. A validação de tipos gera os tipos de rota antes
de rodar o TypeScript.

```sh
npm ci
npm run lint
npm exec --no -- next typegen
npm exec --no -- tsc --noEmit
npm run build
```

A suíte ponta a ponta com dois contextos continua na T25. O lint e o
build aqui verificam o código do frontend em cada PR.

## Triagem de dependências e imagens

Em 19/11/2026, o Trivy 0.76.2 analisou as imagens locais do ensaio da T09
em Linux/amd64. O scanner baixou primeiro apenas as bases públicas.
A base de vulnerabilidades era de 19/11 às 13:02 UTC e o índice Java de
19/11 às 00:58 UTC. As análises dos tarballs rodaram com a rede desligada,
sem enviar código ou imagens. As três imagens tinham Alpine 3.24.4.

| Imagem local | ID de configuração da imagem | Crítica | Alta | Média | Baixa | Desconhecida |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| `trampos-t09-api:local` | `68bb7248e608` | 7 | 12 | 28 | 0 | 3 |
| `trampos-t09-frontend:local` | `32f55c44f3ab` | 0 | 6 | 23 | 1 | 0 |
| `postgres:16-alpine` | `3cbd775daa28` | 1 | 26 | 41 | 3 | 1 |

A análise do `app.jar` da API reconheceu 115 pacotes Java, incluindo
Spring Boot 4.0.7, e apontou 31 ocorrências. O restante das ocorrências
da API veio dos pacotes do Alpine. A tabela abaixo registra a prioridade
da triagem e as versões de correção indicadas na mesma série, quando
disponíveis. Cada ocorrência é uma associação entre alerta e pacote,
por isso um CVE pode aparecer em mais de uma dependência.

| Onde | Pacote analisado | Versão | Achados prioritários | Correção indicada pelo scanner |
| --- | --- | --- | --- | --- |
| API | `tomcat-embed-core` | 11.0.22 | 5 críticos e 1 alto. CVE-2026-55276, CVE-2026-65182, CVE-2026-65905, CVE-2026-68525, CVE-2026-65637 e CVE-2026-53404 | 11.0.23 para os primeiros alertas corrigidos e 11.0.25 para cobrir o conjunto |
| API | `spring-webmvc` | 7.0.8 | 2 críticos. CVE-2026-47884 e CVE-2026-47890 | 7.0.9 |
| API | Jackson Core e Databind (`com.fasterxml.jackson`) | 2.21.4 | 5 altos | 2.21.7 |
| API | Jackson Core e Databind (`tools.jackson`) | 3.1.4 | 5 altos | 3.1.7 |
| API | Driver PostgreSQL | 42.7.11 | 1 alto. CVE-2026-54291 | 42.7.12 |
| Frontend | `brace-expansion` embarcado no npm da imagem | 5.0.9 | 2 altos. CVE-2026-102276 e CVE-2026-102278 | 5.0.11 |
| Frontend | `undici` embarcado no npm da imagem | 6.28.0 | 4 altos. CVE-2026-19534, CVE-2026-109383, CVE-2026-109869 e CVE-2026-87018 | 6.29.1 |
| PostgreSQL | Biblioteca padrão Go no binário auxiliar `gosu` 1.19.0 | Go 1.24.6 | 1 crítico e 26 altos. O crítico é CVE-2025-68121 | Go 1.24.13 para o crítico. O conjunto inclui alertas com correção listada apenas em séries posteriores, até 1.26.11 ou 1.27.4 |

O [inventário consolidado da T10](T10-VULNERABILIDADES.md) contém os
avisos por componente, as fontes e os SHA-256 completos. A varredura
isolada do JAR confirmou as mesmas 31 associações do JAR interno da
imagem da API. São 112 avisos distintos em 131 associações
aviso/componente/versão, após também consolidar pacotes iguais
encontrados em mais de uma imagem.

Os relatórios completos e a tabela por ocorrência ficam localmente em
`target/t10/trivy-*.json` e `target/t10/triagem.tsv`. Para repetir a
varredura, use `trivy image --download-db-only` e
`trivy image --download-java-db-only`, exporte cada imagem com
`docker image save` e rode o scanner sem rede com
`trivy image --offline-scan --skip-db-update --skip-java-db-update --disable-telemetry --scanners vuln --format json --input imagem.tar`.

Priorize a análise dos críticos da API e avalie uma revisão do conjunto
de dependências gerenciado pelo Spring Boot. Para os alertas no npm e
no `gosu`, procure imagens base que já tragam os binários corrigidos e
confirme com nova varredura. Os alertas identificam versões afetadas.
O ensaio não verificou se cada condição descrita nos avisos se aplica
ao TramposEditor. Nenhuma correção de dependência ou imagem foi aplicada.
A triagem local é registrada à parte do resultado obrigatório de build
e testes.


## Resultados locais de 19/11/2026

- Backend em cópia limpa, Java 21 e PostgreSQL 16.16 descartável.
  `clean verify` passou com 157 testes, zero falhas, erros ou ignorados.
  Os 23 recursos esperados do JAR estavam presentes, sem recursos extras.
  As 107 bibliotecas empacotadas coincidiram com o inventário da imagem
  da API analisada pelo Trivy.
- Frontend em cópia limpa com Node 24.22.3 e npm 11.19.1.
  Instalação pelo lockfile, lint, geração de tipos de rota, TypeScript
  e build passaram.
- O workflow passou no actionlint 1.7.12. A primeira execução no GitHub
  e a configuração dos checks obrigatórios continuam pendentes.


## Primeira execução no GitHub

1. No PR da T10 para `develop`, confira se os dois checks aparecem e se
   o resultado do backend inclui os testes de migração e a inspeção do
   JAR. Abra o artefato `backend-surefire` e confira os totais. O lint
   estático do YAML e os testes locais não substituem essa execução.
2. Com acesso administrativo, confirme que o GitHub Actions e as ações
   oficiais usadas pelo workflow estão permitidos. Depois de verificar
   os nomes e a origem dos checks na primeira execução, torne
   `Backend (Java 21)` e `Frontend (Node 24)` obrigatórios na regra de
   `develop`.
3. Confira o comportamento em um PR com check reprovado antes de
   considerar a regra ativa. A configuração fica nas regras do GitHub
   e não é aplicada ao adicionar o arquivo YAML.
