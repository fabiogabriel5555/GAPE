# Testes de Utilizadores, Eliminacao e Auditoria

## Ambito

Este ficheiro documenta os testes automaticos atuais da Fase 5 para dados pessoais,
perfis, pedidos de eliminacao e registo de auditoria.

Os testes seguem o estilo existente do projeto:

* JUnit 5;
* `DatabaseTestSupport`;
* validacao por Service e confirmacao por consultas JDBC.

## Suites

### UserServiceTest

Valida:

* criacao valida de utilizador por administrador com `MANAGE_USERS`;
* rejeicao de email duplicado;
* rejeicao de documento duplicado;
* rejeicao de campos obrigatorios em falta;
* exigencia conjunta de tipo e numero de documento;
* bloqueio e desbloqueio de utilizador;
* edicao do proprio perfil pessoal;
* negacao de leitura de dados pessoais por perfil nao autorizado;
* criacao da permissao base de dashboard ao substituir perfis;
* auditoria de sucesso e falha nas operacoes criticas.

### DeletionRequestServiceTest

Valida:

* submissao de pedido de eliminacao;
* rejeicao de processamento por nao administrador;
* rejeicao de estado final sem data de processamento;
* rejeicao de data de processamento anterior a submissao;
* processamento valido por administrador com `PROCESS_DELETION_REQUESTS`;
* auditoria de submissao e processamento.

### ActivityLogServiceTest

Valida:

* registo de evento critico com campos minimos;
* consulta de logs por perfil autorizado;
* coerencia entre sessao e utilizador no `activity_log`;
* identificadores de auditoria sem email, documento ou motivo textual.

## Comandos

```bash
mvn test -Dtest=UserServiceTest
mvn test -Dtest=DeletionRequestServiceTest
mvn test -Dtest=ActivityLogServiceTest
```

## Resultado Atual

Os tres comandos acima passam quando executados em serie. A execucao em paralelo de
testes que reinicializam a mesma base de dados pode produzir conflitos de schema, pelo
que a validacao recomendada para estas suites e serial.
