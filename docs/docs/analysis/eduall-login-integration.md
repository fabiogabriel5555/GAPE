# Integracao EduAll Login/Logout no GAPE

## Objetivo
Ligar o shell visual do template EduAll ao backend de autenticacao ja existente no GAPE, mantendo o layout do template sem reformulacoes visuais e evitando SQL ou regras de negocio nas JSP.

## Paginas do EduAll analisadas

### Autenticacao e sessao
- `login.jsp`
- `sign-in.jsp`
- `sign-up.jsp`

### Shell publico
- `index.jsp`
- `about-four.jsp`
- `apply-admission.jsp`
- `contact.jsp`
- `course.jsp`
- `course-list-view.jsp`
- `course-details.jsp`
- `courses.jsp`
- `events.jsp`
- `event-details.jsp`
- `instructor/instructor.jsp`
- `instructor/instructor-details.jsp`
- `policy-page-removed.jsp`
- `tutor.jsp`
- `tutor-details.jsp`
- `error-404.jsp`
- `error-500.jsp`

### Shell privado e paginas protegidas
- `admin/dashboard.jsp`
- `admin/admin-dashbord.jsp`
- `admin/admin-dashbord-my-profile.jsp`
- `profile.jsp`
- `messages.jsp`
- `forms.jsp`
- `tables.jsp`
- `content.jsp`
- `dashbord.jsp`
- `admin/admin-dashbord-courses.jsp`
- `admin/admin-dashbord-message.jsp`
- `admin/admin-dashbord-quiz-attempts.jsp`
- `admin/admin-dashbord-settings.jsp`
- `admin/admin-dashbord-wishlist.jsp`
- `admin/admin-dashbord-reviews.jsp`
- `student-dashbord*.jsp`
- `instructor-dashboard*.jsp`
- `instructor/instructor-ashboard.jsp`
- `instructor/instructor-my-profile.jsp`
- `my-propyl.jsp`

## Componentes identificados
- `header` publico repetido nas paginas publicas
- `mobile menu` publico repetido nas paginas publicas
- `sidebar` privada repetida nas paginas de dashboard
- `topbar` privada repetida nas paginas de dashboard
- `dropdown` de utilizador repetido nas paginas de dashboard
- botoes de `logout` repetidos na `sidebar` e no `dropdown`
- mensagens visuais de erro/sucesso no bloco central do login

## Paginas alteradas

### Autenticacao
- `login.jsp`
- `sign-in.jsp`

### Fluxo por perfil
- `coordinator/coordinator-dashboard.jsp`
- `admin/admin-dashbord.jsp`
- `student/student-dashbord.jsp`
- `instructor/instructor-dashboard.jsp`

### Protecao de acesso
- `AuthenticationFilter`
- todas as paginas listadas em `PROTECTED_PATHS`

## Fragments alterados
- `WEB-INF/fragments/user-avatar-content.jspf` e usado para apresentar a imagem/icone do utilizador autenticado no shell do template.
- Os restantes fragments existentes em `WEB-INF/fragments` foram mantidos sem papel central nesta integracao.

## Formularios ligados ao backend
- `login.jsp` envia `POST` para `auth/login`
- `sign-in.jsp` mantem o mesmo layout e envia `POST` para `auth/login`
- Os controlos de logout nas paginas privadas enviam `POST` para `auth/logout` com `csrfToken` da sessao.

## Paginas protegidas
As paginas protegidas continuam a ser as definidas em `AuthenticationFilter.PROTECTED_PATHS`, incluindo:
- `admin/dashboard.jsp`
- `admin/admin-dashbord.jsp`
- `content.jsp`
- `messages.jsp`
- `forms.jsp`
- `tables.jsp`
- `profile.jsp`
- `admin/admin-dashbord-my-profile.jsp`
- `my-propyl.jsp`
- variantes `dashbord-*`
- variantes `student-dashbord*`
- variantes `instructor-dashboard*`
- `instructor/instructor-my-profile.jsp`
- `lesson-details.jsp`

## Paginas publicas
As paginas publicas continuam a ser as definidas em `AuthenticationFilter.PUBLIC_PATHS`, incluindo:
- `index.jsp`
- `login.jsp`
- `sign-in.jsp`
- `sign-up.jsp`
- `contact.jsp`
- `courses.jsp`
- `course.jsp`
- `course-details.jsp`
- `about-four.jsp`
- `instructor/instructor.jsp`
- `instructor/instructor-details.jsp`
- `tutor.jsp`
- `tutor-details.jsp`
- `events.jsp`
- `event-details.jsp`
- `apply-admission.jsp`
- `policy-page-removed.jsp`
- `error-404.jsp`
- `error-500.jsp`

## Decisoes tomadas
- O layout do EduAll foi preservado. A integracao limitou-se a formularios, redirects, protecao de paginas e controlos de logout.
- `login.jsp` e `sign-in.jsp` ficaram visualmente equivalentes, ambos ligados ao `LoginServlet`.
- Os erros de credenciais e as mensagens de sessao expirada continuam a aparecer no bloco visual do login, usando classes do proprio template.
- O `LoginServlet` passou a redirecionar por perfil:
  - `ADMINISTRATOR` -> `admin/admin-dashbord.jsp`
  - `COORDINATOR` -> `coordinator/coordinator-dashboard.jsp`
  - `TEACHER` -> `instructor/instructor-dashboard.jsp`
  - `STUDENT` -> `student/student-dashbord.jsp`
- A rota `/dashboard` redireciona uma sessao valida para a landing page adequada ao perfil.
- `coordinator/coordinator-dashboard.jsp` foi criada a partir de `instructor/instructor-dashboard.jsp`, com o mesmo layout e com o nome/landing page adaptados.
- O `logout` usa `POST` para `auth/logout`, aproveitando o `LogoutServlet` ja existente e evitando logout por `GET`.
- O `AuthenticationFilter` valida autenticacao, expiracao e perfil exigido por area (`admin`, `coordinator`, `instructor` e `student`).
- Nao foi criada pagina dedicada de acesso negado nesta fase; quando um utilizador autenticado segue um link para uma area que nao pertence aos seus perfis, o sistema redireciona para o dashboard permitido.
