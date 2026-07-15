<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Certificate Validation</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
</head>
<body>
<div class="preloader">
    <img src="${pageContext.request.contextPath}/assets/images/icons/preloader.gif" alt="">
</div>
<main class="bg-main-25 min-vh-100 d-flex align-items-center py-80">
    <div class="container container--lg">
        <section class="bg-white rounded-8 px-32 py-32 border border-neutral-30 mx-auto" style="max-width: 720px;">
            <div class="text-center mb-28">
                <a href="${pageContext.request.contextPath}/index.jsp" class="d-inline-flex mb-20">
                    <img src="${pageContext.request.contextPath}/assets/images/logo/logo.svg" alt="GAPE" style="max-height: 44px;">
                </a>
                <h1 class="text-28 fw-semibold text-neutral-800 mb-8">Certificate Validation</h1>
                <p class="text-14 text-neutral-500 mb-0">Enter the code printed on the certificate.</p>
            </div>

            <form action="${pageContext.request.contextPath}/certificates/validate" method="get" class="d-flex gap-12 flex-wrap mb-24">
                <label for="certificate-validation-code" class="visually-hidden">Validation code</label>
                <input id="certificate-validation-code" type="text" name="code" value="<c:out value='${validationCode}'/>" maxlength="80" autocomplete="off" class="form-control px-16 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8 flex-grow-1" style="min-width: 220px;" placeholder="Validation code" required>
                <button type="submit" class="bg-main-600 px-24 py-12 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03 border-0 d-inline-flex align-items-center">
                    <i class="ph ph-magnifying-glass me-8" aria-hidden="true"></i>Validate
                </button>
            </form>

            <c:if test="${not empty validation}">
                <c:choose>
                    <c:when test="${validation.valid}">
                        <div class="bg-success-50 border border-success-600 rounded-8 px-20 py-20" role="status">
                            <h2 class="text-18 fw-semibold text-neutral-800 mb-8">Valid certificate</h2>
                            <p class="text-14 text-neutral-600 mb-12"><c:out value="${validation.title}"/></p>
                            <dl class="row gy-2 text-13 mb-0">
                                <dt class="col-sm-4 text-neutral-500">Certificate</dt><dd class="col-sm-8 text-neutral-700 mb-0">#<c:out value="${validation.certificateId}"/></dd>
                                <dt class="col-sm-4 text-neutral-500">Type</dt><dd class="col-sm-8 text-neutral-700 mb-0"><c:out value="${validation.typeLabel}"/></dd>
                                <dt class="col-sm-4 text-neutral-500">Course</dt><dd class="col-sm-8 text-neutral-700 mb-0"><c:out value="${validation.courseLabel}"/></dd>
                                <dt class="col-sm-4 text-neutral-500">Occurrence</dt><dd class="col-sm-8 text-neutral-700 mb-0"><c:out value="${validation.occurrenceLabel}"/></dd>
                                <dt class="col-sm-4 text-neutral-500">Published</dt><dd class="col-sm-8 text-neutral-700 mb-0" data-gape-datetime-display><c:out value="${validation.issuedAt}"/></dd>
                            </dl>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="bg-danger-50 border border-danger-600 rounded-8 px-20 py-20" role="alert">
                            <h2 class="text-18 fw-semibold text-neutral-800 mb-8">Invalid certificate</h2>
                            <p class="text-14 text-neutral-600 mb-0">The code is not associated with an issued certificate.</p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </c:if>
        </section>
    </div>
</main>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
