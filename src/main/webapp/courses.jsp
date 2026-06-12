<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Courses</title>
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

        <div class="d-flex align-items-end justify-content-between gap-16 flex-wrap mb-32">
            <div>
                <span class="text-main-600 fw-semibold text-16">Catalog</span>
                <h1 class="text-40 fw-semibold text-neutral-800 mb-8">Courses</h1>
                <p class="text-16 text-neutral-500 mb-0">Browse active courses and curricular subjects available in GAPE.</p>
            </div>
            <span class="bg-white border border-neutral-30 rounded-pill px-20 py-10 text-14 text-neutral-600">${courseCount} courses</span>
        </div>

        <form action="${pageContext.request.contextPath}/courses" method="get" class="bg-white rounded-10 px-24 py-24 mb-32">
            <div class="row gy-4">
                <div class="col-lg-4">
                    <label for="q" class="fw-medium text-base text-neutral-800 mb-12">Search</label>
                    <input id="q" name="q" type="search" value="<c:out value='${selectedQuery}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                </div>
                <div class="col-lg-4 gape-select-field">
                    <label for="organizationId" class="fw-medium text-base text-neutral-800 mb-12">Organization</label>
                    <select id="organizationId" name="organizationId" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                        <option value="">All organizations</option>
                        <c:forEach var="organization" items="${catalogOrganizations}">
                            <option value="${organization.id}" ${selectedOrganizationId == organization.id ? 'selected' : ''}>
                                <c:out value="${organization.name}"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-lg-3 gape-select-field">
                    <label for="type" class="fw-medium text-base text-neutral-800 mb-12">Type</label>
                    <select id="type" name="type" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                        <option value="">All types</option>
                        <option value="DEGREE" ${selectedType == 'DEGREE' ? 'selected' : ''}>Degree</option>
                        <option value="MASTER" ${selectedType == 'MASTER' ? 'selected' : ''}>Master</option>
                        <option value="SHORT_COURSE" ${selectedType == 'SHORT_COURSE' ? 'selected' : ''}>Short course</option>
                        <option value="PROFESSIONAL_TRAINING" ${selectedType == 'PROFESSIONAL_TRAINING' ? 'selected' : ''}>Professional training</option>
                        <option value="OTHER" ${selectedType == 'OTHER' ? 'selected' : ''}>Other</option>
                    </select>
                </div>
                <div class="col-lg-1 d-flex align-items-end">
                    <button type="submit" class="bg-main-600 w-100 py-14 rounded-12 text-white text-20 hover-bg-main-700 transition-03" title="Filter">
                        <i class="ph ph-magnifying-glass"></i>
                    </button>
                </div>
            </div>
        </form>

        <div class="row gy-4">
            <c:forEach var="course" items="${catalogCourses}">
                <c:set var="coursePhotoUrl" value=""/>
                <c:if test="${course.hasPhoto}">
                    <c:set var="coursePhotoUrl" value="${pageContext.request.contextPath}/media/${course.photo}?v=${mediaCacheVersion}"/>
                </c:if>
                <div class="col-xl-4 col-md-6">
                    <div class="course-item bg-white rounded-16 p-12 h-100 box-shadow-md">
                        <div class="course-item__thumb rounded-12 overflow-hidden">
                            <a href="${pageContext.request.contextPath}/courses/${course.id}" class="w-100 h-100">
                                <c:choose>
                                    <c:when test="${course.hasPhoto}">
                                        <img src="${coursePhotoUrl}" alt="" class="course-item__img rounded-12 cover-img transition-2" onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                                        <span class="course-item__img gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--course-card d-none transition-2" aria-label="No course photo">
                                            <i class="ph ph-image"></i>
                                        </span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="course-item__img gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--course-card transition-2" aria-label="No course photo">
                                            <i class="ph ph-image"></i>
                                        </span>
                                    </c:otherwise>
                                </c:choose>
                            </a>
                        </div>
                        <div class="course-item__content">
                            <span class="px-12 py-6 text-success-600 fw-medium text-14 bg-success-50 border-success-100 border rounded-10 mb-16 d-inline-block">
                                <c:out value="${course.typeLabel}"/>
                            </span>
                            <h3 class="mb-12 text-20">
                                <a href="${pageContext.request.contextPath}/courses/${course.id}" class="link text-line-2 fw-semibold">
                                    <c:out value="${course.name}"/>
                                </a>
                            </h3>
                            <p class="text-14 text-neutral-500 text-line-2 mb-16"><c:out value="${course.description}"/></p>
                            <div class="d-flex align-items-center gap-16 flex-wrap text-14 text-neutral-600 mb-20">
                                <span><i class="ph ph-buildings me-4"></i><c:out value="${course.organizationName}"/></span>
                                <span><i class="ph ph-book-open me-4"></i><c:out value="${course.subjectCountLabel}"/></span>
                            </div>
                            <div class="flex-between gap-12 flex-wrap border-top border-neutral-30 pt-20">
                                <span class="${course.enrollmentBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                    <c:out value="${course.enrollmentStateLabel}"/>
                                </span>
                                <c:choose>
                                    <c:when test="${canUseStudentActions and course.activeEnrollment}">
                                        <a href="${pageContext.request.contextPath}/courses/${course.id}" class="border-main-600 border px-18 py-9 rounded-12 fw-semibold text-main-600 hover-bg-main-50 transition-03">Open</a>
                                    </c:when>
                                    <c:when test="${canUseStudentActions}">
                                        <form action="${pageContext.request.contextPath}/student/enrollments/courses/${course.id}" method="post" class="m-0">
                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                            <input type="hidden" name="returnTo" value="/courses">
                                            <button type="submit" class="bg-main-600 px-18 py-9 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Enroll</button>
                                        </form>
                                    </c:when>
                                    <c:otherwise>
                                        <a href="${pageContext.request.contextPath}/login.jsp" class="border-main-600 border px-18 py-9 rounded-12 fw-semibold text-main-600 hover-bg-main-50 transition-03">Sign in</a>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </div>
                </div>
            </c:forEach>
            <c:if test="${empty catalogCourses}">
                <div class="col-12">
                    <div class="bg-white rounded-10 px-24 py-40 text-center text-14 text-neutral-500">No active courses match the selected filters.</div>
                </div>
            </c:if>
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
