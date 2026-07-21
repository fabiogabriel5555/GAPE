# Integracao EduAll: utilizadores, preferencias-removidas, eliminacao e auditoria

> Estado de auditoria atualizado: a tabela autónoma foi substituída pela vista
> `Logs` do Dashboard. Consultar `eduall-audit-integration.md` para a integração
> atual, os filtros por âmbito e a validação.

## Paginas analisadas

- `docs/templates/Cursus/HTML/setting.html`: base visual para perfil, preferencias-removidas e encerramento de conta.
- `docs/templates/Cursus/HTML/my_student_profile_view.html` e `my_instructor_profile_view.html`: referencia para detalhe de perfil.
- `docs/templates/Cursus/HTML/create_new_course.html`: referencia para modais Bootstrap.
- `docs/templates/Cursus/HTML/report_history.html`: referencia de historico, adaptada para auditoria em tabela.
- `src/main/webapp/admin/admin-dashbord.jsp`: dashboard administrativo e padrao de tabela/sidebar.
- `src/main/webapp/admin/admin-dashbord-my-profile.jsp`: perfil estatico existente.
- `src/main/webapp/admin/admin-dashbord-settings.jsp`, `student/student-dashbord-settings.jsp`, `instructor/instructor-dashboard-account-settings.jsp`: formularios estaticos de conta, preferencias-removidas e eliminacao.
- `src/main/webapp/forms.jsp` e `src/main/webapp/tables.jsp`: referencias de formulario/tabela administrativa.
- `src/main/webapp/WEB-INF/fragments/template-base-head.jspf`, `template-base-scripts.jspf` e `user-avatar-content.jspf`: assets e avatar reutilizaveis.

## Paginas e componentes alterados

- `src/main/webapp/admin/admin-dashbord.jsp`: menu administrativo recebeu links para utilizadores, preferencias-removidas, pedidos de eliminacao e auditoria.
- `src/main/webapp/WEB-INF/web.xml`: o `AuthenticationFilter` passou a cobrir `/*`, protegendo tambem servlets.
- `src/main/java/pt/isel/gape/web/navigation/DashboardNavigation.java`: `/profile` encaminha para `/account/profile`.
- `src/main/java/pt/isel/gape/security/authorization/AuthorizationPolicy.java`: adicionada regra protegida para `/account/*` e regra explicita para auditoria.
- `src/main/java/pt/isel/gape/security/session/SessionManager.java`: expostos flags de sessao para preferencias-removidas, dados pessoais e processamento de eliminacao.

## Paginas criadas

- `WEB-INF/views/access/users-list.jsp`: listagem administrativa com acoes de detalhe, edicao, bloqueio, desbloqueio e eliminacao.
- `WEB-INF/views/access/user-detail.jsp`: detalhe administrativo de dados pessoais, perfis, estado e atalhos de preferencias-removidas/auditoria.
- `WEB-INF/views/access/user-form.jsp`: criacao e edicao de utilizador.
- `WEB-INF/views/access/account-profile.jsp`: perfil pessoal e edicao dos dados do proprio utilizador.
- `WEB-INF/views/access/account-removed-preferences.jsp`: preferencias de preferencias-removidas do proprio utilizador.
- `WEB-INF/views/access/account-deletion-requests.jsp`: submissao e historico de pedidos de eliminacao.
- `WEB-INF/views/access/admin-removed-preferences.jsp`: gestao administrativa de preferencias por utilizador.
- `WEB-INF/views/access/admin-deletion-requests.jsp`: processamento administrativo de pedidos de eliminacao.
- A auditoria deixou de ter JSP autónoma; é apresentada em `/dashboard?tab=logs`.

## Componentes reutilizados

- `template-base-head.jspf` e `template-base-scripts.jspf` para manter assets EduAll.
- `user-avatar-content.jspf` no topbar das novas paginas.
- Novos fragments `dashboard-sidebar.jspf`, `dashboard-topbar.jspf`, `flash-messages.jspf` e `dashboard-footer.jspf`, mantendo classes EduAll e reduzindo duplicacao.
- Tabelas, cards, badges, switches e modais Bootstrap seguem as classes existentes do EduAll.

## Formularios ligados ao back-end

- `/admin/users` e `/admin/users/{id}` ligam a `UserService.createUser` e `UserService.updateUser`.
- `/admin/users/{id}/block` e `/admin/users/{id}/unblock` ligam a `UserService.blockUser` e `UserService.unblockUser`.
- `/account/profile` liga a `UserService.editPersonalProfile`.
- `/account/removed-preferences` e `/admin/removed-preferences/{id}` ligam a `RemovedPreferenceService.createPreference` e `RemovedPreferenceService.updatePreference`; as opcoes sao fixas para impedir duplicados no UI e o service mantem a garantia.
- `/account/deletion-requests` liga a `DeletionRequestService.submitDeletionRequest`.
- `/admin/deletion-requests/{id}/process` liga a `DeletionRequestService.processDeletionRequest`.
- `/dashboard?tab=logs` liga a `ActivityLogService`; `/admin/activity-log` é
  apenas um redirecionamento de compatibilidade para essa vista.

## Decisoes de interface

- Create User e Edit User nao apresentam nem sincronizam `Student Course Context`. As inscricoes de curso do estudante (`enroll_course`) sao geridas exclusivamente nas paginas de inscricoes; o `UserService` rejeita esse contexto quando submetido pelo CRUD de utilizadores, preservando inscricoes existentes durante uma edicao.

- O CRUD administrativo usa paginas separadas para listagem, detalhe e formulario para preservar clareza e o padrao de dashboard.
- Bloquear, desbloquear, eliminar utilizador, submeter pedido de eliminacao e processar pedido usam modais de confirmacao.
- A edicao do perfil pessoal fica na mesma pagina de perfil para reduzir navegação sem alterar o design base.
- Preferencias de preferencias-removidas sao apresentadas como switches, porque sao valores booleanos.
- Auditoria fica numa tabela administrativa, mais adequada para leitura e filtragem do que o bloco textual do template.
- DTOs de view impedem que JSPs recebam `credentialHash` ou `credentialSalt`.

## Paginas publicas e protegidas

- Publicas: paginas ja existentes em `AuthorizationPolicy.isPublic`, incluindo `index.jsp`, `login.jsp`, `sign-in.jsp`, `sign-up.jsp`, `policy-page-removed.jsp`, assets e `/auth/*`.
- Protegidas autenticadas: `/account/profile`, `/account/removed-preferences`, `/account/deletion-requests`.
- Protegidas administrativas: `/admin/users`, `/admin/removed-preferences` e
  `/admin/deletion-requests`; Logs segue a proteção autenticada de `/dashboard`
  e reforça o âmbito no serviço.
- As restricoes finais continuam no service layer: leitura/alteracao de dados pessoais, preferencias-removidas, eliminacao e auditoria registam operacoes criticas e validam permissoes.
