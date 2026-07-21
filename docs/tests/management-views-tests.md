# Testes de Painéis de Gestão e Relatórios

## Objetivo

Esta cobertura valida a Fase 15 (RF19 / UC19): painéis provisionados pela
configuração do projeto, respetiva configuração, autorização por âmbito,
agregação de indicadores reutilizando os DAOs académicos e auditoria de
operações sensíveis. A aplicação não disponibiliza criação de painéis.

## Isolamento e dados de teste

Os três testes usam a base de dados partilhada de teste com
`@Execution(SAME_THREAD)`, `@ResourceLock("gape-db")`, uma transação por teste
e rollback no fim. A seed base fornece:

- administrador `1`, coordenador `2`, formador `3` e aluno `4`;
- organização `10`, curso `30`, disciplina coordenada `40` e turma lecionada
  `50`;
- disciplina/turma fora do âmbito do coordenador/formador (`41`/`52`).

Os testes fixam a data efetiva em `2026-07-15` e acrescentam uma fixture atual
para o aluno: disciplina `42`, turma `53` no período `3002` e inscrição entre
`2026-07-01` e `2026-12-31`. Isto impede que uma inscrição histórica da turma
`50` seja confundida com acesso atual.

## Cobertura automática

### `ManagementViewServiceTest`

- usa fixtures que provisionam painéis `GLOBAL`, `ORGANIZATION`, `COURSE`,
  `SUBJECT`, `CLASS_GROUP` e `PERSONAL`, sem recorrer a uma API aplicacional
  de criação;
- valida que administrador, coordenador, formador e aluno podem reconfigurar
  apenas os painéis provisionados nos seus âmbitos;
- valida a persistência do tipo de alvo, contexto e proprietário, incluindo o
  proprietário obrigatório de um painel pessoal;
- configura a distribuição explícita e confirma a auditoria
  `MANAGEMENT_VIEW_CONFIGURE`;
- rejeita a reconfiguração fora do âmbito e regista a negação.

### `ManagementViewAccessServiceTest`

- administrador: painel global e organização administrada, mas não organização
  fora da sua gestão;
- coordenador: apenas a disciplina que coordena;
- formador: apenas a turma que leciona;
- aluno: painel pessoal e relatórios `REPORT` do curso/turma temporalmente
  atuais, nunca painel de outro aluno, dashboard contextual ou turma fora da
  inscrição;
- confirma que `access_management_view` é apenas distribuição e não aumenta
  privilégios;
- usa `requireAccess` para verificar auditoria `MANAGEMENT_VIEW_ACCESS` com
  resultados `success` e `denied`.

### `ReportAggregationServiceTest`

- confirma os indicadores determinísticos da turma `50`: 1 curso, 1
  disciplina, 1 turma, 1 inscrição ativa, 1 aula, 2 avaliações, 1 tentativa
  corrigida, 1 registo de assiduidade e nenhum certificado emitido;
- confirma que o painel pessoal provisionado para o aluno agrega apenas a turma atual `53` e
  exclui aulas, avaliações, tentativas, inscrições e assiduidade da turma
  expirada ou não inscrita;
- confirma que a agregação chama a barreira de acesso antes de carregar dados.

## Execução

Com MySQL local configurado para a base de testes GAPE, executar a partir da
raiz do projeto, um comando de cada vez. Estes testes reinicializam o mesmo
esquema MySQL e não devem correr em paralelo com outra bateria que também o
reconstrua:

```bash
mvn test -Dtest=ManagementViewServiceTest
mvn test -Dtest=ManagementViewAccessServiceTest
mvn test -Dtest=ReportAggregationServiceTest
```

## Resultado da execução

Execução final em 16-07-2026:

| Comando | Resultado |
| --- | --- |
| `mvn test -Dtest=ManagementViewServiceTest` | 3 testes, 0 falhas, 0 erros |
| `mvn test -Dtest=ManagementViewAccessServiceTest` | 2 testes, 0 falhas, 0 erros |
| `mvn test -Dtest=ReportAggregationServiceTest` | 3 testes, 0 falhas, 0 erros |

Depois da remoção da API de criação, os três comandos foram repetidos e a
bateria focada também passou com
`ManagementViewsDashboardTemplateTest`.

Depois do reforço temporal dos certificados, o comando
`mvn test -Dtest=ReportAggregationServiceTest` foi repetido com o mesmo resultado
(3 testes, 0 falhas e 0 erros).

Foram corrigidos durante a passagem:

- a FK de `management_view.owner_user_id` deixou de usar ações referenciais
  explícitas, incompatíveis no MySQL com o `CHECK` que protege o âmbito
  pessoal;
- a fixture de organização fora do âmbito passou a reutilizar a organização
  inativa `19`, respeitando a regra de lifecycle que só permite ativar uma
  organização após atribuição administrativa;
- a agregação pessoal passou a intersectar turmas, inscrições de turma e
  inscrição de curso temporalmente válidas, impedindo a inclusão de dados da
  turma expirada `50` no painel atual do aluno.
- certificados do aluno passaram também a ser filtrados pela ocorrência das
  turmas atualmente acessíveis, evitando que um certificado histórico do
  mesmo curso infle um relatório pessoal atual.
- a criação deixou de existir no modelo de comando, serviço, DAO e rota HTTP;
  as inserções SQL presentes nos testes são exclusivamente fixtures de
  provisionamento, equivalentes às seeds do projeto.
