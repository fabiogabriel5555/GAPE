# Testes Automáticos da Base de Dados (Fase 1)

Localização: `src/test/java/pt/isel/gape/transversal/`
Apoio comum: `DatabaseTestSupport.java` (abre ligação, aplica `sql_mode` estrito, executa `drop.sql`+`schema.sql`, lê metadados do `information_schema`, valida exceções de integridade).

## Pré-requisitos

- MySQL a correr em `localhost:3306` com a base `gape` (ver `src/main/resources/config/db.properties`).
- Os testes recriam o schema (`drop.sql` + `schema.sql`) a cada execução — usar sempre base de desenvolvimento, nunca dados reais.

## Como executar

```bash
mvn test            # toda a suite
mvn test -Dtest=DatabaseConnectionTest
mvn test -Dtest=SchemaIntegrityTest
mvn test -Dtest=ValidDataInsertTest
mvn test -Dtest=InvalidDataConstraintTest
mvn test -Dtest=ApplicationConstraintTest
mvn test -Dtest=DatabaseBootstrapServiceTest
mvn test -Dtest=DatabaseRestrictionCoverageTest
```

## Classes de teste e o que verificam

| Classe | Verifica | Testes |
| --- | --- | --- |
| `DatabaseConnectionTest` | Liga via JDBC e executa `SELECT 1` | 1 |
| `SchemaIntegrityTest` | Tabelas esperadas existem; PK/FK/UNIQUE/CHECK presentes; triggers de validação existem; colunas NOT NULL | 4 |
| `ValidDataInsertTest` | `seed/base.sql` carrega dados válidos nas 4 áreas; um utilizador pode acumular perfis | 2 |
| `InvalidDataConstraintTest` | Cada statement de `test/pk.sql`, `fk.sql`, `unique.sql`, `check.sql` falha por integridade | 1 |
| `ApplicationConstraintTest` | Cada statement de `test/application.sql` (regras de trigger) falha | 1 |
| `DatabaseRestrictionCoverageTest` | Cobertura positiva (dataset válido nas tabelas centrais) + negativa (todos os scripts de violação) | 2 |
| `DatabaseBootstrapServiceTest` | `DatabaseBootstrapService` nos modos `schema`/`demo`/`full` | 3 |

## Correspondência com os nomes do planeamento

A implementação usou nomes diferentes dos previstos; o planeamento (2.7/2.8/2.11) foi atualizado para os nomes reais:

| Nome no planeamento original | Classe real |
| --- | --- |
| `DatabaseSchemaTest` | `SchemaIntegrityTest` |
| `DatabaseSeedDataTest` | `ValidDataInsertTest` |
| `DatabaseInvalidDataTest` | `InvalidDataConstraintTest` |
| `DatabaseConstraintsTest` | `ApplicationConstraintTest` |
| `DatabaseRestrictionCoverageTest` | `DatabaseRestrictionCoverageTest` (criado nesta revisão) |

## Resultado da execução

`mvn test` (build limpo, contra MySQL real): **23 testes, 0 falhas, 0 erros.**

```
ApplicationConstraintTest ......... 1 OK
DatabaseBootstrapServiceTest ...... 3 OK
DatabaseConnectionTest ............ 1 OK
DatabaseRestrictionCoverageTest ... 2 OK
InvalidDataConstraintTest ......... 1 OK
SchemaIntegrityTest ............... 4 OK
ValidDataInsertTest ............... 2 OK
XmlValidationTest ................. 9 OK
```
