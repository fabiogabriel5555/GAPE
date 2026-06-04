# Análise de Suporte da Base de Dados (Fase 1)

Objetivo: registar a análise dos ficheiros do professor, do relatório e do modelo EA que serviu de base à criação dos ficheiros de suporte (XML/XSD), do schema MySQL e da configuração JDBC da Fase 1.

Fontes analisadas:

- `1. Planeamento de IA e Código` (`docs/docs/analysis/planeamento-ia-e-codigo.md`, Fase 1);
- `7. Relatorio - 49862 - 7` (RF01–RF22, RNF, casos de uso);
- `0. GAPE - ALL - V3` (modelo EA: 4 partes, entidades, associações, restrições);
- documentos e exemplos do professor (`docs/docs/`; as pastas de exemplo `1. Agenda` e `2. Moradia` foram entretanto removidas do repositorio).

## XML/XSD aproveitados do professor

Copiados/adaptados dos exemplos do professor (pasta `1. Agenda`, entretanto removida do repositorio); residem agora em `src/main/resources/config/`:

- `config/xsd/transversal/calendarioAcademico.xsd` + `config/xsl/transversal/calendarioAcademico.xsl`;
- `config/xsd/transversal/calendarioPlurianual.xsd` + `config/xsl/transversal/calendarioPlurianual.xsl`;
- exemplos validados em `config/xml/transversal/calendarioAcademico.xml` e `config/xml/transversal/calendarioPlurianual.xml`.

Estes ficheiros cobrem o serviço transversal de calendário (académico e plurianual) e são reutilizados como estão, apenas reorganizados para a estrutura `config/`.

## XML/XSD criados (listas controladas do modelo)

Criou-se um esquema central `config/xsd/gape-config.xsd` que valida todas as listas de valores controlados, com:

- um `simpleType` por catálogo, com `xs:enumeration` para os valores fixos;
- um `complexType` por entrada (`code` + `label`);
- um elemento raiz por catálogo com `minOccurs`/`maxOccurs` fixos ao número de valores e `xs:unique` sobre `@code` (impede duplicados).

Catálogos em `config/xml/` (24): estados de utilizador, sessão, eliminação; permissões; tipos/estados de organização; tipos de unidade orgânica; tipos/estados de curso; modalidades/estados de turma; modos de acesso e estados de bloco; formatos de conteúdo; tipos de aula; tipos de avaliação; tipos de pergunta; tipos de evento de horário; estados de assiduidade; estados de certificado; estados de mensagem; **estados de tentativa**, **tipos de certificado** e **tipos de mensagem** (acrescentados nesta revisão para alinhar com a lista mínima do planeamento).

## Decisão: um XSD partilhado vs. um XSD por catálogo

O planeamento sugeria "um XML e um XSD por cada catálogo". A implementação usa **um XSD partilhado** (`gape-config.xsd`) com todos os tipos e raízes. É uma decisão deliberada: reduz duplicação, centraliza os valores controlados e mantém a validação (cada XML referencia `gape-config.xsd` por `xsi:noNamespaceSchemaLocation`). O `XmlValidationTest` confirma que cada XML valida contra o esquema.

## Valores controlados ainda não catalogados (registo para fases futuras)

Estes atributos do modelo são conceptualmente "valores controlados", mas no `schema.sql` atual são `VARCHAR` livres (sem `CHECK` nem catálogo XML). Ficam registados para a fase em que a funcionalidade respetiva for implementada:

- `user_account.document_type` — tipos de documento;
- `class_group.shift` — turnos de turma;
- `assessment.mode` e `assessment.correction_mode` — modos/modalidades e modo de correção de avaliação;
- `management_view.type` e `management_view.visibility_scope` — tipos e âmbitos de visibilidade de painel;
- `channel.type` e `channel.visibility` — tipos e visibilidade de canal;
- `participate_channel.role` — papéis em canal.

Recomendação: quando cada área for implementada, decidir entre `CHECK` + catálogo XML/XSD (se a lista for fixa) ou tabela de referência (se for configurável), e atualizar este documento.

## Estrutura recomendada (a que ficou em uso)

```text
src/main/resources/config/xml/            catálogos de valores controlados
src/main/resources/config/xml/transversal/ exemplos de calendário
src/main/resources/config/xsd/gape-config.xsd  esquema central
src/main/resources/config/xsd/transversal/     XSD de calendário
src/main/resources/config/xsl/transversal/     XSL de calendário
src/main/resources/sql/                    drop.sql, schema.sql
src/main/resources/sql/seed/               base.sql, full.sql
src/main/resources/sql/test/               pk/fk/unique/check/application.sql
src/main/resources/config/db.properties    configuração JDBC
```

## Decisões importantes

- MySQL 8+ (InnoDB, utf8mb4); restrições de integridade simples e cruzadas em SQL (PK/FK/UNIQUE/NOT NULL/CHECK/triggers); restrições dependentes de contexto aplicacional ficam para Services/filtros (ver `database-constraint-coverage.md`).
- JDBC puro, sem Spring/Hibernate/JPA; credenciais fora do código Java (em `config/db.properties`).
- Bootstrap opcional da base de dados ao arranque web por `DatabaseBootstrapListener` + `DatabaseBootstrapService`, controlado por `db.bootstrap.mode` (`none`/`schema`/`demo`/`full`).
