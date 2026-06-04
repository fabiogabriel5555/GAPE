# Testes de Validação XML/XSD (Fase 1)

Teste: `src/test/java/pt/isel/gape/transversal/XmlValidationTest.java`
Esquema central: `src/main/resources/config/xsd/gape-config.xsd`

## Como executar

```bash
mvn test -Dtest=XmlValidationTest
```

Não precisa de base de dados (valida ficheiros, não usa JDBC).

## O que é validado

- **schemasCompile** — `gape-config.xsd` e os XSD de calendário compilam como esquemas válidos.
- **allConfigXmlFilesValidateAgainstTheirSchemas** — todos os XML em `config/xml/` (e `config/xml/transversal/`) validam contra o XSD correspondente (`gape-config.xsd`, ou o XSD de calendário para os ficheiros de calendário).
- **supportXslFilesAreWellFormedXml** — os XSL em `config/xsl/` são XML bem formado (com `disallow-doctype-decl` para evitar XXE).
- **xmlWithInvalidStructureFailsValidation** — XML com raiz/estrutura errada é rejeitado.
- **xmlWithDuplicateControlledValueFailsValidation** — `@code` duplicado num catálogo é rejeitado (via `xs:unique`).
- **xmlWithoutRequiredFieldsFailsValidation** — falta de `@code` ou de `<label>` é rejeitada.
- **xmlWithValueOutsideAllowedPatternFailsValidation** — valor fora da `xs:enumeration`, ou `label` demasiado curto, é rejeitado.
- **invalidAcademicCalendarXmlFailsValidation** / **invalidPlurianualCalendarXmlFailsValidation** — calendários com ordem, atributos ou datas inválidas são rejeitados.

## Cobertura de catálogos

24 catálogos em `config/xml/` + 2 exemplos de calendário em `config/xml/transversal/`. Cada catálogo tem o número exato de entradas fixado no XSD e unicidade de `@code`.

## Resultado da execução

`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0` — verde.

## Notas / correções desta revisão

- Acrescentados os catálogos `attempt-states.xml`, `certificate-types.xml` e `message-types.xml`, com os respetivos tipos e raízes em `gape-config.xsd`. Reexecutado o teste: continua verde, agora a validar também estes três ficheiros.
