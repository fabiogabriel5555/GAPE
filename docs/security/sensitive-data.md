# Proteção de dados sensíveis em repouso

## Configuração obrigatória fora de desenvolvimento

O serviço usa uma chave mestra de exatamente 32 bytes, codificada em Base64,
fornecida **apenas** pela variável de ambiente `GAPE_SENSITIVE_DATA_KEY`. A
chave não pertence ao repositório, ao WAR, a `db.properties`, a imagens Docker
ou a argumentos da JVM.

```powershell
$key = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($key) } finally { $rng.Dispose() }
[Environment]::SetEnvironmentVariable(
  "GAPE_SENSITIVE_DATA_KEY",
  [Convert]::ToBase64String($key),
  "Machine"
)
[Environment]::SetEnvironmentVariable("GAPE_SECURITY_ENVIRONMENT", "production", "Machine")
```

Reinicie o serviço Tomcat depois de configurar as variáveis. Em produção,
teste ou staging, a ausência ou o formato inválido da chave interrompe o
arranque antes de a aplicação atender pedidos. O único modo sem chave é o
modo explícito `development` (o valor por omissão), destinado a base de dados
descartável e aos testes existentes; não o use para dados reais.

Guarde a chave num gestor de segredos com acesso limitado à conta de execução
do Tomcat e uma cópia de recuperação protegida. A perda da chave torna os
valores cifrados irrecuperáveis. A rotação requer uma operação planeada de
reencriptação com a chave antiga e nova; não substitua simplesmente a variável
numa base de dados já cifrada.

## Mecanismo

`SensitiveDataCipher` aplica AES-256-GCM. Cada valor recebe um nonce aleatório
de 96 bits e um envelope versionado `gape:v1:`; o nome lógico da coluna é AAD,
por isso um valor não pode ser movido entre colunas sem falhar a autenticação.
Não se reutiliza o nonce. Para unicidade de documentos, o sistema grava também
um HMAC-SHA-256 separado, com subchave derivada da chave mestra; não é um hash
simples vulnerável a consultas por dicionário.

As colunas de texto cifrado são maiores que os respetivos limites de entrada:
incluem o pior caso UTF-8 e o envelope AES-GCM/Base64. Os limites funcionais
mantêm-se em 300 caracteres para motivos e 500 para notas de decisão.

As palavras-passe não são cifradas: mantêm PBKDF2 com salt individual. Os
tokens de sessão não precisam ser recuperados e passam a ser guardados apenas
como `sha256:<digest>`; o token aleatório do navegador nunca é recuperado da
base de dados.

No arranque, depois de bootstrap/migrations, `SensitiveDataMigrationService`
converte valores legados em claro de forma idempotente e preenche o fingerprint
de documento. O serviço valida envelopes já existentes e falha em caso de
chave errada ou adulteração, em vez de expor conteúdo ilegível.

## Inventário e classificação

| Dado | Classificação | Proteção desta fase |
| --- | --- | --- |
| `user_account.document_number` | Identificador oficial de alto risco | AES-256-GCM + HMAC de unicidade |
| `user_session.token` | Credencial bearer de sessão | SHA-256 unidirecional; comparação em tempo constante |
| `deletion_request.reason` | Motivo potencialmente pessoal de eliminação | AES-256-GCM |
| `absence_justification.reason` | Pode revelar saúde/situação pessoal | AES-256-GCM |
| `absence_justification.decision_notes` | Nota potencialmente pessoal | AES-256-GCM |
| `credential_hash` / `credential_salt` | Verificador de palavra-passe, não dado recuperável | PBKDF2 com salt; não cifrar novamente |
| `activity_log.source_ip` | Dado pessoal técnico | Acesso administrativo/auditável; não alterado nesta fase porque o serviço de consulta/auditoria está isolado |
| `message.title` / `message.body` | Conteúdo potencialmente confidencial, mas não invariavelmente dado sensível | Avaliado: permanece protegido por âmbito/autorização, auditoria e TLS. A cifra de campo exige uma migração específica de mensagens e dos previews em tempo real; não é alegada como concluída nesta fase. |

Nomes, emails e metadados académicos continuam sujeitos a autorização por
âmbito e à política de retenção. Não são indiscriminadamente cifrados porque
são chaves funcionais de autenticação, pesquisa e relacionamentos; uma decisão
de os cifrar exige índices cegos e uma revisão própria para não enfraquecer
unicidade, login e relatórios.

## Verificação operacional

1. Faça primeiro um backup testado.
2. Configure a chave e `GAPE_SECURITY_ENVIRONMENT=production`.
3. Arranque uma única instância; o log indica apenas as quantidades migradas,
   nunca valores sensíveis.
4. Confirme que `document_number`, razões e notas na base começam por
   `gape:v1:` e que tokens começam por `sha256:`.
5. Abra uma conta e uma justificação autorizadas para confirmar a decifragem;
   tente alterar um carácter do envelope numa cópia de teste e confirme que a
   leitura falha autenticadamente.

Não faça a inspeção do passo 4 numa consola, captura de ecrã ou log partilhado
com pessoas sem autorização para dados pessoais.
