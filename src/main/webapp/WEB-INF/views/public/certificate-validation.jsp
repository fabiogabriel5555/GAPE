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
<section class="bg-main-25 min-vh-100 d-flex align-items-center py-80">
    <div class="container container--lg">
        <div class="bg-white rounded-10 px-32 py-32 border border-neutral-30 mx-auto" style="max-width: 720px;">
            <div class="text-center mb-28">
                <a href="${pageContext.request.contextPath}/index.jsp" class="d-inline-flex mb-20">
                    <img src="${pageContext.request.contextPath}/assets/images/logo/logo.svg" alt="GAPE" style="max-height: 44px;">
                </a>
                <h1 class="text-28 fw-semibold text-neutral-800 mb-8">Certificate Validation</h1>
                <p class="text-14 text-neutral-500 mb-0">Check the public validation code printed on a certificate.</p>
            </div>

            <form action="${pageContext.request.contextPath}/certificates/validate" method="get" class="d-flex gap-12 flex-wrap mb-24">
                <label for="certificate-validation-code" class="visually-hidden">Validation code</label>
                <input id="certificate-validation-code" type="text" name="code" value="<c:out value='${validationCode}'/>" maxlength="80" class="form-control px-16 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8 flex-grow-1" style="min-width: 220px;" placeholder="Validation code" required>
                <button type="submit" class="bg-main-600 px-24 py-12 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03 border-0 d-inline-flex align-items-center">
                    <i class="ph ph-magnifying-glass me-8"></i>Validate
                </button>
            </form>

            <c:if test="${not empty validation}">
                <c:choose>
                    <c:when test="${validation.valid}">
                        <div class="bg-success-50 border border-success-600 rounded-10 px-20 py-20">
                            <div class="d-flex align-items-start gap-14">
                                <span class="w-44 h-44 rounded-circle bg-success-600 text-white d-inline-flex align-items-center justify-content-center text-22 flex-shrink-0">
                                    <i class="ph ph-check"></i>
                                </span>
                                <div>
                                    <h2 class="text-18 fw-semibold text-neutral-800 mb-8">Valid certificate</h2>
                                    <p class="text-14 text-neutral-600 mb-10"><c:out value="${validation.title}"/></p>
                                    <div class="d-flex gap-8 flex-wrap">
                                        <span class="bg-white px-12 py-7 rounded-pill text-13 text-neutral-600">Type: <c:out value="${validation.typeLabel}"/></span>
                                        <span class="bg-white px-12 py-7 rounded-pill text-13 text-neutral-600">Course: <c:out value="${validation.courseLabel}"/></span>
                                        <span class="bg-white px-12 py-7 rounded-pill text-13 text-neutral-600">Published: <c:out value="${validation.issuedAt}"/></span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="bg-danger-50 border border-danger-600 rounded-10 px-20 py-20">
                            <div class="d-flex align-items-start gap-14">
                                <span class="w-44 h-44 rounded-circle bg-danger-600 text-white d-inline-flex align-items-center justify-content-center text-22 flex-shrink-0">
                                    <i class="ph ph-x"></i>
                                </span>
                                <div>
                                    <h2 class="text-18 fw-semibold text-neutral-800 mb-8">Invalid certificate</h2>
                                    <p class="text-14 text-neutral-600 mb-0">The code is not associated with a published certificate.</p>
                                </div>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
            </c:if>
        </div>
    </div>
</section>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
