# Analise do template EduAll para integracao no GAPE

## Objetivo
Integrar o template EduAll no GAPE com uma abordagem de copia quase literal `1:1`, convertendo quase todo o vendor HTML para JSP e limitando as alteracoes a `paths`, remapeamento de links, branding minimo para GAPE e correcoes obrigatorias do proprio template.

## Estrutura analisada
- `docs/templates/Eduall/documentation/index.html`
- `docs/templates/Eduall/eduall/*.html`
- `docs/templates/Eduall/eduall/assets/css`
- `docs/templates/Eduall/eduall/assets/js`
- `docs/templates/Eduall/eduall/assets/images`
- `docs/templates/Eduall/eduall/assets/sass`
- `docs/templates/Eduall/rtl-eduall/*`

## Paginas analisadas
Total analisado no ramo principal `eduall`: `75` HTML.

### Homes e landing
- `index.html`
- `index-2.html`
- `index-3.html`
- `index-4.html`
- `index-5.html`
- `index-6.html`

### Institucional e informacao
- `about.html`
- `about-two.html`
- `about-three.html`
- `about-four.html`
- `faq.html`
- `contact.html`
- `policy-page-removed.html`
- `pricing-plan.html`
- `gallery.html`

### Eventos, admissao e processos
- `events.html`
- `event-details.html`
- `apply-admission.html`
- `tuition-jobs.html`

### Cursos, aulas e conteudos
- `course.html`
- `course-list-view.html`
- `course-details.html`
- `lesson-details.html`
- `favorite-course.html`
- `book-online-class.html`

### Tutores e instrutores
- `instructor.html`
- `instructor-two.html`
- `instructor-details.html`
- `tutor.html`
- `tutor-details.html`
- `find-tutors.html`

### Autenticacao e perfil
- `sign-in.html`
- `sign-up.html`
- `my-profile.html`
- `my-propyl.html`
- `instructor-my-profile.html`

### Dashboards e administracao
- `admin-dashbord.html`
- `dashbord.html`
- `dashbord-message.html`
- `dashbord-courses.html`
- `dashbord-wishlist.html`
- `deshbord-reviews.html`
- `dashbord-quiz-attempts.html`
- `dashbord-settings.html`
- `student-dashbord.html`
- `student-dashbord-assignment.html`
- `student-dashbord-enrolled-courses.html`
- `student-dashbord-message.html`
- `student-dashbord-my-profile.html`
- `student-dashbord-my-quiz-attempts.html`
- `student-dashbord-reviews.html`
- `student-dashbord-settings.html`
- `student-dashbord-wishlist.html`
- `instructor-ashboard.html`
- `instructor-dashboard.html`
- `instructor-dashboard-account-settings.html`
- `instructor-dashboard-announcements.html`
- `instructor-dashboard-assignment.html`
- `instructor-dashboard-enrolled-courses.html`
- `instructor-dashboard-message.html`
- `instructor-dashboard-my-courses.html`
- `instructor-dashboard-my-profile.html`
- `instructor-dashboard-my-quiz-attempts.html`
- `instructor-dashboard-order-history.html`
- `instructor-dashboard-quiz-attempts.html`
- `instructor-dashboard-reviews.html`
- `instructor-dashboard-wishlist.html`

### Blog e ecommerce
- `blog.html`
- `blog-list.html`
- `blog-classic.html`
- `blog-details.html`
- `product.html`
- `product-details.html`
- `cart.html`
- `checkout.html`

## Assets analisados
### CSS
- `bootstrap.min.css`
- `main.css`
- `select2.min.css`
- `slick.css`
- `magnific-popup.css`
- `jquery-ui.css`
- `plyr.css`
- `editor-quill.css`
- `animate.css`
- `dataTables.dataTables.min.css`
- `aos.css`

### JavaScript
- `jquery-3.7.1.min.js`
- `boostrap.bundle.min.js`
- `main.js`
- `select2.min.js`
- `slick.min.js`
- `magnific-popup.min.js`
- `jquery-ui.js`
- `plyr.js`
- `editor-quill.js`
- `dataTables.min.js`
- `aos.js`
- `apexcharts.js`
- `phosphor-icon.js`

### Imagens
- `assets/images/bg`
- `assets/images/icons`
- `assets/images/logo`
- `assets/images/shapes`
- `assets/images/thumbs`

## Problemas encontrados no template original
- `courses.html` e referenciado em varios menus, mas o ficheiro nao existe.
- `dashbord.html` aponta para `dashboard.html`, que nao existe.
- `my-propyl.html` e `instructor-my-profile.html` existem, mas estao vazios.
- Existem nomes inconsistentes no vendor: `admin-dashbord.html`, `dashbord-*`, `deshbord-reviews.html`, `instructor-ashboard.html`.
- `index-4.html`, `index-5.html` e `index-6.html` usam `javarscript:void(0)` em vez de `javascript:void(0)`.
- `index-6.html` contem um path errado para `curve-arrow.png`.
- `index-2.html` referencia `assets/images/thumbs/user-two-img6.png`, imagem inexistente.
- `index-5.html` referencia `assets/images/logo/marquee-img-*.png`, imagens inexistentes no pacote entregue.
- O `main.js` inicializa `DataTable` e `Plyr` de forma global e sem guardas.
- O `main.js` contem logica duplicada para toggle de password e ids inconsistentes.

## Paginas aproveitadas
### Estrategia adotada
- O vendor foi espelhado em JSP quase por completo, mas as variantes de homepage `index-2` a `index-6` foram removidas do runtime do GAPE por decisao funcional.
- As areas de `blog` e `ecommerce` (`product`, `product-details`, `cart`, `checkout`) foram inicialmente convertidas para permitir avaliacao funcional, mas depois removidas do runtime por nao fazerem sentido para a base atual do GAPE.
- O submenu `Pages` foi reduzido para manter apenas `about-four`, `instructor`, `instructor-details`, `tutor`, `tutor-details`, `events`, `event-details`, `apply-admission` e `removed-preferences-policy`.
- Para manter compatibilidade com a estrutura ja usada no GAPE, foram mantidas aliases JSP adicionais para as paginas base mais importantes.
- As paginas de erro `404` e `500` foram criadas manualmente porque nao existem no template.

## Paginas convertidas para JSP
### Espelho direto do vendor
Foram criadas JSP com o mesmo basename para quase todos os HTML em `docs/templates/Eduall/eduall`, incluindo:
- homes: `index.jsp`
- institucional: `about.jsp`, `about-two.jsp`, `about-three.jsp`, `about-four.jsp`, `faq.jsp`, `contact.jsp`, `policy-page-removed.jsp`, `pricing-plan.jsp`, `gallery.jsp`
- eventos e processos: `events.jsp`, `event-details.jsp`, `apply-admission.jsp`, `tuition-jobs.jsp`
- cursos e conteudos: `course.jsp`, `course-list-view.jsp`, `course-details.jsp`, `lesson-details.jsp`, `favorite-course.jsp`, `book-online-class.jsp`
- tutores e instrutores: `instructor/instructor.jsp`, `instructor-two.jsp`, `instructor/instructor-details.jsp`, `tutor.jsp`, `tutor-details.jsp`, `find-tutors.jsp`
- autenticacao e perfil: `sign-in.jsp`, `sign-up.jsp`, `admin/admin-dashbord-my-profile.jsp`, `my-propyl.jsp`, `instructor/instructor-my-profile.jsp`
- dashboards e administracao: `admin/admin-dashbord.jsp`, `dashbord.jsp`, `admin/admin-dashbord-message.jsp`, `admin/admin-dashbord-courses.jsp`, `admin/admin-dashbord-wishlist.jsp`, `admin/admin-dashbord-reviews.jsp`, `admin/admin-dashbord-quiz-attempts.jsp`, `admin/admin-dashbord-settings.jsp`, `student-dashbord*.jsp`, `instructor-dashboard*.jsp`, `instructor/instructor-ashboard.jsp`
- blog e ecommerce: convertidos na fase inicial, mas removidos depois do runtime do GAPE

### Aliases adicionais mantidas no GAPE
- `login.jsp` <- copia de `sign-in.jsp`
- `admin/dashboard.jsp` <- copia de `admin/admin-dashbord.jsp`
- `profile.jsp` <- copia de `admin/admin-dashbord-my-profile.jsp`
- `courses.jsp` <- copia de `course.jsp`
- `content.jsp` <- copia de `lesson-details.jsp`
- `messages.jsp` <- copia de `admin/admin-dashbord-message.jsp`
- `forms.jsp` <- copia de `admin/admin-dashbord-settings.jsp`
- `tables.jsp` <- copia de `admin/admin-dashbord-quiz-attempts.jsp`

### Paginas de erro
- `error-404.jsp`
- `error-500.jsp`

### Total final criado
- `51` JSP espelho do vendor em runtime
- `8` aliases JSP do GAPE
- `2` paginas de erro
- Total: `61` JSP em `src/main/webapp`

## Fragments criados
- `WEB-INF/fragments/template-base-head.jspf`
- `WEB-INF/fragments/template-base-scripts.jspf`
- Estes fragments foram adicionados numa fase posterior para suportar testes estruturais e preparar uma futura consolidacao reutilizavel do `head` e do bundle de scripts, sem alterar ainda o runtime `1:1` das paginas.

## Assets copiados
Copiados para `src/main/webapp/assets`:
- todo o conteudo de `docs/templates/Eduall/eduall/assets/css`
- todo o conteudo de `docs/templates/Eduall/eduall/assets/js`
- todo o conteudo de `docs/templates/Eduall/eduall/assets/images`

## Paginas ignoradas
- `docs/templates/Eduall/documentation/index.html`
  Justificacao: documentacao do vendor, nao runtime da aplicacao.
- `docs/templates/Eduall/rtl-eduall/*`
  Justificacao: variante RTL fora do escopo atual.
- `index-2.html`
- `index-3.html`
- `index-4.html`
- `index-5.html`
- `index-6.html`
  Justificacao: variantes de homepage removidas do runtime do GAPE; a unica home mantida e `index.jsp`.
- `blog.html`
- `blog-list.html`
- `blog-classic.html`
- `blog-details.html`
- `product.html`
- `product-details.html`
- `cart.html`
- `checkout.html`
  Justificacao: areas de blog e ecommerce removidas do runtime do GAPE por nao serem necessarias nesta fase.
- `about.html`
- `about-two.html`
- `about-three.html`
- `pricing-plan.html`
- `instructor-two.html`
- `faq.html`
- `tuition-jobs.html`
- `gallery.html`
- `favorite-course.html`
- `find-tutors.html`
- `book-online-class.html`
  Justificacao: removidas para reduzir o submenu `Pages` do GAPE apenas ao conjunto funcional aprovado.

## Tratamento de paginas vazias
- `my-propyl.html`
- `instructor-my-profile.html`

Estas duas paginas existem no vendor com `0` bytes. As correspondentes JSP foram criadas com shell visual do template e um placeholder explicito a indicar que o ficheiro original estava vazio.

## Remapeamento aplicado
### Links internos convertidos para JSP
- Todos os links `.html` do vendor foram convertidos para `.jsp` com o mesmo basename quando a pagina existe no espelho.
- `courses.html` foi remapeado para `courses.jsp`.
- `dashboard.html` foi remapeado para `admin/dashboard.jsp`.
- `faqs.html` foi remapeado para `faq.jsp`.

### Correcao de assets partidos
- `../../assets/images/shapes/curve-arrow.png` -> `assets/images/shapes/curve-arrow.png`
- `assets/images/thumbs/user-two-img6.png` -> `assets/images/thumbs/user-two-img5.png`
- `assets/images/logo/marquee-img-1.png` -> `assets/images/thumbs/brand-img1.png`
- `assets/images/logo/marquee-img-2.png` -> `assets/images/thumbs/brand-img2.png`
- `assets/images/logo/marquee-img-3.png` -> `assets/images/thumbs/brand-img3.png`
- `assets/images/logo/marquee-img-6.png` -> `assets/images/thumbs/brand-img6.png`

## Correcoes aplicadas
- Inclusao de `<base href="${pageContext.request.contextPath}/">` em todas as JSP convertidas.
- Substituicao do `jQuery` por CDN pela copia local `assets/js/jquery-3.7.1.min.js`.
- Correcao de `javarscript:void(0)` para `javascript:void(0)`.
- Guardas adicionadas em `assets/js/main.js` para inicializacao de `DataTable` e `Plyr`.
- Unificacao da logica de toggle de password em `assets/js/main.js` para suportar o markup original sem handlers duplicados.
- Criada a pasta `assets/css/images` com os sprites `ui-icons_*.png` exigidos por `assets/css/jquery-ui.css`.

## Justificacao das decisoes
- O pedido passou a ser espelhar o template inteiro, pelo que a melhor opcao foi converter todas as paginas do vendor em vez de selecionar apenas um subconjunto.
- Manter o mesmo basename nas JSP preserva a organizacao do vendor e torna facil decidir mais tarde quais paginas apagar.
- As aliases do GAPE foram mantidas para nao partir os atalhos ja usados no projeto.
- Os menus e footers foram limpos para remover referencias a `Product`, `Cart`, `Checkout` e `Blog`, e a secao `Recent Articles` da `index.jsp` foi removida para evitar navegacao para paginas excluidas.
- O submenu `Pages` foi reescrito em todas as JSP para expor apenas as paginas aprovadas, e os links antigos foram remapeados ou removidos para evitar caminhos partidos.
- As unicas adaptacoes estruturais inevitaveis foram as paginas de erro e os placeholders para ficheiros-fonte vazios.
- O restante foco foi garantir fidelidade visual ao EduAll e navegacao local sem links partidos.
