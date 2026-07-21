# Plano de eliminação/simplificação das páginas sensíveis

Este plano cobreu as páginas que ainda pertenciam ao template antigo. A
execução foi concluída: a aplicação começa no login e não expõe fluxos de site
público ou de auto-registo. Mantém-se como checklist para futuras remoções de
dependências residuais.

## 1. Inventário e matriz de dependências

Antes de apagar qualquer página, procurar referências em JSP/JSPF, Java,
JavaScript, `web.xml`, testes, documentação e scripts de validação. Cada página
deve ficar classificada como `manter`, `simplificar` ou `remover`, com a rota que
a substitui. A pesquisa tem de ser repetida depois de cada remoção para garantir
que não ficam links, includes ou regras de segurança órfãos.

## 2. Entrada canónica da aplicação

1. Alterar o `welcome-file` de `index.jsp` para `login.jsp` em
   `WEB-INF/web.xml`.
2. Manter `/login.jsp` e `/auth/login` como fluxo canónico de autenticação;
   `LoginServlet` já redireciona/encaminha para `login.jsp`.
3. Substituir links de “Home” que apontam para `index.jsp` por `/login.jsp`
   (visitante) ou pela landing page autenticada apropriada.
4. Atualizar a lista pública de `AuthorizationPolicy` e os caminhos tratados
   pelo `CsrfFilter`, removendo a exceção de `index.jsp` quando a página for
   eliminada.

## 3. Eliminação de `index.jsp` e `sign-in.jsp`

Só depois da fase anterior e de uma pesquisa sem referências:

- apagar `index.jsp`;
- apagar `sign-in.jsp`, mantendo apenas `login.jsp`;
- remover `/sign-in.jsp` de `AuthorizationPolicy` e `CsrfFilter`;
- remover links para `sign-up.jsp`/`sign-in.jsp` do login e das páginas de erro;
- atualizar `TemplateStructureTest`, `TemplateAssetReferenceTest`, testes de
  filtros e documentação.

`sign-up.jsp` deve ser removida na mesma mudança se a criação de contas
continuar exclusivamente nos ecrãs autenticados de gestão de utilizadores. Se
algum fluxo legítimo ainda depender dela, deve ser convertido para esse fluxo
antes da remoção; não se deve reintroduzir auto-registo anónimo.

## 4. Páginas do template antigo

Após a entrada canónica estar estável, avaliar e remover ou simplificar as
páginas públicas sem função na aplicação: `about-four.jsp`,
`apply-admission.jsp`, `contact.jsp`, `content.jsp`, `event-details.jsp`,
`events.jsp`, `forms.jsp`, `lesson-details.jsp`, `tables.jsp`, `tutor.jsp`,
`tutor-details.jsp`, `my-propyl.jsp` e o `profile.jsp` legado. A página de
perfil funcional é a rota autenticada `/profile`; as páginas administrativas,
de aluno e de gestão que continuam referenciadas por servlets devem ser
preservadas.

Para cada remoção:

- migrar qualquer ação útil para a rota funcional existente;
- remover apenas CSS/JavaScript/imagens que deixem de ter consumidores;
- manter as páginas `error-403.jsp`, `error-404.jsp` e `error-500.jsp`, mas
  atualizar os seus links para o login/landing page.

## 5. Dependências que têm de ser tratadas

- `WEB-INF/web.xml`: `welcome-file`, mappings e páginas de erro;
- `LoginServlet`, `AuthenticationFilter`, `AuthorizationFilter`,
  `AuthorizationPolicy` e `CsrfFilter`;
- navegação e fragments, sobretudo `dashboard-sidebar.jspf`, o cabeçalho de
  validação de certificados e os templates de erro;
- links absolutos/relativos em todos os JSP/JSPF e `assets/js/main.js`;
- testes estruturais, de referências locais, autenticação, CSRF e arquitetura;
- documentação e scripts de browser/deploy que ainda mencionem as páginas.

## 6. Validação obrigatória por etapa

1. Parar todos os Tomcat GAPE e confirmar a porta 8080 livre.
2. Executar os testes focados de estrutura/referências e depois a suíte Maven
   completa uma única vez, numa base de dados descartável, sem timeout
   artificial.
3. Com o Tomcat iniciado no endereço real
   `http://localhost:8080/GAPE/`, validar por cliques: `/` abre `login.jsp`,
   login válido chega à landing page do perfil, logout regressa ao login, e as
   rotas apagadas não abrem nem deixam links quebrados.
4. Repetir a pesquisa de referências e confirmar novamente que a porta 8080
   fica livre no final.

Cada etapa deve ser mantida num commit/checkpoint separado para permitir
reversão sem recuperar páginas já eliminadas.

## 7. Resultado da execução

Execução concluída em 20-07-2026:

- `login.jsp` passou a ser o `welcome-file`; `index.jsp`, `sign-in.jsp`,
  `sign-up.jsp` e as restantes páginas antigas listadas neste plano foram
  removidas.
- As políticas de autorização/CSRF, navegação, testes e referências JSP foram
  atualizadas; a pesquisa final não encontrou referências às páginas removidas.
- A suíte Maven completa foi executada numa base descartável: 764 testes,
  0 falhas, 0 erros e 0 ignorados.
- A validação visual real foi feita com Brave/CDP (desktop e móvel) em
  `http://localhost:8080/GAPE/`, com zero erros de browser, rede, consola,
  acessibilidade, overflow ou imagens quebradas. Os screenshots ficam em
  `target/browser-screenshots/login-real-8080.png` e
  `target/browser-screenshots/login-real-8080-mobile.png`.
- A validação HTTP confirmou `/` e `/login.jsp` (200) e o redirecionamento das
  rotas removidas para o login (302). A porta 8080 foi confirmada livre no fim.

## 8. Normalização das vistas da raiz

Execução concluída na mesma alteração:

- `courses.jsp` foi movida para `WEB-INF/views/public/course-catalog.jsp`.
- `course-details.jsp` foi movida para `WEB-INF/views/public/course-detail.jsp`.
- `CourseCatalogServlet` passou a encaminhar diretamente para as vistas
  protegidas em `WEB-INF`.
- `course.jsp`, `course-list-view.jsp`, `courses.jsp`, `course-details.jsp`,
  `messages.jsp` e `dashbord.jsp` deixaram de existir como JSPs na raiz.
- `LegacyPageRedirectServlet` preserva bookmarks antigos, redirecionando-os
  para `/courses`, `/messages` ou `/dashboard` sem duplicar vistas.
- Os links dos templates, páginas de erro, login e dashboard foram atualizados
  para as rotas canónicas.
- Os fluxos reais confirmaram catálogo desktop/móvel, detalhe de curso,
  mensagens e dashboard, todos sem erros de browser, rede, consola,
  acessibilidade ou overflow.
