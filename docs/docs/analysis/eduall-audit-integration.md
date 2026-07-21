# Integração EduAll — Logs de auditoria no Dashboard

## Decisão de produto

Os Logs de auditoria são uma segunda vista do Dashboard autenticado, e não uma
página, rota visual ou item de menu autónomo. A superfície única é:

| Vista | Rota | Objetivo |
| --- | --- | --- |
| Dashboard | `/dashboard` ou `/dashboard?tab=dashboard` | Painéis, relatórios, indicadores e configuração autorizada. |
| Logs | `/dashboard?tab=logs` | Histórico de atividade limitado ao âmbito do perfil atual. |

O antigo `/admin/activity-log` permanece apenas como redirecionamento de
compatibilidade para favoritos antigos; não lê dados, não encaminha para JSP e
não produz uma página própria. O ficheiro
`src/main/webapp/admin/admin/user/admin-audit.jsp` foi removido. Os atalhos de
utilizador passam a abrir `/dashboard?tab=logs&userId={id}`.

## Análise do EduAll completa

Foi usado o inventário completo das 75 páginas HTML em
`docs/templates/Eduall/eduall`, já documentado em
`eduall-template-analysis.md`. Para esta integração foram mapeadas, em
particular:

- `admin-dashbord.html`: shell `dashbord`/`dashbord-body`, cartões brancos
  arredondados, KPI, filtros compactos, tabela e ações; é a referência visual
  principal.
- `dashbord-quiz-attempts.html`, `deshbord-reviews.html`,
  `student-dashbord-my-quiz-attempts.html`,
  `instructor-dashboard-quiz-attempts.html`,
  `instructor-dashboard-reviews.html` e
  `instructor-dashboard-order-history.html`: cabeçalhos de tabela, linhas com
  hover, badges e composição de filtros para histórico.
- `student-dashbord*.html` e `instructor-dashboard*.html`: variantes de
  dashboard por perfil, usadas apenas para confirmar que a mesma hierarquia
  serve os quatro perfis sem criar shells paralelos.
- `assets/js/apexcharts.js`, `assets/js/dataTables.min.js`, CSS de tabelas e
  estilos de dashboard: são recursos existentes; Logs reutiliza o padrão de
  tabela responsiva e não introduz uma biblioteca visual nova.

O Dashboard administrativo existente foi preservado. As classes novas
`gape-management-dashboard-*` limitam-se às tabs, ao painel de filtros e à
tabela de Logs, mantendo cartões, tipografia, cores, espaçamento e estados de
hover do EduAll.

## Ligação segura ao back-end

`DashboardServlet` é a única ponte web para Logs. A JSP recebe apenas uma lista
de `ActivityLogView`; não conhece DAO, `Connection`, SQL nem qualquer objeto de
segurança.

1. O servlet valida a sessão persistida e obtém o perfil ativo.
2. Para filtros gerais chama `ActivityLogService.queryForActor(...)` com
   `ActivityLogQuery`.
3. Quando existe `userId`, usa `ActivityLogService.listForUserAudit(...)` e só
   depois estreita o subconjunto autorizado por operação, entidade, resultado e
   período. Isto preserva o significado anterior de histórico do utilizador:
   ações feitas pelo utilizador ou que o envolvem, nunca resultados fora do
   âmbito do observador.
4. Para aluno, o campo de utilizador é `readonly` e o servlet impõe sempre o ID
   da sessão, inclusive se o parâmetro for alterado no browser.

O serviço aplica o âmbito real antes de devolver qualquer linha:

| Perfil | Registos possíveis |
| --- | --- |
| Administrador | Próprios e dos contextos das organizações que administra. |
| Coordenador | Próprios e das disciplinas que coordena. |
| Formador | Próprios e das turmas que leciona. |
| Aluno | Apenas o histórico pessoal. |

Assim, o filtro User é sempre um estreitamento, e não uma forma de ampliar
acesso por ID conhecido.

## Filtros e apresentação

O cartão Logs contém os filtros User ID, Operation, Entity, From, Until e
Outcome. Os campos de data nativos transportam ISO apenas no input. No servlet,
`LocalDate` é convertido para início do dia (`atStartOfDay`) e fim do dia
(`LocalTime.MAX`), para que os dois limites sejam inclusivos. A tabela mostra a
data através de `ActivityLogView`, usando o formato de data/hora da aplicação
(Lisboa), e apresenta operação, sessão, utilizador, entidade/identificador,
resultado e IP.

Os resultados vazios e filtros inválidos ficam na mesma vista, com uma mensagem
EduAll; não há redirecionamento para uma página de auditoria diferente.

## Atualização sem refresh

`gape-management-views-dashboard.js` usa o mecanismo assíncrono já aplicado a
painéis:

- as tabs fazem `fetch` de `/dashboard?...` e substituem apenas
  `#management-dashboard-main`;
- o formulário de Logs serializa os filtros para a mesma rota e atualiza o
  histórico do browser;
- selects aplicam imediatamente e campos de texto/data usam debounce curto;
- o shell — sidebar, topbar e footer — não é reconstruído;
- sem `fetch` o formulário e links continuam funcionais com navegação normal.

O pedido GET não altera dados e, portanto, não requer CSRF. Os POST existentes
de configuração de painéis mantêm o token CSRF e não são disponibilizados na
vista Logs.

## Ficheiros afetados

- `DashboardServlet.java`: seleção da tab, filtros, conversão de data e
  projeção por `ActivityLogService`.
- `management-dashboard.jsp`: tabs Dashboard/Logs, filtros e tabela.
- `gape-management-views-dashboard.js` e `main.css`: interação AJAX e estilos
  suplementares responsivos.
- `AdminActivityLogServlet.java`: redirecionamento de compatibilidade.
- `dashboard-sidebar.jspf`, `admin-users.jsp` e `admin-user-detail.jsp`:
  atalhos para o Dashboard Logs.
- testes estruturais: remoção da página antiga, rota visual única, filtros,
  atualização AJAX e ausência de links para `/admin/activity-log`.

## Validação executada

Foram executadas as verificações estruturais focadas, a consulta de auditoria
por âmbito e o build:

```powershell
mvn -q "-Dtest=ManagementViewsDashboardTemplateTest,TemplateStructureTest,TemplateAssetReferenceTest" test
mvn -q "-Dtest=ActivityLogQueryServiceTest,ManagementViewsDashboardTemplateTest" test
mvn -q -DskipTests package
.\docs\dev\scripts\browser-prepare.ps1 -SkipPackage
```

Na mesma instância local em `:18080`, a verificação Brave/CDP passou para
administrador, coordenador, formador e aluno em desktop e móvel. Foram
confirmadas a tab ativa, atualização assíncrona da tab e do filtro Outcome,
ausência de overflow horizontal da página, e ausência de erros de consola,
rede, imagens ou acessibilidade. A tabela móvel mantém o seu scroll apenas no
contentor responsivo. O pedido de aluno com `userId=1` mostrou o ID próprio
bloqueado, e `/admin/activity-log?userId=4` redirecionou para Dashboard Logs.

Durante a validação foi identificado e corrigido um `NullPointerException` ao
abrir Logs sem filtro de utilizador: a ramificação não-aluno fazia unboxing de
um `Long` nulo. A seleção agora conserva `Long` nullable, e a ativação por
teclado, assistência ou `HTMLElement.click()` mantém a atualização assíncrona
das tabs.
