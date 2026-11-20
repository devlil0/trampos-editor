# T10 — Inventário de avisos de vulnerabilidade

Varredura local e offline com Trivy 0.76.2, em 19/11/2026. A versão corrigida
vem do aviso indicado pelo scanner. A presença de um componente afetado
no artefato não confirma que a condição descrita no aviso se aplica ao
produto. Nenhuma atualização foi feita.

## Artefatos e digests

| Referência | Artefato analisado | Identificador SHA-256 |
| --- | --- | --- |
| API | `trampos-t09-api:local` | `sha256:68bb7248e60845dfa54d43412aa601cfd5951720502a29dee75e98abd4638bdc` |
| Frontend | `trampos-t09-frontend:local` | `sha256:32f55c44f3ab49d33874784e12ff3052ea2e20ed0c019c2536042528dbaf2b71` |
| PostgreSQL | `postgres:16-alpine` | `sha256:3cbd775daa28461ef484ed88ca1e5a5a72ff126e196deeebce6fec764edf1200` |
| JAR | `TramposEditor-0.0.1-SNAPSHOT.jar` | `sha256:725cb63b586cf95679bccaa7daf4442a3e47feedcf1c80e5440a3928b1e5d29b` |

Para as imagens, o SHA-256 identifica a configuração recuperada do
arquivo analisado. Para o JAR isolado, identifica os bytes do arquivo.
`API (JAR interno)` identifica o `/app/app.jar` da imagem da API.
As imagens analisadas são Linux/amd64.

## Contagem

- Relatórios brutos: 50 ocorrências na API, 30 no frontend, 72 no PostgreSQL e 31 no JAR isolado.
- As 31 associações do JAR isolado também aparecem no JAR interno da API. Cada uma aparece em uma linha, com ambos os artefatos.
- Após consolidar também os pacotes iguais em imagens diferentes, há **131 associações** de aviso, componente e versão e **112 avisos distintos** (111 CVE e 1 GHSA sem CVE no relatório).
  Um aviso que afeta componentes distintos pode ocupar mais de uma linha.
  CVE e GHSA identificados como aliases contam como um aviso.
- A gravidade apresentada é a maior encontrada pelo Trivy para a associação.
  A classificação publicada pelo mantenedor pode ser diferente. Por
  exemplo, o Spring classifica CVE-2026-47884 como média e
  CVE-2026-47890 como baixa. O Apache classifica CVE-2026-55276 e
  CVE-2026-65905 como baixas.
  [Spring CVE-2026-47884](https://spring.io/security/cve-2026-47884/) · [Spring CVE-2026-47890](https://spring.io/security/cve-2026-47890/) · [Apache Tomcat 11](https://tomcat.apache.org/security-11.html)

## Avisos por componente

| CVE / GHSA | Gravidade no Trivy | Componente | Versão encontrada | Artefatos | Versão corrigida indicada | Fonte do aviso |
| --- | --- | --- | --- | --- | --- | --- |
| CVE-2025-68121 | Crítica | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.13, 1.25.7, 1.26.0-rc.3 | [GO-2026-4337](https://pkg.go.dev/vuln/GO-2026-4337) |
| CVE-2026-47884 / GHSA-pc63-qcmh-9cmg | Crítica | `org.springframework:spring-webmvc` | `7.0.8` | API (JAR interno), JAR | 7.0.9 | [GitHub Advisory](https://github.com/advisories/GHSA-pc63-qcmh-9cmg) · [Spring](https://spring.io/security/cve-2026-47884) |
| CVE-2026-47890 / GHSA-j9f9-w8pj-32f8 | Crítica | `org.springframework:spring-webmvc` | `7.0.8` | API (JAR interno), JAR | 7.0.9 | [GitHub Advisory](https://github.com/advisories/GHSA-j9f9-w8pj-32f8) · [Spring](https://spring.io/security/cve-2026-47890) |
| CVE-2026-55276 / GHSA-mqg3-r7h5-24x4 | Crítica | `org.apache.tomcat.embed:tomcat-embed-core` | `11.0.22` | API (JAR interno), JAR | 11.0.23, 10.1.56, 9.0.119 | [Apache Tomcat](https://tomcat.apache.org/security-11.html) · [GitHub Advisory](https://github.com/advisories/GHSA-mqg3-r7h5-24x4) |
| CVE-2026-65182 / GHSA-gcx9-497g-6cp6 | Crítica | `org.apache.tomcat.embed:tomcat-embed-core` | `11.0.22` | API (JAR interno), JAR | 11.0.25, 10.1.58, 9.0.121 | [Apache Tomcat](https://tomcat.apache.org/security-11.html) · [GitHub Advisory](https://github.com/advisories/GHSA-gcx9-497g-6cp6) |
| CVE-2026-65637 / GHSA-f525-44xv-f2qj | Crítica | `org.apache.tomcat.embed:tomcat-embed-core` | `11.0.22` | API (JAR interno), JAR | 11.0.25, 10.1.59, 9.0.121 | [Apache Tomcat](https://tomcat.apache.org/security-11.html) · [GitHub Advisory](https://github.com/advisories/GHSA-f525-44xv-f2qj) |
| CVE-2026-65905 / GHSA-9xv2-5v5q-p794 | Crítica | `org.apache.tomcat.embed:tomcat-embed-core` | `11.0.22` | API (JAR interno), JAR | 11.0.25, 10.1.58, 9.0.121 | [Apache Tomcat](https://tomcat.apache.org/security-11.html) · [GitHub Advisory](https://github.com/advisories/GHSA-9xv2-5v5q-p794) |
| CVE-2026-68525 / GHSA-h3x4-894j-xpx5 | Crítica | `org.apache.tomcat.embed:tomcat-embed-core` | `11.0.22` | API (JAR interno), JAR | 11.0.25, 10.1.58, 9.0.121 | [Apache Tomcat](https://tomcat.apache.org/security-11.html) · [GitHub Advisory](https://github.com/advisories/GHSA-h3x4-894j-xpx5) |
| CVE-2025-61726 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.12, 1.25.6 | [GO-2026-4341](https://pkg.go.dev/vuln/GO-2026-4341) |
| CVE-2025-61729 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.11, 1.25.5 | [GO-2025-4155](https://pkg.go.dev/vuln/GO-2025-4155) |
| CVE-2026-102276 / GHSA-6j4f-fj2g-mc7p | Alta | `brace-expansion` | `5.0.9` | Frontend (npm) | 5.0.10, 3.0.7, 2.1.5, 1.1.19 | [GitHub Advisory](https://github.com/advisories/GHSA-6j4f-fj2g-mc7p) |
| CVE-2026-102278 / GHSA-qhr7-859c-m2p7 | Alta | `brace-expansion` | `5.0.9` | Frontend (npm) | 5.0.11, 3.0.8, 2.1.6, 1.1.20 | [GitHub Advisory](https://github.com/advisories/GHSA-qhr7-859c-m2p7) |
| CVE-2026-109383 / GHSA-2m5v-pch6-74hx | Alta | `undici` | `6.28.0` | Frontend (npm) | 6.29.1, 7.30.1, 8.12.2 | [GitHub Advisory](https://github.com/advisories/GHSA-2m5v-pch6-74hx) |
| CVE-2026-109869 / GHSA-xjj6-f33q-929c | Alta | `undici` | `6.28.0` | Frontend (npm) | 6.29.1, 7.30.1, 8.12.2 | [GitHub Advisory](https://github.com/advisories/GHSA-xjj6-f33q-929c) |
| CVE-2026-111943 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.26.10, 1.27.3 | [GO-2026-6809](https://pkg.go.dev/vuln/GO-2026-6809) |
| CVE-2026-19534 / GHSA-rfgv-xxqx-mfg5 | Alta | `undici` | `6.28.0` | Frontend (npm) | 6.28.1, 7.29.1, 8.10.2 | [GitHub Advisory](https://github.com/advisories/GHSA-rfgv-xxqx-mfg5) |
| CVE-2026-25679 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.8, 1.26.1 | [GO-2026-4601](https://pkg.go.dev/vuln/GO-2026-4601) |
| CVE-2026-27145 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.11, 1.26.4 | [GO-2026-5037](https://pkg.go.dev/vuln/GO-2026-5037) |
| CVE-2026-32280 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.9, 1.26.2 | [GO-2026-4947](https://pkg.go.dev/vuln/GO-2026-4947) |
| CVE-2026-32281 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.9, 1.26.2 | [GO-2026-4946](https://pkg.go.dev/vuln/GO-2026-4946) |
| CVE-2026-32283 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.9, 1.26.2 | [GO-2026-4870](https://pkg.go.dev/vuln/GO-2026-4870) |
| CVE-2026-33811 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.10, 1.26.3 | [GO-2026-4981](https://pkg.go.dev/vuln/GO-2026-4981) |
| CVE-2026-33814 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.10, 1.26.3 | [GO-2026-4918](https://pkg.go.dev/vuln/GO-2026-4918) |
| CVE-2026-33818 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.13, 1.26.6, 1.27.0-rc.3 | [GO-2026-5972](https://pkg.go.dev/vuln/GO-2026-5972) |
| CVE-2026-39820 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.10, 1.26.3 | [GO-2026-4986](https://pkg.go.dev/vuln/GO-2026-4986) |
| CVE-2026-39821 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.13, 1.26.6, 1.27.0-rc.3 | [GO-2026-5026](https://pkg.go.dev/vuln/GO-2026-5026) |
| CVE-2026-39822 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.12, 1.26.5, 1.27.0-rc.2 | [GO-2026-4970](https://pkg.go.dev/vuln/GO-2026-4970) |
| CVE-2026-39836 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.10, 1.26.3 | [GO-2026-4971](https://pkg.go.dev/vuln/GO-2026-4971) |
| CVE-2026-42499 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.10, 1.26.3 | [GO-2026-4977](https://pkg.go.dev/vuln/GO-2026-4977) |
| CVE-2026-42504 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.11, 1.26.4 | [GO-2026-5038](https://pkg.go.dev/vuln/GO-2026-5038) |
| CVE-2026-53404 / GHSA-h792-v28v-ppgr | Alta | `org.apache.tomcat.embed:tomcat-embed-core` | `11.0.22` | API (JAR interno), JAR | 11.0.23, 10.1.56, 9.0.119 | [GitHub Advisory](https://github.com/advisories/GHSA-h792-v28v-ppgr) |
| CVE-2026-54291 / GHSA-j92g-9f8w-j867 | Alta | `org.postgresql:postgresql` | `42.7.11` | API (JAR interno), JAR | 42.7.12 | [GitHub Advisory](https://github.com/advisories/GHSA-j92g-9f8w-j867) |
| CVE-2026-56853 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.13, 1.26.6, 1.27.0-rc.3 | [GO-2026-6089](https://pkg.go.dev/vuln/GO-2026-6089) |
| CVE-2026-56858 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.13, 1.26.6, 1.27.0-rc.3 | [GO-2026-6091](https://pkg.go.dev/vuln/GO-2026-6091) |
| CVE-2026-56859 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.13, 1.26.6, 1.27.0-rc.3 | [GO-2026-6088](https://pkg.go.dev/vuln/GO-2026-6088) |
| CVE-2026-56860 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.13, 1.26.6, 1.27.0-rc.3 | [GO-2026-6218](https://pkg.go.dev/vuln/GO-2026-6218) |
| CVE-2026-56862 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.13, 1.26.6, 1.27.0-rc.3 | [GO-2026-6090](https://pkg.go.dev/vuln/GO-2026-6090) |
| CVE-2026-68497 / GHSA-q4xh-88c3-wmh7 | Alta | `com.fasterxml.jackson.core:jackson-databind` | `2.21.4` | API (JAR interno), JAR | 2.18.10, 2.21.6, 2.22.2 | [GitHub Advisory](https://github.com/advisories/GHSA-q4xh-88c3-wmh7) |
| CVE-2026-68497 / GHSA-q4xh-88c3-wmh7 | Alta | `tools.jackson.core:jackson-databind` | `3.1.4` | API (JAR interno), JAR | 3.2.2, 3.1.6 | [GitHub Advisory](https://github.com/advisories/GHSA-q4xh-88c3-wmh7) |
| CVE-2026-78667 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.26.9, 1.27.2 | [GO-2026-6609](https://pkg.go.dev/vuln/GO-2026-6609) |
| CVE-2026-78669 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.26.9, 1.27.2 | [GO-2026-6611](https://pkg.go.dev/vuln/GO-2026-6611) |
| CVE-2026-87018 / GHSA-qrxm-fqhp-jwvm | Alta | `undici` | `6.28.0` | Frontend (npm) | 6.29.1, 7.30.1, 8.12.2 | [GitHub Advisory](https://github.com/advisories/GHSA-qrxm-fqhp-jwvm) |
| CVE-2026-89407 / GHSA-p6pp-m3f8-5c89 | Alta | `com.fasterxml.jackson.core:jackson-core` | `2.21.4` | API (JAR interno), JAR | 2.18.11, 2.21.7, 2.22.3 | [GitHub Advisory](https://github.com/advisories/GHSA-p6pp-m3f8-5c89) |
| CVE-2026-89407 / GHSA-p6pp-m3f8-5c89 | Alta | `tools.jackson.core:jackson-core` | `3.1.4` | API (JAR interno), JAR | 3.1.7, 3.2.2 | [GitHub Advisory](https://github.com/advisories/GHSA-p6pp-m3f8-5c89) |
| CVE-2026-89425 / GHSA-7hhh-6rmp-j9qf | Alta | `com.fasterxml.jackson.core:jackson-core` | `2.21.4` | API (JAR interno), JAR | 2.21.7, 2.22.3, 2.18.11 | [GitHub Advisory](https://github.com/advisories/GHSA-7hhh-6rmp-j9qf) |
| CVE-2026-89425 / GHSA-7hhh-6rmp-j9qf | Alta | `tools.jackson.core:jackson-core` | `3.1.4` | API (JAR interno), JAR | 3.1.7, 3.2.3 | [GitHub Advisory](https://github.com/advisories/GHSA-7hhh-6rmp-j9qf) |
| CVE-2026-91776 / GHSA-wv8q-qhhj-9h54 | Alta | `com.fasterxml.jackson.core:jackson-databind` | `2.21.4` | API (JAR interno), JAR | 2.18.11, 2.21.7, 2.22.3 | [GitHub Advisory](https://github.com/advisories/GHSA-wv8q-qhhj-9h54) |
| CVE-2026-91776 / GHSA-wv8q-qhhj-9h54 | Alta | `tools.jackson.core:jackson-databind` | `3.1.4` | API (JAR interno), JAR | 3.1.7, 3.2.3 | [GitHub Advisory](https://github.com/advisories/GHSA-wv8q-qhhj-9h54) |
| CVE-2026-91777 / GHSA-cxp5-3px4-pw24 | Alta | `com.fasterxml.jackson.core:jackson-databind` | `2.21.4` | API (JAR interno), JAR | 2.21.7, 2.18.11, 2.22.3 | [GitHub Advisory](https://github.com/advisories/GHSA-cxp5-3px4-pw24) |
| CVE-2026-91777 / GHSA-cxp5-3px4-pw24 | Alta | `tools.jackson.core:jackson-databind` | `3.1.4` | API (JAR interno), JAR | 3.1.7, 3.2.3 | [GitHub Advisory](https://github.com/advisories/GHSA-cxp5-3px4-pw24) |
| CVE-2026-97031 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.26.9, 1.27.2 | [GO-2026-6607](https://pkg.go.dev/vuln/GO-2026-6607) |
| CVE-2026-97054 | Alta | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.26.11, 1.27.4 | [GO-2026-6859](https://pkg.go.dev/vuln/GO-2026-6859) |
| CVE-2025-47912 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.8, 1.25.2 | [GO-2025-4010](https://pkg.go.dev/vuln/GO-2025-4010) |
| CVE-2025-58183 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.8, 1.25.2 | [GO-2025-4014](https://pkg.go.dev/vuln/GO-2025-4014) |
| CVE-2025-58185 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.8, 1.25.2 | [GO-2025-4011](https://pkg.go.dev/vuln/GO-2025-4011) |
| CVE-2025-58187 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.9, 1.25.3 | [GO-2025-4007](https://pkg.go.dev/vuln/GO-2025-4007) |
| CVE-2025-58188 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.8, 1.25.2 | [GO-2025-4013](https://pkg.go.dev/vuln/GO-2025-4013) |
| CVE-2025-58189 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.8, 1.25.2 | [GO-2025-4008](https://pkg.go.dev/vuln/GO-2025-4008) |
| CVE-2025-61723 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.8, 1.25.2 | [GO-2025-4009](https://pkg.go.dev/vuln/GO-2025-4009) |
| CVE-2025-61724 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.8, 1.25.2 | [GO-2025-4015](https://pkg.go.dev/vuln/GO-2025-4015) |
| CVE-2025-61725 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.8, 1.25.2 | [GO-2025-4006](https://pkg.go.dev/vuln/GO-2025-4006) |
| CVE-2025-61727 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.11, 1.25.5 | [GO-2025-4175](https://pkg.go.dev/vuln/GO-2025-4175) |
| CVE-2025-61728 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.12, 1.25.6 | [GO-2026-4342](https://pkg.go.dev/vuln/GO-2026-4342) |
| CVE-2025-61730 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.12, 1.25.6 | [GO-2026-4340](https://pkg.go.dev/vuln/GO-2026-4340) |
| CVE-2026-101910 / GHSA-2vr4-cq9g-pvrc | Média | `ip-address` | `10.5.0` | Frontend (npm) | 10.5.1 | [GitHub Advisory](https://github.com/advisories/GHSA-2vr4-cq9g-pvrc) |
| CVE-2026-101911 / GHSA-h3mg-xc3c-68pw | Média | `ip-address` | `10.5.0` | Frontend (npm) | 10.7.1 | [GitHub Advisory](https://github.com/advisories/GHSA-h3mg-xc3c-68pw) |
| CVE-2026-101912 / GHSA-j6r3-76f7-8jcv | Média | `ip-address` | `10.5.0` | Frontend (npm) | 10.7.1 | [GitHub Advisory](https://github.com/advisories/GHSA-j6r3-76f7-8jcv) |
| CVE-2026-101913 / GHSA-rpw4-54j3-4h4q | Média | `ip-address` | `10.5.0` | Frontend (npm) | 10.5.1 | [GitHub Advisory](https://github.com/advisories/GHSA-rpw4-54j3-4h4q) |
| CVE-2026-102277 / GHSA-q2hr-2g5m-vwhr | Média | `brace-expansion` | `5.0.9` | Frontend (npm) | 5.0.12, 3.0.9, 2.1.7, 1.1.21 | [GitHub Advisory](https://github.com/advisories/GHSA-q2hr-2g5m-vwhr) |
| CVE-2026-104844 / GHSA-rj75-hqrm-r3gf | Média | `postcss-selector-parser` | `7.1.4` | Frontend (npm) | 7.1.6, 6.1.5 | [GitHub Advisory](https://github.com/advisories/GHSA-rj75-hqrm-r3gf) |
| CVE-2026-107496 | Média | `libcrypto3` | `3.5.9-r0` | API, Frontend, PostgreSQL | 3.5.9-r1 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-107496) |
| CVE-2026-107496 | Média | `libssl3` | `3.5.9-r0` | API, Frontend, PostgreSQL | 3.5.9-r1 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-107496) |
| CVE-2026-107496 | Média | `openssl` | `3.5.9-r0` | API | 3.5.9-r1 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-107496) |
| CVE-2026-109924 / GHSA-4qv8-2p9j-2685 | Média | `undici` | `6.28.0` | Frontend (npm) | 6.29.1, 7.30.1, 8.12.2 | [GitHub Advisory](https://github.com/advisories/GHSA-4qv8-2p9j-2685) |
| CVE-2026-109932 / GHSA-9fm3-97px-rjf6 | Média | `undici` | `6.28.0` | Frontend (npm) | 6.29.1, 7.30.1, 8.12.2 | [GitHub Advisory](https://github.com/advisories/GHSA-9fm3-97px-rjf6) |
| CVE-2026-111997 / GHSA-rxp7-6hxx-x8q5 | Média | `ip-address` | `10.5.0` | Frontend (npm) | 10.7.4 | [GitHub Advisory](https://github.com/advisories/GHSA-rxp7-6hxx-x8q5) |
| CVE-2026-118764 / GHSA-g3hv-75wg-m249 | Média | `minimatch` | `10.2.5` | Frontend (npm) | 11.0.1 | [GitHub Advisory](https://github.com/advisories/GHSA-g3hv-75wg-m249) |
| CVE-2026-118773 / GHSA-3g3m-2fjr-6pw4 | Média | `glob` | `13.0.6` | Frontend (npm) | 14.0.2 | [GitHub Advisory](https://github.com/advisories/GHSA-3g3m-2fjr-6pw4) |
| CVE-2026-19032 / GHSA-wjgm-6hv5-3cvf | Média | `com.fasterxml.jackson.core:jackson-databind` | `2.21.4` | API (JAR interno), JAR | 2.18.10, 2.21.6, 2.22.2 | [GitHub Advisory](https://github.com/advisories/GHSA-wjgm-6hv5-3cvf) |
| CVE-2026-19032 / GHSA-wjgm-6hv5-3cvf | Média | `tools.jackson.core:jackson-databind` | `3.1.4` | API (JAR interno), JAR | 3.1.6, 3.2.2 | [GitHub Advisory](https://github.com/advisories/GHSA-wjgm-6hv5-3cvf) |
| CVE-2026-27142 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.8, 1.26.1 | [GO-2026-4603](https://pkg.go.dev/vuln/GO-2026-4603) |
| CVE-2026-32282 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.9, 1.26.2 | [GO-2026-4864](https://pkg.go.dev/vuln/GO-2026-4864) |
| CVE-2026-32288 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.9, 1.26.2 | [GO-2026-4869](https://pkg.go.dev/vuln/GO-2026-4869) |
| CVE-2026-32289 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.9, 1.26.2 | [GO-2026-4865](https://pkg.go.dev/vuln/GO-2026-4865) |
| CVE-2026-39823 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.10, 1.26.3 | [GO-2026-4982](https://pkg.go.dev/vuln/GO-2026-4982) |
| CVE-2026-39825 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.10, 1.26.3 | [GO-2026-4976](https://pkg.go.dev/vuln/GO-2026-4976) |
| CVE-2026-39826 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.10, 1.26.3 | [GO-2026-4980](https://pkg.go.dev/vuln/GO-2026-4980) |
| CVE-2026-42505 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.12, 1.26.5, 1.27.0-rc.2 | [GO-2026-5856](https://pkg.go.dev/vuln/GO-2026-5856) |
| CVE-2026-42507 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.11, 1.26.4 | [GO-2026-5039](https://pkg.go.dev/vuln/GO-2026-5039) |
| CVE-2026-4738 | Média | `zlib` | `1.3.2-r1` | API, Frontend | 1.4-r0 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-4738) |
| CVE-2026-49844 / GHSA-qv9r-c865-cp47 | Média | `org.apache.logging.log4j:log4j-api` | `2.25.4` | API (JAR interno), JAR | 2.25.5, 2.26.1 | [GitHub Advisory](https://github.com/advisories/GHSA-qv9r-c865-cp47) |
| CVE-2026-54515 / GHSA-5jmj-h7xm-6q6v | Média | `com.fasterxml.jackson.core:jackson-databind` | `2.21.4` | API (JAR interno), JAR | 3.1.4, 2.18.9, 2.21.5, 2.22.1 | [GitHub Advisory](https://github.com/advisories/GHSA-5jmj-h7xm-6q6v) |
| CVE-2026-55956 / GHSA-5v3h-fjv2-54fg | Média | `org.apache.tomcat.embed:tomcat-embed-core` | `11.0.22` | API (JAR interno), JAR | 11.0.23, 10.1.56, 9.0.119 | [GitHub Advisory](https://github.com/advisories/GHSA-5v3h-fjv2-54fg) |
| CVE-2026-56866 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.26.9, 1.27.2 | [GO-2026-6605](https://pkg.go.dev/vuln/GO-2026-6605) |
| CVE-2026-59889 / GHSA-5gvw-p9qm-jgwh | Média | `com.fasterxml.jackson.core:jackson-databind` | `2.21.4` | API (JAR interno), JAR | 2.21.5, 2.18.9, 2.22.1 | [GitHub Advisory](https://github.com/advisories/GHSA-5gvw-p9qm-jgwh) |
| CVE-2026-59889 / GHSA-5gvw-p9qm-jgwh | Média | `tools.jackson.core:jackson-databind` | `3.1.4` | API (JAR interno), JAR | 3.1.5, 3.2.1 | [GitHub Advisory](https://github.com/advisories/GHSA-5gvw-p9qm-jgwh) |
| CVE-2026-61801 / GHSA-mjcv-p78q-w5fw | Média | `github.com/moby/sys/user` | `v0.1.0` | PostgreSQL (gosu) | 0.4.1 | [GitHub Advisory](https://github.com/advisories/GHSA-mjcv-p78q-w5fw) |
| CVE-2026-77310 / GHSA-vvgp-rfg2-7rr6 | Média | `com.fasterxml.jackson.core:jackson-databind` | `2.21.4` | API (JAR interno), JAR | 2.18.9, 2.21.5, 2.22.1 | [GitHub Advisory](https://github.com/advisories/GHSA-vvgp-rfg2-7rr6) |
| CVE-2026-77310 / GHSA-vvgp-rfg2-7rr6 | Média | `tools.jackson.core:jackson-databind` | `3.1.4` | API (JAR interno), JAR | 3.1.5, 3.2.1 | [GitHub Advisory](https://github.com/advisories/GHSA-vvgp-rfg2-7rr6) |
| CVE-2026-78659 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.26.9, 1.27.2 | [GO-2026-6603](https://pkg.go.dev/vuln/GO-2026-6603) |
| CVE-2026-78660 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.26.9, 1.27.2 | [GO-2026-6610](https://pkg.go.dev/vuln/GO-2026-6610) |
| CVE-2026-78663 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.26.9, 1.27.2 | [GO-2026-6612](https://pkg.go.dev/vuln/GO-2026-6612) |
| CVE-2026-83557 / GHSA-gx83-3vf8-gh7j | Média | `com.fasterxml.jackson.core:jackson-databind` | `2.21.4` | API (JAR interno), JAR | 2.18.10, 2.21.6, 2.22.2 | [GitHub Advisory](https://github.com/advisories/GHSA-gx83-3vf8-gh7j) |
| CVE-2026-83557 / GHSA-gx83-3vf8-gh7j | Média | `tools.jackson.core:jackson-databind` | `3.1.4` | API (JAR interno), JAR | 3.1.6, 3.2.2 | [GitHub Advisory](https://github.com/advisories/GHSA-gx83-3vf8-gh7j) |
| CVE-2026-85024 / GHSA-3wwx-pv8p-q78v | Média | `undici` | `6.28.0` | Frontend (npm) | 6.28.1, 7.29.1, 8.10.2 | [GitHub Advisory](https://github.com/advisories/GHSA-3wwx-pv8p-q78v) |
| CVE-2026-88822 | Média | `libcrypto3` | `3.5.9-r0` | API, Frontend, PostgreSQL | 3.5.9-r1 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-88822) |
| CVE-2026-88822 | Média | `libssl3` | `3.5.9-r0` | API, Frontend, PostgreSQL | 3.5.9-r1 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-88822) |
| CVE-2026-88822 | Média | `openssl` | `3.5.9-r0` | API | 3.5.9-r1 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-88822) |
| CVE-2026-88823 | Média | `libcrypto3` | `3.5.9-r0` | API, Frontend, PostgreSQL | 3.5.9-r1 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-88823) |
| CVE-2026-88823 | Média | `libssl3` | `3.5.9-r0` | API, Frontend, PostgreSQL | 3.5.9-r1 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-88823) |
| CVE-2026-88823 | Média | `openssl` | `3.5.9-r0` | API | 3.5.9-r1 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-88823) |
| CVE-2026-94439 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.26.9, 1.27.2 | [GO-2026-6613](https://pkg.go.dev/vuln/GO-2026-6613) |
| CVE-2026-94440 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.26.9, 1.27.2 | [GO-2026-6608](https://pkg.go.dev/vuln/GO-2026-6608) |
| CVE-2026-94448 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.26.9, 1.27.2 | [GO-2026-6599](https://pkg.go.dev/vuln/GO-2026-6599) |
| CVE-2026-95691 | Média | `libcrypto3` | `3.5.9-r0` | API, Frontend, PostgreSQL | 3.5.9-r1 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-95691) |
| CVE-2026-95691 | Média | `libssl3` | `3.5.9-r0` | API, Frontend, PostgreSQL | 3.5.9-r1 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-95691) |
| CVE-2026-95691 | Média | `openssl` | `3.5.9-r0` | API | 3.5.9-r1 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-95691) |
| CVE-2026-95692 | Média | `libcrypto3` | `3.5.9-r0` | API, Frontend, PostgreSQL | 3.5.9-r1 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-95692) |
| CVE-2026-95692 | Média | `libssl3` | `3.5.9-r0` | API, Frontend, PostgreSQL | 3.5.9-r1 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-95692) |
| CVE-2026-95692 | Média | `openssl` | `3.5.9-r0` | API | 3.5.9-r1 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-95692) |
| CVE-2026-97030 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.26.9, 1.27.2 | [GO-2026-6600](https://pkg.go.dev/vuln/GO-2026-6600) |
| CVE-2026-97032 | Média | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.26.9, 1.27.2 | [GO-2026-6617](https://pkg.go.dev/vuln/GO-2026-6617) |
| GHSA-mhm7-754m-9p8w | Média | `com.fasterxml.jackson.core:jackson-databind` | `2.21.4` | API (JAR interno), JAR | 2.18.9, 2.21.5 | [GitHub Advisory](https://github.com/advisories/GHSA-mhm7-754m-9p8w) |
| CVE-2025-58186 | Baixa | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.24.8, 1.25.2 | [GO-2025-4012](https://pkg.go.dev/vuln/GO-2025-4012) |
| CVE-2026-18540 / GHSA-r53p-7pc4-xj5r | Baixa | `undici` | `6.28.0` | Frontend (npm) | 6.28.1, 7.29.1, 8.10.2 | [GitHub Advisory](https://github.com/advisories/GHSA-r53p-7pc4-xj5r) |
| CVE-2026-27139 | Baixa | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.25.8, 1.26.1 | [GO-2026-4602](https://pkg.go.dev/vuln/GO-2026-4602) |
| CVE-2026-56857 | Baixa | `stdlib` | `v1.24.6` | PostgreSQL (gosu) | 1.26.9, 1.27.2 | [GO-2026-6604](https://pkg.go.dev/vuln/GO-2026-6604) |
| CVE-2026-127039 | Desconhecida | `gnutls` | `3.8.13-r0` | API | 3.8.14-r0 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-127039) |
| CVE-2026-127040 | Desconhecida | `gnutls` | `3.8.13-r0` | API | 3.8.14-r0 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-127040) |
| CVE-2026-127231 | Desconhecida | `gnutls` | `3.8.13-r0` | API | 3.8.14-r0 | [Alpine SecDB](https://secdb.alpinelinux.org/) · [Aviso](https://avd.aquasec.com/nvd/cve-2026-127231) |
| CVE-2026-39824 | Desconhecida | `golang.org/x/sys` | `v0.1.0` | PostgreSQL (gosu) | 0.44.0 | [GO-2026-5024](https://pkg.go.dev/vuln/GO-2026-5024) |

## Atualização do backend para Spring Boot 4.0.9

Validação em 19/11/2026 (horário de Brasília), a partir de `adbddf4`.
O inventário acima registra a T10 original. A comparação a seguir usa o
JAR e a imagem da API construídos com base no mesmo código, com o parent
Spring Boot 4.0.7 na triagem e 4.0.9 na retomada. As duas varreduras rodaram
offline com Trivy 0.76.2 e as mesmas bases, publicadas em 19/11/2026 às
13h02 UTC (vulnerabilidades) e 00h58 UTC (Java). As bases ainda estavam
na janela de atualização informada pelo Trivy.

| Artefato | SHA-256 antes | SHA-256 depois |
| --- | --- | --- |
| JAR extraído de `/app/app.jar` | `c1f2f5256d68baee7f1b1792c72f38c297173cbf51bb4851e58768b120c5a1fa` | `3bad0a9b6e88dc60103162c40481f30f5e999fe8a8d8fa1a1bea5c58e4bb8f8d` |
| Configuração da imagem da API (Linux/amd64) | `d0ddde8fb11f25ab3ad68e1a188d869ce510e8cbad20f41e1240b4ce9df8bc51` | `e974230151a9cb0d41a47511291bdbbeb30b1da1a8e4c8fa4dc38f6cb7629c2a` |

| Artefato | Antes | Depois |
| --- | --- | --- |
| JAR | 31 ocorrências (22 avisos): 7 críticas, 12 altas, 12 médias | 0 ocorrências |
| Imagem da API | 50 ocorrências (31 avisos), incluindo as 31 do JAR | 19 ocorrências (9 avisos): 16 médias e 3 de gravidade desconhecida |

Cada ocorrência corresponde a um aviso, pacote e versão. As 31
ocorrências Java que deixaram de aparecer são as mesmas no JAR isolado
e na imagem. Entre os avisos que saíram estão CVE-2026-65182 (Tomcat),
CVE-2026-91776 e CVE-2026-91777 (Jackson), CVE-2026-47884 e
CVE-2026-47890 (Spring MVC) e CVE-2026-54291 (pgJDBC). A tabela histórica
acima registra os identificadores completos. Os 19 resultados de Alpine
permaneceram iguais, com destaque
para OpenSSL `3.5.9-r0`, zlib `1.3.2-r1` e GnuTLS `3.8.13-r0`.
Nenhuma ocorrência nova foi registrada nos dois artefatos nesta rodada.
O zero do JAR se refere às bases e ao escopo do scanner nesta execução.

A inspeção do JAR confirmou Tomcat 11.0.27, Jackson 2.21.7 e 3.1.7,
Spring Framework 7.0.10, Spring Security 7.0.8, pgJDBC 42.7.14,
Hibernate 7.2.25.Final e Flyway 11.14.1. A mudança usa as versões
gerenciadas pelo parent. O verificador aprovou os 23 recursos esperados
e o arquivo contém as 19 migrações, incluindo a V19.
[Spring Boot 4.0.9](https://spring.io/blog/2026/10/23/spring-boot-4-0-9-available-now/)
· [BOM oficial](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.9/spring-boot-dependencies-4.0.9.pom)

A suíte do backend executou 174 testes com Java 21 e PostgreSQL
descartável via Testcontainers, sem falhas, erros ou testes ignorados.
SMTP e OAuth reais ficaram fora da validação.

Os avisos nas imagens do frontend e do PostgreSQL seguem fora desta
atualização. A falha de upgrade com dados legados afetados pela V12
continua registrada separadamente em [CI.md](CI.md#falha-reproduzida-na-v12).
