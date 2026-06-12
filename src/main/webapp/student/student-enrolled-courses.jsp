<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "courses");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - My Courses</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
</head>
<body>
<div class="preloader">
    <img src="${pageContext.request.contextPath}/assets/images/icons/preloader.gif" alt="">
</div>
<div class="overlay"></div>
<div class="side-overlay"></div>
<div class="dashbord bg-main-25 w-100 overflow-hidden">
    <div class="d-flex">
        <%@ include file="/WEB-INF/fragments/dashboard-sidebar.jspf" %>
        <div class="dashbord-body flex-grow-1 d-flex flex-column min-vh-100">
            <%@ include file="/WEB-INF/fragments/dashboard-topbar.jspf" %>
            <div class="px-24 py-24 flex-grow-1">
                <%@ include file="/WEB-INF/fragments/flash-messages.jspf" %>

                <div class="bg-white rounded-10 px-24 py-24 mb-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">My Course Enrollments</h2>
                            <span class="text-14 text-neutral-500">Course and subject enrollments managed by GAPE.</span>
                        </div>
                        <a href="${pageContext.request.contextPath}/courses" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Open Catalog</a>
                    </div>
                </div>

                <div class="row gy-4 mb-24">
                    <c:forEach var="enrollment" items="${courseEnrollments}">
                        <c:set var="coursePhotoUrl" value=""/>
                        <c:if test="${enrollment.course.hasPhoto}">
                            <c:set var="coursePhotoUrl" value="${pageContext.request.contextPath}/media/${enrollment.course.photo}?v=${mediaCacheVersion}"/>
                        </c:if>
                        <div class="col-xl-4 col-md-6">
                            <div class="course-item bg-white rounded-16 p-12 h-100 box-shadow-md">
                                <div class="course-item__thumb rounded-12 overflow-hidden">
                                    <a href="${pageContext.request.contextPath}/courses/${enrollment.course.id}" class="w-100 h-100">
                                        <c:choose>
                                            <c:when test="${enrollment.course.hasPhoto}">
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
                                    <span class="${enrollment.badgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13 d-inline-block mb-12">
                                        <c:out value="${enrollment.stateLabel}"/>
                                    </span>
                                    <h3 class="text-18 fw-semibold mb-8">
                                        <a href="${pageContext.request.contextPath}/courses/${enrollment.course.id}" class="link text-neutral-700 hover-text-main-600">
                                            <c:out value="${enrollment.title}"/>
                                        </a>
                                    </h3>
                                    <p class="text-14 text-neutral-500 mb-16"><c:out value="${enrollment.contextLabel}"/></p>
                                    <div class="d-flex align-items-center gap-12 flex-wrap">
                                        <a href="${pageContext.request.contextPath}/courses/${enrollment.course.id}" class="border-main-600 border px-18 py-9 rounded-12 fw-semibold text-main-600 hover-bg-main-50 transition-03">Open</a>
                                        <c:if test="${enrollment.activeEnrollment}">
                                            <form action="${pageContext.request.contextPath}/student/enrollments/courses/${enrollment.course.id}/withdraw" method="post" class="m-0">
                                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                <input type="hidden" name="returnTo" value="/student/enrollments">
                                                <button type="submit" class="bg-main-600 px-18 py-9 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Withdraw</button>
                                            </form>
                                        </c:if>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                    <c:if test="${empty courseEnrollments}">
                        <div class="col-12">
                            <div class="bg-white rounded-10 px-24 py-40 text-center text-14 text-neutral-500">No course enrollments yet.</div>
                        </div>
                    </c:if>
                </div>

                <div class="bg-white rounded-10 px-24 py-24 mb-24">
                    <h3 class="text-18 fw-medium text-neutral-700 mb-20">Subject Enrollments</h3>
                    <div class="overflow-x-auto">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Subject</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Course</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Action</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="enrollment" items="${subjectEnrollments}">
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20 fw-medium text-14 text-neutral-700"><c:out value="${enrollment.title}"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${enrollment.contextLabel}"/></td>
                                    <td class="py-20 px-20">
                                        <span class="${enrollment.badgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                            <c:out value="${enrollment.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20 text-end">
                                        <c:if test="${enrollment.activeEnrollment}">
                                            <form action="${pageContext.request.contextPath}/student/enrollments/courses/${enrollment.course.id}/subjects/${enrollment.subject.subjectId}/withdraw" method="post" class="m-0 d-inline-block">
                                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                <input type="hidden" name="returnTo" value="/student/enrollments">
                                                <button type="submit" class="border-main-600 border px-18 py-9 rounded-12 fw-semibold text-main-600 hover-bg-main-50 transition-03">Withdraw</button>
                                            </form>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty subjectEnrollments}">
                                <tr>
                                    <td colspan="4" class="py-32 px-20 text-center text-14 text-neutral-500">No subject enrollments yet.</td>
                                </tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24">
                    <h3 class="text-18 fw-medium text-neutral-700 mb-20">Available Courses</h3>
                    <div class="row gy-4">
                        <c:forEach var="course" items="${availableCourses}">
                            <div class="col-xl-4 col-md-6">
                                <div class="border border-neutral-30 rounded-12 px-20 py-20 h-100">
                                    <h4 class="text-16 fw-semibold text-neutral-700 mb-8"><c:out value="${course.name}"/></h4>
                                    <p class="text-13 text-neutral-500 mb-16"><c:out value="${course.organizationName}"/></p>
                                    <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap">
                                        <span class="${course.enrollmentBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                            <c:out value="${course.enrollmentStateLabel}"/>
                                        </span>
                                        <c:choose>
                                            <c:when test="${course.activeEnrollment}">
                                                <a href="${pageContext.request.contextPath}/courses/${course.id}" class="border-main-600 border px-18 py-9 rounded-12 fw-semibold text-main-600 hover-bg-main-50 transition-03">Open</a>
                                            </c:when>
                                            <c:otherwise>
                                                <form action="${pageContext.request.contextPath}/student/enrollments/courses/${course.id}" method="post" class="m-0">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                    <input type="hidden" name="returnTo" value="/student/enrollments">
                                                    <button type="submit" class="bg-main-600 px-18 py-9 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Enroll</button>
                                                </form>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
