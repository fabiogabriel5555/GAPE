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
    <title>GAPE - Courses</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-action-button {
            align-items: center;
            border: 1px solid transparent;
            cursor: pointer;
            display: inline-flex;
            justify-content: center;
            line-height: 1.25;
            text-decoration: none;
        }

        .gape-action-delete {
            background-color: #dc2626 !important;
            border-color: #dc2626 !important;
            color: #fff !important;
        }

        .gape-learning-table-photo {
            border-radius: 12px;
            height: 44px;
            object-fit: cover;
            width: 44px;
        }
    </style>
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

                <div class="row gy-4 mb-24">
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Total</span>
                            <h2 class="text-32 fw-semibold text-neutral-700 mb-0">${courseCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Active</span>
                            <h2 class="text-32 fw-semibold text-success-600 mb-0">${activeCourses}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Archived</span>
                            <h2 class="text-32 fw-semibold text-danger-600 mb-0">${archivedCourses}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Subjects</span>
                            <h2 class="text-32 fw-semibold text-main-600 mb-0">${subjectTotal}</h2>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Course Management</h2>
                            <span class="text-14 text-neutral-500">Courses, curricular position and subject associations.</span>
                        </div>
                        <c:if test="${canCreateCourses}">
                            <a href="${pageContext.request.contextPath}/admin/courses/new" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                <i class="ph ph-plus-circle me-8"></i>New Course
                            </a>
                        </c:if>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Course</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Context</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Type</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Subjects</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="course" items="${courses}">
                                <c:set var="canModifyCourseRow" value="${canModifyCourseById[course.id]}" />
                                <c:set var="canManageCourseChildrenRow" value="${canManageCourseChildrenById[course.id]}" />
                                <c:set var="coursePhotoUrl" value=""/>
                                <c:if test="${course.hasPhoto}">
                                    <c:set var="coursePhotoUrl" value="${pageContext.request.contextPath}/media/${course.photo}?v=${mediaCacheVersion}"/>
                                </c:if>
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-12">
                                            <c:choose>
                                                <c:when test="${course.hasPhoto}">
                                                    <img src="${coursePhotoUrl}"
                                                         alt=""
                                                         class="gape-learning-table-photo flex-shrink-0"
                                                         onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table d-none" aria-label="No course photo">
                                                        <i class="ph ph-image"></i>
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table" aria-label="No course photo">
                                                        <i class="ph ph-image"></i>
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                            <div>
                                                <a href="${pageContext.request.contextPath}/admin/courses/${course.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                    <c:out value="${course.name}"/>
                                                </a>
                                                <span class="d-block text-12 text-neutral-500">
                                                    <span class="gape-acronym-token" tabindex="0" title="<c:out value='${course.name}'/>"><c:out value="${course.acronym}"/></span>
                                                    | <c:out value="${course.ectsLabel}"/>
                                                </span>
                                            </div>
                                        </div>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500" title="<c:out value='${course.courseManagementContextTitle}'/>"><c:out value="${course.courseManagementContextHtml}" escapeXml="false"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${course.typeLabel}"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${course.subjectCount}"/></td>
                                    <td class="py-20 px-20">
                                        <span class="${course.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                            <c:out value="${course.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-12 justify-content-end">
                                            <a href="${pageContext.request.contextPath}/admin/courses/${course.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                                                <i class="ph ph-eye"></i>
                                            </a>
                                            <c:if test="${not course.archived}">
                                                <c:if test="${canModifyCourseRow}">
                                                    <a href="${pageContext.request.contextPath}/admin/courses/${course.id}/edit" class="text-22 text-neutral-500 hover-text-main-600" title="Edit">
                                                        <i class="ph ph-pencil-simple-line"></i>
                                                    </a>
                                                </c:if>
                                                <c:if test="${canManageCourseChildrenRow}">
                                                    <a href="${pageContext.request.contextPath}/admin/courses/${course.id}/subjects/new" class="text-22 text-neutral-500 hover-text-main-600" title="Associate subject">
                                                        <i class="ph ph-link-simple"></i>
                                                    </a>
                                                </c:if>
                                                <c:if test="${canModifyCourseRow}">
                                                    <button type="button" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteCourse${course.id}">
                                                        <i class="ph ph-trash"></i>
                                                    </button>
                                                </c:if>
                                            </c:if>
                                            <c:if test="${course.archived and canModifyCourseRow}">
                                                <form action="${pageContext.request.contextPath}/admin/courses/${course.id}/unarchive" method="post" class="m-0">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                    <button type="submit" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Unarchive">
                                                        <i class="ph ph-arrow-u-up-left"></i>
                                                    </button>
                                                </form>
                                            </c:if>
                                        </div>

                                        <c:if test="${not course.archived and canModifyCourseRow}">
                                        <div class="modal fade" id="deleteCourse${course.id}" tabindex="-1" aria-hidden="true">
                                            <div class="modal-dialog modal-dialog-centered">
                                                <div class="modal-content rounded-12 border-0">
                                                    <div class="modal-header border-neutral-30">
                                                        <h5 class="modal-title text-18 fw-semibold">Delete Course</h5>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                    </div>
                                                    <div class="modal-body">
                                                        <p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${course.name}"/></strong> if it has no dependencies.</p>
                                                    </div>
                                                    <div class="modal-footer border-neutral-30">
                                                        <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                        <form action="${pageContext.request.contextPath}/admin/courses/${course.id}/delete" method="post" class="m-0">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                            <button type="submit" class="gape-action-button gape-action-delete px-20 py-10 rounded-12 fw-semibold transition-03">Delete</button>
                                                        </form>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty courses}">
                                <tr>
                                    <td colspan="6" class="py-32 px-20 text-center text-14 text-neutral-500">No managed courses found.</td>
                                </tr>
                            </c:if>
                            </tbody>
                        </table>
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
