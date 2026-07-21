# Executar o GAPE

## Requisitos

Para compilar o projeto a partir do código-fonte, instale:

- JDK 25;
- Apache Maven 3.9.16, com o comando `mvn` disponível no `PATH`;
- MySQL 8;
- Apache Tomcat 10.1.

O projeto inclui a biblioteca WebP corrigida em
`src/dependencies/maven-repository`. Não apague essa pasta: ela permite que
`mvn package` funcione sem instalar dependências WebP manualmente.

Confirme o Java e o Maven num terminal novo:

~~~powershell
mvn --version
java --version
~~~

## Base de dados local

Defina as credenciais sem editar ficheiros do projeto:

~~~powershell
$env:GAPE_DB_URL = "jdbc:mysql://localhost:3306/gape?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=LOCAL&forceConnectionTimeZoneToSession=false&preserveInstants=false"
$env:GAPE_DB_USER = "root"
$env:GAPE_DB_PASSWORD = "root"
~~~

`GAPE_DB_BOOTSTRAP_MODE=full` recria e semeia a base de dados no arranque.
Use-o apenas numa base de demonstração descartável. Para preservar uma base
existente, defina `GAPE_DB_BOOTSTRAP_MODE=none`.

## Compilar o WAR

Na raiz do projeto, execute:

~~~powershell
mvn package
~~~

O ficheiro a instalar no Tomcat é `target/gape.war`. Também é possível
executar a aplicação com um `gape.war` já compilado; nesse caso, a compilação
e o Maven não são necessários nesse computador.

## Executar no Tomcat

1. Copie `target/gape.war` para a pasta `webapps` do Tomcat 10.1.
2. Inicie o Tomcat.
3. Abra `http://localhost:8080/gape/`.

O Tomcat não precisa de configuração adicional para a biblioteca WebP. A
conta que executa o Tomcat só precisa de permissão para escrever na pasta de
utilizador ou temporária, onde a biblioteca nativa é extraída durante a
conversão de imagens.

## Segurança de produção

Os exemplos HTTP e `useSSL=false` deste guia são exclusivos para desenvolvimento
local. Antes de publicar dados reais, configure HTTPS, a chave de dados
sensíveis e backups diários conforme
[`security/https.md`](security/https.md),
[`security/sensitive-data.md`](security/sensitive-data.md) e
[`security/backups.md`](security/backups.md).
