# Testes de Cursos, Disciplinas e Inscricoes

Data: 13-07-2026

## Cobertura automatica

- `CourseServiceTest`
  - criacao de curso valido em organizacao administrada;
  - persistencia e recarregamento da foto WebP do curso;
  - rejeicao de caminhos de foto inseguros;
  - rejeicao de curso sem organizacao;
  - rejeicao de curso com unidade organica de outra organizacao;
  - rejeicao de criacao por utilizador sem permissao;
  - validacao de duracao apenas numerica;
  - desarquivo de curso arquivado;
  - bloqueio de operacoes apos arquivo;
  - bloqueio de apagar curso com dependencias.

- `SubjectServiceTest`
  - criacao de disciplina valida, inclusive sem qualquer associacao a curso;
  - inativacao permitida mesmo com associacoes de curso ativas, preservando-as;
  - reativacao da disciplina inativa no respetivo contexto organizacional;
  - persistencia e recarregamento da foto WebP da disciplina;
  - rejeicao de caminhos de foto inseguros;
  - atribuicao de coordenador apenas depois da criacao, pelo mecanismo contextual da pagina Subject Details;
  - rejeicao de nova atribuicao de coordenador numa disciplina inativa;
  - rejeicao de disciplina sem organizacao;
  - permissao do coordenador para gerir disciplina atribuida;
  - rejeicao de criacao por utilizador sem permissao;
  - bloqueio de apagar disciplina com dependencias.

- `CourseSubjectServiceTest`
  - associacao disciplina-curso valida;
  - rejeicao de associacao duplicada;
  - exigencia de ano e periodo curricular em conjunto;
  - rejeicao de disciplina de outra organizacao;
  - rejeicao de associacao por utilizador sem permissao;
  - rejeicao de associacao nova ou reativada para disciplina inativa;
  - encerramento historico da ultima associacao, deixando a disciplina sem curso ativo;
  - preservacao dos registos historicos ao encerrar uma associacao.

- `EnrollmentServiceTest`
  - inscricao de aluno numa ocorrencia concreta de curso;
  - persistencia do identificador da ocorrencia na inscricao do curso;
  - bloqueio de inscricao ativa sobreposta no tempo;
  - atualizacao e eliminacao da inscricao no curso;
  - desistencias de curso e retirada automatica das inscricoes ativas em turmas desse curso;
  - confirmacao de que o fluxo de inscricao usa apenas ocorrencias de curso e turmas elegiveis;
  - rejeicao de operacao por utilizador sem permissao.

- `UserServiceTest`
  - bloqueio de apagar, inativar ou retirar a permissao do unico administrador ativo com `MANAGE_ALL`.
  - rejeicao de tentativas de gerir coordenacoes de disciplinas pelo formulario de utilizador; essas atribuicoes pertencem exclusivamente a `Subject Details`.

- `ProfilePhotoStorageTest`
  - conversao de imagem de utilizador para WebP;
  - armazenamento de foto de organizacao em `organizations/{id}/profile.webp`;
  - armazenamento de foto de curso em `courses/{id}/profile.webp`;
  - armazenamento de foto de disciplina em `subjects/{id}/profile.webp`;
  - resolucao de diretorio relativo sob o root da aplicacao.

## Execucao prevista

```bash
mvn test -Dtest=CourseServiceTest
mvn test -Dtest=SubjectServiceTest
mvn test -Dtest=CourseSubjectServiceTest
mvn test -Dtest=EnrollmentServiceTest
mvn test -Dtest=ProfilePhotoStorageTest
mvn test -Dtest=UserServiceTest
```

## Execucao de 12-06-2026

- `mvn -q -DskipTests test-compile`: sucesso.
- `mvn test "-Dtest=CourseServiceTest,SubjectServiceTest,CourseSubjectServiceTest,EnrollmentServiceTest,UserServiceTest,ApplicationConstraintTest,DatabaseRestrictionCoverageTest,ValidDataInsertTest"`: sucesso, 65 testes.
