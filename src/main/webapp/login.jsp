<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="pt">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GAPE - Login</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>
    <%@ include file="/WEB-INF/fragments/header.jsp" %>

    <main class="content content--centered">
        <section class="card login-card">
            <h1>Entrar no GAPE</h1>

            <!-- TODO Fase 1+: este formulario ainda nao autentica.
                 Ligar a um Servlet em web/controller -> security/auth. -->
            <form class="form" action="#" method="post">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" placeholder="utilizador@exemplo.pt" autocomplete="username">

                <label for="password">Palavra-passe</label>
                <input type="password" id="password" name="password" placeholder="********" autocomplete="current-password">

                <button class="btn" type="submit">Iniciar sessao</button>
            </form>

            <p><a href="${pageContext.request.contextPath}/index.jsp">Voltar ao inicio</a></p>
        </section>
    </main>

    <%@ include file="/WEB-INF/fragments/footer.jsp" %>
    <script src="${pageContext.request.contextPath}/assets/js/app.js"></script>
</body>
</html>
