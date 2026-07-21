# Backups diários da base de dados

O script executável é
[`backup-database.ps1`](../dev/scripts/backup-database.ps1). Ele cria um dump
MySQL consistente (`--single-transaction`, rotinas, eventos e triggers), só
publica o ficheiro final depois de `mysqldump` terminar com sucesso, cria um
SHA-256 lateral e aplica retenção conservadora.

O valor por omissão é 14 dias; o parâmetro `-RetentionDays` recusa qualquer
valor inferior a 7 e a limpeza nunca remove as sete cópias completas mais
recentes. Um backup completo é um `.sql` com `.sha256` correspondente.

## Configurar sem guardar segredos no projeto

Defina estas variáveis na conta de serviço do Task Scheduler ou no gestor de
segredos usado pelo servidor:

```powershell
[Environment]::SetEnvironmentVariable("GAPE_BACKUP_DB_NAME", "gape", "Machine")
[Environment]::SetEnvironmentVariable("GAPE_BACKUP_DB_HOST", "db.example.pt", "Machine")
[Environment]::SetEnvironmentVariable("GAPE_BACKUP_DB_PORT", "3306", "Machine")
[Environment]::SetEnvironmentVariable("GAPE_BACKUP_DB_USER", "gape_backup", "Machine")
[Environment]::SetEnvironmentVariable("GAPE_BACKUP_DB_PASSWORD", "<segredo-no-gestor>", "Machine")
[Environment]::SetEnvironmentVariable("GAPE_MYSQLDUMP_PATH", "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqldump.exe", "Machine")
```

O utilizador MySQL de backup deve ter só os privilégios necessários para ler o
schema e executar o dump. A palavra-passe não é aceita como argumento nem
escrita em log; o script só a expõe ao processo `mysqldump` através de
`MYSQL_PWD` durante essa execução. Restrinja ACLs da pasta de backups e use
volume/disco cifrado. Para uma base remota, configure também TLS MySQL segundo
a política de produção.

Valide a configuração sem criar um dump:

```powershell
.\docs\dev\scripts\backup-database.ps1 `
  -OutputDirectory "D:\GAPE\backups" `
  -RetentionDays 14 `
  -ValidateOnly
```

Execute uma cópia real:

```powershell
.\docs\dev\scripts\backup-database.ps1 `
  -OutputDirectory "D:\GAPE\backups" `
  -RetentionDays 14
```

## Agendamento diário no Windows

Crie a tarefa com uma conta de serviço que tenha acesso às variáveis acima,
ao `mysqldump` e à pasta de destino. Execute todos os dias antes da janela de
uso normal, por exemplo às 02:15:

```powershell
$script = "C:\gape\docs\dev\scripts\backup-database.ps1"
$action = New-ScheduledTaskAction `
  -Execute "PowerShell.exe" `
  -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$script`" -OutputDirectory `"D:\GAPE\backups`" -RetentionDays 14"
$trigger = New-ScheduledTaskTrigger -Daily -At 02:15
Register-ScheduledTask -TaskName "GAPE daily database backup" `
  -Action $action -Trigger $trigger -User "DOMAIN\gape-backup" -RunLevel Limited
```

Após criar a tarefa, execute-a manualmente uma vez e confirme no histórico do
Task Scheduler que termina com sucesso. Nunca use `down -v`, limpeza Docker
ou scripts de reset contra o volume que contém backups de produção.

## Verificação e restauro trimestral

1. Confirme a soma antes de restaurar:

   ```powershell
   Get-FileHash "D:\GAPE\backups\gape-AAAAMMDD-HHMMSS.sql" -Algorithm SHA256
   Get-Content "D:\GAPE\backups\gape-AAAAMMDD-HHMMSS.sql.sha256"
   ```

2. Restaure **apenas numa base descartável** com utilizador autorizado. Em
   PowerShell, use o cliente MySQL com o comando `SOURCE` (a redireção `<`
   não é um operador de entrada do PowerShell):

   ```powershell
   mysql --host=localhost --user=gape_restore --password `
     -e "SOURCE D:/GAPE/backups/gape-AAAAMMDD-HHMMSS.sql" gape_restore_test
   ```

3. Valide tabelas, migrations, login numa cópia isolada e uma consulta de
   documento/justificação autorizada. Registe a data, operador, ficheiro e
   resultado no procedimento operacional.

Uma falha de backup ou de teste de restauro é um incidente operacional: não
reduza a retenção, não apague cópias existentes e corrija a causa antes da
próxima janela diária.
