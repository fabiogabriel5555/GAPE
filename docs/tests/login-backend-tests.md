# Login Backend Tests

## Ambito
Valida o back-end de autenticacao, logout e sessao da Fase 3.

## Classes cobertas
- `AuthService`
- `SessionService`
- `AuthenticationFilter`
- `SessionManager`
- `PasswordHasher`
- `ActivityLogDAO` / `AuditService`

## Testes criados
- `src/test/java/pt/isel/gape/access/AuthServiceTest.java`
- `src/test/java/pt/isel/gape/access/SessionServiceTest.java`
- `src/test/java/pt/isel/gape/security/AuthenticationFilterTest.java`
- `src/test/java/pt/isel/gape/web/controller/LogoutServletTest.java`

## Cenarios verificados
- login valido cria sessao
- password errada e rejeitada
- email inexistente e rejeitado
- utilizador inativo ou bloqueado nao autentica
- utilizador ativo sem perfil de acesso nao autentica
- `last_activity` e atualizado
- logout fecha a sessao
- logout por `GET` e rejeitado
- logout por `POST` exige token CSRF valido
- sessao recente continua valida segundo a restricao 18
- expiracao por inatividade aos 30 minutos
- fronteira de expiracao validada em 29m59s, 30m00s e 30m01s
- filtro bloqueia acesso sem sessao
- filtro bloqueia pagina privada nao-dashboard sem sessao
- filtro bloqueia acesso com sessao expirada
- filtro permite acesso a todas as areas correspondentes aos perfis que a sessao possui
- login e logout geram registo em `activity_log`
- expiracao gera registo `SESSION_EXPIRED` em `activity_log`

## Restricao aplicacional 18
Aplicada na camada aplicacional:

> A expiracao da sessao deve ser calculada pela aplicacao com base em `Session.last_activity` e na politica de inatividade definida pelo sistema.

Implementacao:
- politica fixa de `30` minutos em `SessionService.INACTIVITY_TIMEOUT`
- calculo em `SessionService.isExpired`
- aplicacao no `AuthenticationFilter`
- cobertura explicita de:
  - sessao recente confirmada valida
  - sessao com `last_activity` antiga confirmada expirada
  - fronteira de 30 minutos confirmada
  - bloqueio no filtro com redirecionamento para `login.jsp?auth=expired`

## Comandos
```bash
mvn test -Dtest=AuthServiceTest
mvn test -Dtest=SessionServiceTest
mvn test -Dtest=AuthenticationFilterTest
mvn test -Dtest=LogoutServletTest
mvn test
mvn package -DskipTests
```

## Ajustes feitos durante a execucao
- `DatabaseTestSupport` foi tornado `public` para poder ser reutilizado pelos novos testes de autenticacao fora do package `transversal`.
- `AuthenticationFilterTest` recebeu `imports` e helpers em falta no fake `HttpSession`.
- Os casos de expiracao passaram a usar timestamps mais antigos para evitar fragilidade de fixture entre JDBC, `DATETIME` e UTC.
- Foi acrescentado teste de fronteira de 30 minutos diretamente sobre a regra de dominio.
- `LogoutServletTest` cobre rejeicao de `GET`, rejeicao de CSRF invalido e logout valido por `POST`.
- O template visual foi ligado aos endpoints reais:
  - `login.jsp` submete para `POST /auth/login`
  - dashboards submetem logout por `POST /auth/logout` com token CSRF

## Resultado final
- `mvn test -Dtest=AuthServiceTest,SessionServiceTest,AuthenticationFilterTest,LogoutServletTest`: passou
- `mvn test`: passou
- `mvn package -DskipTests`: passou
