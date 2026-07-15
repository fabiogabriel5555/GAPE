<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("course") == null) {
        response.sendRedirect(request.getContextPath() + "/courses");
        return;
    }
%>
<c:set var="coursePhotoUrl" value=""/>
<c:if test="${course.hasPhoto}">
    <c:set var="coursePhotoUrl" value="${pageContext.request.contextPath}/media/${course.photo}?v=${mediaCacheVersion}"/>
</c:if>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Course Detail</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
</head>
<body>
<div class="preloader">
    <img src="${pageContext.request.contextPath}/assets/images/icons/preloader.gif" alt="">
</div>
<div class="overlay"></div>
<div class="side-overlay"></div>

<header class="header bg-white border-bottom border-neutral-30">
    <div class="container container--xl">
        <nav class="header-inner flex-between gap-8">
            <a href="${pageContext.request.contextPath}/index.jsp" class="link">
                <img src="${pageContext.request.contextPath}/assets/images/logo/logo.svg" alt="GAPE">
            </a>
            <div class="header-menu d-lg-block d-none">
                <ul class="nav-menu flex-align">
                    <li class="nav-menu__item"><a href="${pageContext.request.contextPath}/index.jsp" class="nav-menu__link">Home</a></li>
                    <li class="nav-menu__item activePage"><a href="${pageContext.request.contextPath}/courses" class="nav-menu__link">Courses</a></li>
                    <li class="nav-menu__item"><a href="${pageContext.request.contextPath}/contact.jsp" class="nav-menu__link">Contact</a></li>
                </ul>
            </div>
            <a href="${pageContext.request.contextPath}/dashboard" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Dashboard</a>
        </nav>
    </div>
</header>

<main class="bg-main-25 py-80">
    <div class="container">
        <%@ include file="/WEB-INF/fragments/flash-messages.jspf" %>

        <div class="row gy-4 align-items-start">
            <div class="col-xl-8">
                <div class="bg-white rounded-16 overflow-hidden box-shadow-md">
                    <c:choose>
                        <c:when test="${course.hasPhoto}">
                            <img src="${coursePhotoUrl}" alt="" class="w-100" style="max-height: 360px; object-fit: cover;" onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                            <div class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--course-hero d-none" aria-label="No course photo">
                                <i class="ph ph-image"></i>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--course-hero" aria-label="No course photo">
                                <i class="ph ph-image"></i>
                            </div>
                        </c:otherwise>
                    </c:choose>
                    <div class="p-32">
                        <span class="px-12 py-6 text-success-600 fw-medium text-14 bg-success-50 border-success-100 border rounded-10 mb-16 d-inline-block">
                            <c:out value="${course.typeLabel}"/>
                        </span>
                        <h1 class="text-40 fw-semibold text-neutral-800 mb-12"><c:out value="${course.name}"/></h1>
                        <p class="text-16 text-neutral-500 mb-24"><c:out value="${course.description}"/></p>
                        <div class="row gy-4">
                            <div class="col-md-3">
                                <span class="text-13 text-neutral-500 d-block">Organization</span>
                                <span class="text-15 fw-medium text-neutral-700"><c:out value="${course.organizationName}"/></span>
                            </div>
                            <div class="col-md-3">
                                <span class="text-13 text-neutral-500 d-block">Organic Unit</span>
                                <span class="text-15 fw-medium text-neutral-700"><c:out value="${course.organicUnitLabel}"/></span>
                            </div>
                            <div class="col-md-3">
                                <span class="text-13 text-neutral-500 d-block">ECTS</span>
                                <span class="text-15 fw-medium text-neutral-700"><c:out value="${course.ectsLabel}"/></span>
                            </div>
                            <div class="col-md-3">
                                <span class="text-13 text-neutral-500 d-block">Duration</span>
                                <span class="text-15 fw-medium text-neutral-700"><c:out value="${course.durationLabel}"/></span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-16 px-32 py-32 mt-24">
                    <h2 class="text-24 fw-semibold text-neutral-800 mb-20">Curriculum</h2>
                    <div class="d-flex flex-column gap-12">
                        <c:forEach var="association" items="${courseSubjects}">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18">
                                <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap">
                                    <div>
                                        <h3 class="text-16 fw-semibold text-neutral-700 mb-4"><c:out value="${association.subjectName}"/></h3>
                                        <span class="text-13 text-neutral-500"><c:out value="${association.curricularPositionLabel}"/> | <c:out value="${association.mandatoryLabel}"/></span>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                        <c:if test="${empty courseSubjects}">
                            <div class="border border-neutral-30 rounded-12 px-20 py-24 text-center text-14 text-neutral-500">No active subjects are associated with this course.</div>
                        </c:if>
                    </div>
                </div>
            </div>

            <div class="col-xl-4">
                <div class="bg-white rounded-16 px-24 py-24 box-shadow-md">
                    <h2 class="text-20 fw-semibold text-neutral-800 mb-16">Enrollment</h2>
                    <div class="d-flex align-items-center justify-content-between gap-12 mb-20">
                        <span class="text-14 text-neutral-500">Current state</span>
                        <span class="${course.enrollmentBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                            <c:out value="${course.enrollmentStateLabel}"/>
                        </span>
                    </div>
                    <c:choose>
                        <c:when test="${canUseStudentActions and course.activeEnrollment}">
                            <form action="${pageContext.request.contextPath}/student/enrollments/courses/${course.id}/withdraw" method="post" class="m-0">
                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                <input type="hidden" name="returnTo" value="${returnTo}">
                                <button type="submit" class="border-main-600 border w-100 py-12 rounded-12 fw-semibold text-main-600 hover-bg-main-50 transition-03">Leave Course</button>
                            </form>
                        </c:when>
                        <c:when test="${canUseStudentActions}">
                            <form action="${pageContext.request.contextPath}/student/enrollments/courses/${course.id}" method="post" class="m-0">
                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                <input type="hidden" name="returnTo" value="${returnTo}">
                                <button type="submit" class="bg-main-600 w-100 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Enroll in Course</button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <a href="${pageContext.request.contextPath}/login.jsp" class="bg-main-600 w-100 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03 text-center d-inline-block">Sign in to Enroll</a>
                        </c:otherwise>
                    </c:choose>
                    <a href="${pageContext.request.contextPath}/courses" class="border-main-600 border w-100 py-12 rounded-12 fw-semibold text-main-600 hover-bg-main-50 transition-03 text-center d-inline-block mt-12">Back to Catalog</a>
                </div>
            </div>
        </div>
    </div>
</main>

<footer class="footer bg-neutral-900 py-32">
    <div class="container">
        <!-- bottom Footer -->
        <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap">
            <p class="text-white text-16 fw-normal mb-0">Copyright &copy; 2026 <span class="text-main-600">GAPE</span> All Rights Reserved.</p>
            <a href="#" class="text-white text-16 fw-normal hover-text-warning-600">Terms & Conditions</a>
        </div>
    </div>
</footer>

<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
