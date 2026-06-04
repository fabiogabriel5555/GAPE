# Template Tests

## Objetivo
Validar a integridade estrutural da integracao visual EduAll no GAPE sem depender de runtime manual no browser.

## Ficheiros criados
- `src/test/java/pt/isel/gape/structure/TemplateStructureTest.java`
- `src/test/java/pt/isel/gape/structure/TemplateAssetReferenceTest.java`
- `src/main/webapp/WEB-INF/fragments/template-base-head.jspf`
- `src/main/webapp/WEB-INF/fragments/template-base-scripts.jspf`

## O que os testes cobrem

### `TemplateStructureTest`
- existencia de `src/main/webapp/assets`
- existencia de `assets/css`, `assets/js`, `assets/images` e `assets/css/images`
- existencia dos fragments reutilizaveis base em `WEB-INF/fragments`
- existencia das paginas JSP base usadas como runtime atual
- existencia de `docs/docs/analysis/eduall-template-analysis.md`

### `TemplateAssetReferenceTest`
- referencias CSS validas em `.jsp` e `.jspf`
- referencias JS validas em `.jsp` e `.jspf`
- referencias de imagens validas em `.jsp`, `.jspf` e `url(...)` de ficheiros CSS
- includes JSP validos quando existirem
- ausencia de caminhos locais quebrados obvios em `href`, `src` e `url(...)`

## Correcao aplicada durante a criacao dos testes
- `assets/css/jquery-ui.css` tinha referencias locais para `images/ui-icons_*.png`, mas a pasta `src/main/webapp/assets/css/images` nao existia.
- Foram copiados os sprites `ui-icons_*.png` para `src/main/webapp/assets/css/images` para alinhar os assets com o CSS entregue.

## Comandos de execucao
```bash
mvn test -Dtest=TemplateStructureTest
mvn test -Dtest=TemplateAssetReferenceTest
```

## Passo a passo manual

Verificacao manual no browser (complementa os testes automaticos):

1. Executar `mvn clean package`.
2. Iniciar o Tomcat e fazer deploy do WAR.
3. Abrir a pagina inicial (`index.jsp`).
4. Abrir o login (`login.jsp` / `sign-in.jsp`).
5. Abrir o dashboard (`dashboard.jsp` / `admin-dashbord.jsp`).
6. Abrir as restantes paginas base (cursos, conteudos, mensagens, perfil, eventos, erro).
7. Abrir o DevTools do browser na aba `Network`.
8. Recarregar a pagina.
9. Confirmar que nao ha CSS com `404`.
10. Confirmar que nao ha JS com `404`.
11. Confirmar que nao ha imagens com `404`.
12. Confirmar o aspeto visual em Chrome, Edge e Firefox.
13. Confirmar que nao existe login real (os formularios nao submetem para Servlet).
14. Confirmar que nao existe CRUD real.
15. Confirmar que a documentacao (`docs/docs/analysis/eduall-template-analysis.md`) indica que paginas do EduAll foram analisadas, aproveitadas, ignoradas e alteradas.

## Notas
- As JSP publicas atuais nao usam `<%@ include %>` nem `<jsp:include>`, mas o teste cobre esses casos para futuras extracoes em fragments.
- Os dois fragments criados sao uma base reutilizavel para consolidacao futura do head e do bundle de scripts sem alterar ainda o runtime 1:1 das paginas.
