# EduAll Admin Turmas E Conteudos Pedagogicos

## Objetivo

Ligar a pagina de detalhe de turmas do administrador aos conteudos pedagogicos associados aos respetivos blocos pedagogicos, mantendo o shell visual do EduAll e sem alterar as paginas de coordenador ou professor.

## Analise Antes Da Alteracao

Foram analisadas as superficies relacionadas com:

- turmas: `admin/admin/class-group/admin-class-groups.jsp`, `admin-class-group-detail.jsp`, `admin-class-group-form.jsp`, `admin-content-block-form.jsp` e `ClassGroupManagementServlet`;
- cursos e disciplinas: `admin-course-detail.jsp`, `admin-subject-detail.jsp`, `CourseManagementServlet` e `SubjectManagementServlet`;
- aulas/conteudos do template: `lesson-details.jsp`, `content.jsp` e paginas HTML do EduAll em `docs/templates/Eduall`;
- blocos pedagogicos: `ContentBlockDAO`, `ContentBlockService`, `ContentBlockView` e a seccao `Pedagogical Blocks` no detalhe da turma;
- documentos, materiais, upload e download: `ContentUploadServlet`, `ContentDownloadServlet`, `ContentItemService`, `ContentAssociationService`, `ContentAssociationDAO`, `PdfUploadService` e tabelas `content_item` / `associate_block_content`.

Tambem foram consultadas as imagens em `docs/fotos-exemplos`. A referencia principal foi o ecran de outline do LearnWorlds, em que o curso e organizado por secoes e cada secao contem learning activities com icone, formato e acoes.

Referencias online consultadas:

- LearnWorlds, "Learning activities supported by LearnWorlds": https://support.learnworlds.com/support/solutions/articles/5000652836-learning-activities-supported-by-learnworlds
- LearnWorlds, "How to Upload a PDF file to your Courses (PDF Learning Activity)": https://support.learnworlds.com/support/solutions/articles/5000694081-how-to-upload-a-pdf-file-to-your-courses-pdf-learning-activity-
- LearnWorlds, "General overview: Creating courses": https://support.learnworlds.com/support/solutions/articles/12000079954-general-overview-creating-courses
- LearnWorlds, "How to use Sections to Organize your Course Content": https://support.learnworlds.com/support/solutions/articles/12000001679-how-to-use-sections-to-organize-your-course-content

## Decisao De UX

A pagina admin de detalhe da turma continua centrada na turma. A seccao `Pedagogical Blocks` passa a funcionar como outline:

- cada bloco pedagogico e uma seccao;
- os conteudos do bloco aparecem dentro dessa seccao;
- PDFs aparecem primeiro e recebem destaque visual;
- cada conteudo mostra formato, estado, obrigatoriedade, papel na associacao e ordem;
- PDFs tem acoes separadas para visualizar inline e descarregar;
- links externos/embed ficam com acao `Open`;
- restantes formatos aparecem como registados, mas sem preview quando ainda nao existe visualizador especifico.

## Alteracoes Implementadas

- Criado `BlockContentItem` para transportar o `ContentItem` com metadata da associacao ao bloco.
- Criado `BlockContentItemView` para preparar labels, badges, icones e flags usadas pelo JSP.
- Adicionado `ContentAssociationDAO.findBlockContentItems(...)`, lendo `associate_block_content` e ordenando PDFs antes dos restantes formatos.
- Adicionado `ContentAssociationService.listBlockContentItems(...)`, reutilizando a politica de acesso de conteudos.
- Atualizado `ClassGroupManagementServlet.showDetail(...)` para preencher `blockContentsByBlock`, `blockContentCount`, `pdfContentCount` e estado de erro/sucesso.
- Atualizado `admin-class-group-detail.jsp` para mostrar conteudos por bloco com estados de loading, vazio, erro e sucesso.
- Atualizado `ContentDownloadServlet` para aceitar `?disposition=inline`, mantendo `attachment` por defeito.

## Limites Mantidos

- A listagem principal de turmas nao foi redesenhada.
- Paginas de coordenador e professor nao foram alteradas.
- Nao foi criada uma UI nova de upload na pagina de turmas, porque o pedido principal era mostrar e usar conteudos ja associados aos blocos.
- O preview/download continua limitado a PDF, alinhado com a prioridade definida e com o backend atual.

## Validacao Planeada

- Compilar o projeto.
- Executar `mvn test -Dtest=ContentAssociationServiceTest`.
- Verificar que a pagina de detalhe de turma recebe blocos, conteudos por bloco, resumo de conteudos/PDFs e links `/contents/download/{id}`.
- Verificar visualmente a pagina no browser quando o servidor local estiver disponivel.

## Validacao Executada

- `mvn -q -DskipTests compile`: sucesso.
- `mvn -q -DskipTests package`: sucesso.
- `mvn test -Dtest=ContentAssociationServiceTest`: sucesso.
- `mvn test "-Dtest=ContentAssociationServiceTest,ContentItemServiceTest,PdfUploadServiceTest"`: sucesso, 30 testes.
- Tomcat local de QA em `http://localhost:18080/GAPE`: listagem `/learning/class-groups` respondeu 200 e apresentou links de detalhe.
- Browser interno `iab` recuperado usando a versao correta do plugin Browser (`26.609.71450`) em vez da referencia antiga `26.602.40724`.
- Browser interno, detalhe `/learning/class-groups/50` em desktop: pagina correta, `Pedagogical Blocks`, card de conteudo, `Guia da UC`, link inline, link de download e zero erros de consola.
- Browser interno, detalhe `/learning/class-groups/50` em mobile `390x1100`: card visivel, sem overflow horizontal, `View` e `Download` visiveis e zero erros de consola.
- Browser interno, detalhe `/learning/class-groups/52`: apresentou o estado vazio de conteudos por bloco e zero erros de consola.
- Browser interno, PDF `/contents/download/70?disposition=inline`: abriu em separador sem erros de consola.
- Capturas de validacao geradas com `docs/dev/scripts/browser-screenshot.ps1` apos aumentar timeouts e corrigir o fallback para usar Edge headless, perfil temporario sem espacos e cookies isolados:
  - `target/browser-screenshots/admin-class-group-content-desktop-tall.png`
  - `target/browser-screenshots/admin-class-group-content-mobile-full.png`
- PDF `/contents/download/70?disposition=inline`: respondeu 200, `Content-Type: application/pdf`, `Content-Disposition: inline; filename="guia_da_uc.pdf"`.
- PDF `/contents/download/70`: respondeu 200, `Content-Disposition: attachment; filename="guia_da_uc.pdf"`.

A chamada de screenshot direta do Browser interno ainda pode falhar por timeout em `Page.captureScreenshot`; quando isso acontece, o fallback oficial do projeto gera PNGs com Edge headless e timeout de 300 segundos. A navegacao, DOM, viewport mobile, PDF inline e consola foram validados no Browser interno. O download `attachment` foi validado por HTTP/header, porque o Browser bloqueia a navegacao direta para ficheiro descarregado.

## Atualizacao - Wizard De Criacao E Edicao De Turmas

Depois da revisao visual das capturas em `docs/fotos-exemplos`, a intervencao correta passou a ser a pagina de criacao/edicao de turmas do admin, nao apenas o detalhe da turma.

Foram usadas como referencia principal as capturas LearnWorlds:

- `Captura de ecra 16-06-2026 13-23-22.png`: wizard inicial com preview lateral, barras de progresso e formulario principal;
- `Captura de ecra 16-06-2026 13-24-06.png`: passo de acesso com escolhas tipo radio e botoes `Back` / `Continue`;
- `Captura de ecra 16-06-2026 13-25-13.png`: outline/revisao com secoes numeradas e conteudo estruturado.

### Decisao De UX Do Wizard

O ficheiro `src/main/webapp/admin/admin/class-group/admin-class-group-form.jsp` foi adaptado para uma sequencia de quatro passos, mantendo o contrato do backend:

1. `Context`: curso e disciplina, editaveis tanto em criacao como em edicao.
2. `Setup`: codigo da turma, turno e modalidade.
3. `Access`: estado da turma, capacidade e datas.
4. `Review`: resumo em outline numerado antes de gravar.

O formulario continua a submeter os mesmos nomes de campos:

- `courseId`;
- `subjectId`;
- `code`;
- `modality`;
- `state`;
- `minStudents`;
- `maxStudents`;
- `startsAt`;
- `endsAt`;
- `shift`.

Assim, `ClassGroupManagementServlet`, `ClassGroupFormData` e os services existentes continuam compativeis.

### Alteracoes Implementadas No Formulario

- Preview lateral persistente, inspirado no card de curso LearnWorlds, adaptado para turma: estado, codigo, curso/disciplina, modalidade, turno, capacidade e datas.
- Progress bar de quatro passos no topo da area principal.
- `Course` e `Subject` estao disponiveis tambem em `Edit Class Group`, permitindo alterar o contexto da turma.
- `Subject` e reconstruido a partir do `Course` escolhido, evitando opcoes de outras associacoes.
- `Course` e `Subject` usam selects nativos nesta pagina, em vez de Select2, porque a combinacao anterior deixava o dropdown de `Subject` inconsistente e por vezes nao clicavel.
- `modality` e `state` passaram de selects para cards/radios, mantendo os mesmos valores enviados ao backend.
- Revisao final em formato outline, com blocos numerados `01`, `02`, `03`.
- Validacao client-side por passo antes de avancar.
- Validacao cruzada client-side:
  - `maxStudents` tem de ser maior ou igual a `minStudents`;
  - `endsAt` nao pode ser anterior a `startsAt`.
- `code` e `shift` passaram a ter `maxlength="30"`, alinhado com o schema.
- Responsividade revista para mobile: preview, progresso, formulario e acoes empilham sem overflow horizontal.

### Validacao Executada Para O Wizard

- `mvn -q -DskipTests package`: sucesso.
- Tomcat local de QA: `http://localhost:18080/GAPE`.
- New Class Group `/learning/class-groups/new`:
  - pagina correta;
  - wizard presente;
  - passo inicial renderizado;
  - selecao de curso/disciplina;
  - avanco para `Setup`;
  - preenchimento de codigo/turno/modalidade;
  - avanco para `Access`;
  - preenchimento de capacidade/datas;
  - avanco para `Review`;
  - preview e review atualizados.
- Validacao cruzada: `minStudents=50` e `maxStudents=10` bloqueou o avanco e mostrou a mensagem HTML esperada.
- Edit Class Group `/learning/class-groups/50/edit`:
  - `Course` e `Subject` aparecem como selects editaveis;
  - ao trocar `Course` de `Engenharia Informatica` para `Analise de Dados`, a lista de `Subject` passou a conter apenas `Matematica Aplicada` do novo curso;
  - preview e review refletem o novo par `Course`/`Subject`;
  - passos `Setup`, `Access` e `Review` renderizados;
  - botao final `Save Changes`.
- Mobile `390x1150`:
  - sem overflow horizontal;
  - preview aparece antes do wizard;
  - selects e botoes cabem no viewport.
- Capturas geradas:
  - `target/browser-screenshots/admin-class-group-wizard-fallback-first-step.png`;
  - `target/browser-screenshots/admin-class-group-wizard-new-review-desktop-clean.png`;
  - `target/browser-screenshots/admin-class-group-wizard-edit-review-desktop.png`;
  - `target/browser-screenshots/admin-class-group-wizard-new-mobile.png`.

### Atualizacao De Regra Backend Para Edicao De Contexto

- `ClassGroupService.updateClassGroup(...)` passou a aceitar mudanca de `courseId`/`subjectId` apenas para `ADMINISTRATOR`.
- Coordenadores e professores continuam impedidos de mover uma turma para outro curso/disciplina.
- `ClassGroupDAO.update(...)` passou a atualizar `id_course` e `id_subject`.
- O trigger `bu_class_group_validate` deixou de bloquear a mudanca de contexto, mas continua a validar que o novo `Course` integra o `Subject` e que o contexto nao esta arquivado.
- Teste atualizado: `ClassGroupServiceTest` valida admin a mudar contexto e nao-admin bloqueado.
- Captura adicional:
  - `target/browser-screenshots/admin-class-group-edit-course-subject-selects.png`.
- Correcao final de sincronizacao:
  - `target/browser-screenshots/admin-class-group-create-native-selects-first-step.png`;
  - validado por DOM que `Create Class Group` com `Course=30` mostra `Matematica Aplicada`, `Bases de Dados` e `Projeto`;
  - validado por DOM que `Edit Class Group` ao mudar para `Course=31` mostra apenas `Matematica Aplicada` desse curso.

Nesta sessao, o plugin Browser instalado tinha o `SKILL.md`, mas nao tinha `scripts/browser-client.mjs`. Por isso a validacao interativa foi feita com Edge headless via debug remoto/CDP, e a captura independente foi confirmada com `docs/dev/scripts/browser-screenshot.ps1 -TimeoutSeconds 300`.
