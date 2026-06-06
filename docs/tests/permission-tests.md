# Testes de Permissoes e Atribuicao Contextual

## Ambito

Este ficheiro documenta os testes automaticos introduzidos para a Fase 4, focados no back-end de permissoes, grants, atribuicoes contextuais e bloqueio de acesso direto por URL.

## Testes Automaticos

### PermissionServiceTest

Valida:

* permissao ativa pode ser carregada;
* permissao inativa nao pode ser atribuida;
* permissao global critica nao pode ser atribuida a perfil nao administrador;
* administrador com `MANAGE_PERMISSIONS` consegue atribuir permissao ativa;
* falhas de grant geram registo em `activity_log`.

### RoleAssignmentServiceTest

Valida:

* formador ativo pode ser associado a turma ativa;
* utilizador inativo nao pode receber associacao contextual ativa;
* associacao expirada nao autoriza o contexto.

### AuthorizationFilterTest

Valida:

* pagina privada sem sessao redireciona para login;
* administrador com grant exigido acede a pagina administrativa;
* aluno autenticado nao acede diretamente a URL administrativa;
* perfil sem grant exigido recebe `403`;
* negacao de autorizacao gera registo de auditoria.

## Comandos

```bash
mvn test -Dtest=PermissionServiceTest
mvn test -Dtest=RoleAssignmentServiceTest
mvn test -Dtest=AuthorizationFilterTest
```

## Validacao Manual Recomendada

1. Entrar como administrador e abrir paginas administrativas.
2. Entrar como aluno e tentar abrir manualmente uma URL `/admin/`.
3. Confirmar resposta `403`.
4. Atribuir formador a turma e confirmar que a verificacao contextual fica ativa.
5. Repetir com coordenador/disciplina e administrador/organizacao.

## Notas

As atribuicoes concretas a organizacoes, disciplinas e turmas ficam prontas nesta fase para serem reutilizadas nas fases funcionais seguintes. Os menus dinamicos e a pagina visual de acesso negado pertencem ao prompt de front-end da mesma fase.
