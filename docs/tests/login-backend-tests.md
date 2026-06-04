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

## Cenarios verificados
- login valido cria sessao
- password errada e rejeitada
- email inexistente e rejeitado
- utilizador inativo ou bloqueado nao autentica
- `last_activity` e atualizado
- logout fecha a sessao
- sessao recente continua valida segundo a restricao 18
- expiracao por inatividade aos 30 minutos
- filtro bloqueia acesso sem sessao
- filtro bloqueia acesso com sessao expirada
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
  - bloqueio no filtro com redirecionamento para `login.jsp?auth=expired`

## Comandos
```bash
mvn test -Dtest=AuthServiceTest
mvn test -Dtest=SessionServiceTest
mvn test -Dtest=AuthenticationFilterTest
mvn test
mvn package -DskipTests
```

## Ajustes feitos durante a execucao
- `DatabaseTestSupport` foi tornado `public` para poder ser reutilizado pelos novos testes de autenticacao fora do package `transversal`.
- `AuthenticationFilterTest` recebeu `imports` e helpers em falta no fake `HttpSession`.
- Os casos de expiracao passaram a usar timestamps mais antigos para evitar fragilidade de fixture entre JDBC, `DATETIME` e UTC.
- O template visual foi ligado aos endpoints reais:
  - `login.jsp` submete para `POST /auth/login`
  - dashboards usam `/auth/logout`

## Resultado final
- `mvn test -Dtest=AuthServiceTest,SessionServiceTest,AuthenticationFilterTest`: passou
- `mvn test`: passou
- `mvn package -DskipTests`: passou
