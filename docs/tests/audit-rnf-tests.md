# Testes de auditoria e requisitos não funcionais

Este documento cobre a fase de auditoria por âmbito, imutabilidade do registo
e proteção operacional de dados sensíveis. Os testes isolados usam a seed de
teste e uma transação com rollback; não dependem de dados manuais persistentes.

## Testes automatizados

| Classe | Cobertura principal |
| --- | --- |
| `ActivityLogQueryServiceTest` | Campos obrigatórios, correspondência sessão-utilizador, snapshot de âmbito e consultas de administrador, coordenador, formador e aluno. |
| `AuditCoverageTest` | Operação canónica de auditoria, inventário dos nove domínios críticos, gatilhos que impedem `UPDATE`/`DELETE` de `activity_log` e `activity_log_scope`, e documentação HTTPS/cifra/backups. |
| `SensitiveDataCipherTest` | AES-256-GCM, envelope `gape:v1:`, nonce aleatório, AAD, adulteração, chave errada, fingerprint HMAC e configuração fail-closed. |

Execute-os de forma serial, pois a suite integra uma base MySQL comum:

```bash
mvn test -Dtest=ActivityLogQueryServiceTest
mvn test -Dtest=AuditCoverageTest
mvn test -Dtest=SensitiveDataCipherTest
```

## Critérios de acesso e auditoria

- Administrador consulta a sua história e apenas os registos no âmbito das
  organizações ativamente geridas.
- Coordenador consulta a sua história e apenas disciplinas coordenadas.
- Formador consulta a sua história e apenas turmas lecionadas.
- Aluno consulta apenas a sua própria história; filtros exatos nunca ampliam
  esse âmbito.
- Cada novo registo guarda um snapshot em `activity_log_scope`. Os triggers
  rejeitam alterações ou eliminações tanto do log como do snapshot.
- O inventário automatizado confirma hooks de auditoria para dados pessoais,
  permissões, conteúdos, assiduidade, notas, certificados, mensagens,
  justificações e pedidos de eliminação.

## Verificação operacional antes de publicar

1. Em produção, configure `GAPE_REQUIRE_HTTPS=true`, uma origem em
   `GAPE_PUBLIC_HTTPS_ORIGIN` e confirme o redirect `308`, HSTS e cookies
   `Secure`, `HttpOnly` e `SameSite=Lax`, conforme
   [`https.md`](../security/https.md).
2. Configure `GAPE_SENSITIVE_DATA_KEY` com Base64 de 32 bytes e
   `GAPE_SECURITY_ENVIRONMENT=production`. Sem chave, o arranque tem de falhar;
   dados protegidos usam AES-256-GCM e fingerprint HMAC conforme
   [`sensitive-data.md`](../security/sensitive-data.md).
3. Valide o backup sem o executar e, depois, faça um backup real para uma pasta
   protegida. A retenção nunca pode ser inferior a sete dias:

   ```powershell
   .\docs\dev\scripts\backup-database.ps1 `
     -OutputDirectory "D:\GAPE\backups" `
     -RetentionDays 14 `
     -ValidateOnly
   ```

4. Confirme periodicamente o SHA-256 e execute um restauro numa base
   descartável, conforme [`backups.md`](../security/backups.md).
