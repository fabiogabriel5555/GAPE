<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="pt">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GAPE - Inicio</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>
    <%@ include file="/WEB-INF/fragments/header.jsp" %>

    <div class="layout">
        <%@ include file="/WEB-INF/fragments/sidebar.jsp" %>

        <main class="content">
            <h1>Bem-vindo ao GAPE</h1>
            <p>Plataforma de Gestao Academica e Pedagogica.</p>
            <p>Estrutura inicial do projeto (Fase 0) - ainda sem funcionalidades.</p>
            <p><a class="btn" href="${pageContext.request.contextPath}/login.jsp">Entrar</a></p>
        </main>
    </div>

    <%@ include file="/WEB-INF/fragments/footer.jsp" %>
    <script src="${pageContext.request.contextPath}/assets/js/app.js"></script>
</body>
</html>
