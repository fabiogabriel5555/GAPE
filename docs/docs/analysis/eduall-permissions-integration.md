# Integracao EduAll com permissoes

## Ambito analisado

Foram analisados o template original em `docs/templates/Eduall`, a navegacao atual em `src/main/webapp`, e a implementacao de autenticao/autorizacao em:

- `AuthenticationFilter`
- `AuthorizationFilter`
- `AuthorizationPolicy`
- `PermissionChecker`
- `DashboardNavigation`
- `LoginServlet`
- `DashboardServlet`
- `WEB-INF/web.xml`
- testes de autenticacao e autorizacao

Tambem foram revistos os documentos `docs/docs/0. GAPE - ALL - V3.docx` e `docs/docs/7. Relatorio - 49862 - 7.docx` para manter a separacao entre autenticacao, autorizacao, sessao, perfis e permissoes.

## Paginas analisadas

Foram analisadas as familias principais de JSP:

- paginas publicas da raiz, incluindo `index.jsp`, `login.jsp`, `sign-in.jsp`, `sign-up.jsp`, `course*.jsp`, `events*.jsp`, `contact.jsp`, `about-four.jsp`, `policy-page-removed.jsp`;
- paginas de dashboard em `admin/*.jsp`;
- paginas de dashboard em `student/*.jsp`;
- paginas de dashboard em `instructor/*.jsp`;
- paginas de dashboard em `coordinator/*.jsp`;
- paginas de erro `error-404.jsp` e `error-500.jsp`;
- fragments em `WEB-INF/fragments`.

## Paginas e componentes alterados

Nota: a tentativa inicial de trocar os menus do template por fragments dinamicos foi revertida a pedido para preservar integralmente o design e a navegacao original do EduAll. Assim, os menus do template continuam estaticos e mantem os links originais, incluindo links uteis para testes entre dashboards.

Foi mantida apenas `error-403.jsp`, reutilizando o estilo das paginas de erro EduAll existentes, e `WEB-INF/web.xml` passou a mapear o erro 403 para essa pagina.

## Representacao visual por perfil

Os menus visuais do template nao foram alterados. A representacao visual dos perfis continua a seguir as paginas originais do EduAll:

- Administrador: mostra links de dashboard, perfil e mensagens de administrador; `Settings` aparece apenas com `MANAGE_SETTINGS`.
- Coordenador: mostra links de dashboard, perfil e mensagens de coordenador.
- Formador: mostra links de dashboard, perfil e cursos de formador.
- Estudante: mostra links de dashboard, perfil e cursos inscritos.

Utilizadores com mais do que um perfil continuam autorizados pelo back-end a aceder aos perfis que possuem. O template nao esconde links por perfil; a validacao real permanece no back-end.

## Permissoes que afetam o front-end

O front-end usa apenas flags ja calculadas no back-end e guardadas na sessao:

- `gape.auth.hasAdministratorProfile`;
- `gape.auth.hasCoordinatorProfile`;
- `gape.auth.hasTeacherProfile`;
- `gape.auth.hasStudentProfile`;
- `gape.auth.canViewReports`;
- `gape.auth.canManageUsers`;
- `gape.auth.canManagePermissions`;
- `gape.auth.canManageSettings`.

As JSP nao consultam a base de dados e nao contem regras de negocio. A seguranca continua em `AuthenticationFilter`, `AuthorizationFilter`, `AuthorizationPolicy` e `PermissionChecker`.

## Redirecionamento e acesso negado

Mantem-se a politica de autenticacao:

- sem sessao: `login.jsp?auth=required`;
- sessao persistida ausente: `login.jsp?auth=missing`;
- token/utilizador invalido: `login.jsp?auth=invalid`;
- sessao expirada: `login.jsp?auth=expired`.

Quando um utilizador autenticado tenta aceder diretamente a uma area de outro perfil que nao possui, o `AuthorizationFilter` redireciona para o dashboard autorizado definido por `DashboardNavigation`.

Quando o utilizador possui o perfil correto mas falta permissao concreta ou contexto estrutural, o sistema devolve 403 e apresenta `error-403.jsp`. Estes casos nao devem ser escondidos por redirecionamento porque representam uma rejeicao real da operacao.

## Separacao entre UX e seguranca

Como o design do EduAll foi preservado, os menus podem continuar a mostrar links de outras areas. Isto e deliberado para manter o template original e permitir testes manuais. A validacao aplicacional continua a garantir que:

- acesso direto por URL continua protegido por `AuthorizationFilter`;
- permissoes reais sao verificadas por `PermissionChecker`;
- sessoes continuam validadas por `AuthenticationFilter`;
- o front-end apenas mostra ou esconde elementos de navegacao com base no estado ja calculado em sessao.

## Testes e verificacoes

Foram executados:

```bash
mvn test "-Dtest=AuthorizationFilterTest,AuthenticationFilterTest,AuthServiceTest,SessionServiceTest"
```

Resultado: 21 testes executados, 0 falhas, 0 erros.

Verificacoes cobertas:

- falta de sessao redireciona para `login.jsp?auth=required`;
- sessao expirada redireciona para `login.jsp?auth=expired`;
- login valido cria sessao;
- logout e sessao continuam cobertos pelos testes existentes;
- utilizador autenticado sem perfil da area pedida e redirecionado para o dashboard autorizado;
- falta de permissao no perfil correto continua a resultar em 403.
