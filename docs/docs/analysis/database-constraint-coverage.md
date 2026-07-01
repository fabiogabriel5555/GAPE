# Cobertura Atual de Restrições na Base de Dados

Legenda de implementação (Relatório 7)

- `SEC-Filtro`: filtros/listeners da camada de negócio (autenticação, sessão, autorização).
- `BN-Pre`: validação na camada de negócio (Servlet -> Service) antes de chamar DAO.
- `BN-Tx`: validação transacional na camada de negócio com apoio DAO (queries + write atómico).
- `BN-Post`: regras pós-escrita (auditoria, envio, agendamento).

---

## Restrições Implementadas

### 1.3 Acesso, Identidade e Controlo — garantidas

- `[1], [2], [3], [4], [5], [6], [7], [8], [9], [10], [11], [12], [13], [16], [17], [19], [20]`
  Garantida por: `CHECK`, `UNIQUE`, `PRIMARY KEY`, `FOREIGN KEY` e `NOT NULL` já presentes no `schema.sql`.

- `[21], [22]`
  Garantida por: triggers `bi_deletion_request_validate` e `bu_deletion_request_validate`, que exigem coerência entre `processor_admin_user_id`, `processed_at` e estados finais do pedido de eliminação.

### 2.3 Estrutura Organizacional e Formativa — garantidas

- `[3], [5]`
  Garantida por: triggers `bi_organic_unit_validate` e `bu_organic_unit_validate`, que impedem auto-subordinação e exigem que a unidade orgânica pai pertença à mesma organização.

- `[6]`
  Garantida por: triggers `bi_course_validate` e `bu_course_validate`, que obrigam o `Course` e a `Organic_Unit` associada a pertencerem à mesma `Organization`.

- `[7]`
  Garantida por: triggers `bi_class_group_validate` e `bu_class_group_validate`, que exigem integração ativa da `Subject` no `Course`.

- `[8], [9], [10], [11], [12]`
  Garantida por: `CHECK` sobre pares condicionais e coerência de intervalos em `integrate_subject` e `class_group`.

- `[13], [14], [15]`
  Garantida por: `UNIQUE` em `class_group` e `content_block`.

- `[16], [17], [18]`
  Garantida por: `CHECK` em `content_block`, incluindo obrigatoriedade de `available_from` para blocos `scheduled`.

### 3.3 Conteúdos, Aulas e Avaliação — garantidas

- `[12], [13]`
  Garantida por: trigger `bi_content_item_validate`/`bu_content_item_validate` e `CHECK` temporal em `content_item`.

- `[20], [21]`
  Garantida por: `CHECK` em `physical_room.capacity` e triggers `bi_physical_room_validate`/`bu_physical_room_validate`.

- `[25], [26], [27], [28], [29], [30], [31]`
  Garantida por: `CHECK` em `lesson` e triggers `bi_lesson_validate`/`bu_lesson_validate`, cobrindo datas, tipo de aula, URL, sala física, coerência com `Content_Block`, sala ativa e conflitos por sobreposição.

- `[33], [34], [35]`
  Garantida por: `CHECK` em `assessment`.

- `[36], [37], [38]`
  Garantida por: triggers `bi_assessment_validate` e `bu_assessment_validate`, que impõem coerência entre tipo de avaliação, `Subject` e `Content_Block`.

- `[41], [43]`
  Garantida por: `UNIQUE` em `question` e `question_option`.

- `[46], [48], [49], [51], [52], [54], [55], [56], [57]`
  Garantida por: `UNIQUE`, `CHECK` e triggers `bi_attempt_validate`, `bi_response_validate` e `bi_response_option_validate`, cobrindo elegibilidade do aluno, limite de tentativas, submissão, unicidade de resposta, coerência entre `Attempt`/`Question`/`Option` e cardinalidade de respostas objetivas.

### 4.3 Horários, Assiduidade, Resultados, Certificação e Serviços Transversais — garantidas

- `[1], [2], [3], [4]`
  Garantida por: `CHECK` e triggers `bi_schedule_event_validate`/`bu_schedule_event_validate`, cobrindo datas, lembretes e coerência temporal com `Lesson` e `Assessment`.

- `[10], [12], [13]`
  Garantida por: `CHECK`, `UNIQUE` e triggers `bi_attendance_record_validate`/`bu_attendance_record_validate`, que exigem inscrição ativa do aluno na turma da aula.

- `[18], [19], [20], [21]`
  Garantida por: `CHECK` e triggers `bi_absence_justification_validate`/`bu_absence_justification_validate`, cobrindo compatibilidade do registo, identidade do aluno e processamento da justificação.

- `[26], [27], [28], [29], [30], [32]`
  Garantida por: triggers `bi_associate_grade_sheet_class_group_validate`, `bi_based_on_assessment_validate`/`bu_based_on_assessment_validate`, `bi_grade_record_validate`/`bu_grade_record_validate` e `CHECK` em `based_on_assessment.weight`/`assessment.final_grade_weight`. A soma exata dos pesos a 100% e a redistribuicao no fim do periodo sao regras de lifecycle aplicacional.

- `[39], [40], [41]`
  Garantida por: `CHECK`, `UNIQUE` e trigger `bi_bgsc_validate`, cobrindo emissão coerente e associação entre `Certificate`, `Grade_Sheet`, `Course` e `Subject`.

- `[52], [53], [54], [56], [57], [58], [59], [61], [62], [63]`
  Garantida por: `PRIMARY KEY`, `CHECK` e triggers `bi_message_validate`/`bu_message_validate` e `bi_receive_message_validate`.

- `[70], [71]`
  Garantida por: `NOT NULL` em `activity_log` e trigger `bi_activity_log_validate`.

---

## Restrições Não Implementadas

### 1.3 Acesso, Identidade e Controlo — não garantidas

- `[18]`
  Motivo: expiração de sessão depende de política temporal da aplicação.
  Onde/quando: `BN-Post` + `SEC-Filtro`.

- `[23], [24], [25], [26]`
  Motivo: permissões por perfil/escopo e RBAC continuam dependentes do utilizador autenticado e do contexto funcional.
  Onde/quando: `SEC-Filtro` + `BN-Pre`.

- `[15]` (parcial)
  Motivo: a base de dados valida listas fixas via `CHECK`, mas não sincroniza dinamicamente com catálogos XML/XSD.
  Onde/quando: `BN-Pre`.

### 2.3 Estrutura Organizacional e Formativa — não garantidas

- `[1], [2]`
  Motivo: cardinalidade mínima e manutenção de estado ativo dependem de contagem transacional sobre associações `Manage`.
  Onde/quando: `BN-Tx`.

- `[4]`
  Motivo: prevenção geral de ciclos na hierarquia de `Organic_Unit` exige navegação recursiva; a base de dados atual só bloqueia auto-subordinação direta.
  Onde/quando: `BN-Tx`.

- `[21]` (parcial)
  Motivo: existe cobertura localizada para datas relevantes, mas não há validação universal para todas as associações com `start`/`end`.
  Onde/quando: `BN-Pre` + `BN-Tx`.

- `[22], [23], [24], [25], [26], [27], [28]`
  Motivo: regras de acesso e percurso académico dependem do ator autenticado e do contexto de uso.
  Onde/quando: `SEC-Filtro` + `BN-Pre`.

- `[29], [30], [31], [32]`
  Motivo: sobreposição de inscrições e controlo de lotação exigem contagem/conflito transacional sobre associações de inscrição, ainda não levadas para triggers.
  Onde/quando: `BN-Tx`.

- `[33]`
  Motivo: bloqueio de operações por estado arquivado é regra de workflow aplicacional.
  Onde/quando: `BN-Pre`.

### 3.3 Conteúdos, Aulas e Avaliação — não garantidas

- `[1], [2], [3], [4], [5], [6], [7], [8]`
  Motivo: ownership, autorização contextual e exceções administrativas continuam dependentes do utilizador autenticado.
  Onde/quando: `SEC-Filtro` + `BN-Pre`.

- `[9], [10], [11], [19], [40]`
  Motivo: ciclo de vida, arquivamento e bloqueios após reutilização ou submissões continuam a exigir lógica funcional mais rica do que a que está no schema.
  Onde/quando: `BN-Tx`.

- `[14], [15], [16], [17], [18]`
  Motivo: ainda não foi implementado, em SQL, o encadeamento estrutural completo das associações de conteúdos com `Organization`, `Organic_Unit`, `Course`, `Subject`, `Class_Group`, `Content_Block` e `Assessment`.
  Onde/quando: `BN-Tx`.

- `[22], [23], [24], [32], [39]`
  Motivo: gestão de salas, aulas e avaliações por perfil/contexto continua dependente de autorização aplicacional.
  Onde/quando: `SEC-Filtro` + `BN-Pre`.

- `[42], [44], [45]`
  Motivo: a semântica das opções corretas ainda não está totalmente garantida em `question_option` por tipo de pergunta e modo de correção.
  Onde/quando: `BN-Tx`.

- `[47]`
  Motivo: bloqueio de tentativas para perfis não elegíveis depende do perfil autenticado, não apenas dos dados persistidos.
  Onde/quando: `SEC-Filtro` + `BN-Pre`.

- `[50]`
  Motivo: a base de dados ainda não impede formalmente o arranque de tentativas fora da janela temporal da avaliação.
  Onde/quando: `BN-Tx`.

- `[53]`
  Motivo: `Attempt.score` já é não-negativa, mas ainda não fica limitada por `Assessment.max_grade` em todos os cenários.
  Onde/quando: `BN-Tx`.

- `[58], [59], [60]`
  Motivo: respostas múltiplas, obrigatoriedade de `answer`/`attachment` por tipo de pergunta e política de correção automática/manual continuam parciais.
  Onde/quando: `BN-Tx`.

### 4.3 Horários, Assiduidade, Resultados, Certificação e Serviços Transversais — não garantidas

- `[5]`
  Motivo: coerência estrutural completa de `Schedule_Event` com `Class_Group`, `Subject` e `Course` ainda não está validada em trigger.
  Onde/quando: `BN-Tx`.

- `[6], [7], [8], [9], [15], [16], [17], [22], [23], [24], [25], [35], [36], [37], [38], [44], [45], [46], [47], [48], [49], [50], [51], [66], [67], [68], [69], [74], [75], [76]`
  Motivo: visibilidade, moderação, publicação, consulta e gestão por perfil/contexto continuam dependentes de autorização aplicacional.
  Onde/quando: `SEC-Filtro` + `BN-Pre`.

- `[11], [14], [31], [43], [55], [60], [64], [72], [73]`
  Motivo: regras operacionais, cálculo derivado, auditoria, agendamento efetivo e imutabilidade lógica dependem de serviços e processamento pós-escrita.
  Onde/quando: `BN-Post` + `BN-Tx`.

- `[33]` (parcial)
  Motivo: `Grade_Record.value` já é não-negativa e fica limitada por `Assessment.max_grade` quando existe `Attempt`, mas não existe ainda uma escala explícita universal da `Grade_Sheet` para todos os registos manuais.
  Onde/quando: `BN-Pre` + `BN-Tx`.

- `[34]`
  Motivo: bloqueio de alterações diretas a notas de pautas publicadas continua a ser regra de workflow; pesos das avaliações de turma continuam corrigiveis pela aplicação em qualquer estado da pauta.
  Onde/quando: `BN-Tx`.

- `[42]`
  Motivo: elegibilidade material do aluno para emissão de certificado ainda depende de regras académicas de negócio.
  Onde/quando: `BN-Tx`.

- `[65], [77]`
  Motivo: encadeamento estrutural completo de `Channel` e das restantes associações transversais ainda não está codificado em SQL.
  Onde/quando: `BN-Tx`.

- `[78], [79]` (parcial)
  Motivo: `CHECK` cobre listas fixas, mas não governa catálogos XML/XSD dinâmicos em runtime.
  Onde/quando: `BN-Pre`.
