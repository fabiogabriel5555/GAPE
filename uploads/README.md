# GAPE Uploads

Esta pasta guarda apenas a estrutura local de uploads. Os ficheiros reais carregados por utilizadores nao devem ser versionados.

Estrutura:

- `users/` - fotografias reais de perfil dos utilizadores.
- `contents/` - conteudos pedagogicos carregados na plataforma, como PDF, imagem, audio, video, apresentacao, SCORM ou outros recursos.
- `messages/` - anexos associados a mensagens.
- `justifications/` - anexos de justificacoes de faltas.
- `tmp/` - area temporaria para uploads em processamento.
- `quarantine/` - area para ficheiros rejeitados ou pendentes de validacao de seguranca.

Em producao, a localizacao deve ser configuravel e pode apontar para uma pasta externa, por exemplo `C:\gape\uploads` ou `/var/gape/uploads`.
