# Consola CRUD da Base de Dados (Fase 1) — Testes Manuais

Aplicação de consola para testar manualmente dados reais da base de dados.

Localização (em `src/main/java/pt/isel/gape/dev/`):

- `DevDatabaseCrudConsoleApp` — classe principal (`main`), menu;
- `DevCrudService` — operações ver/criar/atualizar/apagar;
- `DevTableMetadataService` — leitura de metadados (tabelas, colunas, PK) via `DatabaseMetaData` e CRUD genérico com `PreparedStatement`;
- `DevConsoleInputReader` — leitura de input da consola;
- `SqlErrorTranslator` — tradução de erros SQL para linguagem simples.

> Não usar em produção. Não expor via web. Não integrar com login/fluxos funcionais do GAPE.

## Como executar

1. Confirmar MySQL ligado e `src/main/resources/config/db.properties`.
2. `mvn test` (garante schema + dados, opcional).
3. Executar pela IDE ou, de forma reproduzivel, a partir da raiz do repositorio:

```powershell
mvn -q -DskipTests compile exec:java
```

Menu:

```text
==============================
 GAPE - CRUD da Base de Dados
==============================
1. Ver Dados
2. Atualizar Dados
3. Criar Dados
4. Apagar Dados
0. Sair
```

## O que a consola permite

- listar tabelas e escolher uma;
- ver registos (até 200) e detalhe por chave primária;
- criar registo (pede só os campos não auto-incremento, indica obrigatório/opcional);
- atualizar registo (Enter mantém o valor atual, `:null` coloca NULL);
- apagar registo (mostra o registo e pede confirmação);
- traduzir erros SQL: mostra operação, tabela, dados enviados, motivo provável, restrição provável e erro técnico.

## Passo a passo manual (checklist)

Ver dados
- [ ] `1. Ver Dados` → `user_account` → ver registos → escolher ID → confirmar detalhe.
- [ ] Repetir para `organization`, `course`, `class_group`, `content_block`.

Criar válidos
- [ ] `3. Criar Dados` → `user_account` → preencher obrigatórios → confirmar `[OK]` → confirmar em `Ver Dados`.

Criar inválidos (deve mostrar `[ERRO]` com motivo e restrição)
- [ ] `user_account` com email duplicado → UNIQUE.
- [ ] `user_account` sem campos obrigatórios → NOT NULL.
- [ ] `user_session` com utilizador inexistente → FOREIGN KEY.
- [ ] `class_group` com `min_students > max_students` → CHECK.
- [ ] `content_block` com ordem duplicada na turma → UNIQUE.

Atualizar
- [ ] `2. Atualizar Dados` → `user_account` → escolher ID → alterar nome → `[OK]`.
- [ ] Tentar email duplicado → rejeição.

Apagar
- [ ] `4. Apagar Dados` → escolher tabela/ID → confirmar visualização → confirmar → `[OK]`.
- [ ] Tentar apagar `organization` com dependências → erro FK/dependências.

## Tradução de erros (SqlErrorTranslator)

Mapeia, entre outros: 1062 (UNIQUE), 1452/1451 (FOREIGN KEY), 1048 (NOT NULL), 3819 (CHECK), 1366/1292 (tipo/data inválidos), e reconhece constraints nomeadas (ex.: `uq_user_account_email`, `ck_class_group_students_range`, `ck_content_block_state`) para dar uma explicação específica.

## Resultado

Consola compila no módulo principal e executa o menu e as 4 operações contra a base `gape`. As mensagens de erro identificam a restrição provável nos casos inválidos acima.
