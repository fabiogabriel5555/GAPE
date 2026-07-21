# HTTPS/TLS em produção

O ambiente local de desenvolvimento pode continuar em HTTP para suportar os
helpers de browser. Essa exceção é explícita: não é uma configuração de
produção. Em produção, staging e demonstrações com dados reais, use TLS no
proxy ou Tomcat e configure a aplicação para recusar HTTP.

## Variáveis da aplicação

```powershell
[Environment]::SetEnvironmentVariable("GAPE_SECURITY_ENVIRONMENT", "production", "Machine")
[Environment]::SetEnvironmentVariable("GAPE_REQUIRE_HTTPS", "true", "Machine")
[Environment]::SetEnvironmentVariable("GAPE_PUBLIC_HTTPS_ORIGIN", "https://gape.example.pt", "Machine")
```

`GAPE_PUBLIC_HTTPS_ORIGIN` tem de ser uma origem HTTPS sem caminho, query ou
fragmento. Quando chega HTTP, `HttpsEnforcementFilter` responde com redirect
permanente `308` para essa origem, sem confiar no cabeçalho `Host` enviado pelo
cliente. Cookies de sessão tornam-se `Secure`, além de `HttpOnly` e
`SameSite=Lax`; HSTS é emitido em respostas seguras.

Se existir um reverse proxy TLS que reencaminha para Tomcat em HTTP interno,
configure adicionalmente:

```powershell
[Environment]::SetEnvironmentVariable("GAPE_TRUST_FORWARDED_HEADERS", "true", "Machine")
```

Faça isto apenas quando o proxy for controlado e remover cabeçalhos
`Forwarded`/`X-Forwarded-Proto` recebidos diretamente da Internet. Caso
contrário, deixe a variável ausente/`false`: a aplicação ignora esses
cabeçalhos para impedir spoofing de transporte seguro.

## Exemplo de conector Tomcat

Copie e adapte
[`tomcat-https-connector.xml.example`](tomcat-https-connector.xml.example)
para `conf/server.xml` no servidor. O ficheiro de keystore e a respetiva senha
ficam fora do projeto e são acessíveis apenas à conta Tomcat. Mantenha também
um conector HTTP apenas para redirecionar para a porta TLS ou faça o redirect
no proxy de entrada.

Com um proxy (Nginx, Apache HTTPD, balanceador), termine TLS no proxy com
TLS 1.2/1.3, certificado válido e redireção HTTP→HTTPS antes de encaminhar ao
Tomcat. O tráfego proxy→Tomcat deve ficar numa rede privada; quando atravessar
uma rede não confiável, use também TLS nesse salto.

## JDBC e Docker

`useSSL=false` aparece apenas nas configurações **explicitamente locais**:
`db.properties` de desenvolvimento, documentação local e Compose de
desenvolvimento numa rede Docker isolada. Não copie esses URLs para produção.
Use MySQL Connector/J com, no mínimo, validação de certificado, por exemplo:

```text
jdbc:mysql://db.example.pt:3306/gape?sslMode=VERIFY_IDENTITY&connectionTimeZone=LOCAL&forceConnectionTimeZoneToSession=false&preserveInstants=false
```

Disponibilize a CA/keystore de confiança ao processo Java conforme a política
do ambiente. Credenciais e URLs de produção são variáveis de ambiente, nunca
ficheiros versionados.

## Verificação antes de publicar

```powershell
curl.exe -I http://gape.example.pt/GAPE/
# Esperado: 308 e Location https://gape.example.pt/GAPE/

curl.exe -I https://gape.example.pt/GAPE/
# Esperado: Strict-Transport-Security, X-Content-Type-Options e CSP
```

Verifique também que o browser recebe `JSESSIONID` com atributos `Secure`,
`HttpOnly` e `SameSite=Lax`, e que nenhuma página autenticada é servida em
HTTP. Só ative HSTS numa origem cuja disponibilidade HTTPS já esteja validada.
