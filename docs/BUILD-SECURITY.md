# Proteção de artefatos backend — T02

## Limites do empacotamento

O .gitignore controla o Git; ele não protege o contexto Docker nem os recursos Maven.
Por isso as duas ferramentas usam listas explícitas de arquivos permitidos.

O contexto do Dockerfile da raiz é exclusivo da API: pom.xml, Dockerfile,
.dockerignore, fontes Java e recursos de runtime selecionados. Frontend, Git,
arquivos de ambiente, configurações locais, exemplos, ferramentas locais, caches
e dependências instaladas não são enviados ao build.
O Dockerfile copia somente src/main. O frontend mantém seu próprio contexto e
.dockerignore, sem alteração nesta tarefa.

O Maven inclui apenas:
- application.yaml, application-dev.yaml e application-prod.yaml compartilhados;
- migrações SQL de db/migration, inclusive subpastas;
- email/logo-mark.png, necessário ao envio HTML.

Arquivos application-local.*, exemplos e demais arquivos fora dessa lista não entram
no JAR. Os exemplos sanitizados continuam no Git como modelos de configuração.
Novos recursos de runtime devem ser revisados e incluídos tanto no pom.xml quanto
no .dockerignore. Não coloque credenciais nos arquivos compartilhados permitidos:
a seleção de arquivos não detecta um segredo inserido em um arquivo permitido.

Credenciais de operação continuam externas ao build, fornecidas no runtime por
ambiente/configuração externa. Não use COPY, ARG ou ENV de build para secrets.
A exclusão protege builds futuros; não remove informações de imagens ou commits
antigos. Qualquer exposição real precisa de avaliação e rotação autorizada.

## Verificação reproduzível, sem produção

Usar uma cópia descartável dos arquivos versionados, acrescida das alterações da
T02. Não copiar arquivos privados, .env, configurações pessoais ou caches da máquina.

Com JDK 21 e Docker, na cópia limpa:

    .\mvnw.cmd clean verify '-Dspring.config.location=classpath:/application.yaml' '-Dspring.profiles.active=test' '-Dtramposeditor.notifications.transport=log' '-Dtramposeditor.security.jwt.secret=teste-isolado-chave-sem-uso-externo-123456789'

Esse comando compila, executa a suíte backend com banco descartável do Testcontainers
e monta o JAR. Não inicia o servidor de desenvolvimento; e-mail fica simulado.
Os relatórios ficam em target e não devem ser versionados.

Para verificar o JAR, usando o script do repositório:

    python scripts/verify_backend_jar.py caminho/para/TramposEditor-0.0.1-SNAPSHOT.jar

O script usa somente a biblioteca padrão do Python. Confere todos os recursos
necessários e rejeita recursos extras, sem exibir seu conteúdo.

Na cópia descartável, criar arquivos application-local.yaml, .yml e .properties,
arquivos de ambiente/chaves e caches com um marcador textual fictício exclusivo.
Nunca usar credenciais reais como sentinela. Reempacotar e conferir também o marcador:

    .\mvnw.cmd clean package -DskipTests
    python scripts/verify_backend_jar.py caminho/para/TramposEditor-0.0.1-SNAPSHOT.jar --marker MARCADOR_FICTICIO

O segundo build verifica empacotamento; -DskipTests não substitui o clean verify anterior.
Não inserir marcadores no checkout usado para desenvolvimento.

Para conferir o contexto de verdade, usar um Dockerfile auxiliar fora da cópia,
com FROM scratch e COPY . /, mantendo o .dockerignore original. Exportar com
docker build --file caminho/Dockerfile.context --output type=local,dest=caminho/contexto caminho/copia.
Conferir nomes e ausência do marcador nos arquivos exportados.

Construir a partir dessa cópia os alvos build e final, usando tags locais exclusivas.
Exportar com docker image save; inspecionar todas as camadas indicadas no manifest.json,
não apenas o filesystem final. Procurar os arquivos privados fictícios e o marcador,
e executar a inspeção do JAR extraído de cada estágio.
Não iniciar a API, conectar a DB/SMTP real nem publicar as imagens para essa verificação.

## Varredura de secrets

Usar Gitleaks com regras padrão, redaction total (--redact=100), saída JSON e sem
imprimir os campos Match/Secret. Rodar separado:
- git com --log-opts="--all --full-history", para histórico alcançável pelas refs locais;
- dir em snapshot apenas dos arquivos versionados e alterações propostas da T02.

O scanner pode rodar em container sem rede, com fontes somente para leitura e
relatórios em target. Não escanear arquivos privados desta máquina como se fossem
conteúdo versionado. O resultado não garante ausência de todos os tipos de segredo.
Refs remotas não obtidas e objetos não alcançáveis não fazem parte dessa cobertura.

Triar por regra, caminho, linha, commit e contexto de uso, sem copiar valores.
Não criar uma exclusão ampla para src/test nem mascarar achados reais.
Não rotacionar ou reescrever histórico automaticamente.

Nesta execução foi usado Gitleaks v8.30.1, imagem oficial:
ghcr.io/gitleaks/gitleaks@sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f

A varredura do histórico encontrou 20 alertas generic-api-key. A revisão confirmou
payloads fictícios de senha em testes de autenticação/cadastro, incluindo cenários
de duplicidade e conta social sem senha. Não foi identificada credencial real nesses
achados. Nenhuma credencial foi rotacionada e nenhum valor foi incluído neste documento.

## Resultados da execução local em 2026-11-15

- Base: fix/backend a partir de origin/develop, com a T01 incluída.
- Cópia limpa dos arquivos versionados, com as alterações T02: clean verify aprovado,
  81 testes, zero falhas/erros/ignorados. Banco descartável, sem SMTP real.
- JAR limpo: 22 recursos necessários presentes e nenhum recurso extra.
- Cópia descartável com 20 arquivos de configuração/chaves/caches fictícios:
  reempacotamento aprovado; nenhum marcador entrou no JAR.
- Contexto Docker corrigido: 149 arquivos necessários, sem arquivos privados fictícios,
  marcadores, frontend, Git, target ou caches.
- Estágios build e runtime reconstruídos com o .dockerignore corrigido:
  14 e 9 camadas inspecionadas, respectivamente; nenhum marcador encontrado.
- JAR extraído de cada estágio: os mesmos 22 recursos necessários, sem recurso extra
  ou marcador fictício.
- Gitleaks: 20 alertas no histórico e 20 no conteúdo atual. Todos triados como dados
  fictícios de senha em testes; nenhum desses achados indica credencial real.
  O scanner retorna 1 ao encontrar esses alertas; a triagem foi feita sem suprimir
  globalmente as regras e sem publicar valores.

O primeiro teste de contexto detectou que liberar pastas-pai também liberava
arquivos fictícios indevidos. A lista foi corrigida para permitir somente arquivos.
Os resultados acima de contexto/imagens são das verificações após essa correção.

Logs, relatórios redigidos, cópia descartável e exportações estão em target/t02,
ignorados pelo Git. As imagens têm apenas tags locais de teste; não foram publicadas
nem iniciadas. Nenhum arquivo frontend foi alterado, nenhuma credencial foi rotacionada
e nenhum serviço de produção foi acessado.

Limitações: a validação cobre os arquivos e marcadores testados, os artefatos produzidos
e o histórico alcançável pelas referências disponíveis. Não inspeciona imagens antigas
já distribuídas, refs não obtidas ou serviços externos. O scanner não detecta toda
categoria possível de segredo. A lista de recursos deve ser mantida quando houver
novos arquivos de runtime. A API da imagem não foi iniciada, conforme o escopo.
