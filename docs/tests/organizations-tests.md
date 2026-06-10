# Testes de Organizacoes e Unidades Organicas

Data: 2026-06-10

## Cobertura automatica

- `OrganizationServiceTest`
  - criacao de organizacao ativa com administrador ativo;
  - persistencia do campo opcional de foto da organizacao;
  - rejeicao de organizacao ativa sem administrador;
  - validacao de nome obrigatorio;
  - bloqueio de atualizacao sem atribuicao contextual;
  - atribuicao inicial de administrador com permissao `MANAGE_USERS`;
  - bloqueio de apagar organizacao com dependencias de dominio;
  - bloqueio de operacoes apos arquivo;
  - auditoria de criacao.
  - isolamento dos testes com `ResourceLock("gape-db")`.

- `OrganicUnitServiceTest`
  - criacao de unidade organica em organizacao administrada;
  - codigo da unidade gerado pela aplicacao a partir do tipo selecionado;
  - organizacao obrigatoria;
  - pai obrigatoriamente na mesma organizacao;
  - bloqueio de auto-subordinacao;
  - prevencao de ciclos por navegacao da cadeia de pais;
  - bloqueio de apagar unidade organica com dependencias de dominio;
  - rejeicao de utilizador sem perfil/permissao;
  - bloqueio de operacoes apos arquivo.
  - isolamento dos testes com `ResourceLock("gape-db")`.

- `UserServiceTest`
  - bloqueio de remocao/troca do ultimo administrador ativo de uma organizacao ativa;
  - permissao para trocar o perfil de administrador quando ja existe outro administrador ativo associado;
  - regressao do grant base de dashboard ao mudar de perfil.

## Execucao prevista

```bash
mvn test -Dtest=OrganizationServiceTest
mvn test -Dtest=OrganicUnitServiceTest
mvn test -Dtest=UserServiceTest
```

## Execucao de 2026-06-10

- `mvn test -Dtest=OrganizationServiceTest`: sucesso, 7 testes.
- `mvn test -Dtest=OrganicUnitServiceTest`: sucesso, 9 testes.
- `mvn test -Dtest=UserServiceTest`: sucesso, 13 testes.
- `mvn test -Dtest=DatabaseRestrictionCoverageTest`: sucesso, 2 testes.
- `mvn test -Dtest=TemplateStructureTest`: sucesso, 18 testes.
- `mvn test`: sucesso, 160 testes.
- `mvn package -DskipTests`: sucesso, `target/gape.war` gerado.
