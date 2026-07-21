# Integração EduAll — painéis de gestão e relatórios

## Objetivo e decisão de integração

Os painéis de gestão e os relatórios vivem exclusivamente na rota autenticada `/dashboard`, que usa o mesmo shell do Dashboard administrativo EduAll. Não foi criado um dashboard visual paralelo para coordenador, formador ou aluno: o mesmo layout adapta os painéis, indicadores e ações que o servidor autorizou para o perfil ativo. Um relatório é aberto no mesmo Dashboard por `/dashboard?viewId={id}`; não existe uma página, rota ou item de menu Reports separado.

Esta decisão preserva o desenho já consolidado do projeto e evita expor indicadores de outro âmbito apenas porque o utilizador conhece um identificador de painel.

## Análise completa do template EduAll

Foi percorrido o conjunto completo de 75 páginas HTML em `docs/templates/Eduall/eduall`. As 29 páginas cujo nome contém `dashbord`/`dashboard`, mais `deshbord-reviews.html`, foram identificadas como superfícies de dashboard/perfil:

- Base administrativa e variantes: `admin-dashbord.html`, `dashbord.html`, `dashbord-courses.html`, `dashbord-message.html`, `dashbord-quiz-attempts.html`, `dashbord-settings.html`, `dashbord-wishlist.html` e `deshbord-reviews.html`.
- Área de aluno: `student-dashbord.html` e as variantes `assignment`, `enrolled-courses`, `message`, `my-profile`, `my-quiz-attempts`, `reviews`, `settings` e `wishlist`.
- Área de formador: `instructor-dashboard.html` e as variantes `account-settings`, `announcements`, `assignment`, `enrolled-courses`, `message`, `my-courses`, `my-profile`, `my-quiz-attempts`, `order-history`, `quiz-attempts`, `reviews` e `wishlist`.

Também foram analisados os componentes transversais de gráficos e dados: `assets/js/apexcharts.js`, `assets/js/dataTables.min.js`, `assets/css/dataTables.dataTables.min.css`, `assets/sass/components/_table.scss`, `assets/sass/components/_dataTable.scss` e `assets/sass/pages/othersPage/_dashboard.scss`.

`admin-dashbord.html` é a referência visual principal. Fornece o shell `dashbord`/`dashbord-body`, cartões KPI brancos arredondados, gráfico linear `#react-chart`, donut `#donutChart`, filtros compactos, tabelas de resumo e estados de ação. As variantes de aluno e formador foram usadas como referência secundária para cartões de progresso e listas de atividades, mas não substituem a composição administrativa escolhida. `dashbord.html` não contém conteúdo funcional suficiente para ser uma referência.

## Componentes EduAll reutilizados

| Necessidade | Referência EduAll | Integração GAPE |
| --- | --- | --- |
| Shell e hierarquia visual | `admin-dashbord.html` | `dashboard-sidebar.jspf`, `dashboard-topbar.jspf`, `dashboard-footer.jspf`, `dashbord-body` |
| Indicadores | cartões `px-20 py-20 bg-white rounded-10` | Cursos, turmas, inscrições ativas e aulas do âmbito selecionado |
| Gráfico geral | line chart administrativo | `#management-overview-chart`, com valores reais do âmbito, sem alegar série temporal |
| Fluxo de avaliações | donut administrativo | `#management-distribution-chart`, com avaliações, tentativas submetidas e corrigidas |
| Resumo | tabelas administrativas | tabela de indicadores e catálogo de painéis autorizados |
| Filtros e ações | selects/botões EduAll | seletor de painel, filtro de tipo, pesquisa local e modais Bootstrap |
| Estados | cards/avisos EduAll | mensagem vazia, sucesso, erro e estado somente de leitura |

As novas classes `gape-management-dashboard-*` apenas resolvem dimensionamento, pesquisa, tabela, legenda e responsividade; não substituem a paleta, tipografia, cartões ou espaçamentos EduAll.

## Rotas, perfis e autorização

| Superfície | Rota | Dados autorizados |
| --- | --- | --- |
| Dashboard | `/dashboard` ou `/dashboard?viewId={id}` | painéis e relatórios ativos devolvidos por `ManagementViewAccessService.listAccessible` |
| Configuração de painel existente | `POST /dashboard` | edição e destinatários por `ManagementViewService` |

`AuthorizationPolicy` protege `/dashboard`. O `DashboardServlet` cria o `AccessContext` da sessão autenticada e depende apenas de `ManagementDashboardQueryService`, `ManagementViewService`, `ManagementViewAccessService`, `ReportAggregationService` e `ApplicationReadService`; não instancia DAOs nem abre JDBC.

- Administrador: painéis e relatórios globais e de organização nas organizações que administra; também pode configurar painéis de curso nas organizações sob a sua gestão.
- Coordenador: painéis e relatórios das disciplinas que coordena.
- Formador: painéis e relatórios das turmas que leciona.
- Aluno: painéis pessoais e relatórios de curso, disciplina ou turma pertencentes ao respetivo contexto atual.

Uma listagem não é uma abertura sensível. Quando o Dashboard abre o painel selecionado, `ReportAggregationService.aggregate` chama `requireAccess`, que verifica o âmbito em tempo real e regista auditoria. Uma tentativa por URL de abrir um painel fora do âmbito recebe `403` sem revelar se esse painel existe.

## Modelo de dados apresentado

O frontend só recebe `ManagementDashboardView`, uma projeção segura de `ManagementView` e `ReportAggregation`. Os cartões, gráficos e tabela usam:

- cursos, disciplinas e turmas;
- inscrições ativas;
- aulas e avaliações;
- tentativas submetidas, corrigidas e pendentes de correção;
- presenças e certificados emitidos.

Não existem valores fictícios. Se um aluno não possuir painel pessoal ou relatório ativo no seu contexto, vê o estado vazio em vez de números de exemplo.

O `full` seed disponibiliza um cenário utilizável para cada perfil padrão, sem conceder acessos fora do âmbito: administrador com painel global e relatórios global/organizacional, coordenador com o painel e relatório da disciplina Project, formador com o painel e relatório da turma ativa `RC-MATH-05`, e aluno com painel pessoal e relatório atual de Information Systems Master. O relatório de curso usa a inscrição ativa já existente do aluno, sem alterar a coorte histórica de Mathematics.

## Configuração e distribuição

O modal de painel só é entregue quando o servidor deteta pelo menos um painel configurável. A criação de painéis foi removida da aplicação para simplificar o projeto: não existe comando de criação, método de serviço, operação de DAO nem operação HTTP para esse fim. Os painéis disponíveis são apenas provisionados pela configuração/seeds do projeto. As opções de alvo vêm de contextos ativos atribuídos ao perfil: organizações/cursos do administrador, disciplinas do coordenador, turmas do formador e o próprio utilizador para um painel pessoal. Ao editar, o modal preserva o alvo do painel. O serviço volta a validar tudo no `POST` para impedir que atributos HTML ou IDs alterados no browser ampliem permissões.

O modal de destinatários aceita IDs de utilizador separados por vírgula, espaço ou nova linha. A relação explícita é de distribuição e nunca substitui a regra de âmbito; essa limitação é mostrada na interface e imposta pelo `ManagementViewService`. Todos os `POST` incluem o token CSRF da sessão.

## Atualização sem recarregar o Dashboard

O seletor superior, os botões `Open` do catálogo, a gravação de configuração e a gravação de destinatários usam `fetch` com as mesmas rotas protegidas. Após uma resposta válida, o cliente substitui somente `#management-dashboard-main`, os modais e os dados dos gráficos; o shell EduAll, sidebar, topbar e footer não são reconstruídos. Os gráficos ApexCharts anteriores são destruídos antes da substituição, os filtros de catálogo são preservados e o histórico do browser é atualizado para permitir voltar/avançar sem reload completo.

Durante a operação, o conteúdo recebe um indicador discreto de carregamento e `aria-busy`. Os formulários são codificados como `application/x-www-form-urlencoded`, mantendo o token CSRF. Quando a sessão é propagada por URL (`;jsessionid`), o cliente preserva esse segmento também nos pedidos assíncronos; sessões normais por cookie não sofrem alteração. Como salvaguarda, browsers sem `fetch` submetem os formulários e ligações normalmente.

## Menus e compatibilidade

Os dois sidebars (`dashboard-sidebar.jspf` e `student-dashboard-sidebar.jspf`) apontam apenas o item Dashboard para `/dashboard`. O catálogo e o filtro de tipo do Dashboard incluem relatórios no mesmo ecrã, sem `Dashboard?section=reports`, rota `/reports/*` ou item Reports. Os dashboards JSP legados continuam presentes para não remover superfícies fora do âmbito desta alteração, mas `DashboardNavigation` envia todos os perfis para a rota unificada.

## Validação funcional e visual

Além dos testes de serviço de painéis provisionados, acesso por âmbito e agregação, foram adicionadas verificações estruturais para o shell EduAll, gráficos, formulários com CSRF, Dashboard unificado, ausência de DAOs no Servlet/JSP e entradas de menu. A verificação confirma também que o comando/modelo/API de criação não existem e que os controlos do Dashboard usam atualização assíncrona.

Para a validação visual local, quando o browser integrado não está disponível, seguir estritamente `docs/tests/browser-validation.md`:

1. Executar testes focados e `mvn -q -DskipTests package`.
2. Preparar uma única instância com `docs/dev/scripts/browser-prepare.ps1 -SkipPackage`.
3. Executar `browser-check-flow.ps1` serialmente em 1920×1200 e 390×844 para `admin@gape.local`, `coord@gape.local`, `teacher@gape.local` e `student@gape.local`.
4. Verificar os quatro dashboards, abrir no mesmo Dashboard os relatórios global, organização, disciplina, turma, curso e pessoal, usar o filtro de tipo e os modais de configuração/destinatários e confirmar a ausência de overflow horizontal, erros de consola, pedidos falhados, imagens partidas e problemas de acessibilidade.
5. Capturar screenshots finais em `target/browser-screenshots`, inspecioná-los visualmente e terminar a instância com `browser-stop.ps1`.

A validação deve também confirmar que não existe ação de criação de painel, verificar a distribuição de destinatários e testar que uma tentativa de abrir `/reports/{id}` já não apresenta o Dashboard.

### Execução desta integração (16-07-2026)

- Na passagem final, `ManagementViewServiceTest` (3), `ManagementViewAccessServiceTest` (2), `ReportAggregationServiceTest` (3) e `ManagementViewsDashboardTemplateTest` passaram sem falhas nem erros; `mvn -q -DskipTests package` também passou.
- Brave/CDP confirmou os quatro dashboards em desktop e mobile, os seis relatórios autorizados abertos pelo catálogo do Dashboard (`210`, `211`, `212`, `213`, `209`, `214`) e o filtro de tipo de cada perfil. A configuração e distribuição de destinatários foram exercitadas; a criação de painel não é exposta.
- A validação visual encontrou e corrigiu um rodapé de modal que ficava fora de viewports móveis baixos. Os modais agora usam a altura útil do viewport, rolam apenas o corpo interno e mantêm as ações visíveis.
- A atualização assíncrona foi testada no seletor, no catálogo, na configuração e nos destinatários. A validação com latência controlada confirmou o estado de carregamento sem reconstrução do shell; foram corrigidos o transporte de `;jsessionid` e a codificação `application/x-www-form-urlencoded` dos POSTs com CSRF.
- Não foram encontrados erros de rede, consola, acessibilidade, imagens partidas ou overflow horizontal. Foram inspecionadas visualmente as capturas do Dashboard administrativo, formador, aluno, relatórios e modais em desktop/mobile, incluindo os gráficos móveis.
