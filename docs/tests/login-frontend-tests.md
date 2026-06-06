# Login Frontend Tests

## Ambito
Valida a integracao visual EduAll do login, logout, sessao e protecao de paginas da Fase 3.

## Paginas cobertas
- `login.jsp`
- `sign-in.jsp`
- paginas publicas do shell EduAll
- dashboards `admin`, `coordinator`, `instructor` e `student`

## Verificacoes realizadas
- `login.jsp` submete por `POST` para `${pageContext.request.contextPath}/auth/login`
- `sign-in.jsp` submete por `POST` para `${pageContext.request.contextPath}/auth/login`
- mensagens de erro de autenticacao aparecem no bloco visual do EduAll
- mensagens de logout e sessao expirada aparecem no bloco visual do EduAll
- botoes de logout submetem formulario `POST` para `${pageContext.request.contextPath}/auth/logout`
- formularios de logout incluem `csrfToken` da sessao autenticada
- paginas privadas sem sessao redirecionam para `login.jsp?auth=required`
- sessoes expiradas redirecionam para `login.jsp?auth=expired`
- utilizador autenticado aparece no template atraves do fragment `WEB-INF/fragments/user-avatar-content.jspf`
- nao ha SQL nas JSP
- nao ha chamadas diretas a DAOs ou Services nas JSP
- o design visual do EduAll foi preservado, sem reformulacoes de layout

## Passo a passo manual
1. Abrir `index.jsp` sem sessao e confirmar que a pagina publica carrega.
2. Abrir `login.jsp` e confirmar que o formulario visual do EduAll aparece.
3. Submeter credenciais invalidas e confirmar mensagem de erro.
4. Submeter credenciais validas de administrador e confirmar redirecionamento para `admin/admin-dashbord.jsp`.
5. Fazer logout a partir do dashboard e confirmar submissao `POST` para `auth/logout` e redirecionamento para `login.jsp?logout=1`.
6. Abrir um dashboard sem sessao e confirmar redirecionamento para `login.jsp?auth=required`.
7. Repetir login com estudante, formador e coordenador e confirmar landing page do respetivo perfil.
8. Confirmar que formularios de logout funcionam em paginas dentro de subpastas.
9. Confirmar que o avatar/nome do utilizador autenticado aparece sem quebrar o layout.

## Resultado
- Os pontos acima foram revistos no codigo durante a revisao da Fase 3.
- A validacao visual em browser deve ser repetida antes de demonstracao final.
