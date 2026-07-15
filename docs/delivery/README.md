# Entrega ao juri

Esta pasta explica como preparar uma copia limpa do projeto para entrega. Os
ficheiros de desenvolvimento nao devem ser deslocados para esta pasta se uma
ferramenta precisar deles na raiz; devem simplesmente ficar fora da copia ou
arquivo de entrega.

## Incluir

- `pom.xml`, `src/` e apenas `docs/EXECUCAO.md`;
- `uploads/` apenas com a estrutura e os fixtures de demonstracao versionados;
- Apache Maven 3.9.16 instalado no sistema que vai receber a entrega; o projeto
  usa o comando `mvn` e nao inclui Maven Wrapper;
- `.gitignore` e `.gitattributes` apenas se a entrega for tambem um repositorio
  Git.

## Excluir

Nunca incluir artefactos locais ou credenciais: `.git/`, `.github/`, `target/`,
`.idea/`, `docs/dev/`, `docs/tests/`, `docs/docs/`,
`docs/agents/`, `docs/templates/`, `docs/Credenciais.txt` e uploads criados
durante testes ou execucao local.

A aplicacao normal guarda a cache WebP fora do projeto, em
`~/.gape/webp-native`. O guia de execucao nao depende dos restantes
ficheiros de desenvolvimento.
