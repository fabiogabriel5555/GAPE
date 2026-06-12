# Testes de Cursos, Disciplinas e Inscricoes

Data: 2026-06-12

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
  - criacao de disciplina valida;
  - persistencia e recarregamento da foto WebP da disciplina;
  - rejeicao de caminhos de foto inseguros;
  - atribuicao de coordenador via mecanismo contextual da Fase 4;
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
  - bloqueio de remocao/arquivo da ultima associacao da disciplina.

- `EnrollmentServiceTest`
  - inscricao de aluno em curso;
  - inscricao de aluno em disciplina integrada num curso onde esta inscrito;
  - rejeicao de disciplina nao integrada no curso;
  - rejeicao de inscricao em disciplina sem inscricao ativa no curso;
  - rejeicao de inscricao em disciplina fora do periodo completo da inscricao no curso;
  - rejeicao de inscricao ativa na mesma disciplina em cursos diferentes;
  - bloqueio de inscricao ativa sobreposta no tempo;
  - desistencias de curso e retirada automatica das disciplinas ativas do curso;
  - rejeicao de operacao por utilizador sem permissao.

- `UserServiceTest`
  - bloqueio de apagar, inativar ou retirar a permissao do unico administrador ativo com `MANAGE_ALL`.

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

## Execucao de 2026-06-12

- `mvn -q -DskipTests test-compile`: sucesso.
- `mvn test "-Dtest=CourseServiceTest,SubjectServiceTest,CourseSubjectServiceTest,EnrollmentServiceTest,UserServiceTest,ApplicationConstraintTest,DatabaseRestrictionCoverageTest,ValidDataInsertTest"`: sucesso, 65 testes.
