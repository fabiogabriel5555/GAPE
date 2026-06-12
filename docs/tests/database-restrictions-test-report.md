# Relatório de Restrições Testadas (Fase 1)

Este relatório indica que restrições do modelo são garantidas e testadas em SQL na Fase 1, e quais ficam para fases futuras (camada de negócio). O mapa completo de cobertura está em `docs/docs/analysis/database-constraint-coverage.md`.

Teste dedicado: `src/test/java/pt/isel/gape/transversal/DatabaseRestrictionCoverageTest.java`

## Como executar

```bash
mvn test -Dtest=DatabaseRestrictionCoverageTest
```

## Estratégia

- **Cenário positivo** — recria o schema, carrega `seed/base.sql` e confirma que as tabelas centrais das 4 áreas ficam com dados válidos.
- **Cenário negativo** — sobre o mesmo dataset, executa cada statement dos scripts de violação; cada um tem de falhar com exceção de integridade (`SQLState 23xxx` ou códigos 1062/1452/1451/1048/3819, ou `45000/1644` de trigger). Cada statement corre num `savepoint` com rollback, para isolar os casos.

## Restrições testadas em SQL

Scripts negativos em `src/main/resources/sql/test/`:

- `pk.sql` — PK duplicada (simples e composta), unicidade de PK em entidades fracas.
- `fk.sql` — FK para registos inexistentes em curso, sessão, eliminação, unidade orgânica, turma, bloco, aula, opção, tentativa, resposta, certificado, permissão.
- `unique.sql` — UNIQUE de email, token de sessão, documento, ordem de bloco na turma.
- `check.sql` — domínios de estado/estado inválido, pares condicionais (datas, min/max alunos, bloco scheduled, capacidade, processamento de eliminação/justificação, certificado emitido, mensagem agendada/anexo, delivery_mode).
- `application.sql` — regras cruzadas garantidas por triggers: coerência de organização entre unidade orgânica/curso/sala, existência de pelo menos um administrador ativo com `MANAGE_ALL`, cobertura temporal de inscrição disciplina-curso, subject integrada na turma, aula online/presencial e sala/sobreposição, coerência avaliação/bloco/subject, limite e elegibilidade de tentativas, coerência resposta/opção, eventos de horário, assiduidade e justificação, pautas e pesos, certificados, mensagens e participação em canal, e activity_log.

Tabelas cobertas (lista do planeamento 2.8): `user_account`, `user_session`, `deletion_request`, `organization`, `organic_unit`, `course`, `subject`, `integrate_subject`, `class_group`, `enroll_course`, `enroll_subject`, `enroll_class_group`, `content_block`, `content_item`, `lesson`, `physical_room`, `assessment`, `question`, `question_option`, `attempt`, `response`, `certificate`, `message`.

## Restrições deixadas para fases futuras

Restrições que dependem de contexto aplicacional (utilizador autenticado, perfil/âmbito, contagem transacional, processamento pós-escrita) **não** são forçadas em SQL. Estão listadas, com a camada-alvo (`SEC-Filtro`, `BN-Pre`, `BN-Tx`, `BN-Post`), em `docs/docs/analysis/database-constraint-coverage.md`, secção "Restrições Não Implementadas". Exemplos: expiração de sessão, RBAC e permissões por perfil/âmbito, prevenção geral de ciclos na hierarquia de unidades orgânicas, lotação de turmas, bloqueio de pautas publicadas, elegibilidade material para certificado, visibilidade/moderação de mensagens e painéis.

## Resultado da execução

`DatabaseRestrictionCoverageTest` — `Tests run: 2, Failures: 0, Errors: 0`.
Conjunto de restrições SQL (`InvalidDataConstraintTest` + `ApplicationConstraintTest` + `DatabaseRestrictionCoverageTest`) — todos verdes.
