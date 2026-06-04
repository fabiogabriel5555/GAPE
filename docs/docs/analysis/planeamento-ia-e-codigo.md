# 0. Instruções para criar agentes

Este ponto define os agentes usados no desenvolvimento, teste, correção e revisão do projeto GAPE.

Os agentes Codex são usados para:

* implementar código;
* criar testes;
* executar testes;
* analisar erros encontrados nos testes;
* corrigir erros encontrados;
* repetir testes depois das correções;
* atualizar documentação técnica;
* atualizar documentação de testes;
* preparar dados de demonstração.

Os agentes Claude Code são usados para:

* rever a implementação;
* comparar a implementação com o planeamento;
* verificar se a fase cumpriu o objetivo;
* verificar se os testes existem e foram executados;
* verificar se o Codex corrigiu erros encontrados nos testes;
* validar arquitetura;
* validar segurança;
* validar SQL;
* validar XML/XSD;
* validar integração front-end/back-end;
* corrigir erros pequenos;
* propor correções grandes e pedir autorização antes de as aplicar.

Documentos obrigatórios do projeto:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

---

## 0.1 Regra geral para agentes Codex

Todos os agentes Codex devem seguir este ciclo:

1. analisar os documentos obrigatórios;
2. implementar a parte pedida;
3. criar testes automáticos;
4. criar documentação de testes;
5. executar os testes;
6. analisar erros encontrados;
7. corrigir os erros;
8. repetir os testes;
9. atualizar documentação;
10. só considerar a tarefa concluída quando os testes relevantes passarem.

Os agentes Codex podem corrigir:

* erros de compilação;
* erros de imports;
* erros de nomes de classes;
* erros de caminhos;
* erros em SQL;
* erros em XML/XSD;
* erros em DAOs;
* erros em Services;
* erros em Servlets;
* erros em filtros;
* erros em JSP;
* erros nos testes;
* erros nos dados de teste;
* erros de integração entre front-end e back-end.

Se a correção exigir mudar arquitetura, modelo EA, estrutura global da base de dados ou uma decisão importante do projeto, o Codex deve:

1. explicar o problema;
2. explicar a solução proposta;
3. indicar os ficheiros que pretende alterar;
4. pedir autorização antes de aplicar a alteração.

---

## 0.2 Regra geral para prompts de back-end

Todos os prompts de back-end devem dar liberdade suficiente à IA para implementar corretamente a partir do modelo EA, das restrições aplicacionais e dos casos de uso.

Sempre que uma fase tiver back-end, o Codex Backend Agent, e o Codex Security Agent quando aplicável, deve:

1. analisar a parte do modelo EA relevante para a fase, incluindo entidades, atributos, associações, cardinalidades e restrições de integridade aplicacional;
2. analisar os requisitos e os casos de uso da fase, incluindo cenários principais e alternativos;
3. identificar todas as entidades, entidades fracas e associações que a fase tem de suportar;
4. decidir quais Models criar;
5. decidir quais DAOs criar e que operações JDBC são necessárias;
6. decidir quais Services criar e onde fica cada regra de negócio;
7. decidir quais Servlets, filtros, listeners e validadores criar;
8. implementar as restrições de integridade aplicacional na camada correta, normalmente Service ou filtro, quando não estejam garantidas em SQL;
9. respeitar a organização em pacotes do projeto;
10. reutilizar Models, DAOs, Services e helpers já existentes em vez de duplicar;
11. tratar transações quando várias escritas tiverem de ser atómicas;
12. registar auditoria das operações críticas;
13. criar e executar os testes do back-end;
14. corrigir os erros encontrados e repetir os testes;
15. documentar as classes criadas e as regras implementadas;
16. criar todas as classes necessárias para a funcionalidade ficar completa.

O agente não deve limitar-se às classes indicadas no prompt.

As classes indicadas no prompt são apenas exemplos ou pontos mínimos de partida.

Se o modelo exigir mais Models, DAOs, Services, filtros ou validadores, o agente deve criá-los.

O agente deve respeitar sempre:

* não usar Spring;
* não usar Hibernate;
* não usar JPA;
* não colocar SQL nas JSP;
* não colocar regras de negócio nas JSP;
* não colocar regras de negócio nos DAOs;
* não colocar lógica de negócio pesada nos Servlets;
* não aceder a JDBC a partir de Servlets ou JSP;
* não guardar credenciais no código;
* não duplicar em SQL regras que dependem de contexto aplicacional.

---

## 0.3 Regra geral para prompts de front-end

Todos os prompts de front-end devem dar liberdade suficiente à IA para trabalhar corretamente com o template EduAll.

Sempre que uma fase tiver front-end, o Codex Frontend/JSP Agent deve:

1. analisar o template EduAll completo;
2. identificar todas as páginas do template relacionadas com a funcionalidade da fase;
3. identificar fragments reutilizáveis;
4. identificar menus, breadcrumbs, cards, dashboards, formulários, tabelas, modais, páginas de detalhe, páginas de listagem e componentes reutilizáveis;
5. identificar CSS, JavaScript, imagens e assets afetados;
6. decidir quais ficheiros devem ser alterados;
7. decidir quais páginas devem ser criadas;
8. decidir quais páginas existentes devem deixar de ser estáticas;
9. transformar as páginas necessárias em JSP dinâmicas;
10. ligar formulários e ações aos Servlets corretos;
11. apresentar mensagens de erro e sucesso no estilo do EduAll;
12. manter o estilo visual original sempre que possível;
13. alterar várias páginas se isso for necessário para a funcionalidade ficar completa;
14. documentar quais páginas foram alteradas e porquê;
15. testar a ligação ao back-end;
16. corrigir erros encontrados.

O agente não deve limitar-se às páginas mencionadas no prompt.

As páginas mencionadas no prompt são apenas exemplos ou pontos mínimos de partida.

Se o template tiver outras páginas mais adequadas, o agente deve usá-las.

Se várias páginas forem necessárias para completar a funcionalidade, o agente deve alterar todas as necessárias.

O agente deve respeitar sempre:

* não colocar SQL nas JSP;
* não colocar regras de negócio nas JSP;
* não duplicar segurança apenas no front-end;
* não substituir validações do back-end por validações visuais;
* não quebrar o estilo visual do EduAll;
* não alterar páginas sem relação com a fase, exceto se isso for necessário para navegação, layout, menus ou integração.

---

## 0.4 Regra geral para prompts Claude Code

Todos os prompts do Claude Code devem seguir esta estrutura:

```text
Usa os agentes:
- [lista de agentes Claude Code aplicáveis]

Objetivo da revisão:
[explicar claramente o que a revisão deve validar]

Antes de rever, analisa obrigatoriamente:
- 1. Planeamento de IA e Código;
- 7. Relatorio - 49862 - 7;
- 0. GAPE - ALL - V3;
- código implementado na fase;
- testes criados na fase;
- documentação de testes da fase;
- ficheiros alterados na fase.

Importante:
Não deves rever os prompts em si.
Deves rever se aquilo que estava previsto para a fase foi realmente implementado no projeto.

Deves verificar:
- objetivo da fase;
- resultado esperado;
- critérios de conclusão;
- ficheiros/classes/páginas/scripts criados;
- arquitetura;
- testes automáticos;
- testes manuais;
- correções feitas pelo Codex depois dos testes;
- restrições aplicacionais aplicáveis;
- integração front-end/back-end, quando existir;
- documentação.

Executa os testes aplicáveis.

Se os testes falharem:
- identifica a causa provável;
- indica se o erro está no código, nos testes, nos dados, no SQL, na configuração ou na integração;
- podes corrigir erros pequenos;
- para erros grandes, explica o problema, propõe solução e pede autorização.

No final, produz um relatório com:
- estado da fase: aprovada, aprovada com reservas ou reprovada;
- elementos corretos;
- elementos em falta;
- erros encontrados;
- testes executados;
- testes em falta;
- correções pequenas feitas;
- correções grandes propostas;
- autorização necessária, se aplicável.
```

---

## 0.5 Agentes a criar no Codex

### Prompt para criar o Codex Document Analyst

Cria um agente chamado `Codex Document Analyst`.

Função:

* analisar documentação do projeto;
* analisar relatórios do projeto;
* analisar o documento `0. GAPE - ALL - V3`;
* analisar ficheiros XML, XSD e documentos do professor;
* analisar código de exemplo feito pelo professor;
* extrair regras úteis para implementação;
* produzir ficheiros em `docs/docs/analysis/`;
* ajudar outros agentes a perceberem o que deve ser implementado com base na documentação;
* corrigir relatórios de análise quando forem encontrados erros ou incoerências.

---

### Prompt para criar o Codex Database Agent

Cria um agente chamado `Codex Database Agent`.

Função:

* analisar o modelo de dados do GAPE;
* copiar ficheiros XML/XSD úteis do professor;
* criar ficheiros XML/XSD em falta;
* criar `schema.sql`;
* criar `drop.sql`;
* criar `data-test-valid.sql`;
* criar `data-test-invalid.sql`;
* criar `data-demo.sql`;
* criar configuração JDBC;
* criar testes automáticos da base de dados;
* executar os testes da base de dados;
* corrigir erros encontrados nos testes da base de dados;
* corrigir SQL, dados de teste, triggers, constraints e configuração JDBC quando os testes falharem;
* repetir os testes até passarem.

---

### Prompt para criar o Codex Backend Agent

Cria um agente chamado `Codex Backend Agent`.

Função:

* criar Models;
* criar DAOs com JDBC;
* criar Services;
* criar Servlets;
* criar filtros;
* criar listeners, quando necessário;
* aplicar regras de negócio na camada correta;
* aplicar as restrições de integridade aplicacional do modelo EA na camada de negócio;
* respeitar a arquitetura `JSP → Servlet → Service → DAO → JDBC → MySQL`;
* executar ou apoiar testes do back-end;
* corrigir erros encontrados nos testes do back-end;
* corrigir erros em Models, DAOs, Services, Servlets e filtros;
* não usar Spring;
* não usar Hibernate;
* não usar JPA;
* não colocar SQL nas JSP;
* não colocar regras de negócio nos DAOs;
* não colocar lógica de negócio pesada nos Servlets.

---

### Prompt para criar o Codex Frontend/JSP Agent

Cria um agente chamado `Codex Frontend/JSP Agent`.

Função:

* analisar o template EduAll completo;
* identificar autonomamente as páginas que precisam de alteração;
* identificar fragments reutilizáveis;
* identificar páginas de listagem, detalhe, criação, edição, dashboards, menus, cards, tabelas, formulários e modais úteis;
* integrar o template EduAll;
* criar fragments JSP;
* criar páginas JSP;
* transformar páginas HTML estáticas em JSP dinâmicas;
* criar formulários;
* criar tabelas;
* criar dashboards;
* adaptar menus;
* adaptar header;
* adaptar sidebar;
* adaptar mensagens de erro e sucesso;
* reaproveitar o estilo visual do template;
* ligar formulários e tabelas JSP aos Servlets;
* testar páginas JSP;
* corrigir erros de caminhos, formulários, mensagens, includes, assets e ligação ao back-end;
* documentar páginas alteradas e motivo da alteração;
* não colocar SQL nas JSP;
* não colocar regras de negócio nas JSP.

Regra obrigatória:

O agente não deve limitar-se a uma página indicada no prompt.

Deve analisar o template EduAll completo e decidir todas as páginas, fragments, componentes e assets que precisam de alteração para a funcionalidade ficar completa.

---

### Prompt para criar o Codex Test Agent

Cria um agente chamado `Codex Test Agent`.

Função:

* criar testes unitários;
* criar testes DAO;
* criar testes Service;
* criar testes de Servlets;
* criar testes de filtros;
* criar testes de permissões;
* criar testes de dados válidos;
* criar testes de dados inválidos;
* criar testes de base de dados;
* criar checklists de testes manuais;
* executar testes;
* analisar falhas;
* corrigir testes incorretos;
* corrigir dados de teste incorretos;
* pedir ao agente Codex adequado para corrigir código de produção quando a falha não for do teste;
* voltar a executar os testes depois da correção;
* atualizar documentação de testes.

Importante:

O Codex Test Agent não deve apenas listar o que testar.

Deve criar testes executáveis, explicar como os executar, executar os testes, analisar falhas e coordenar correções até os testes passarem.

---

### Prompt para criar o Codex Security Agent

Cria um agente chamado `Codex Security Agent`.

Função:

* implementar autenticação;
* implementar logout;
* implementar sessão;
* implementar expiração de sessão;
* implementar permissões;
* implementar filtros de segurança;
* validar uploads;
* proteger dados pessoais;
* bloquear acessos indevidos;
* garantir auditoria de operações críticas;
* testar segurança;
* corrigir erros de segurança encontrados nos testes;
* corrigir falhas em filtros, sessão, permissões e validações.

---

### Prompt para criar o Codex Demo Agent

Cria um agente chamado `Codex Demo Agent`.

Função:

* criar `data-demo.sql`;
* criar dados finais de demonstração;
* criar testes de demonstração;
* preparar fluxos de demonstração;
* executar testes de demonstração;
* corrigir dados incoerentes encontrados nos testes;
* garantir que os fluxos principais funcionam no template EduAll.

---

## 0.6 Agentes a criar no Claude Code

### Prompt para criar o Architecture Reviewer

Cria um agente chamado `Architecture Reviewer`.

Função:

* verificar se o projeto respeita `JSP → Servlet → Service → DAO → JDBC → MySQL`;
* confirmar que JSP não tem SQL;
* confirmar que JSP não tem regras de negócio;
* confirmar que Servlet não tem lógica de negócio pesada;
* confirmar que Service contém regras de negócio;
* confirmar que DAO só trata de JDBC/SQL;
* verificar se o código de desenvolvimento fica separado do código web;
* comparar a implementação com o planeamento;
* executar testes de arquitetura quando existirem;
* corrigir erros pequenos;
* para erros grandes, pedir autorização antes de alterar.

---

### Prompt para criar o SQL Reviewer

Cria um agente chamado `SQL Reviewer`.

Função:

* rever `schema.sql`;
* rever `drop.sql`;
* rever scripts SQL;
* rever DAOs;
* verificar PK, FK, UNIQUE, NOT NULL, CHECK, triggers e índices;
* verificar PreparedStatement;
* verificar try-with-resources;
* verificar coerência com `0. GAPE - ALL - V3`;
* executar testes SQL quando existirem;
* corrigir erros pequenos;
* para erros grandes, pedir autorização antes de alterar.

---

### Prompt para criar o XML/XSD Reviewer

Cria um agente chamado `XML/XSD Reviewer`.

Função:

* rever XML;
* rever XSD;
* confirmar se XML valida contra XSD;
* verificar estados, tipos, modalidades e formatos;
* encontrar valores em falta;
* encontrar valores duplicados;
* executar testes XML/XSD quando existirem;
* corrigir erros pequenos;
* para erros grandes, pedir autorização antes de alterar.

---

### Prompt para criar o Security Reviewer

Cria um agente chamado `Security Reviewer`.

Função:

* rever login;
* rever logout;
* rever sessão;
* rever expiração de sessão;
* rever permissões;
* rever filtros;
* rever uploads;
* rever dados pessoais;
* rever auditoria;
* executar testes de segurança;
* corrigir erros pequenos;
* para erros grandes, pedir autorização antes de alterar.

---

### Prompt para criar o Test Reviewer

Cria um agente chamado `Test Reviewer`.

Função:

* executar testes;
* rever testes criados pelo Codex;
* identificar testes em falta;
* verificar se os testes explicam como são executados;
* verificar se existe passo a passo manual;
* validar se o Codex corrigiu erros encontrados nos testes;
* validar se os testes foram repetidos depois das correções;
* corrigir testes pequenos;
* para erros grandes na estratégia de testes, pedir autorização antes de alterar.

---

### Prompt para criar o Demo Reviewer

Cria um agente chamado `Demo Reviewer`.

Função:

* rever a demonstração final;
* validar dados de demonstração;
* verificar se o fluxo principal funciona;
* executar testes de demonstração;
* indicar funcionalidades instáveis;
* corrigir erros pequenos;
* para erros grandes, pedir autorização antes de alterar.

---

### Prompt para criar o Document Analyst Reviewer

Cria um agente chamado `Document Analyst Reviewer`.

Função:

* rever os ficheiros de análise em `docs/docs/analysis/`;
* confirmar que as análises são coerentes com `0. GAPE - ALL - V3`, com o relatório e com os ficheiros do professor;
* identificar regras do modelo em falta nas análises;
* identificar incoerências entre análise e implementação;
* corrigir erros pequenos;
* para erros grandes, pedir autorização antes de alterar.

---

### Prompt para criar o Frontend/JSP Reviewer

Cria um agente chamado `Frontend/JSP Reviewer`.

Função:

* verificar se a IA analisou o template EduAll completo;
* verificar se o front-end não ficou limitado a uma página;
* verificar se todas as páginas necessárias foram alteradas;
* verificar se a documentação indica páginas analisadas, aproveitadas, ignoradas e alteradas;
* verificar se não há SQL nas JSP;
* verificar se não há regras de negócio nas JSP;
* verificar se os formulários ligam aos Servlets corretos;
* verificar CSS, JS, imagens e includes;
* corrigir erros pequenos;
* para erros grandes, pedir autorização antes de alterar.

---

# 1. Fase 0 — Preparação obrigatória

## Objetivo da fase

Criar a estrutura inicial do projeto GAPE, preparar Maven, organizar pastas, configurar Git e garantir que o projeto compila.

## Resultado esperado

No final deve existir:

* projeto Maven;
* estrutura base Java organizada por pacotes funcionais;
* estrutura base web;
* estrutura base de recursos;
* estrutura base de testes;
* `.gitignore`;
* `pom.xml`;
* `index.jsp`;
* `login.jsp`;
* Git inicializado.

## Critérios de conclusão

* o projeto abre no IntelliJ;
* `mvn clean package` passa;
* o Tomcat arranca;
* `index.jsp` abre sem erro 404;
* `index.jsp` abre sem erro 500;
* existe documentação de testes da fase.

---

## 1.1 Prompt para Codex — estrutura inicial

Usa o agente `Codex Backend Agent`.

Antes de criar a estrutura, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Cria a estrutura inicial do projeto GAPE em Java, JSP, Servlets, JDBC, MySQL, Tomcat e Maven, organizada pelos pacotes funcionais do relatório.

Cria:

* `pom.xml`;
* `.gitignore`;
* `src/main/java/pt/isel/gape`;
* `src/main/java/pt/isel/gape/common`;
* `src/main/java/pt/isel/gape/web`;
* `src/main/java/pt/isel/gape/security`;
* `src/main/java/pt/isel/gape/access`;
* `src/main/java/pt/isel/gape/structure`;
* `src/main/java/pt/isel/gape/learning`;
* `src/main/java/pt/isel/gape/transversal`;
* `src/main/java/pt/isel/gape/integration`;
* `src/main/java/pt/isel/gape/dev`;
* `src/main/webapp`;
* `src/main/webapp/WEB-INF/views`;
* `src/main/webapp/WEB-INF/fragments`;
* `src/main/resources`;
* `src/main/resources/sql`;
* `src/main/resources/config/xml`;
* `src/main/resources/config/xsd`;
* `src/test/java`;
* `docs/docs`;
* `docs/docs/analysis`;
* `docs/tests`;
* `index.jsp` simples;
* `login.jsp` simples.

Tens liberdade para criar pacotes ou pastas adicionais se forem necessários para respeitar a organização do relatório.

Não implementes funcionalidades.

Depois de criar:

1. executa `mvn clean package`;
2. se houver erro, identifica a causa;
3. corrige o erro;
4. volta a executar;
5. só termina quando o projeto compilar.

---

## 1.2 Prompt para Codex Test Agent — testar e corrigir estrutura inicial

Usa o agente `Codex Test Agent`.

Cria:

* `ProjectStructureTest.java`;
* `docs/tests/fase-0-testes.md`.

O teste deve verificar:

1. existência de `pom.xml`;
2. existência de `.gitignore`;
3. existência dos pacotes `common`, `web`, `security`, `access`, `structure`, `learning`, `transversal`, `integration` e `dev`;
4. existência de `src/main/webapp`;
5. existência de `WEB-INF/views`;
6. existência de `WEB-INF/fragments`;
7. existência de `src/main/resources/sql`;
8. existência de `src/main/resources/config/xml`;
9. existência de `src/main/resources/config/xsd`;
10. existência de `src/test/java`;
11. existência de `docs/docs`;
12. existência de `docs/docs/analysis`;
13. existência de `docs/tests`;
14. existência de `index.jsp`;
15. existência de `login.jsp`.

Executa:

```bash
mvn clean package
mvn test -Dtest=ProjectStructureTest
```

Se algum teste falhar:

1. identifica a causa;
2. corrige a pasta, ficheiro, dependência ou configuração em falta;
3. repete os testes;
4. atualiza `docs/tests/fase-0-testes.md`.

---

## 1.3 Testes manuais feitos por ti

1. Abrir o IntelliJ.
2. Escolher `File > Open`.
3. Selecionar a pasta do projeto GAPE.
4. Confirmar que o IntelliJ reconhece o projeto como Maven.
5. Abrir o terminal do IntelliJ.
6. Executar:

```bash
mvn clean package
```

7. Confirmar `BUILD SUCCESS`.
8. Configurar Tomcat.
9. Iniciar Tomcat.
10. Abrir:

```text
http://localhost:8080/gape
```

11. Confirmar que `index.jsp` aparece.
12. Confirmar ausência de erro 404.
13. Confirmar ausência de erro 500.
14. Verificar visualmente as pastas principais.
15. Se faltar algo, pedir ao Codex para corrigir e repetir os testes.

---

## 1.4 Prompt para Claude Code — Revisão da Fase 0

Usa os agentes:

* `Architecture Reviewer`;
* `Test Reviewer`.

Objetivo da revisão:

Verificar se a Fase 0 foi realmente implementada de acordo com o planeamento, com a arquitetura esperada e com a estrutura necessária para o desenvolvimento do GAPE.

Antes de rever, analisa obrigatoriamente:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* `pom.xml`;
* `.gitignore`;
* estrutura `src/main/java`;
* estrutura `src/main/webapp`;
* estrutura `src/main/resources`;
* estrutura `src/test/java`;
* `docs/tests/fase-0-testes.md`;
* `ProjectStructureTest.java`.

Importante:

Não deves rever os prompts em si.

Deves rever se aquilo que era suposto estar feito na Fase 0 foi realmente implementado.

Verifica:

* se a estrutura Maven existe;
* se `pom.xml` está correto;
* se as pastas estão bem organizadas pelos pacotes funcionais;
* se não existem dependências desnecessárias;
* se o projeto compila;
* se `index.jsp` e `login.jsp` existem;
* se existe separação inicial entre os pacotes `common`, `web`, `security`, `access`, `structure`, `learning`, `transversal`, `integration` e `dev`;
* se os testes automáticos existem;
* se os testes automáticos foram executados;
* se existe passo a passo manual em `docs/tests/fase-0-testes.md`;
* se o Codex corrigiu erros encontrados nos testes.

Executa:

```bash
mvn clean package
mvn test -Dtest=ProjectStructureTest
```

Se os testes falharem:

* identifica a causa;
* podes corrigir erros pequenos;
* se for erro estrutural grande, explica o problema, propõe solução e pede autorização.

No final, produz relatório com:

* estado da fase;
* elementos corretos;
* elementos em falta;
* testes executados;
* erros encontrados;
* correções feitas;
* correções propostas.

---

## 1.5 Commit esperado

```bash
git add .
git commit -m "Cria estrutura inicial do projeto GAPE"
```

---

# 2. Fase 1 — Ficheiros de suporte, XML/XSD, base de dados e JDBC

## Objetivo da fase

Criar a base técnica inicial do GAPE: ficheiros de suporte, XML/XSD, base de dados MySQL completa, scripts SQL, configuração JDBC, testes pesados e consola CRUD para testar dados reais.

## Resultado esperado

No final deve existir:

* análise dos ficheiros do professor;
* XML/XSD copiados ou criados;
* validação XML/XSD;
* `drop.sql`;
* `schema.sql`;
* `seed/base.sql`;
* `seed/full.sql`;
* `test/pk.sql`, `test/fk.sql`, `test/unique.sql`, `test/check.sql`, `test/application.sql`;
* `DatabaseConfig.java`;
* `db.properties`;
* testes automáticos da base de dados;
* relatório de restrições testadas e de restrições deixadas para fases futuras;
* `DevDatabaseCrudConsoleApp.java`;
* classes auxiliares da consola;
* documentação de testes.

## Critérios de conclusão

* base de dados criada sem erro;
* scripts executam repetidamente;
* XML valida contra XSD;
* JDBC liga;
* testes automáticos passam;
* restrições SQL testáveis foram testadas;
* restrições não testáveis em SQL foram registadas para fases futuras;
* consola CRUD permite ver, criar, atualizar e apagar dados;
* consola explica erros SQL;
* não existe JSP `/dev/db-tests`;
* não existe Servlet `/dev/db-tests`;
* não foi usado EduAll;
* não foi implementado login.

---

## 2.1 Prompt para Codex — análise dos documentos e ficheiros do professor

Usa os agentes:

* `Codex Document Analyst`;
* `Codex Database Agent`.

Analisa:

* `1. Planeamento de IA e Código`;
* `docs/docs`;
* código do professor;
* XML do professor;
* XSD do professor;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Cria:

* `docs/docs/analysis/database-support-analysis.md`.

O ficheiro deve indicar:

* XML/XSD a copiar;
* XML/XSD a adaptar;
* XML/XSD em falta;
* ficheiros de suporte úteis;
* estrutura recomendada;
* valores controlados necessários;
* decisões importantes.

Se encontrares incoerências ou ficheiros em falta:

1. regista o problema;
2. propõe solução;
3. cria os ficheiros de análise necessários;
4. não alteres ainda SQL ou Java.

---

## 2.2 Prompt para Codex — copiar ficheiros de suporte

Usa os agentes:

* `Codex Document Analyst`;
* `Codex Database Agent`.

Com base em `docs/docs/analysis/database-support-analysis.md`, copia/adapta os ficheiros úteis para:

* `src/main/resources/config/xml`;
* `src/main/resources/config/xsd`;
* `src/main/resources/config/support`, se necessário.

Depois:

1. verifica se os ficheiros copiados existem;
2. verifica se os nomes estão coerentes;
3. corrige nomes/caminhos se estiverem errados;
4. atualiza a análise se encontrares erro.

---

## 2.3 Prompt para Codex — criar XML/XSD em falta

Usa os agentes:

* `Codex Database Agent`;
* `Codex Test Agent`.

Cria os XML/XSD em falta para os valores controlados do modelo EA.

Tens liberdade para identificar todos os atributos de valores controlados do modelo e garantir um XML e um XSD por cada catálogo. No mínimo, cobre:

* estados de utilizador;
* estados de sessão;
* tipos de documento;
* estados de pedidos de eliminação;
* permissões;
* tipos de organização e de unidade orgânica;
* tipos de curso;
* modalidades e turnos de turma;
* modos de acesso a blocos;
* tipos, formatos e estados de conteúdo;
* tipos de aula;
* tipos, modos e modalidades de avaliação;
* tipos de pergunta;
* estados de tentativa;
* tipos e estados de certificado;
* tipos e estados de mensagem;
* estados de assiduidade;
* tipos de evento de horário;
* âmbitos de visibilidade de painel.

Depois:

1. verifica se cada XML tem XSD;
2. verifica se não há valores duplicados;
3. corrige ficheiros inválidos;
4. prepara os testes XML/XSD.

---

## 2.4 Prompt para Codex Test Agent — testar e corrigir XML/XSD

Usa o agente `Codex Test Agent`.

Cria:

* `XmlValidationTest.java`;
* `docs/tests/xml-xsd-validation.md`.

O teste deve:

1. procurar XML em `src/main/resources/config/xml`;
2. procurar XSD correspondente em `src/main/resources/config/xsd`;
3. validar XML contra XSD;
4. falhar se XML estiver mal formado;
5. falhar se XML não validar;
6. falhar se houver valores duplicados;
7. falhar se faltar valor obrigatório;
8. imprimir mensagem clara de erro.

Executa:

```bash
mvn test -Dtest=XmlValidationTest
```

Se falhar:

1. identifica se o erro está no XML, XSD ou teste;
2. corrige;
3. repete o teste;
4. atualiza `docs/tests/xml-xsd-validation.md`.

---

## 2.5 Prompt para Codex — criar base de dados completa

Usa o agente `Codex Database Agent`.

Com base em:

* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* XML/XSD;
* `docs/docs/analysis/database-support-analysis.md`;

cria:

* `drop.sql`;
* `schema.sql`;
* `seed/base.sql`;
* `seed/full.sql`;
* `test/pk.sql`, `test/fk.sql`, `test/unique.sql`, `test/check.sql`, `test/application.sql`.

A base de dados deve cobrir todas as entidades, entidades fracas e associações das quatro partes do modelo:

1. Acesso, Identidade e Controlo;
2. Estrutura Organizacional e Formativa;
3. Conteúdos, Aulas e Avaliação;
4. Horários, Assiduidade, Resultados, Certificação e Serviços Transversais.

Tens liberdade para decidir as tabelas, tabelas de associação e triggers necessárias para representar fielmente o modelo, incluindo a hierarquia de unidades orgânicas e as associações com atributos.

Regras:

* usar MySQL;
* criar PK;
* criar FK;
* criar UNIQUE;
* criar NOT NULL;
* criar CHECK;
* criar triggers quando necessário;
* criar índices;
* não inventar entidades;
* implementar em SQL apenas o que faz sentido em SQL;
* deixar para Services/filtros/regras aplicacionais o que depende de contexto.

Cria também:

* `docs/docs/analysis/database-constraint-coverage.md`, indicando quais restrições aplicacionais ficam garantidas em SQL e quais ficam para a camada de negócio nas fases futuras.

Depois de criar:

1. executa `drop.sql`;
2. executa `schema.sql`;
3. se houver erro SQL, corrige;
4. repete até criar a base sem erro.

---

## 2.6 Prompt para Codex — criar JDBC

Usa o agente `Codex Database Agent`.

Cria:

* `DatabaseConfig.java`;
* `db.properties`;
* teste simples de ligação.

Regras:

* não colocar password no Java;
* usar try-with-resources;
* usar JDBC puro;
* não usar Spring/Hibernate/JPA.

Depois:

1. executa o teste de ligação;
2. se falhar, corrige `db.properties`, driver, URL ou `DatabaseConfig`;
3. repete o teste.

---

## 2.7 Prompt para Codex Test Agent — testes automáticos da base de dados com correção

Usa o agente `Codex Test Agent`.

Cria:

* `DatabaseConnectionTest.java`;
* `SchemaIntegrityTest.java`;
* `ValidDataInsertTest.java`;
* `InvalidDataConstraintTest.java`;
* `ApplicationConstraintTest.java`;
* `DatabaseBootstrapServiceTest.java`;
* `docs/tests/database-tests.md`.

Executa:

```bash
mvn test -Dtest=DatabaseConnectionTest
mvn test -Dtest=SchemaIntegrityTest
mvn test -Dtest=ValidDataInsertTest
mvn test -Dtest=InvalidDataConstraintTest
mvn test -Dtest=ApplicationConstraintTest
mvn test -Dtest=DatabaseBootstrapServiceTest
```

Se algum teste falhar:

1. identifica se a falha está no schema, nos dados, na constraint, na trigger, na configuração JDBC ou no próprio teste;
2. corrige o ficheiro correto;
3. não ignores o teste;
4. não removas o teste só para passar;
5. executa novamente;
6. repete até passar;
7. atualiza `docs/tests/database-tests.md`.

---

## 2.8 Prompt para Codex Test Agent — testar e corrigir restrições obrigatórias

Usa o agente `Codex Test Agent`.

Cria:

* `DatabaseRestrictionCoverageTest.java`;
* `docs/tests/database-restrictions-test-report.md`.

O teste deve criar cenários positivos e negativos para:

* `users`;
* `user_sessions`;
* `privacy_settings`;
* `deletion_requests`;
* `organizations`;
* `organic_units`;
* `courses`;
* `subjects`;
* `course_subjects`;
* `class_groups`;
* `student_class_groups`;
* `content_blocks`;
* `content_items`;
* `lessons`;
* `physical_rooms`;
* `assessments`;
* `questions`;
* `options`;
* `attempts`;
* `responses`;
* `certificates`;
* `messages`.

Executa:

```bash
mvn test -Dtest=DatabaseRestrictionCoverageTest
```

Se uma restrição que devia estar em SQL não for aplicada:

1. identifica a tabela;
2. identifica a operação;
3. confirma no `0. GAPE - ALL - V3` se a restrição deve mesmo existir;
4. corrige `schema.sql`, CHECK, UNIQUE, FK ou trigger;
5. corrige `data-test-invalid.sql`, se necessário;
6. volta a executar o teste.

Se a restrição depender de contexto aplicacional:

1. não forces em SQL;
2. regista em `docs/tests/database-restrictions-test-report.md`;
3. indica a fase futura onde será testada.

---

## 2.9 Prompt para Codex — aplicação de consola CRUD da base de dados

Usa o agente `Codex Test Agent`.

Cria uma aplicação de consola Java para testar manualmente dados reais da base de dados.

Classe principal:

* `DevDatabaseCrudConsoleApp.java`

Local:

* `src/main/java/pt/isel/gape/dev/DevDatabaseCrudConsoleApp.java`

Criar também:

* `DevCrudService.java`;
* `DevTableMetadataService.java`;
* `DevConsoleInputReader.java`;
* `SqlErrorTranslator.java`;
* `docs/tests/dev-database-crud-console-testes.md`.

Menu obrigatório:

```text
==============================
 GAPE - CRUD da Base de Dados
==============================

1. Ver Dados
2. Atualizar Dados
3. Criar Dados
4. Apagar Dados
0. Sair

Escolha uma opção:
```

A consola deve permitir:

* ver tabelas;
* escolher tabela;
* ver registos;
* ver detalhe;
* criar registo;
* atualizar registo;
* apagar registo;
* enviar dados inválidos;
* mostrar erro técnico;
* mostrar explicação simples;
* mostrar restrição provável.

Depois de implementar:

1. compila o projeto;
2. executa a consola;
3. testa pelo menos `users`, `organizations`, `courses`, `class_groups` e `content_blocks`;
4. se a consola falhar, corrige;
5. se mensagens de erro forem confusas, melhora `SqlErrorTranslator`;
6. repete os testes manuais documentados.

---

## 2.10 Testes manuais da base de dados e consola

### Preparação

1. Abrir IntelliJ.
2. Confirmar MySQL ligado.
3. Confirmar `db.properties`.
4. Executar:

```bash
mvn clean test
```

5. Abrir `DevDatabaseCrudConsoleApp.java`.
6. Executar `main`.

### Ver dados

1. Escolher `1. Ver Dados`.
2. Escolher `users`.
3. Ver registos.
4. Escolher um ID.
5. Confirmar detalhe.
6. Repetir para `organizations`, `courses`, `class_groups`.

### Criar dados válidos

1. Escolher `3. Criar Dados`.
2. Escolher `users`.
3. Preencher campos obrigatórios.
4. Confirmar `[OK]`.
5. Voltar a `Ver Dados`.
6. Confirmar que o registo existe.

### Criar dados inválidos

1. Criar `users` com email duplicado.
2. Confirmar `[ERRO]`.
3. Confirmar que mostra operação, tabela, dados, erro técnico, motivo provável e restrição.
4. Repetir com:

   * utilizador sem campos obrigatórios;
   * sessão com utilizador inexistente;
   * turma com `min_students > max_students`;
   * bloco com ordem duplicada.

### Atualizar dados

1. Escolher `2. Atualizar Dados`.
2. Escolher `users`.
3. Escolher ID.
4. Alterar nome.
5. Carregar Enter nos campos a manter.
6. Confirmar `[OK]`.
7. Tentar email duplicado.
8. Confirmar rejeição.

### Apagar dados

1. Escolher `4. Apagar Dados`.
2. Escolher tabela.
3. Escolher ID.
4. Confirmar visualização antes de apagar.
5. Confirmar eliminação.
6. Confirmar `[OK]`.
7. Tentar apagar organização com dependências.
8. Confirmar erro FK/dependências.

---

## 2.11 Prompt para Claude Code — Revisão da Fase 1

Usa os agentes:

* `SQL Reviewer`;
* `XML/XSD Reviewer`;
* `Test Reviewer`;
* `Architecture Reviewer`;
* `Document Analyst Reviewer`.

Objetivo da revisão:

Verificar se a Fase 1 foi realmente implementada de acordo com o planeamento, o relatório do projeto, o modelo EA e os critérios de conclusão da fase.

Antes de rever, analisa obrigatoriamente:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* `docs/docs/analysis/database-support-analysis.md`;
* XML/XSD copiados;
* XML/XSD criados;
* `drop.sql`;
* `schema.sql`;
* `seed/base.sql`;
* `seed/full.sql`;
* `test/pk.sql`, `test/fk.sql`, `test/unique.sql`, `test/check.sql`, `test/application.sql`;
* `DatabaseConfig.java`;
* `db.properties`;
* `DatabaseConnectionTest.java`;
* `SchemaIntegrityTest.java`;
* `ValidDataInsertTest.java`;
* `InvalidDataConstraintTest.java`;
* `ApplicationConstraintTest.java`;
* `DatabaseBootstrapServiceTest.java`;
* `DatabaseRestrictionCoverageTest.java`;
* `DevDatabaseCrudConsoleApp.java`;
* `DevCrudService.java`;
* `DevTableMetadataService.java`;
* `DevConsoleInputReader.java`;
* `SqlErrorTranslator.java`;
* documentação em `docs/tests`.

Importante:

Não deves rever os prompts em si.

Deves rever se aquilo que era suposto estar feito na Fase 1 foi realmente implementado.

Verifica:

* se XML/XSD existem e validam;
* se a base de dados cobre as quatro áreas;
* se os scripts SQL executam;
* se existem PK, FK, UNIQUE, NOT NULL, CHECK e triggers necessárias;
* se JDBC funciona;
* se os testes automáticos existem;
* se os testes foram executados;
* se o Codex corrigiu erros encontrados nos testes;
* se existe relatório de restrições;
* se restrições não aplicáveis em SQL ficaram registadas para fases futuras;
* se a consola CRUD existe;
* se a consola permite ver, criar, atualizar e apagar dados;
* se a consola traduz erros SQL;
* se não existe `/dev/db-tests`;
* se não foram criados JSP/Servlet nesta fase.

Executa:

```bash
mvn test -Dtest=XmlValidationTest
mvn test -Dtest=DatabaseConnectionTest
mvn test -Dtest=SchemaIntegrityTest
mvn test -Dtest=ValidDataInsertTest
mvn test -Dtest=InvalidDataConstraintTest
mvn test -Dtest=ApplicationConstraintTest
mvn test -Dtest=DatabaseBootstrapServiceTest
mvn test -Dtest=DatabaseRestrictionCoverageTest
```

Se os testes falharem:

* identifica a causa;
* podes corrigir erros pequenos;
* para erro grande, explica o problema e pede autorização.

No final, produz relatório com:

* estado da fase;
* elementos implementados;
* elementos em falta;
* testes executados;
* erros encontrados;
* correções pequenas feitas;
* correções grandes propostas;
* autorização necessária.

---

## 2.12 Commit esperado

```bash
git add .
git commit -m "Cria base de dados JDBC testes SQL e consola CRUD de desenvolvimento"
```

---

# 3. Fase 2 — Template EduAll

## Objetivo da fase

Integrar o template EduAll no projeto GAPE sem implementar funcionalidades reais.

## Resultado esperado

* template EduAll analisado por completo;
* páginas relevantes identificadas;
* assets copiados;
* CSS organizado;
* JS organizado;
* imagens organizadas;
* fragments JSP criados;
* páginas base criadas;
* dashboard visual inicial;
* documentação das páginas analisadas e alteradas;
* testes de assets;
* documentação de testes.

## Critérios de conclusão

* template abre;
* CSS/JS/imagens carregam;
* sem 404 nos assets;
* sem 500;
* sem SQL nas JSP;
* sem regras de negócio nas JSP;
* documentação indica quais páginas do EduAll foram aproveitadas, alteradas ou ignoradas.

---

## 3.1 Prompt para Codex — integrar template EduAll

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar qualquer ficheiro, analisa o template EduAll completo.

Deves analisar:

* todas as páginas HTML do template;
* estrutura de pastas;
* assets;
* CSS;
* JavaScript;
* imagens;
* menus;
* header;
* sidebar;
* footer;
* dashboards;
* formulários;
* tabelas;
* páginas de autenticação;
* páginas de perfil;
* páginas administrativas;
* páginas de cursos;
* páginas de aulas;
* páginas de conteúdos;
* páginas de mensagens;
* páginas de erro;
* componentes reutilizáveis.

Objetivo:

Integrar o template EduAll no projeto GAPE, criando uma base visual reutilizável para as fases seguintes.

Tens liberdade para decidir:

* que páginas do template devem ser copiadas;
* que páginas devem ser convertidas para JSP;
* que fragments devem ser criados;
* que componentes devem ser reaproveitados;
* que ficheiros devem ser ignorados;
* que páginas base fazem sentido para o GAPE.

Cria ou adapta, no mínimo:

* estrutura de `assets`;
* fragments reutilizáveis;
* página inicial;
* página de login visual;
* dashboard visual;
* páginas de erro;
* documentação da integração.

Também podes criar outras páginas base se o template já tiver páginas úteis para:

* perfil;
* cursos;
* aulas;
* conteúdos;
* mensagens;
* tabelas;
* formulários;
* dashboards administrativos.

Cria:

* `docs/docs/analysis/eduall-template-analysis.md`.

Esse ficheiro deve indicar:

* páginas analisadas;
* páginas aproveitadas;
* páginas convertidas para JSP;
* fragments criados;
* assets copiados;
* páginas ignoradas;
* justificação das decisões.

Regras:

* não implementar login real;
* não implementar permissões;
* não implementar CRUD real;
* não ligar formulários à base de dados;
* não colocar SQL nas JSP;
* não colocar regras de negócio nas JSP;
* manter o estilo visual do EduAll;
* garantir acessibilidade adequada e navegação simples;
* corrigir caminhos quebrados;
* testar assets;
* corrigir erros encontrados.

---

## 3.2 Prompt para Codex Test Agent — testar e corrigir template

Usa o agente `Codex Test Agent`.

Cria:

* `TemplateStructureTest.java`;
* `TemplateAssetReferenceTest.java`;
* `docs/tests/template-tests.md`.

Os testes devem verificar:

* existência da pasta de assets;
* existência dos fragments criados;
* existência das páginas JSP base;
* existência da documentação `docs/docs/analysis/eduall-template-analysis.md`;
* referências CSS válidas;
* referências JS válidas;
* referências de imagens válidas;
* includes JSP válidos;
* ausência de caminhos quebrados óbvios.

Executa:

```bash
mvn test -Dtest=TemplateStructureTest
mvn test -Dtest=TemplateAssetReferenceTest
```

Se falhar:

1. identifica asset em falta;
2. corrige caminho ou copia ficheiro;
3. corrige include JSP quebrado;
4. repete teste;
5. atualiza documentação.

---

## 3.3 Testes manuais

1. Executar `mvn clean package`.
2. Iniciar Tomcat.
3. Abrir página inicial.
4. Abrir login.
5. Abrir dashboard.
6. Abrir todas as páginas base criadas.
7. Abrir DevTools > Network.
8. Recarregar.
9. Confirmar sem CSS 404.
10. Confirmar sem JS 404.
11. Confirmar sem imagens 404.
12. Confirmar visual em Chrome, Edge e Firefox.
13. Confirmar que não há login real.
14. Confirmar que não há CRUD real.
15. Confirmar que a documentação indica que páginas do EduAll foram usadas.

---

## 3.4 Prompt para Claude Code — Revisão da Fase 2

Usa os agentes:

* `Architecture Reviewer`;
* `Test Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se a Fase 2 integrou corretamente o template EduAll, se a IA analisou o template completo e se teve liberdade suficiente para alterar todas as páginas necessárias para preparar a base visual do GAPE.

Antes de rever, analisa obrigatoriamente:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* template EduAll completo;
* `docs/docs/analysis/eduall-template-analysis.md`;
* assets copiados;
* fragments JSP criados;
* páginas JSP criadas;
* páginas de erro;
* `TemplateStructureTest.java`;
* `TemplateAssetReferenceTest.java`;
* `docs/tests/template-tests.md`.

Importante:

Não deves rever os prompts em si.

Deves rever se aquilo que estava previsto para a Fase 2 foi realmente implementado.

Verifica:

* se o template foi analisado por completo;
* se a documentação indica páginas analisadas, aproveitadas, ignoradas e alteradas;
* se os assets existem;
* se os caminhos CSS estão corretos;
* se os caminhos JS estão corretos;
* se as imagens carregam;
* se os fragments JSP existem;
* se as páginas criadas fazem sentido para o GAPE;
* se a IA não ficou limitada a uma página específica;
* se não existe SQL nas JSP;
* se não existem regras de negócio nas JSP;
* se não foi implementado login real;
* se não foi implementado CRUD real;
* se os testes automáticos existem;
* se os testes foram executados;
* se o Codex corrigiu erros encontrados nos testes;
* se existe passo a passo manual.

Executa:

```bash
mvn test -Dtest=TemplateStructureTest
mvn test -Dtest=TemplateAssetReferenceTest
```

Se os testes falharem:

* identifica o asset, caminho ou ficheiro em falta;
* podes corrigir erros pequenos;
* para erro grande, pede autorização.

No final, produz relatório completo da revisão.

---

## 3.5 Commit

```bash
git add .
git commit -m "Integra template visual EduAll"
```

---

# 4. Fase 3 — Login, logout e sessão

## Objetivo da fase

Implementar autenticação, logout, sessão e expiração de sessão, primeiro no back-end e depois no EduAll. Cobre o RF01 e o UC01.

## Resultado esperado

* `AuthService`;
* `SessionService`;
* `LoginServlet`;
* `LogoutServlet`;
* `AuthenticationFilter`;
* login no EduAll;
* dashboard protegido;
* logout;
* header com utilizador autenticado;
* páginas protegidas;
* testes e correções.

## Critérios de conclusão

* login válido funciona;
* login inválido é rejeitado;
* utilizador bloqueado/inativo não entra;
* sessão é criada;
* `last_activity` é atualizado;
* sessão expira por inatividade aos 30 minutos;
* logout funciona;
* dashboard exige sessão;
* restrição aplicacional 18 é testada;
* front-end liga ao back-end.

---

## 4.1 Prompt para Codex — back-end de login, logout e sessão

Usa os agentes:

* `Codex Backend Agent`;
* `Codex Security Agent`.

Antes de implementar, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Cria, no mínimo:

* `User.java`;
* `AccessProfile.java`;
* `Session.java`;
* `UserDAO.java`;
* `SessionDAO.java`;
* `UserService.java`;
* `SessionService.java`;
* `AuthService.java`;
* `PasswordHasher.java`;
* `SessionUser.java`;
* `SessionManager.java`;
* `LoginServlet.java`;
* `LogoutServlet.java`;
* `AuthenticationFilter.java`.

Tens liberdade para criar mais Models, DAOs, Services ou filtros se o modelo de identidade e sessão exigir. Não te limites à lista acima.

Implementa:

* login;
* rejeição de login inválido;
* bloqueio de utilizador inativo/bloqueado;
* criação de sessão;
* atualização de `last_activity`;
* logout;
* expiração por inatividade aos 30 minutos, calculada no `SessionService` e aplicada no `AuthenticationFilter`, conforme a restrição aplicacional 18;
* coerência temporal da sessão.

Segurança:

* guardar passwords apenas com hash e salt;
* nunca guardar passwords em claro;
* não guardar credenciais no código;
* registar login, logout e expiração no registo de auditoria.

Depois:

1. compila o projeto;
2. executa testes de back-end existentes;
3. corrige erros encontrados;
4. repete os testes.

---

## 4.2 Prompt para Codex Test Agent — testar e corrigir back-end

Usa o agente `Codex Test Agent`.

Cria:

* `AuthServiceTest.java`;
* `SessionServiceTest.java`;
* `AuthenticationFilterTest.java`;
* `docs/tests/login-backend-tests.md`.

Executa:

```bash
mvn test -Dtest=AuthServiceTest
mvn test -Dtest=SessionServiceTest
mvn test -Dtest=AuthenticationFilterTest
```

Testa:

1. login válido;
2. password errada;
3. email inexistente;
4. utilizador inativo;
5. utilizador bloqueado;
6. criação de sessão;
7. atualização de `last_activity`;
8. logout;
9. sessão expirada;
10. acesso sem sessão;
11. acesso com sessão expirada.

Testar explicitamente a restrição aplicacional 18:

1. criar sessão recente;
2. confirmar válida;
3. criar sessão com `last_activity` antiga, com mais de 30 minutos;
4. confirmar expirada;
5. confirmar bloqueio no filtro;
6. confirmar redirecionamento.

Se algum teste falhar:

1. identificar se erro está em Service, DAO, Servlet, filtro, dados ou teste;
2. corrigir;
3. repetir os testes.

---

## 4.3 Prompt para Codex — front-end EduAll de login, logout e sessão

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar qualquer ficheiro, analisa o template EduAll completo.

Deves identificar autonomamente todas as páginas e componentes do template relacionados com:

* login;
* autenticação;
* sessão;
* dashboard;
* perfil do utilizador;
* header;
* sidebar;
* dropdown de utilizador;
* botão de logout;
* mensagens de erro;
* mensagens de sucesso;
* páginas de acesso negado;
* páginas protegidas;
* páginas de redirecionamento;
* páginas de erro.

Tens liberdade para alterar todas as páginas, fragments e componentes necessários para que o fluxo de login/logout fique completo no template.

Não te limites a `login.jsp`, `dashboard.jsp`, `header.jsp` ou `sidebar.jsp`. Esses ficheiros são apenas exemplos mínimos. Se o template tiver outras páginas mais adequadas, usa-as.

Implementa a integração front-end com o back-end já criado.

Deves garantir que:

* o formulário de login chama `LoginServlet`;
* erros de login aparecem no estilo EduAll;
* login válido redireciona para dashboard;
* utilizador autenticado aparece no header/dropdown;
* botão de logout chama `LogoutServlet`;
* dashboard e páginas protegidas exigem sessão;
* sessão expirada mostra mensagem adequada;
* páginas públicas e privadas estão visualmente separadas;
* menus se comportam corretamente para utilizador autenticado;
* não existe SQL nas JSP;
* não existem regras de negócio nas JSP.

Cria ou atualiza:

* `docs/docs/analysis/eduall-login-integration.md`.

Esse ficheiro deve indicar:

* páginas do EduAll analisadas;
* páginas alteradas;
* fragments alterados;
* formulários ligados ao back-end;
* páginas protegidas;
* páginas públicas;
* decisões tomadas.

Depois:

1. verifica se o formulário chama `LoginServlet`;
2. verifica se logout chama `LogoutServlet`;
3. verifica se o header mostra utilizador autenticado;
4. verifica se dashboard está protegido;
5. verifica se sessão expirada apresenta mensagem;
6. corrige erros encontrados.

---

## 4.4 Prompt para Codex Test Agent — testar e corrigir front-end

Usa o agente `Codex Test Agent`.

Cria:

* `docs/tests/login-frontend-tests.md`.

Verifica:

1. formulário aponta para `LoginServlet`;
2. campos existem;
3. mensagens aparecem;
4. logout existe;
5. dashboard protegido;
6. header mostra utilizador autenticado;
7. páginas públicas abrem sem sessão;
8. páginas privadas bloqueiam sem sessão;
9. CSS/JS carregam;
10. documentação `docs/docs/analysis/eduall-login-integration.md` existe.

Se falhar:

1. corrige JSP;
2. corrige caminhos;
3. corrige formulário;
4. corrige ligação ao Servlet;
5. repete testes.

---

## 4.5 Testes manuais

1. Executar testes automáticos.
2. Iniciar Tomcat.
3. Abrir login.
4. Login válido.
5. Confirmar dashboard.
6. Confirmar nome no header.
7. Logout.
8. Login inválido.
9. Confirmar erro.
10. Testar utilizador bloqueado.
11. Alterar `last_activity` na BD para antigo.
12. Tentar abrir dashboard.
13. Confirmar redirecionamento para login.

---

## 4.6 Prompt para Claude Code — Revisão da Fase 3

Usa os agentes:

* `Architecture Reviewer`;
* `Security Reviewer`;
* `Test Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se a Fase 3 implementou corretamente login, logout, gestão de sessão, expiração de sessão, proteção de páginas e integração com o template EduAll.

Antes de rever, analisa obrigatoriamente:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* `User.java`;
* `AccessProfile.java`;
* `Session.java`;
* `UserDAO.java`;
* `SessionDAO.java`;
* `UserService.java`;
* `SessionService.java`;
* `AuthService.java`;
* `PasswordHasher.java`;
* `SessionUser.java`;
* `SessionManager.java`;
* `LoginServlet.java`;
* `LogoutServlet.java`;
* `AuthenticationFilter.java`;
* páginas JSP alteradas;
* fragments JSP alterados;
* `docs/docs/analysis/eduall-login-integration.md`;
* `AuthServiceTest.java`;
* `SessionServiceTest.java`;
* `AuthenticationFilterTest.java`;
* `docs/tests/login-backend-tests.md`;
* `docs/tests/login-frontend-tests.md`.

Importante:

Não deves rever os prompts em si.

Deves rever se aquilo que estava previsto para a Fase 3 foi realmente implementado.

Verifica:

* se o back-end funciona sem depender do front-end;
* se o login válido cria sessão;
* se login inválido é rejeitado;
* se utilizador inativo/bloqueado é rejeitado;
* se `last_activity` é atualizado;
* se logout invalida a sessão;
* se sessão expirada é rejeitada;
* se a sessão expira aos 30 minutos de inatividade;
* se a restrição aplicacional 18 foi testada;
* se `AuthenticationFilter` protege páginas privadas;
* se dashboard não abre sem sessão;
* se as passwords são guardadas com hash e salt;
* se não há credenciais no código;
* se o front-end analisou o template completo;
* se o front-end alterou todas as páginas necessárias;
* se a documentação indica páginas alteradas;
* se `login.jsp` ou página equivalente chama `LoginServlet`;
* se logout chama `LogoutServlet`;
* se o utilizador autenticado aparece no template;
* se não há SQL nas JSP;
* se não há regras de negócio nas JSP;
* se Services, DAOs, Servlets e filtros estão bem separados;
* se os testes automáticos existem;
* se os testes foram executados;
* se o Codex corrigiu erros encontrados nos testes;
* se existe passo a passo manual.

Executa:

```bash
mvn test -Dtest=AuthServiceTest
mvn test -Dtest=SessionServiceTest
mvn test -Dtest=AuthenticationFilterTest
```

Se os testes falharem:

* identifica se o problema está no Service, DAO, Servlet, filtro, JSP, dados ou teste;
* podes corrigir erros pequenos;
* para erro grande, explica o problema, propõe solução e pede autorização.

No final, produz relatório com:

* estado da fase;
* funcionalidades corretas;
* funcionalidades em falta;
* testes executados;
* erros encontrados;
* correções feitas;
* correções propostas;
* autorização necessária.

---

## 4.7 Commit

```bash
git add .
git commit -m "Implementa login logout sessao e integracao EduAll"
```

---

# 5. Fase 4 — Permissões e atribuição contextual de perfis

## Objetivo da fase

Implementar as permissões no back-end e a atribuição contextual de perfis às entidades estruturais, mais os menus e botões dinâmicos no EduAll. Cobre o RF03 e o UC03, e cria a base de autorização usada por todas as fases seguintes.

A atribuição contextual liga administradores a organizações, coordenadores a disciplinas e formadores a turmas. Sem esta atribuição não é possível garantir as muitas regras do modelo que dependem do perfil e do contexto.

## Resultado esperado

* `Permission`;
* `PermissionDAO`;
* `PermissionService`;
* `PermissionChecker`;
* `AccessContext`;
* `RoleAssignmentService` e DAOs para as associações de gestão, coordenação e lecionação;
* `AuthorizationFilter`;
* menus dinâmicos por perfil;
* botões dinâmicos por permissão;
* páginas protegidas;
* página de acesso negado;
* testes e correções.

## Critérios de conclusão

* permissões verificadas no back-end e não apenas no front-end;
* acesso direto por URL bloqueado;
* perfis diferentes têm acessos diferentes;
* permissão inativa não é atribuível;
* só o administrador tem permissões de gestão global de utilizadores, permissões e configurações críticas;
* o mecanismo de atribuição contextual de administradores, coordenadores e formadores funciona e é exigido pelas verificações de contexto;
* uma associação ativa entre ator e entidade estrutural só envolve utilizadores ativos.

---

## 5.1 Prompt para Codex — back-end de permissões e atribuição contextual

Usa os agentes:

* `Codex Backend Agent`;
* `Codex Security Agent`.

Antes de implementar, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Cria, no mínimo:

* `Permission.java`;
* `PermissionDAO.java`;
* `PermissionService.java`;
* `PermissionChecker.java`;
* `AccessContext.java`;
* `GrantService.java` para ligar permissões aos perfis;
* `RoleAssignmentService.java` e os DAOs das associações de gestão, coordenação e lecionação;
* `AuthorizationFilter.java`.

Tens liberdade para criar mais Models, DAOs, Services ou filtros se o modelo de permissões e de atribuição de perfis exigir. Não te limites à lista acima.

Implementa:

* atribuição de permissões aos perfis, recusando permissões inativas;
* verificação de permissões no back-end para cada operação restrita;
* contexto de acesso que junta utilizador, perfil e entidade estrutural;
* mecanismo de atribuição contextual de perfis: administrador a organização, coordenador a disciplina e formador a turma;
* regra de que só o administrador tem permissões de gestão global de utilizadores, permissões e configurações críticas;
* regra de que uma associação ativa entre ator e entidade estrutural só envolve utilizadores ativos;
* bloqueio real no `AuthorizationFilter`, incluindo o acesso direto por URL;
* registo de auditoria das operações de gestão de permissões e de atribuições.

As atribuições às organizações, disciplinas e turmas concretas são usadas nas fases das organizações, dos cursos e das turmas; nesta fase fica pronto o mecanismo e a sua verificação.

Depois:

1. compila;
2. executa testes;
3. corrige erros;
4. repete testes.

---

## 5.2 Prompt para Codex Test Agent — testar e corrigir permissões

Usa o agente `Codex Test Agent`.

Cria:

* `PermissionServiceTest.java`;
* `RoleAssignmentServiceTest.java`;
* `AuthorizationFilterTest.java`;
* `docs/tests/permission-tests.md`.

Executa:

```bash
mvn test -Dtest=PermissionServiceTest
mvn test -Dtest=RoleAssignmentServiceTest
mvn test -Dtest=AuthorizationFilterTest
```

Testa:

1. administrador com acesso;
2. aluno sem acesso;
3. professor com acesso parcial, apenas às turmas que leciona;
4. coordenador com acesso contextual, apenas às disciplinas que coordena;
5. acesso direto por URL bloqueado;
6. operação crítica sem permissão;
7. permissão inativa não atribuível;
8. gestão global apenas pelo administrador;
9. atribuição de perfil a utilizador inativo recusada.

Se falhar, corrigir Service, filtro, DAO, dados ou teste e repetir.

---

## 5.3 Prompt para Codex — front-end EduAll de permissões

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar, analisa o template EduAll completo.

Deves identificar autonomamente todas as páginas, fragments e componentes relacionados com:

* menus;
* sidebar;
* header;
* dashboards por perfil;
* botões de ação;
* links administrativos;
* links de aluno;
* links de professor;
* links de coordenador;
* links de administrador;
* páginas de acesso negado;
* páginas protegidas;
* mensagens de erro;
* breadcrumbs;
* cards de dashboard.

Tens liberdade para alterar todas as páginas necessárias para refletir permissões no front-end. Não te limites à sidebar. Se o template tiver menus em várias páginas, headers diferentes, dashboards diferentes ou componentes repetidos, altera todos os necessários.

Implementa:

* menus dinâmicos por perfil;
* botões visíveis/invisíveis por permissão;
* links protegidos;
* página de acesso negado no estilo EduAll;
* mensagens de acesso negado;
* adaptação visual para perfis diferentes;
* navegação simples, com as funcionalidades de cada perfil acessíveis em poucos níveis.

Importante:

O front-end apenas melhora a experiência do utilizador. A segurança real deve continuar no back-end. Não esconder menus como substituto de validação no back-end.

Cria ou atualiza:

* `docs/docs/analysis/eduall-permissions-integration.md`.

Esse ficheiro deve indicar:

* páginas analisadas;
* páginas alteradas;
* menus alterados;
* componentes alterados;
* como cada perfil é representado visualmente;
* quais permissões afetam o front-end.

Depois:

1. testar menus como administrador;
2. testar menus como coordenador;
3. testar menus como professor;
4. testar menus como aluno;
5. corrigir páginas ou menus incoerentes;
6. garantir que acesso direto continua bloqueado pelo back-end.

---

## 5.4 Testes manuais

1. Login administrador.
2. Confirmar menus.
3. Atribuir um formador a uma turma e um coordenador a uma disciplina.
4. Login aluno.
5. Confirmar menus restritos.
6. Escrever URL administrativo manualmente.
7. Confirmar acesso negado.
8. Repetir com professor e coordenador, confirmando que só veem os seus contextos.

---

## 5.5 Prompt para Claude Code — Revisão da Fase 4

Usa os agentes:

* `Architecture Reviewer`;
* `Security Reviewer`;
* `Test Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se a Fase 4 implementou corretamente as permissões no back-end, a atribuição contextual de perfis e a adaptação visual no EduAll, sem depender apenas do front-end para segurança.

Antes de rever, analisa obrigatoriamente:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* `Permission.java`;
* `PermissionDAO.java`;
* `PermissionService.java`;
* `PermissionChecker.java`;
* `AccessContext.java`;
* `GrantService.java`;
* `RoleAssignmentService.java` e DAOs das associações de gestão, coordenação e lecionação;
* `AuthorizationFilter.java`;
* JSPs/menus alterados;
* página de acesso negado;
* `docs/docs/analysis/eduall-permissions-integration.md`;
* `PermissionServiceTest.java`;
* `RoleAssignmentServiceTest.java`;
* `AuthorizationFilterTest.java`;
* `docs/tests/permission-tests.md`.

Verifica:

* se as permissões são verificadas no back-end;
* se menus escondidos não substituem segurança real;
* se acesso direto por URL é bloqueado;
* se perfis diferentes têm acessos diferentes;
* se o mecanismo de atribuição contextual funciona;
* se permissão inativa não é atribuível;
* se só o administrador tem gestão global;
* se uma associação ativa só envolve utilizadores ativos;
* se o front-end analisou o template completo;
* se todas as páginas/menus necessários foram alterados;
* se a documentação indica decisões de front-end;
* se a página de acesso negado funciona;
* se não há SQL em JSP;
* se não há regras de negócio em JSP;
* se os testes existem;
* se os testes foram executados;
* se o Codex corrigiu falhas dos testes;
* se há passo a passo manual.

Executa:

```bash
mvn test -Dtest=PermissionServiceTest
mvn test -Dtest=RoleAssignmentServiceTest
mvn test -Dtest=AuthorizationFilterTest
```

Se falhar:

* identifica causa;
* corrige erros pequenos;
* para erro grande, pede autorização.

Produz relatório final da fase.

---

## 5.6 Commit

```bash
git add .
git commit -m "Implementa permissoes e atribuicao contextual de perfis"
```

---

# 6. Fase 5 — Utilizadores, privacidade e direito ao esquecimento

## Objetivo da fase

Implementar a gestão de utilizadores, a gestão do perfil pessoal, as preferências de privacidade, os pedidos de eliminação e a base do registo de auditoria. Cobre o RF02, o RF03 na parte de gestão de utilizadores e os UC02 e UC03.

## Resultado esperado

* Models, DAOs e Services de utilizadores e perfis;
* Models, DAOs e Services de privacidade;
* Models, DAOs e Services de pedidos de eliminação;
* Model, DAO e Service do registo de atividades para auditoria;
* Servlets das operações;
* front-end EduAll de utilizadores, perfil, privacidade e eliminação;
* testes e correções.

## Critérios de conclusão

* CRUD de utilizadores;
* email duplicado rejeitado;
* documento duplicado rejeitado;
* bloqueio de utilizador;
* gestão de privacidade sem preferências duplicadas;
* submissão de pedido de eliminação;
* processamento do pedido apenas pelo administrador com permissão e com datas e estado coerentes;
* operações sobre dados pessoais registadas em auditoria;
* dados pessoais só visíveis a perfis autorizados.

---

## 6.1 Prompt para Codex — back-end de utilizadores, privacidade e eliminação

Usa os agentes:

* `Codex Backend Agent`;
* `Codex Security Agent`.

Antes de implementar, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Cria, no mínimo, Models, DAOs e Services para:

* utilizadores;
* perfis;
* privacidade;
* pedidos de eliminação;
* registo de atividades para auditoria.

Tens liberdade para criar mais Models, DAOs, Services, Servlets ou validadores se o modelo de utilizadores, privacidade, eliminação e auditoria exigir. Não te limites à lista acima.

Implementa:

* CRUD de utilizadores;
* validação de email duplicado;
* validação de documento duplicado;
* exigência conjunta do tipo e do número de documento, quando o documento for preenchido;
* bloqueio e desbloqueio de utilizador;
* edição do perfil pessoal;
* gestão de preferências de privacidade sem permitir preferências duplicadas para o mesmo utilizador;
* submissão de pedido de eliminação;
* processamento do pedido apenas pelo administrador com permissão, garantindo que a data de processamento não é anterior à data de submissão e que um estado final tem data de processamento;
* registo de auditoria de todas as operações críticas sobre dados pessoais, privacidade e eliminação;
* restrição de leitura e alteração de dados pessoais a perfis autorizados.

Executa testes e corrige erros encontrados.

---

## 6.2 Prompt para Codex Test Agent — testar e corrigir

Usa o agente `Codex Test Agent`.

Cria:

* `UserServiceTest.java`;
* `PrivacyServiceTest.java`;
* `DeletionRequestServiceTest.java`;
* `ActivityLogServiceTest.java`;
* `docs/tests/users-privacy-deletion-tests.md`.

Executa:

```bash
mvn test -Dtest=UserServiceTest
mvn test -Dtest=PrivacyServiceTest
mvn test -Dtest=DeletionRequestServiceTest
mvn test -Dtest=ActivityLogServiceTest
```

Testa criação válida, email duplicado, documento duplicado, campos obrigatórios em falta, bloqueio, privacidade duplicada, submissão de pedido de eliminação, processamento sem permissão, processamento com datas incoerentes, registo de auditoria e leitura de dados pessoais por perfil não autorizado.

Se falhar, corrigir código, teste ou dados e repetir.

---

## 6.3 Prompt para Codex — front-end EduAll de utilizadores, privacidade e eliminação

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar, analisa o template EduAll completo.

Deves identificar autonomamente todas as páginas e componentes relacionados com:

* utilizadores;
* perfil;
* edição de perfil;
* tabelas administrativas;
* páginas de detalhes;
* formulários;
* privacidade;
* preferências;
* pedidos de eliminação;
* modais de confirmação;
* botões de bloquear/desbloquear;
* mensagens;
* auditoria visível, se fizer sentido;
* dashboards administrativos.

Tens liberdade para alterar todas as páginas necessárias. Não te limites a uma página de listagem. Se o template tiver páginas de perfil, páginas de utilizadores, páginas administrativas ou formulários já prontos, adapta os mais adequados.

Cria ou adapta:

* páginas de listagem;
* páginas de detalhe;
* páginas de criação;
* páginas de edição;
* páginas de privacidade;
* páginas de pedidos de eliminação;
* modais de confirmação;
* mensagens de erro/sucesso;
* links e botões no menu.

Liga tudo ao back-end já testado.

Cria ou atualiza:

* `docs/docs/analysis/eduall-users-integration.md`.

Esse ficheiro deve indicar:

* páginas analisadas;
* páginas alteradas;
* componentes reutilizados;
* formulários ligados ao back-end;
* decisões de interface;
* páginas públicas e protegidas.

Depois:

1. testar criação visual de utilizador;
2. testar edição;
3. testar bloqueio;
4. testar privacidade;
5. testar pedido de eliminação;
6. corrigir erros visuais e de integração.

---

## 6.4 Testes manuais

1. Login admin.
2. Criar utilizador.
3. Tentar email duplicado.
4. Tentar documento duplicado.
5. Editar utilizador.
6. Bloquear utilizador.
7. Testar login bloqueado.
8. Alterar privacidade.
9. Submeter pedido de eliminação.
10. Processar como admin.
11. Confirmar auditoria.

---

## 6.5 Prompt para Claude Code — Revisão da Fase 5

Usa os agentes:

* `Architecture Reviewer`;
* `Security Reviewer`;
* `Test Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se a Fase 5 implementou corretamente a gestão de utilizadores, perfis, privacidade, pedidos de eliminação, auditoria e integração EduAll.

Antes de rever, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* Models, DAOs e Services de utilizadores;
* Models, DAOs e Services de privacidade;
* Models, DAOs e Services de pedidos de eliminação;
* Model, DAO e Service de auditoria;
* JSPs da fase;
* `docs/docs/analysis/eduall-users-integration.md`;
* `UserServiceTest.java`;
* `PrivacyServiceTest.java`;
* `DeletionRequestServiceTest.java`;
* `ActivityLogServiceTest.java`;
* `docs/tests/users-privacy-deletion-tests.md`.

Verifica:

* CRUD de utilizadores;
* validação de email duplicado;
* validação de documento duplicado;
* bloqueio de utilizador;
* gestão de privacidade;
* submissão de pedido de eliminação;
* processamento de pedido apenas por administrador com permissão e com datas coerentes;
* auditoria das operações sobre dados pessoais;
* acesso a dados pessoais restrito a perfis autorizados;
* integração EduAll;
* se o front-end analisou o template completo;
* se todas as páginas necessárias foram alteradas;
* testes automáticos;
* correções feitas pelo Codex;
* testes manuais documentados.

Executa:

```bash
mvn test -Dtest=UserServiceTest
mvn test -Dtest=PrivacyServiceTest
mvn test -Dtest=DeletionRequestServiceTest
mvn test -Dtest=ActivityLogServiceTest
```

Se falhar, identifica causa e segue regras de correção.

Produz relatório final.

---

## 6.6 Commit

```bash
git add .
git commit -m "Implementa utilizadores privacidade e direito ao esquecimento"
```

---

# 7. Fase 6 — Organizações e unidades orgânicas

## Objetivo da fase

Implementar organizações e unidades orgânicas com hierarquia, validações, atribuição de administradores e permissões. Cobre o RF04, o RF05 e os UC04 e UC05.

## Resultado esperado

* Models, DAOs e Services de organizações e unidades orgânicas;
* atribuição de administradores às organizações;
* validação de hierarquia;
* front-end EduAll de organizações e unidades;
* testes e correções.

## Critérios de conclusão

* CRUD de organizações e unidades;
* auto-subordinação bloqueada;
* ciclos na hierarquia bloqueados;
* unidade pai da mesma organização;
* organização ativa exige pelo menos um administrador associado;
* apagar com dependências controlado;
* permissões aplicadas.

---

## 7.1 Prompt para Codex — back-end

Usa o agente `Codex Backend Agent`.

Antes de implementar, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Cria, no mínimo, Models, DAOs e Services de organizações e de unidades orgânicas, mais a validação da hierarquia.

Tens liberdade para criar mais Models, DAOs, Services ou validadores se o modelo exigir. Não te limites à lista acima.

Implementa:

* CRUD de organizações;
* CRUD de unidades orgânicas;
* atribuição de administradores às organizações, usando o mecanismo de atribuição contextual da Fase 4;
* bloqueio de auto-subordinação;
* prevenção de ciclos na hierarquia, com navegação pela cadeia de unidades;
* exigência de que a unidade pai pertença à mesma organização;
* regra de que uma organização ativa tem de ter pelo menos um administrador associado e ativo;
* bloqueio de operações sobre entidades arquivadas;
* permissões, de modo que cada administrador gere as organizações que administra;
* registo de auditoria das operações.

Executa testes e corrige erros.

---

## 7.2 Prompt para Codex Test Agent — testar e corrigir

Usa o agente `Codex Test Agent`.

Cria:

* `OrganizationServiceTest.java`;
* `OrganicUnitServiceTest.java`;
* `docs/tests/organizations-tests.md`.

Executa:

```bash
mvn test -Dtest=OrganizationServiceTest
mvn test -Dtest=OrganicUnitServiceTest
```

Testa organização válida, organização sem nome, unidade sem organização, auto-subordinação, unidade de outra organização, ciclo na hierarquia, organização ativa sem administrador, apagar com dependências e operação sem permissão.

Corrige erros e repete.

---

## 7.3 Prompt para Codex — front-end EduAll de organizações

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar, analisa o template EduAll completo.

Deves identificar autonomamente todas as páginas e componentes relacionados com:

* organizações;
* instituições;
* unidades;
* hierarquias;
* departamentos;
* tabelas;
* formulários;
* páginas de detalhe;
* dashboards administrativos;
* menus administrativos;
* árvores/hierarquias visuais;
* modais de confirmação.

Tens liberdade para alterar todas as páginas necessárias. Se o template tiver páginas de instituições, departamentos, categorias, organizações ou gestão administrativa, adapta as mais adequadas ao GAPE.

Cria ou adapta:

* listagens;
* detalhes;
* formulários;
* visualização de hierarquia;
* atribuição de administradores;
* ações de criar, editar e apagar;
* mensagens de erro/sucesso;
* menus e links relacionados.

Liga ao back-end já testado.

Cria ou atualiza:

* `docs/docs/analysis/eduall-organizations-integration.md`.

Depois:

1. testar criação;
2. testar edição;
3. testar hierarquia;
4. testar mensagens;
5. corrigir erros visuais ou de integração.

---

## 7.4 Testes manuais

1. Criar organização.
2. Atribuir administrador.
3. Criar unidade.
4. Ver hierarquia.
5. Tentar auto-subordinação.
6. Tentar ciclo.
7. Tentar apagar organização com dependências.
8. Testar acesso sem permissão.

---

## 7.5 Prompt para Claude Code — Revisão da Fase 6

Usa os agentes:

* `Architecture Reviewer`;
* `SQL Reviewer`;
* `Test Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se a Fase 6 implementou corretamente organizações, unidades orgânicas, hierarquia, atribuição de administradores, validações e integração com EduAll.

Antes de rever, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* Models, DAOs e Services de organizações;
* Models, DAOs e Services de unidades orgânicas;
* JSPs da fase;
* `docs/docs/analysis/eduall-organizations-integration.md`;
* `OrganizationServiceTest.java`;
* `OrganicUnitServiceTest.java`;
* `docs/tests/organizations-tests.md`.

Verifica:

* CRUD de organizações;
* CRUD de unidades;
* auto-subordinação bloqueada;
* ciclos bloqueados;
* unidade pai de outra organização bloqueada;
* organização ativa com administrador;
* permissões;
* integração EduAll;
* se o front-end analisou o template completo;
* se todas as páginas necessárias foram alteradas;
* testes automáticos;
* correções feitas pelo Codex;
* testes manuais documentados.

Executa:

```bash
mvn test -Dtest=OrganizationServiceTest
mvn test -Dtest=OrganicUnitServiceTest
```

Produz relatório final.

---

## 7.6 Commit

```bash
git add .
git commit -m "Implementa organizacoes e unidades organicas"
```

---

# 8. Fase 7 — Cursos, disciplinas e inscrições

## Objetivo da fase

Implementar cursos, disciplinas, associações curriculares, atribuição de coordenadores e inscrição e desistência de alunos em cursos e disciplinas. Cobre os RF06A, RF06B, RF06C, RF07A, RF07B e RF07C, e os UC06 e UC07.

## Resultado esperado

* Models, DAOs e Services de cursos;
* Models, DAOs e Services de disciplinas;
* associação disciplina-curso;
* atribuição de coordenadores às disciplinas;
* inscrição e desistência em cursos e disciplinas;
* front-end EduAll;
* testes e correções.

## Critérios de conclusão

* CRUD de cursos e disciplinas;
* curso sem organização rejeitado;
* coerência entre organização e unidade do curso;
* associação disciplina-curso sem duplicação;
* posição curricular com ano e período em conjunto;
* inscrição de aluno em disciplina apenas se integrada num curso onde está inscrito ou autorizado;
* sem inscrições ativas sobrepostas no tempo;
* permissões aplicadas.

---

## 8.1 Prompt para Codex — back-end

Usa os agentes:

* `Codex Backend Agent`;
* `Codex Security Agent`.

Antes de implementar, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Cria, no mínimo, Models, DAOs e Services de cursos, de disciplinas, da associação disciplina-curso e das inscrições.

Tens liberdade para criar mais Models, DAOs, Services ou validadores se o modelo exigir. Não te limites à lista acima.

Implementa:

* CRUD de cursos;
* CRUD de disciplinas;
* associação disciplina-curso, incluindo posição curricular, obrigatoriedade e estado;
* atribuição de coordenadores às disciplinas, usando o mecanismo da Fase 4;
* rejeição de curso sem organização;
* coerência entre o curso e a unidade orgânica, que devem pertencer à mesma organização;
* rejeição de associação disciplina-curso duplicada;
* exigência de ano e período em conjunto na posição curricular;
* inscrição e desistência de alunos em cursos e disciplinas;
* inscrição de aluno numa disciplina apenas se integrada num curso onde está inscrito ou autorizado;
* bloqueio de inscrições ativas sobrepostas no tempo;
* bloqueio de operações sobre entidades arquivadas;
* permissões, com gestão de cursos pelo administrador e gestão de disciplinas pelo administrador e pelo coordenador;
* registo de auditoria das operações.

Executa testes e corrige erros.

---

## 8.2 Prompt para Codex Test Agent — testar e corrigir

Usa o agente `Codex Test Agent`.

Cria:

* `CourseServiceTest.java`;
* `SubjectServiceTest.java`;
* `CourseSubjectServiceTest.java`;
* `EnrollmentServiceTest.java`;
* `docs/tests/courses-subjects-tests.md`.

Executa:

```bash
mvn test -Dtest=CourseServiceTest
mvn test -Dtest=SubjectServiceTest
mvn test -Dtest=CourseSubjectServiceTest
mvn test -Dtest=EnrollmentServiceTest
```

Testa curso válido, curso sem organização, unidade de outra organização, disciplina válida, associação duplicada, posição curricular incompleta, inscrição válida em curso e disciplina, inscrição em disciplina não integrada no curso, inscrições sobrepostas, desistência e operação sem permissão.

Corrige e repete.

---

## 8.3 Prompt para Codex — front-end EduAll de cursos e disciplinas

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar, analisa o template EduAll completo.

Deves identificar autonomamente todas as páginas e componentes relacionados com:

* cursos;
* disciplinas;
* módulos;
* categorias de cursos;
* páginas de catálogo;
* páginas de detalhe de curso;
* páginas de gestão de cursos;
* páginas de gestão de disciplinas;
* tabelas;
* formulários;
* filtros;
* cards;
* dashboards de curso;
* menus de navegação.

Tens liberdade para alterar todas as páginas necessárias. Se o template já tiver páginas de cursos, detalhes de cursos, instrutores, aulas ou categorias, adapta as mais adequadas ao GAPE.

Cria ou adapta:

* listagem de cursos;
* detalhe de curso;
* criação e edição de curso;
* listagem de disciplinas;
* criação e edição de disciplinas;
* associação disciplina-curso;
* inscrição e desistência do aluno em cursos e disciplinas;
* mensagens de erro/sucesso;
* menus e links relacionados.

Liga ao back-end já testado.

Cria ou atualiza:

* `docs/docs/analysis/eduall-courses-subjects-integration.md`.

Depois:

1. testar criação de curso;
2. testar criação de disciplina;
3. testar associação;
4. testar duplicação;
5. testar inscrição e desistência;
6. corrigir erros de interface e integração.

---

## 8.4 Testes manuais

1. Criar curso.
2. Tentar curso sem organização.
3. Criar disciplina.
4. Associar disciplina.
5. Tentar duplicar associação.
6. Inscrever aluno em curso e disciplina.
7. Tentar inscrição sobreposta.
8. Desistir.
9. Testar permissões.

---

## 8.5 Prompt para Claude Code — Revisão da Fase 7

Usa os agentes:

* `Architecture Reviewer`;
* `SQL Reviewer`;
* `Security Reviewer`;
* `Test Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se a Fase 7 implementou corretamente cursos, disciplinas, associações curriculares, inscrições, validações e integração com EduAll.

Antes de rever, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* Models, DAOs e Services de cursos;
* Models, DAOs e Services de disciplinas;
* associação curso-disciplina;
* Services de inscrição;
* JSPs da fase;
* `docs/docs/analysis/eduall-courses-subjects-integration.md`;
* `CourseServiceTest.java`;
* `SubjectServiceTest.java`;
* `CourseSubjectServiceTest.java`;
* `EnrollmentServiceTest.java`;
* `docs/tests/courses-subjects-tests.md`.

Verifica:

* CRUD de cursos;
* CRUD de disciplinas;
* associação disciplina-curso;
* duplicação bloqueada;
* coerência organização/unidade;
* inscrição contextual e sem sobreposição;
* permissões;
* integração EduAll;
* se o front-end analisou o template completo;
* se todas as páginas necessárias foram alteradas;
* testes;
* correções Codex;
* documentação manual.

Executa os testes da fase e produz relatório.

---

## 8.6 Commit

```bash
git add .
git commit -m "Implementa cursos disciplinas associacoes curriculares e inscricoes"
```

---

# 9. Fase 8 — Turmas, inscrições e blocos pedagógicos

## Objetivo da fase

Implementar turmas, inscrição e desistência em turmas, lotação, atribuição de formadores e blocos pedagógicos. Cobre os RF08A, RF08B, RF08C, RF09A e RF09B, e os UC08 e UC09.

## Resultado esperado

* Models, DAOs e Services de turmas;
* inscrição e desistência em turmas;
* atribuição de formadores às turmas;
* Models, DAOs e Services de blocos pedagógicos;
* front-end EduAll;
* testes e correções.

## Critérios de conclusão

* CRUD de turmas;
* turma apenas num curso que integra a disciplina;
* `min_students <= max_students`;
* datas válidas;
* inscrição e desistência;
* sem inscrições sobrepostas;
* lotação respeitada;
* blocos com código único e ordem única de bloco ativo;
* datas e disponibilidade do bloco válidas;
* bloqueio por arquivamento;
* permissões aplicadas.

---

## 9.1 Prompt para Codex — back-end

Usa os agentes:

* `Codex Backend Agent`;
* `Codex Security Agent`.

Antes de implementar, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Cria, no mínimo, Models, DAOs e Services de turmas, de inscrições em turmas e de blocos pedagógicos.

Tens liberdade para criar mais Models, DAOs, Services ou validadores se o modelo exigir. Não te limites à lista acima.

Implementa:

* CRUD de turmas;
* exigência de que a turma só exista num curso que integra a respetiva disciplina;
* regra `min_students <= max_students` quando ambos preenchidos;
* coerência das datas de início e fim;
* inscrição e desistência de alunos;
* atribuição de formadores às turmas, usando o mecanismo da Fase 4;
* inscrição de aluno apenas em turma de uma disciplina onde está inscrito;
* bloqueio de inscrições ativas sobrepostas;
* lotação que não excede o número máximo de alunos;
* blocos pedagógicos com código único na turma;
* ordem única entre blocos ativos da mesma turma;
* coerência das datas de disponibilidade do bloco e exigência de data de início quando o acesso é por calendarização;
* bloqueio de operações sobre entidades arquivadas;
* permissões, com gestão por administrador, coordenador e formador conforme o contexto;
* registo de auditoria das operações.

Executa testes e corrige erros.

---

## 9.2 Prompt para Codex Test Agent — testar e corrigir

Usa o agente `Codex Test Agent`.

Cria:

* `ClassGroupServiceTest.java`;
* `StudentClassGroupServiceTest.java`;
* `ContentBlockServiceTest.java`;
* `docs/tests/class-groups-blocks-tests.md`.

Executa:

```bash
mvn test -Dtest=ClassGroupServiceTest
mvn test -Dtest=StudentClassGroupServiceTest
mvn test -Dtest=ContentBlockServiceTest
```

Testa turma válida, turma sem disciplina, turma num curso que não integra a disciplina, `min_students > max_students`, datas inválidas, inscrição duplicada, inscrição sobreposta, lotação excedida, desistência, bloco sem turma, ordem de bloco duplicada, datas de bloco inválidas e bloco por calendarização sem data de início.

Corrige e repete.

---

## 9.3 Prompt para Codex — front-end EduAll de turmas, inscrições e blocos

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar, analisa o template EduAll completo.

Deves identificar autonomamente todas as páginas e componentes relacionados com:

* turmas;
* grupos;
* aulas/classes;
* inscrições;
* alunos inscritos;
* listas de alunos;
* blocos pedagógicos;
* conteúdos de turma;
* páginas de turma;
* páginas de detalhe;
* tabelas;
* formulários;
* ordenação;
* dashboards de curso/turma;
* menus contextuais.

Tens liberdade para alterar todas as páginas necessárias. Se o template tiver páginas de turmas, cursos, aulas, grupos, estudantes, conteúdos ou gestão académica, adapta as mais adequadas.

Cria ou adapta:

* listagem de turmas;
* detalhe de turma;
* criação/edição de turma;
* inscrição de alunos;
* desistência;
* lista de alunos;
* blocos pedagógicos;
* ordenação visual de blocos;
* mensagens;
* menus e links relacionados.

Liga ao back-end já testado.

Cria ou atualiza:

* `docs/docs/analysis/eduall-class-groups-blocks-integration.md`.

Depois:

1. testar turma;
2. testar inscrição;
3. testar lotação;
4. testar blocos;
5. testar ordem;
6. corrigir erros encontrados.

---

## 9.4 Testes manuais

1. Criar turma.
2. Tentar `min_students > max_students`.
3. Inscrever aluno.
4. Tentar inscrição duplicada.
5. Testar lotação.
6. Criar bloco.
7. Tentar ordem duplicada.
8. Testar permissões.

---

## 9.5 Prompt para Claude Code — Revisão da Fase 8

Usa os agentes:

* `Architecture Reviewer`;
* `SQL Reviewer`;
* `Security Reviewer`;
* `Test Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se a Fase 8 implementou corretamente turmas, inscrições, desistências, blocos pedagógicos, regras de lotação, datas e permissões.

Antes de rever, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* Models, DAOs e Services de turmas;
* Models, DAOs e Services de inscrições;
* Models, DAOs e Services de blocos;
* JSPs da fase;
* `docs/docs/analysis/eduall-class-groups-blocks-integration.md`;
* `ClassGroupServiceTest.java`;
* `StudentClassGroupServiceTest.java`;
* `ContentBlockServiceTest.java`;
* `docs/tests/class-groups-blocks-tests.md`.

Verifica:

* CRUD turmas;
* turma apenas num curso que integra a disciplina;
* inscrição;
* desistência;
* lotação;
* sobreposição;
* ordem única;
* datas;
* permissões;
* front-end;
* se o front-end analisou o template completo;
* se todas as páginas necessárias foram alteradas;
* testes;
* correções Codex;
* documentação manual.

Executa testes e produz relatório.

---

## 9.6 Commit

```bash
git add .
git commit -m "Implementa turmas inscricoes e blocos pedagogicos"
```

---

# 10. Fase 9 — Conteúdos pedagógicos

## Objetivo da fase

Implementar a gestão, reutilização e associação de conteúdos pedagógicos em vários formatos, como texto, imagem, vídeo, áudio, PDF e URL, dando prioridade ao caso de uso do PDF associado a blocos pedagógicos. Cobre o RF10 e o UC10.

## Resultado esperado

* Models, DAOs e Services do repositório de conteúdos;
* associação de conteúdos a organizações, unidades, cursos, disciplinas, turmas, blocos e avaliações;
* serviço de upload e validação, com prioridade ao PDF;
* Servlets de upload e download;
* front-end EduAll;
* testes e correções.

## Critérios de conclusão

* conteúdo criado em vários formatos;
* PDF válido aceite e PDF inválido, vazio ou demasiado grande rejeitado;
* referência ou valor do conteúdo coerente com o formato;
* responsável pelo conteúdo registado;
* eliminação respeita o autor e a exceção do administrador;
* conteúdo com tentativas submetidas ou em bloco ativo obrigatório não é eliminado fisicamente;
* associações respeitam o encadeamento estrutural do modelo;
* permissões aplicadas.

---

## 10.1 Prompt para Codex — back-end

Usa os agentes:

* `Codex Backend Agent`;
* `Codex Security Agent`.

Antes de implementar, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Cria, no mínimo, Models, DAOs e Services do repositório de conteúdos, das associações de conteúdo e do serviço de upload e validação, mais os Servlets de upload e download.

Tens liberdade para criar mais Models, DAOs, Services ou validadores se o modelo de conteúdos e as suas associações exigirem. Não te limites à lista acima.

Implementa:

* criação de conteúdos em vários formatos, com prioridade ao PDF;
* validação de upload de PDF: tipo correto, ficheiro não vazio e tamanho dentro do limite, com a estrutura extensível aos restantes formatos;
* exigência de referência ou valor do conteúdo coerente com o formato;
* registo do responsável que introduziu o conteúdo;
* associação de conteúdos a organizações, unidades, cursos, disciplinas, turmas, blocos e avaliações, respeitando o encadeamento estrutural do modelo;
* regra de que só o autor, ou o administrador, pode eliminar um conteúdo;
* regra de que um conteúdo associado a uma avaliação com tentativas submetidas ou a um bloco ativo como obrigatório não é eliminado fisicamente, sendo arquivado ou removido apenas da associação;
* gestão de conteúdos por perfil e contexto;
* segurança de upload, com nomes seguros e sem permitir caminhos perigosos;
* registo de auditoria das operações.

Executa testes e corrige erros.

---

## 10.2 Prompt para Codex Test Agent — testar e corrigir

Usa o agente `Codex Test Agent`.

Cria:

* `ContentItemServiceTest.java`;
* `ContentAssociationServiceTest.java`;
* `PdfUploadServiceTest.java`;
* `docs/tests/content-pdf-tests.md`.

Executa:

```bash
mvn test -Dtest=ContentItemServiceTest
mvn test -Dtest=ContentAssociationServiceTest
mvn test -Dtest=PdfUploadServiceTest
```

Testa criação por formato com referência válida e inválida, PDF válido, não PDF, ficheiro vazio, tamanho excessivo, sem responsável, eliminação por não-autor, eliminação pelo autor e pelo administrador, conteúdo com tentativas ou bloco ativo não eliminado fisicamente, associação que viola o encadeamento estrutural e operação sem permissão.

Corrige e repete.

---

## 10.3 Prompt para Codex — front-end EduAll de conteúdos

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar, analisa o template EduAll completo.

Deves identificar autonomamente todas as páginas e componentes relacionados com:

* conteúdos;
* documentos;
* materiais;
* ficheiros;
* recursos pedagógicos;
* upload;
* download;
* visualização;
* biblioteca;
* cards de conteúdos;
* tabelas de conteúdos;
* páginas de detalhe;
* formulários de upload;
* filtros por curso/turma/bloco;
* páginas de aulas ou cursos onde conteúdos aparecem.

Tens liberdade para alterar todas as páginas necessárias. Se o template tiver páginas de materiais, recursos, ficheiros, cursos, aulas ou biblioteca, adapta as mais adequadas.

Cria ou adapta:

* listagem de conteúdos;
* formulário de upload, com prioridade ao PDF;
* detalhe de conteúdo;
* visualização/download;
* associação a bloco/turma/curso;
* mensagens de erro/sucesso;
* menus e links relacionados.

Liga ao back-end já testado.

Cria ou atualiza:

* `docs/docs/analysis/eduall-content-pdf-integration.md`.

Depois:

1. testar upload;
2. testar listagem;
3. testar detalhe;
4. testar download;
5. testar erros;
6. corrigir integração.

---

## 10.4 Testes manuais

1. Upload PDF válido.
2. Confirmar listagem.
3. Abrir detalhe.
4. Descarregar/visualizar.
5. Tentar `.txt`, `.exe` ou imagem como PDF.
6. Tentar ficheiro vazio.
7. Associar a um bloco.
8. Testar sem permissão.

---

## 10.5 Prompt para Claude Code — Revisão da Fase 9

Usa os agentes:

* `Architecture Reviewer`;
* `Security Reviewer`;
* `Test Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se a Fase 9 implementou corretamente o repositório de conteúdos em vários formatos, o upload e validação de PDF, o registo do responsável, o encadeamento estrutural das associações, a segurança de upload e a integração EduAll.

Antes de rever, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* Models, DAOs e Services de conteúdos;
* Services das associações de conteúdo;
* Services de upload;
* Servlets de upload/download;
* JSPs da fase;
* `docs/docs/analysis/eduall-content-pdf-integration.md`;
* `ContentItemServiceTest.java`;
* `ContentAssociationServiceTest.java`;
* `PdfUploadServiceTest.java`;
* `docs/tests/content-pdf-tests.md`.

Verifica:

* criação em vários formatos;
* PDF válido aceite e PDF inválido, vazio ou excessivo rejeitado;
* referência coerente com o formato;
* responsável validado;
* eliminação por autor e administrador;
* conteúdo com tentativas ou bloco ativo não eliminado fisicamente;
* encadeamento estrutural das associações;
* segurança de upload;
* permissões;
* se o front-end analisou o template completo;
* se todas as páginas necessárias foram alteradas;
* ausência de SQL em JSP;
* testes;
* correções Codex;
* documentação manual.

Executa testes e produz relatório.

---

## 10.6 Commit

```bash
git add .
git commit -m "Implementa conteudos pedagogicos multiformato com prioridade PDF"
```

---

# 11. Fase 10 — Aulas online e presenciais

## Objetivo da fase

Implementar aulas online, aulas presenciais e salas físicas, com associação a blocos pedagógicos e integração com ferramentas externas de videoconferência. Cobre os RF11A, RF11B, RF12A, RF12B e RF13C, e os UC11, UC12 e UC13C.

## Resultado esperado

* Models, DAOs e Services de aulas;
* Models, DAOs e Services de salas físicas;
* adaptador de videoconferência no pacote de integração;
* front-end EduAll;
* testes e correções.

## Critérios de conclusão

* CRUD de aulas e salas;
* aula online com ligação de acesso;
* aula presencial com sala ativa;
* aula híbrida com ligação e sala opcional;
* conflito de sala por sobreposição bloqueado;
* aula apenas associada a bloco da mesma turma;
* capacidade da sala validada;
* permissões aplicadas.

---

## 11.1 Prompt para Codex — back-end

Usa os agentes:

* `Codex Backend Agent`;
* `Codex Security Agent`.

Antes de implementar, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Cria, no mínimo, Models, DAOs e Services de aulas e de salas físicas, mais um adaptador de videoconferência no pacote de integração.

Tens liberdade para criar mais Models, DAOs, Services, validadores ou adaptadores se o modelo exigir. Não te limites à lista acima.

Implementa:

* CRUD de aulas e de salas físicas;
* capacidade da sala superior a zero;
* sala associada à mesma organização;
* coerência das datas da aula;
* aula online com ligação de acesso obrigatória;
* aula presencial com sala física atribuída;
* aula híbrida com ligação de acesso e sala opcional;
* sala atribuída ativa e disponível no período da aula;
* bloqueio de sobreposição de aulas na mesma sala;
* aula apenas associada a um bloco da mesma turma;
* integração externa de videoconferência isolada no pacote de integração, apenas guardando e validando os dados de acesso;
* permissões, com gestão de salas por administrador e coordenador e gestão de aulas por administrador, coordenador e formador, e acesso pelo aluno;
* registo de auditoria das operações.

Executa testes e corrige erros.

---

## 11.2 Prompt para Codex Test Agent — testar e corrigir

Usa o agente `Codex Test Agent`.

Cria:

* `LessonServiceTest.java`;
* `PhysicalRoomServiceTest.java`;
* `docs/tests/lessons-rooms-tests.md`.

Executa:

```bash
mvn test -Dtest=LessonServiceTest
mvn test -Dtest=PhysicalRoomServiceTest
```

Testa sala válida, capacidade inválida, sala de outra organização, aula online válida, aula online sem ligação, ligação inválida, aula presencial sem sala, sala inexistente, conflito de sala, datas inválidas e aula associada a bloco de outra turma.

Corrige e repete.

---

## 11.3 Prompt para Codex — front-end EduAll de aulas

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar, analisa o template EduAll completo.

Deves identificar autonomamente todas as páginas e componentes relacionados com:

* aulas;
* lições;
* calendário;
* videoconferência;
* aulas online;
* aulas presenciais;
* salas;
* horários;
* detalhe de aula;
* formulários de aula;
* cards de aula;
* listagens de aulas;
* links de reunião;
* gestão de salas;
* páginas de curso/turma onde aulas aparecem.

Tens liberdade para alterar todas as páginas necessárias. Se o template tiver páginas de aulas, classes, eventos, calendário, reuniões ou detalhe de curso, adapta as mais adequadas.

Cria ou adapta:

* listagem de aulas;
* detalhe de aula;
* criação/edição de aula;
* formulário de aula online;
* formulário de aula presencial;
* gestão de salas;
* calendário/listagem;
* links de videoconferência;
* mensagens;
* menus relacionados.

Liga ao back-end já testado.

Cria ou atualiza:

* `docs/docs/analysis/eduall-lessons-rooms-integration.md`.

Depois:

1. testar aula online;
2. testar aula presencial;
3. testar sala;
4. testar conflito;
5. testar calendário/listagem;
6. corrigir erros.

---

## 11.4 Testes manuais

1. Criar sala.
2. Criar aula presencial.
3. Criar aula no mesmo horário/sala.
4. Confirmar conflito.
5. Criar aula online válida.
6. Criar aula online com ligação inválida.
7. Testar permissões.

---

## 11.5 Prompt para Claude Code — Revisão da Fase 10

Usa os agentes:

* `Architecture Reviewer`;
* `SQL Reviewer`;
* `Security Reviewer`;
* `Test Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se a Fase 10 implementou corretamente aulas online, aulas presenciais, salas físicas, conflitos, datas, capacidade, integração externa e permissões.

Antes de rever, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* Models, DAOs e Services de aulas;
* Models, DAOs e Services de salas;
* adaptador de videoconferência;
* JSPs da fase;
* `docs/docs/analysis/eduall-lessons-rooms-integration.md`;
* `LessonServiceTest.java`;
* `PhysicalRoomServiceTest.java`;
* `docs/tests/lessons-rooms-tests.md`.

Verifica:

* CRUD aulas;
* CRUD salas;
* ligação online validada;
* sala presencial validada e ativa;
* conflito bloqueado;
* coerência aula-bloco;
* capacidade validada;
* integração isolada no pacote de integração;
* permissões;
* integração EduAll;
* se o front-end analisou o template completo;
* se todas as páginas necessárias foram alteradas;
* testes;
* correções Codex;
* documentação manual.

Executa testes e produz relatório.

---

## 11.6 Commit

```bash
git add .
git commit -m "Implementa aulas online presenciais e salas fisicas"
```

---

# 12. Fase 11 — Questionários e exames

## Objetivo da fase

Implementar avaliações, abrangendo questionários e exames, em modo online e presencial, com perguntas, opções, tentativas, respostas e correção automática e manual. Cobre os RF13A, RF13B, RF13D, RF13E, RF14A, RF14B, RF14C e RF14D, e os UC13 e UC14.

Um questionário associa-se a turmas através de um bloco pedagógico. Um exame associa-se à disciplina e, quando aplicável, às turmas abrangidas.

## Resultado esperado

* Models, DAOs e Services de avaliações, perguntas, opções, tentativas e respostas;
* serviço de correção automática e manual e cálculo de classificações;
* front-end EduAll de gestão, realização e correção;
* testes e correções.

## Critérios de conclusão

* criar questionários e exames, online e presenciais;
* gerir perguntas e opções;
* aluno realiza avaliação online dentro da janela e do limite de tentativas;
* respostas válidas por tipo de pergunta;
* correção automática apenas das perguntas objetivas e correção manual com classificação;
* pontuação entre zero e a classificação máxima;
* alterações estruturais bloqueadas após existirem tentativas submetidas;
* semântica de opções corretas por tipo de pergunta;
* permissões aplicadas.

---

## 12.1 Prompt para Codex — back-end

Usa os agentes:

* `Codex Backend Agent`;
* `Codex Security Agent`.

Antes de implementar, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Cria, no mínimo, Models, DAOs e Services de avaliações, perguntas, opções, tentativas e respostas, mais o serviço de correção e o cálculo de classificações.

Tens liberdade para criar mais Models, DAOs, Services ou validadores se o modelo de avaliação exigir. Não te limites à lista acima.

Implementa:

* avaliações do tipo questionário e do tipo exame, em modo online e presencial;
* associação de questionários a turmas através de bloco e de exames à disciplina e às turmas abrangidas;
* gestão de perguntas e de opções, com ordem única de pergunta e ordem única de opção;
* opções apenas em perguntas que as admitem;
* numa pergunta de escolha única ou lista, no máximo uma opção correta;
* numa pergunta de escolha múltipla com correção automática, pelo menos uma opção correta;
* classificação mínima de aprovação não superior à máxima e limite de tentativas superior a zero quando definido;
* realização de avaliação pelo aluno apenas dentro da janela de disponibilidade e dentro do limite de tentativas;
* numeração única de tentativa por aluno e avaliação;
* respostas válidas por tipo de pergunta, incluindo texto, ficheiro e seleção de opções;
* pontuação da tentativa entre zero e a classificação máxima;
* bloqueio de alterações estruturais a perguntas, opções e cotações depois de existirem tentativas submetidas;
* correção automática apenas das perguntas objetivas e correção manual ou mista das perguntas de texto e de ficheiro;
* permissões, com gestão por administrador, coordenador e formador e realização pelo aluno;
* registo de auditoria das operações de gestão e de correção.

Executa testes e corrige erros.

---

## 12.2 Prompt para Codex Test Agent — testar e corrigir

Usa o agente `Codex Test Agent`.

Cria:

* `AssessmentServiceTest.java`;
* `QuestionServiceTest.java`;
* `QuestionOptionServiceTest.java`;
* `AttemptServiceTest.java`;
* `ResponseServiceTest.java`;
* `CorrectionServiceTest.java`;
* `docs/tests/assessments-tests.md`.

Executa:

```bash
mvn test -Dtest=AssessmentServiceTest
mvn test -Dtest=QuestionServiceTest
mvn test -Dtest=QuestionOptionServiceTest
mvn test -Dtest=AttemptServiceTest
mvn test -Dtest=ResponseServiceTest
mvn test -Dtest=CorrectionServiceTest
```

Testa questionário e exame válidos, online e presencial, classificação de aprovação superior à máxima, limite de tentativas inválido, pergunta sem avaliação, opção sem pergunta, escolha única com duas opções corretas, escolha múltipla automática sem opção correta, tentativa fora da janela, acima do limite de tentativas, não-aluno a realizar, resposta de tipo errado, pontuação acima da máxima, correção automática só de objetivas, correção manual com classificação e alteração estrutural após submissão.

Corrige e repete.

---

## 12.3 Prompt para Codex — front-end EduAll de questionários e exames

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar, analisa o template EduAll completo.

Deves identificar autonomamente todas as páginas e componentes relacionados com:

* quizzes;
* questionários;
* avaliações;
* exames;
* testes;
* perguntas;
* opções;
* tentativas;
* resultados;
* progresso;
* formulários;
* páginas de aluno;
* páginas de professor;
* dashboards;
* cards de avaliação;
* páginas de curso/turma onde avaliações aparecem.

Tens liberdade para alterar todas as páginas necessárias. Se o template tiver páginas de quiz, exame, avaliação, aula de curso, dashboard de aluno ou dashboard de professor, adapta as mais adequadas.

Cria ou adapta:

* listagem de questionários e exames;
* criação/edição de avaliação;
* criação/edição de perguntas;
* criação/edição de opções;
* página para o aluno responder;
* página de resultado;
* página de correção automática e manual;
* página de revisão, se fizer sentido;
* mensagens;
* menus relacionados.

Liga ao back-end já testado.

Cria ou atualiza:

* `docs/docs/analysis/eduall-assessments-integration.md`.

Depois:

1. testar criação;
2. testar perguntas;
3. testar opções;
4. testar resposta como aluno;
5. testar resultado;
6. testar correção;
7. corrigir erros.

---

## 12.4 Testes manuais

1. Criar questionário.
2. Criar exame.
3. Criar perguntas.
4. Criar opções.
5. Definir corretas.
6. Entrar como aluno.
7. Responder.
8. Submeter.
9. Ver resultado.
10. Tentar acima do limite.
11. Testar fora da janela.
12. Corrigir manualmente como formador.

---

## 12.5 Prompt para Claude Code — Revisão da Fase 11

Usa os agentes:

* `Architecture Reviewer`;
* `Security Reviewer`;
* `Test Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se a Fase 11 implementou corretamente avaliações, abrangendo questionários e exames, online e presenciais, com perguntas, opções, tentativas, respostas, regras de tentativa, semântica de opções, correção e cálculo.

Antes de rever, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* Models, DAOs e Services de avaliações;
* Models, DAOs e Services de perguntas;
* Models, DAOs e Services de opções;
* Models, DAOs e Services de tentativas e respostas;
* serviço de correção e cálculo;
* JSPs da fase;
* `docs/docs/analysis/eduall-assessments-integration.md`;
* `AssessmentServiceTest.java`;
* `QuestionServiceTest.java`;
* `QuestionOptionServiceTest.java`;
* `AttemptServiceTest.java`;
* `ResponseServiceTest.java`;
* `CorrectionServiceTest.java`;
* `docs/tests/assessments-tests.md`.

Verifica:

* criação de questionários e exames, online e presenciais;
* criação de perguntas;
* criação de opções;
* opção correta por tipo de pergunta;
* tentativa;
* limite de tentativas;
* janela temporal;
* respostas por tipo;
* pontuação dentro dos limites;
* bloqueio de alterações após submissão;
* correção automática e manual;
* permissões;
* se o front-end analisou o template completo;
* se todas as páginas necessárias foram alteradas;
* testes;
* correções Codex;
* documentação manual.

Executa testes e produz relatório.

---

## 12.6 Commit

```bash
git add .
git commit -m "Implementa questionarios e exames online e presenciais"
```

---

# 13. Fase 12 — Horários e assiduidade

## Objetivo da fase

Implementar horários e eventos de calendário, registo de assiduidade e justificações de faltas, com processamento e visibilidade por perfil. Cobre os RF17A, RF17B, RF18A e RF18B, e os UC17 e UC18.

## Resultado esperado

* Models, DAOs e Services de eventos de horário;
* Models, DAOs e Services de assiduidade;
* Models, DAOs e Services de justificações;
* cálculo do tempo de permanência;
* front-end EduAll;
* testes e correções.

## Critérios de conclusão

* eventos coerentes com aula, avaliação e turma;
* datas válidas e lembretes coerentes;
* assiduidade apenas de aluno inscrito;
* um registo de assiduidade ativo por aluno e aula;
* justificação apenas do próprio aluno e para registo compatível;
* processamento apenas por perfil autorizado;
* visibilidade de eventos por perfil;
* permissões aplicadas.

---

## 13.1 Prompt para Codex — back-end

Usa os agentes:

* `Codex Backend Agent`;
* `Codex Security Agent`.

Antes de implementar, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Cria, no mínimo, Models, DAOs e Services de eventos de horário, de assiduidade e de justificações, mais o cálculo do tempo de permanência.

Tens liberdade para criar mais Models, DAOs, Services ou validadores se o modelo exigir. Não te limites à lista acima.

Implementa:

* eventos de horário associados a aulas, avaliações e turmas, com datas coerentes e lembretes coerentes;
* coerência do período do evento com a aula ou avaliação associada;
* visibilidade de eventos por perfil, de forma que cada utilizador só recebe os eventos do seu contexto;
* registo de assiduidade apenas para aluno inscrito na turma da aula;
* um único registo de assiduidade ativo por aluno e aula;
* coerência das horas de entrada e saída e cálculo do tempo de permanência;
* submissão de justificação apenas pelo próprio aluno e apenas para registo compatível com ausência, atraso ou presença parcial;
* processamento da justificação apenas por perfil autorizado, com datas coerentes e estado final com data de processamento;
* permissões, com gestão por formador, coordenador e administrador conforme o contexto;
* registo de auditoria das operações de assiduidade e de processamento.

Executa testes e corrige erros.

---

## 13.2 Prompt para Codex Test Agent — testar e corrigir

Usa o agente `Codex Test Agent`.

Cria:

* `ScheduleEventServiceTest.java`;
* `AttendanceRecordServiceTest.java`;
* `AbsenceJustificationServiceTest.java`;
* `docs/tests/schedule-attendance-tests.md`.

Executa:

```bash
mvn test -Dtest=ScheduleEventServiceTest
mvn test -Dtest=AttendanceRecordServiceTest
mvn test -Dtest=AbsenceJustificationServiceTest
```

Testa evento válido, datas inválidas, lembrete sem minutos, assiduidade de aluno não inscrito, dois registos ativos do mesmo aluno e aula, horas de entrada e saída incoerentes, justificação de registo incompatível, justificação de outro aluno, datas de processamento incoerentes e processamento sem permissão.

Corrige e repete.

---

## 13.3 Prompt para Codex — front-end EduAll de horários e assiduidade

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar, analisa o template EduAll completo.

Deves identificar autonomamente todas as páginas e componentes relacionados com:

* calendário;
* horários;
* eventos;
* aulas;
* assiduidade;
* presenças;
* faltas;
* justificações;
* dashboards de aluno;
* dashboards de professor;
* formulários;
* tabelas;
* filtros;
* páginas de processamento;
* notificações de faltas.

Tens liberdade para alterar todas as páginas necessárias. Se o template tiver páginas de calendário, eventos, assiduidade, horário, dashboard de aluno ou dashboard de professor, adapta as mais adequadas.

Cria ou adapta:

* calendário;
* página de horários;
* página de assiduidade;
* registo de presença/falta;
* formulário de justificação;
* processamento de justificações;
* mensagens;
* menus relacionados.

Liga ao back-end já testado.

Cria ou atualiza:

* `docs/docs/analysis/eduall-schedule-attendance-integration.md`.

Depois:

1. testar calendário;
2. testar assiduidade;
3. testar justificação;
4. testar processamento;
5. corrigir erros.

---

## 13.4 Testes manuais

1. Criar evento.
2. Ver calendário.
3. Registar falta.
4. Entrar como aluno.
5. Submeter justificação.
6. Processar como professor/admin.
7. Tentar processar sem permissão.

---

## 13.5 Prompt para Claude Code — Revisão da Fase 12

Usa os agentes:

* `Architecture Reviewer`;
* `Security Reviewer`;
* `Test Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se a Fase 12 implementou corretamente horários, eventos, assiduidade, justificações, processamento, visibilidade e integração EduAll.

Antes de rever, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* Models, DAOs e Services de horários;
* Models, DAOs e Services de assiduidade;
* Models, DAOs e Services de justificações;
* JSPs da fase;
* `docs/docs/analysis/eduall-schedule-attendance-integration.md`;
* `ScheduleEventServiceTest.java`;
* `AttendanceRecordServiceTest.java`;
* `AbsenceJustificationServiceTest.java`;
* `docs/tests/schedule-attendance-tests.md`.

Verifica:

* eventos;
* datas;
* assiduidade;
* aluno inscrito;
* um registo ativo por aluno e aula;
* justificação do próprio e compatível;
* processamento por perfil;
* visibilidade;
* permissões;
* se o front-end analisou o template completo;
* se todas as páginas necessárias foram alteradas;
* testes;
* correções Codex;
* documentação manual.

Executa testes e produz relatório.

---

## 13.6 Commit

```bash
git add .
git commit -m "Implementa horarios assiduidade e justificacoes"
```

---

# 14. Fase 13 — Pautas e certificados

## Objetivo da fase

Implementar pautas, classificações com pesos, cálculo e publicação, e certificados com emissão e validação. Cobre os RF15A, RF15B, RF16A e RF16B, e os UC15 e UC16.

## Resultado esperado

* Models, DAOs e Services de pautas;
* Models, DAOs e Services de classificações;
* Models, DAOs e Services de certificados;
* cálculo do resultado e validação pública de certificado;
* front-end EduAll;
* testes e correções.

## Critérios de conclusão

* criar pauta;
* lançar nota válida;
* peso superior a 100% rejeitado;
* nota negativa ou acima da escala rejeitada;
* publicar pauta e bloquear alteração direta de pauta publicada;
* resultado calculado;
* certificado válido emitido apenas com elegibilidade e com código de validação único;
* certificado revogado não é válido;
* permissões aplicadas.

---

## 14.1 Prompt para Codex — back-end

Usa os agentes:

* `Codex Backend Agent`;
* `Codex Security Agent`.

Antes de implementar, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Cria, no mínimo, Models, DAOs e Services de pautas, de classificações e de certificados, mais o cálculo do resultado e a validação pública de certificado.

Tens liberdade para criar mais Models, DAOs, Services ou validadores se o modelo exigir. Não te limites à lista acima.

Implementa:

* pautas associadas à disciplina e, quando aplicável, às turmas da mesma disciplina;
* pautas que se baseiam em avaliações coerentes com a disciplina, com peso superior a zero;
* soma dos pesos das avaliações não superior a 100% quando a pauta usa ponderações percentuais;
* um único registo de classificação ativo por pauta e aluno;
* cálculo do resultado a partir da classificação e das regras de avaliação;
* classificação entre zero e a escala definida para a pauta;
* publicação da pauta e bloqueio de alterações diretas a pautas publicadas;
* certificados associados ao curso, com código de validação único e data de emissão quando emitidos;
* emissão de certificado apenas quando o aluno cumpre as condições definidas;
* certificado revogado não apresentado como válido;
* validação pública de certificado por código;
* permissões, com gestão por administrador, coordenador e formador e consulta pelo aluno apenas das suas classificações e certificados;
* registo de auditoria das operações de lançamento, publicação, emissão e revogação.

Executa testes e corrige erros.

---

## 14.2 Prompt para Codex Test Agent — testar e corrigir

Usa o agente `Codex Test Agent`.

Cria:

* `GradeSheetServiceTest.java`;
* `GradeRecordServiceTest.java`;
* `CertificateServiceTest.java`;
* `docs/tests/grades-certificates-tests.md`.

Executa:

```bash
mvn test -Dtest=GradeSheetServiceTest
mvn test -Dtest=GradeRecordServiceTest
mvn test -Dtest=CertificateServiceTest
```

Testa pauta válida, peso superior a 100%, nota negativa, nota acima da escala, publicar pauta, alterar pauta publicada, resultado calculado, certificado válido, certificado sem elegibilidade, código de validação duplicado, certificado revogado não validado e consulta ou gestão sem permissão.

Corrige e repete.

---

## 14.3 Prompt para Codex — front-end EduAll de pautas e certificados

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar, analisa o template EduAll completo.

Deves identificar autonomamente todas as páginas e componentes relacionados com:

* notas;
* classificações;
* pautas;
* resultados;
* progresso;
* certificados;
* diplomas;
* conquistas;
* dashboards de aluno;
* dashboards de professor;
* tabelas de notas;
* páginas de certificado;
* emissão;
* validação;
* consulta.

Tens liberdade para alterar todas as páginas necessárias. Se o template tiver páginas de notas, resultados, certificados, conquistas, dashboard de aluno ou dashboard de professor, adapta as mais adequadas.

Cria ou adapta:

* página de pautas;
* lançamento de notas;
* consulta de notas;
* publicação de pauta;
* emissão de certificado;
* consulta de certificado;
* validação de certificado;
* mensagens;
* menus relacionados.

Liga ao back-end já testado.

Cria ou atualiza:

* `docs/docs/analysis/eduall-grades-certificates-integration.md`.

Depois:

1. testar pauta;
2. testar nota;
3. testar publicação;
4. testar certificado;
5. corrigir erros.

---

## 14.4 Testes manuais

1. Criar pauta.
2. Lançar nota válida.
3. Tentar nota inválida.
4. Publicar pauta.
5. Tentar alterar publicada.
6. Consultar como aluno.
7. Emitir certificado.
8. Validar certificado.

---

## 14.5 Prompt para Claude Code — Revisão da Fase 13

Usa os agentes:

* `Architecture Reviewer`;
* `SQL Reviewer`;
* `Security Reviewer`;
* `Test Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se a Fase 13 implementou corretamente pautas, classificações, pesos, publicação, bloqueio de alterações, certificados e validação.

Antes de rever, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* Models, DAOs e Services de pautas;
* Models, DAOs e Services de classificações;
* Models, DAOs e Services de certificados;
* JSPs da fase;
* `docs/docs/analysis/eduall-grades-certificates-integration.md`;
* `GradeSheetServiceTest.java`;
* `GradeRecordServiceTest.java`;
* `CertificateServiceTest.java`;
* `docs/tests/grades-certificates-tests.md`.

Verifica:

* criação de pauta;
* pesos;
* classificações;
* publicação;
* bloqueio de pauta publicada;
* resultado calculado;
* emissão de certificados com elegibilidade e código único;
* certificado revogado não válido;
* validação pública;
* permissões;
* se o front-end analisou o template completo;
* se todas as páginas necessárias foram alteradas;
* testes;
* correções Codex;
* documentação manual.

Executa testes e produz relatório.

---

## 14.6 Commit

```bash
git add .
git commit -m "Implementa pautas classificacoes e certificados"
```

---

# 15. Fase 14 — Notificações, mensagens, fóruns e comentários

## Objetivo da fase

Implementar a comunicação entre utilizadores através de mensagens, fóruns e comentários, mais os canais, a participação, as respostas, o agendamento e as notificações e alertas internos e por email. Cobre os RF20 e RF21, e os UC20 e UC21.

## Resultado esperado

* Models, DAOs e Services de canais e mensagens;
* participação e receção de mensagens;
* notificações com agendamento;
* adaptador de email no pacote de integração;
* front-end EduAll;
* testes e correções.

## Critérios de conclusão

* enviar e receber mensagens;
* responder no mesmo canal;
* uma participação ativa por utilizador e canal;
* mensagem apenas por participante, exceto quando gerada pelo sistema;
* agendamento respeitado;
* anexos coerentes;
* notificações por email apenas com email válido;
* canais coerentes com o contexto estrutural;
* moderação por perfil;
* permissões aplicadas.

---

## 15.1 Prompt para Codex — back-end

Usa os agentes:

* `Codex Backend Agent`;
* `Codex Security Agent`.

Antes de implementar, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Cria, no mínimo, Models, DAOs e Services de canais, de mensagens, de participação e de receção de mensagens, mais o serviço de notificações e um adaptador de email no pacote de integração.

Tens liberdade para criar mais Models, DAOs, Services ou validadores se o modelo de comunicação exigir. Não te limites à lista acima.

Implementa:

* canais de mensagem, fórum, comentários, anúncios e canais do sistema, com visibilidade adequada;
* uma única participação ativa por utilizador e canal;
* envio de mensagem apenas por participante do canal, exceto quando gerada automaticamente pelo sistema;
* respostas a mensagens apenas dentro do mesmo canal;
* agendamento de mensagens e envio efetivo a partir da data agendada;
* coerência das datas de criação, atualização e envio;
* exigência de anexo quando o tipo de mensagem é anexo;
* notificações e alertas internos e por email, com envio por email apenas para destinatários com email válido e ativo;
* coerência da data de leitura em relação à data de entrega;
* mensagens originadas por eventos de horário a respeitar o período e os destinatários do evento;
* canais associados a turmas, blocos ou avaliações a respeitar o encadeamento estrutural;
* moderação por perfil, com o formador a moderar canais das turmas que leciona, o coordenador das disciplinas que coordena e o administrador das organizações que administra;
* adaptador de email isolado no pacote de integração;
* registo de auditoria das operações relevantes.

Executa testes e corrige erros.

---

## 15.2 Prompt para Codex Test Agent — testar e corrigir

Usa o agente `Codex Test Agent`.

Cria:

* `ChannelServiceTest.java`;
* `MessageServiceTest.java`;
* `ChannelParticipationServiceTest.java`;
* `NotificationServiceTest.java`;
* `docs/tests/notifications-messages-tests.md`.

Executa:

```bash
mvn test -Dtest=ChannelServiceTest
mvn test -Dtest=MessageServiceTest
mvn test -Dtest=ChannelParticipationServiceTest
mvn test -Dtest=NotificationServiceTest
```

Testa notificação, mensagem, mensagem gerada pelo sistema sem remetente, mensagem por não-participante, resposta a si própria ou fora do canal, segunda participação ativa, agendamento respeitado, mensagem de anexo sem anexo, email sem endereço válido, canal incoerente com o contexto e moderação sem permissão.

Corrige e repete.

---

## 15.3 Prompt para Codex — front-end EduAll de notificações, mensagens e comunicação

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar, analisa o template EduAll completo.

Deves identificar autonomamente todas as páginas e componentes relacionados com:

* notificações;
* alertas;
* mensagens;
* caixa de entrada;
* chat;
* comentários;
* fóruns;
* canais;
* dashboards;
* dropdowns de notificação;
* badges;
* páginas de detalhe;
* formulários de envio;
* respostas;
* páginas administrativas.

Tens liberdade para alterar todas as páginas necessárias. Se o template tiver páginas de mensagens, notificações, caixa de entrada, chat, fóruns ou dashboard administrativo, adapta as mais adequadas.

Cria ou adapta:

* notificações e badges no header;
* caixa de entrada;
* detalhe de mensagem;
* envio de mensagem;
* resposta;
* fóruns e comentários;
* canais;
* menus relacionados;
* mensagens de erro/sucesso.

Liga ao back-end já testado.

Cria ou atualiza:

* `docs/docs/analysis/eduall-notifications-messages-integration.md`.

Depois:

1. testar notificações;
2. testar mensagens;
3. testar respostas;
4. testar fóruns e comentários;
5. corrigir erros.

---

## 15.4 Testes manuais

1. Enviar mensagem.
2. Responder.
3. Ver caixa de entrada.
4. Ver notificação.
5. Usar um fórum ou comentários.
6. Agendar uma notificação.
7. Confirmar moderação por perfil.

---

## 15.5 Prompt para Claude Code — Revisão da Fase 14

Usa os agentes:

* `Architecture Reviewer`;
* `Security Reviewer`;
* `Test Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se a Fase 14 implementou corretamente canais, mensagens, fóruns, comentários, respostas, agendamento, notificações e moderação.

Antes de rever, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* Models, DAOs e Services de canais;
* Models, DAOs e Services de mensagens;
* Models, DAOs e Services de participação e receção;
* serviço de notificações;
* adaptador de email;
* JSPs da fase;
* `docs/docs/analysis/eduall-notifications-messages-integration.md`;
* `ChannelServiceTest.java`;
* `MessageServiceTest.java`;
* `ChannelParticipationServiceTest.java`;
* `NotificationServiceTest.java`;
* `docs/tests/notifications-messages-tests.md`.

Verifica:

* participação única;
* mensagem por participante ou sistema;
* resposta no mesmo canal;
* agendamento;
* anexos;
* email válido;
* canal coerente com o contexto;
* moderação por perfil;
* email isolado no pacote de integração;
* se o front-end analisou o template completo;
* se todas as páginas necessárias foram alteradas;
* testes;
* correções Codex;
* documentação manual.

Executa testes e produz relatório.

---

## 15.6 Commit

```bash
git add .
git commit -m "Implementa notificacoes mensagens foruns e comentarios"
```

---

# 16. Fase 15 — Painéis de gestão, controlo e relatórios

## Objetivo da fase

Implementar o acesso e a configuração de painéis de gestão, controlo e relatórios, de acordo com o perfil e o âmbito de visibilidade. Cobre o RF19 e o UC19.

Os painéis reutilizam os dados das fases anteriores e apresentam-nos por âmbito, que pode ser global, de organização, de curso, de disciplina, de turma ou pessoal.

## Resultado esperado

* Models, DAOs e Services de painéis;
* serviço de acesso a painéis por âmbito;
* serviço de agregação de dados para relatórios;
* front-end EduAll com dashboards e relatórios por perfil;
* testes e correções.

## Critérios de conclusão

* criar e configurar painéis com âmbito de visibilidade;
* acesso restrito por permissão e por âmbito;
* cada perfil acede apenas aos painéis do seu contexto;
* relatórios apresentam dados coerentes das áreas anteriores;
* auditoria dos acessos a painéis sensíveis.

---

## 16.1 Prompt para Codex — back-end

Usa os agentes:

* `Codex Backend Agent`;
* `Codex Security Agent`.

Antes de implementar, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Cria, no mínimo, Models, DAOs e Services de painéis, o serviço de acesso por âmbito e o serviço de agregação de dados para relatórios.

Tens liberdade para criar mais Models, DAOs, Services ou agregadores se o modelo exigir. Não te limites à lista acima.

Implementa:

* criação e configuração de painéis com âmbito de visibilidade global, de organização, de curso, de disciplina, de turma ou pessoal;
* acesso a painéis apenas com permissão adequada ao âmbito;
* acesso por perfil: administrador a painéis globais e de organização das organizações que administra, coordenador a painéis das disciplinas que coordena, formador a painéis das turmas que leciona e aluno a painéis pessoais e a relatórios do seu contexto;
* agregação de indicadores reutilizando os DAOs das áreas anteriores;
* registo de auditoria dos acessos a painéis sensíveis e das configurações.

Executa testes e corrige erros.

---

## 16.2 Prompt para Codex Test Agent — testar e corrigir

Usa o agente `Codex Test Agent`.

Cria:

* `ManagementViewServiceTest.java`;
* `ManagementViewAccessServiceTest.java`;
* `ReportAggregationServiceTest.java`;
* `docs/tests/management-views-tests.md`.

Executa:

```bash
mvn test -Dtest=ManagementViewServiceTest
mvn test -Dtest=ManagementViewAccessServiceTest
mvn test -Dtest=ReportAggregationServiceTest
```

Testa criar painel por âmbito, acesso sem permissão ao âmbito, administrador a painel global e de organização, coordenador apenas às suas disciplinas, formador apenas às suas turmas, aluno apenas a painéis pessoais e do seu contexto e agregações com dados coerentes.

Corrige e repete.

---

## 16.3 Prompt para Codex — front-end EduAll de painéis e relatórios

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar, analisa o template EduAll completo.

Deves identificar autonomamente todas as páginas e componentes relacionados com:

* dashboards;
* painéis;
* relatórios;
* gráficos;
* cards de indicadores;
* tabelas de resumo;
* widgets;
* dashboards por perfil.

Tens liberdade para alterar todas as páginas necessárias. O template EduAll tem vários dashboards e widgets; usa os mais adequados a cada âmbito e perfil.

Cria ou adapta:

* dashboards por perfil, para administrador, coordenador, formador e aluno;
* páginas de relatório por âmbito;
* cards e gráficos de indicadores;
* filtros;
* mensagens;
* menus relacionados.

Liga ao back-end já testado.

Cria ou atualiza:

* `docs/docs/analysis/eduall-management-views-integration.md`.

Depois:

1. testar dashboards por perfil;
2. testar relatórios por âmbito;
3. corrigir erros.

---

## 16.4 Testes manuais

1. Login admin e ver painel global e de organização.
2. Login coordenador e ver painéis das suas disciplinas.
3. Login formador e ver painéis das suas turmas.
4. Login aluno e ver painel pessoal.
5. Tentar aceder a painel fora do âmbito e confirmar acesso negado.

---

## 16.5 Prompt para Claude Code — Revisão da Fase 15

Usa os agentes:

* `Architecture Reviewer`;
* `Security Reviewer`;
* `Test Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se a Fase 15 implementou corretamente os painéis de gestão, controlo e relatórios, o acesso por perfil e âmbito e a integração EduAll.

Antes de rever, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* Models, DAOs e Services de painéis;
* serviço de acesso por âmbito;
* serviço de agregação;
* JSPs da fase;
* `docs/docs/analysis/eduall-management-views-integration.md`;
* `ManagementViewServiceTest.java`;
* `ManagementViewAccessServiceTest.java`;
* `ReportAggregationServiceTest.java`;
* `docs/tests/management-views-tests.md`.

Verifica:

* acesso por âmbito;
* administrador, coordenador, formador e aluno com acessos corretos;
* relatórios coerentes;
* auditoria dos acessos sensíveis;
* se o front-end analisou o template completo;
* se todas as páginas necessárias foram alteradas;
* testes;
* correções Codex;
* documentação manual.

Executa testes e produz relatório.

---

## 16.6 Commit

```bash
git add .
git commit -m "Implementa paineis de gestao controlo e relatorios"
```

---

# 17. Fase 16 — Registo de atividades e requisitos não funcionais

## Objetivo da fase

Consolidar o registo imutável de atividades para auditoria e rastreabilidade, e garantir os requisitos não funcionais de segurança, proteção de dados e cópias de segurança. Cobre o RF22 e o UC22.

## Resultado esperado

* consulta do registo de atividades por perfil;
* garantia de imutabilidade do registo;
* proteção dos dados sensíveis em repouso e ligação segura;
* rotina de cópias de segurança;
* front-end EduAll de auditoria;
* testes e correções.

## Critérios de conclusão

* operações críticas de todas as áreas geram registo de atividade;
* o registo é imutável para utilizadores comuns;
* consulta do registo por perfil, com o administrador a ver o registo global, o coordenador e o formador apenas o seu contexto e o aluno apenas o próprio histórico;
* dados sensíveis protegidos em repouso e comunicações por ligação segura;
* cópias de segurança diárias com retenção mínima de sete dias;
* revisão final de acessibilidade, navegação e compatibilidade.

---

## 17.1 Prompt para Codex — back-end

Usa os agentes:

* `Codex Backend Agent`;
* `Codex Security Agent`.

Antes de implementar, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`.

Revê todas as fases anteriores e cria, no mínimo, o serviço de consulta do registo de atividades por perfil, o reforço do serviço de auditoria onde faltar, a proteção dos dados sensíveis em repouso, a configuração de ligação segura e a rotina de cópias de segurança.

Tens liberdade para criar mais Services, filtros ou componentes de segurança se for necessário para fechar os requisitos não funcionais. Não te limites à lista acima.

Implementa:

* registo de atividade com os campos mínimos e coerência com a sessão do utilizador;
* geração de registo de atividade em todas as operações críticas das áreas anteriores, abrangendo dados pessoais, permissões, conteúdos, assiduidade, classificações, certificados, mensagens, justificações e eliminações;
* imutabilidade do registo para utilizadores comuns;
* consulta do registo por perfil, com o administrador a ver o registo global das organizações que administra, o coordenador e o formador apenas o seu contexto e o aluno apenas o próprio histórico;
* autorização em todas as funcionalidades restritas;
* proteção dos dados sensíveis em repouso com cifra forte;
* preparação e documentação da ligação segura por HTTPS;
* rotina de cópias de segurança diárias com retenção mínima de sete dias e respetiva documentação.

Executa testes e corrige erros.

---

## 17.2 Prompt para Codex Test Agent — testar e corrigir

Usa o agente `Codex Test Agent`.

Cria:

* `ActivityLogQueryServiceTest.java`;
* `AuditCoverageTest.java`;
* `SensitiveDataCipherTest.java`;
* `docs/tests/audit-rnf-tests.md`.

Executa:

```bash
mvn test -Dtest=ActivityLogQueryServiceTest
mvn test -Dtest=AuditCoverageTest
mvn test -Dtest=SensitiveDataCipherTest
```

Testa campos mínimos do registo, coerência com a sessão, geração de registo numa operação crítica de cada área, tentativa de alterar ou eliminar o registo por utilizador comum, consulta por administrador, coordenador, formador e aluno, cifra e decifra de dados sensíveis e verificação documental de ligação segura e cópias de segurança.

Corrige e repete.

---

## 17.3 Prompt para Codex — front-end EduAll de auditoria

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar, analisa o template EduAll completo.

Deves identificar autonomamente todas as páginas e componentes relacionados com:

* registos;
* atividade;
* auditoria;
* histórico;
* tabelas de registos;
* filtros;
* páginas administrativas.

Tens liberdade para alterar todas as páginas necessárias.

Cria ou adapta:

* página de consulta de auditoria, com filtros por tipo, entidade, data e utilizador;
* histórico próprio do aluno;
* mensagens;
* menus relacionados.

Liga ao back-end já testado.

Cria ou atualiza:

* `docs/docs/analysis/eduall-audit-integration.md`.

Depois:

1. testar consulta por perfil;
2. corrigir erros.

---

## 17.4 Testes manuais

1. Executar operações críticas em várias áreas.
2. Login admin e abrir auditoria.
3. Confirmar registos com utilizador, data, tipo e resultado.
4. Tentar alterar ou eliminar um registo e confirmar bloqueio.
5. Login aluno e tentar auditoria global, confirmando acesso negado e apenas o próprio histórico.
6. Confirmar documentalmente a ligação segura, a cifra de dados sensíveis e as cópias de segurança.

---

## 17.5 Prompt para Claude Code — Revisão da Fase 16

Usa os agentes:

* `Security Reviewer`;
* `Architecture Reviewer`;
* `SQL Reviewer`;
* `Test Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se a Fase 16 consolidou a auditoria e garantiu os requisitos não funcionais de segurança, proteção de dados e cópias de segurança.

Antes de rever, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* serviço de consulta de auditoria;
* proteção de dados sensíveis;
* configuração de ligação segura e cópias de segurança;
* JSPs de auditoria;
* `docs/docs/analysis/eduall-audit-integration.md`;
* `ActivityLogQueryServiceTest.java`;
* `AuditCoverageTest.java`;
* `SensitiveDataCipherTest.java`;
* `docs/tests/audit-rnf-tests.md`.

Verifica:

* operações críticas de todas as áreas geram registo;
* imutabilidade do registo;
* consulta por perfil;
* cifra dos dados sensíveis;
* ligação segura documentada;
* cópias de segurança diárias com retenção de sete dias;
* autorização em todas as funcionalidades restritas;
* revisão de acessibilidade, navegação e compatibilidade;
* se o front-end analisou o template completo;
* sem SQL nem regras de negócio nas JSP;
* testes;
* correções Codex;
* documentação manual.

Executa testes e produz relatório.

---

## 17.6 Commit

```bash
git add .
git commit -m "Consolida auditoria e requisitos nao funcionais"
```

---

# 18. Fase 17 — Testes completos e correções finais

## Objetivo da fase

Executar uma revisão completa do projeto, corrigir falhas e estabilizar o GAPE.

---

## 18.1 Prompt para Claude Code — Revisão Final do Projeto

Usa os agentes:

* `Architecture Reviewer`;
* `SQL Reviewer`;
* `XML/XSD Reviewer`;
* `Security Reviewer`;
* `Test Reviewer`;
* `Demo Reviewer`;
* `Document Analyst Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se o projeto completo GAPE cumpre o planeamento, o relatório, o modelo EA, a arquitetura definida, os critérios de qualidade, os requisitos não funcionais, os testes e a demonstração esperada.

Antes de rever, analisa obrigatoriamente:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* código completo;
* base de dados;
* XML/XSD;
* DAOs;
* Services;
* Servlets;
* filtros;
* JSP;
* template EduAll completo;
* documentação de análise EduAll de todas as fases;
* testes;
* documentação;
* dados de demonstração.

Verifica:

* arquitetura;
* segurança;
* base de dados;
* XML/XSD;
* autenticação;
* sessão;
* permissões e atribuição contextual;
* utilizadores, privacidade e eliminação;
* organizações;
* cursos e disciplinas;
* turmas e blocos;
* conteúdos;
* aulas e salas;
* questionários e exames;
* horários e assiduidade;
* pautas e certificados;
* notificações, mensagens, fóruns e comentários;
* painéis e relatórios;
* auditoria;
* requisitos não funcionais;
* testes automáticos;
* testes manuais;
* correções feitas pelo Codex;
* dados de demonstração;
* se o front-end analisou o template completo em todas as fases aplicáveis;
* se todas as páginas necessárias foram alteradas;
* se não existem páginas estáticas que deviam estar dinâmicas;
* se não há SQL nas JSP;
* se não há regras de negócio nas JSP.

Executa:

```bash
mvn clean package
mvn test
```

Se encontrar erros pequenos, pode corrigir.

Se encontrar erro grande:

1. explica o problema;
2. explica o impacto;
3. propõe solução;
4. indica ficheiros a alterar;
5. pede autorização.

Produz relatório final com problemas por gravidade:

* crítico;
* alto;
* médio;
* baixo.

---

## 18.2 Prompt para Codex — corrigir problemas autorizados

Usa o agente Codex adequado.

Para cada problema autorizado pelo utilizador:

1. analisar descrição do Claude;
2. identificar ficheiros afetados;
3. corrigir;
4. executar testes da área;
5. corrigir novas falhas;
6. repetir até passar;
7. atualizar documentação.

---

## 18.3 Prompt para Codex Test Agent — bateria final com correção

Usa o agente `Codex Test Agent`.

Cria:

* `FullSystemSmokeTest.java`, se viável;
* `docs/tests/full-system-tests.md`;
* `docs/tests/manual-final-checklist.md`.

Executa:

```bash
mvn clean package
mvn test
```

Se falhar:

1. identificar módulo;
2. pedir correção ao agente Codex adequado;
3. repetir testes;
4. atualizar documentação.

Inclui na documentação a verificação dos requisitos não funcionais: acessibilidade, navegação em poucos níveis, compatibilidade com Chrome, Edge e Firefox, ligação segura, proteção de dados sensíveis e cópias de segurança.

---

## 18.4 Testes manuais finais

1. `mvn clean package`.
2. `mvn test`.
3. Recriar BD.
4. Carregar dados.
5. Iniciar Tomcat.
6. Login admin.
7. Criar organização.
8. Criar unidade.
9. Criar curso.
10. Criar disciplina.
11. Criar turma.
12. Criar bloco.
13. Upload PDF.
14. Criar aula.
15. Criar questionário ou exame.
16. Login aluno.
17. Ver conteúdo.
18. Realizar avaliação.
19. Consultar resultado.
20. Consultar horário.
21. Justificar falta.
22. Consultar pauta e certificado.
23. Enviar mensagem.
24. Ver painel.
25. Login admin.
26. Ver auditoria.
27. Confirmar sem erros 500.
28. Confirmar sem assets 404 em Chrome, Edge e Firefox.

---

## 18.5 Commit

```bash
git add .
git commit -m "Estabiliza versao final do GAPE"
```

---

# 19. Fase 18 — Dados de demonstração e versão final

## Objetivo da fase

Preparar os dados finais de demonstração e validar que o fluxo completo do GAPE é demonstrável de forma estável, coerente e apresentável.

---

## 19.1 Prompt para Codex — dados de demonstração

Usa o agente `Codex Demo Agent`.

Antes de criar, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* `data-demo.sql` existente;
* todas as áreas implementadas.

Cria dados de demonstração coerentes que cubram todas as áreas:

* organização;
* unidade;
* admin;
* coordenador;
* formador;
* aluno;
* permissões e atribuições contextuais;
* curso;
* disciplina;
* turma;
* bloco;
* conteúdo PDF e outros formatos;
* aula online;
* aula presencial;
* sala;
* questionário e exame;
* perguntas;
* opções;
* tentativa;
* respostas;
* horário e evento;
* assiduidade;
* justificação;
* pauta e classificações;
* certificado;
* canais e mensagens;
* notificações;
* painel;
* auditoria.

Depois:

1. executar `data-demo.sql`;
2. corrigir dados incoerentes, respeitando todas as restrições;
3. repetir até carregar sem erro.

---

## 19.2 Prompt para Codex Test Agent — testar e corrigir dados de demonstração

Usa o agente `Codex Test Agent`.

Cria:

* `DemoDataTest.java`;
* `DemoFlowTest.java`, se viável;
* `docs/tests/demo-tests.md`.

Executa:

```bash
mvn test -Dtest=DemoDataTest
```

Testa:

1. `data-demo.sql` executa;
2. utilizadores de cada perfil existem;
3. logins funcionam;
4. organização existe;
5. curso existe;
6. turma existe;
7. conteúdo existe;
8. aula existe;
9. avaliação existe;
10. pauta e certificado existem;
11. mensagens existem;
12. painel existe;
13. auditoria existe.

Se falhar:

1. corrigir `data-demo.sql`;
2. corrigir dados incoerentes;
3. repetir teste.

---

## 19.3 Prompt para Codex — ajustar front-end para demonstração final

Usa o agente `Codex Frontend/JSP Agent`.

Antes de alterar, analisa o template EduAll completo e todas as integrações front-end já feitas nas fases anteriores.

Objetivo:

Garantir que a demonstração final tem uma navegação coerente, estável e visualmente apresentável.

Deves verificar autonomamente:

* página inicial;
* login;
* dashboard;
* menus;
* cursos;
* turmas;
* conteúdos;
* aulas;
* questionários e exames;
* horários;
* pautas;
* certificados;
* mensagens;
* notificações;
* painéis;
* auditoria;
* páginas de erro;
* páginas sem dados;
* mensagens de sucesso;
* mensagens de erro.

Tens liberdade para alterar todas as páginas necessárias para que a demonstração fique coerente.

Podes:

* reorganizar menus;
* melhorar links;
* corrigir breadcrumbs;
* corrigir labels;
* corrigir botões;
* corrigir mensagens;
* corrigir páginas incompletas;
* melhorar páginas vazias;
* corrigir assets;
* melhorar consistência visual.

Não podes:

* alterar regras de negócio;
* esconder erros reais;
* remover validações;
* colocar SQL nas JSP;
* colocar regras de negócio nas JSP.

Cria ou atualiza:

* `docs/docs/analysis/eduall-demo-final-adjustments.md`.

Depois:

1. testar fluxo da demonstração;
2. corrigir problemas visuais;
3. corrigir links quebrados;
4. repetir até o fluxo estar apresentável.

---

## 19.4 Prompt para Claude Code — Validação da Demonstração Final

Usa os agentes:

* `Demo Reviewer`;
* `Test Reviewer`;
* `Architecture Reviewer`;
* `Frontend/JSP Reviewer`.

Objetivo da revisão:

Verificar se os dados de demonstração e o fluxo final permitem apresentar o GAPE de forma estável, coerente e alinhada com o planeamento.

Antes de rever, analisa:

* `1. Planeamento de IA e Código`;
* `7. Relatorio - 49862 - 7`;
* `0. GAPE - ALL - V3`;
* `data-demo.sql`;
* `DemoDataTest.java`;
* `docs/tests/demo-tests.md`;
* template EduAll;
* dados carregados;
* fluxo de demonstração;
* `docs/docs/analysis/eduall-demo-final-adjustments.md`.

Verifica:

* se `data-demo.sql` executa;
* se utilizadores de demonstração existem;
* se logins funcionam;
* se existem dados para todas as áreas principais;
* se o fluxo principal é demonstrável;
* se não existem erros 500;
* se não existem assets 404;
* se o Codex corrigiu erros encontrados nos testes;
* se a documentação da demonstração está clara;
* se o front-end final está coerente e apresentável.

Executa:

```bash
mvn test -Dtest=DemoDataTest
```

Se falhar:

* identifica a causa;
* podes corrigir erro pequeno;
* para erro grande, pede autorização.

Produz relatório final da demonstração.

---

## 19.5 Testes manuais da demonstração

1. Recriar BD.
2. Executar `data-demo.sql`.
3. Executar `DemoDataTest`.
4. Iniciar Tomcat.
5. Login admin.
6. Mostrar dashboard.
7. Mostrar organização.
8. Mostrar curso.
9. Mostrar disciplina.
10. Mostrar turma.
11. Mostrar conteúdo PDF.
12. Mostrar aula.
13. Login aluno.
14. Realizar avaliação.
15. Ver resultado.
16. Ver horário.
17. Ver pauta.
18. Ver certificado.
19. Enviar mensagem.
20. Login admin.
21. Mostrar auditoria e painéis.

---

## 19.6 Commit final

```bash
git add .
git commit -m "Prepara dados de demonstracao e versao final"
```

---

# Checklist antes de cada commit

Antes de cada commit, confirmar:

* `mvn clean package` passa;
* testes automáticos passam;
* o Codex executou os testes relevantes;
* o Codex corrigiu erros encontrados nos testes;
* os testes foram repetidos depois das correções;
* existe documentação de como executar os testes;
* existe passo a passo manual;
* não há imports quebrados;
* não há credenciais no código;
* não há SQL nas JSP;
* não há regras de negócio nas JSP;
* não há lógica pesada nos Servlets;
* Services contêm regras de negócio;
* DAOs usam JDBC;
* DAOs usam PreparedStatement;
* DAOs usam try-with-resources;
* mensagens de erro são claras;
* páginas EduAll funcionam;
* o template EduAll foi analisado por completo nas fases com front-end;
* o front-end não ficou limitado a uma página específica;
* todas as páginas necessárias foram alteradas;
* existe documentação das páginas EduAll analisadas e alteradas;
* a IA teve liberdade no back-end e criou todas as classes necessárias, não só as listadas;
* CSS carrega;
* JS carrega;
* imagens carregam;
* não há erros 404 em assets;
* não há erros 500 nos fluxos principais;
* permissões foram testadas;
* dados inválidos foram testados;
* restrições aplicacionais aplicáveis foram testadas e as não aplicáveis em SQL ficaram registadas;
* operações críticas geram registo de auditoria;
* front-end foi testado depois do back-end;
* Claude comparou a implementação com o planeamento;
* Claude não se limitou a rever prompts;
* Claude verificou se o front-end analisou todo o template;
* erros grandes foram explicados antes de serem corrigidos;
* commit tem mensagem clara.
