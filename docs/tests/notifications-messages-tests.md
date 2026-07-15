# Testes de notificacoes, mensagens, foruns e comentarios

## Objetivo

Validar a camada backend de comunicacao transversal dentro da aplicacao: canais, participacao, mensagens, rececao/leitura, agendamento, notificacoes internas e auditoria basica das operacoes relevantes.

## Suites automáticas

Executar:

```bash
mvn test -Dtest=ChannelServiceTest
mvn test -Dtest=MessageServiceTest
mvn test -Dtest=ChannelParticipationServiceTest
mvn test -Dtest=NotificationServiceTest
```

## Dados de seed usados

- Utilizadores: administrador `1`, coordenador `2`, formador `3`, aluno `4`, utilizador inativo `5`.
- Turmas e contexto: turma `50`, bloco `60`, avaliacao `90`, evento de horario `140`.
- Canais existentes: `190` e `191`.
- Mensagem existente: `222`.

## Cobertura

`ChannelServiceTest` valida:

- criacao de canais `MESSAGE`, `FORUM`, `COMMENTS`, `ANNOUNCEMENT` e `SYSTEM`;
- participacao ativa automatica do criador como `OWNER`;
- canal de turma criado por formador da turma;
- rejeicao de canal incoerente entre turma e bloco;
- rejeicao de criacao contextual por aluno sem permissao de moderacao.

`ChannelParticipationServiceTest` valida:

- moderador adiciona participante ativo ao canal;
- segunda participacao ativa no mesmo canal falha;
- aluno ativo sem acesso a turma nao entra no canal contextual;
- utilizador sem moderacao nao adiciona participantes;
- listagem de participantes reflete a participacao persistida.

`MessageServiceTest` valida:

- mensagem humana por participante ativo;
- mensagem gerada pelo sistema sem remetente;
- bloqueio de envio por nao participante;
- bloqueio de spoofing de remetente;
- resposta fora do canal rejeitada;
- resposta a mensagem propria rejeitada;
- mensagem agendada fica pendente ate `processScheduledMessages`;
- mensagem de anexo exige anexo;
- leitura antes da entrega falha;
- mensagem originada por evento de horario respeita canal e destinatarios do evento.

`NotificationServiceTest` valida:

- notificacao cria mensagem de sistema e recibo interno;
- tipo de mensagem nao-sistema e rejeitado em notificacoes;
- notificacao agendada mantem o recibo pendente ate ao processamento interno.

## Resultado da execucao

Em 02-07-2026, as quatro suites acima foram executadas individualmente com sucesso.

## Notas de risco residual

- O processamento de mensagens agendadas continua a ocorrer quando o fluxo de mensagens invoca `processScheduledMessages`; nao existe job/outbox autonomo.
- A visibilidade de listagem/leitura por UI ainda deve ser coberta quando os Servlets/JSP da fase forem integrados.
