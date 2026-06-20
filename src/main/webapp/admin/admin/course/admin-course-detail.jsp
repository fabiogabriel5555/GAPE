<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "courses");
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
    <style>
        .gape-action-button {
            align-items: center;
            border: 1px solid transparent;
            cursor: pointer;
            display: inline-flex;
            justify-content: center;
            line-height: 1.25;
            min-height: 48px;
            text-decoration: none;
        }

        .gape-action-delete {
            background-color: #dc2626 !important;
            border-color: #dc2626 !important;
            color: #fff !important;
        }

        .gape-action-archive {
            background-color: #f97316 !important;
            border-color: #f97316 !important;
            color: #fff !important;
        }

        .gape-learning-detail-photo {
            border-radius: 16px;
            height: 72px;
            object-fit: cover;
            width: 96px;
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

                <div class="bg-white rounded-10 px-24 py-24 mb-24">
                    <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                        <div class="d-flex align-items-center gap-16">
                            <c:choose>
                                <c:when test="${course.hasPhoto}">
                                    <img src="${coursePhotoUrl}"
                                         alt=""
                                         class="gape-learning-detail-photo flex-shrink-0"
                                         onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--learning-detail d-none" aria-label="No course photo">
                                        <i class="ph ph-image"></i>
                                    </span>
                                </c:when>
                                <c:otherwise>
                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--learning-detail" aria-label="No course photo">
                                        <i class="ph ph-image"></i>
                                    </span>
                                </c:otherwise>
                            </c:choose>
                            <div>
                                <h2 class="text-20 fw-semibold text-neutral-700 mb-4"><c:out value="${course.name}"/></h2>
                                <span class="text-14 text-neutral-500" title="<c:out value='${course.courseManagementContextTitle}'/>"><c:out value="${course.courseManagementContextHtml}" escapeXml="false"/></span>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-12 flex-wrap">
                            <a href="${pageContext.request.contextPath}${courseBasePath}" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                            <c:if test="${not course.archived and canModifyCourse}">
                                <a href="${pageContext.request.contextPath}${courseBasePath}/${course.id}/edit" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Edit</a>
                            </c:if>
                            <c:if test="${course.archived and canModifyCourse}">
                                <form action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/unarchive" method="post" class="m-0">
                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                    <button type="submit" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03 border-0">Unarchive</button>
                                </form>
                            </c:if>
                        </div>
                    </div>
                    <div class="row gy-4">
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Type</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${course.typeLabel}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">State</span>
                                <div class="mt-8">
                                    <span class="${course.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                        <c:out value="${course.stateLabel}"/>
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">ECTS</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${course.ectsLabel}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Duration</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${course.durationLabel}"/></p>
                            </div>
                        </div>
                    </div>
                    <p class="text-14 text-neutral-600 mt-20 mb-0"><c:out value="${course.description}"/></p>
                </div>

                <div class="bg-white rounded-10 px-24 py-24 mb-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h3 class="text-18 fw-medium text-neutral-700 mb-4">Curricular Subjects</h3>
                            <span class="text-14 text-neutral-500">Position, mandatory state and active associations.</span>
                        </div>
                        <c:if test="${not course.archived and canManageCourseChildren}">
                            <a href="${pageContext.request.contextPath}${courseBasePath}/${course.id}/subjects/new" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                <i class="ph ph-plus-circle me-8"></i>Associate Subject
                            </a>
                        </c:if>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Subject</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Position</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Requirement</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="association" items="${courseSubjects}">
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20">
                                        <a href="${pageContext.request.contextPath}${subjectBasePath}/${association.subjectId}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                            <c:out value="${association.subjectName}"/>
                                        </a>
                                        <span class="d-block text-12 text-neutral-500">
                                            <span class="gape-acronym-token" tabindex="0" title="<c:out value='${association.subjectName}'/>"><c:out value="${association.subjectAcronym}"/></span>
                                            | <c:out value="${association.subjectEctsLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${association.curricularPositionLabel}"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${association.mandatoryLabel}"/></td>
                                    <td class="py-20 px-20">
                                        <span class="${association.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                            <c:out value="${association.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-12 justify-content-end">
                                            <c:if test="${not association.archived and canManageCourseChildren}">
                                                <a href="${pageContext.request.contextPath}${courseBasePath}/${course.id}/subjects/new" class="text-22 text-neutral-500 hover-text-main-600" title="Edit associations">
                                                    <i class="ph ph-pencil-simple-line"></i>
                                                </a>
                                                <button type="button" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Archive" data-bs-toggle="modal" data-bs-target="#archiveAssociation${association.subjectId}">
                                                    <i class="ph ph-archive-box"></i>
                                                </button>
                                                <button type="button" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteAssociation${association.subjectId}">
                                                    <i class="ph ph-trash"></i>
                                                </button>
                                            </c:if>
                                        </div>

                                        <c:if test="${canManageCourseChildren}">
                                        <div class="modal fade" id="archiveAssociation${association.subjectId}" tabindex="-1" aria-hidden="true">
                                            <div class="modal-dialog modal-dialog-centered">
                                                <div class="modal-content rounded-12 border-0">
                                                    <div class="modal-header border-neutral-30">
                                                        <h5 class="modal-title text-18 fw-semibold">Archive Association</h5>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                    </div>
                                                    <div class="modal-body">
                                                        <p class="text-14 text-neutral-600 mb-0">Confirm archiving this course-subject association?</p>
                                                    </div>
                                                    <div class="modal-footer border-neutral-30">
                                                        <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                        <form action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/subjects/${association.subjectId}/archive" method="post" class="m-0">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                            <button type="submit" class="gape-action-button gape-action-archive px-20 py-10 rounded-12 fw-semibold transition-03">Archive</button>
                                                        </form>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>

                                        <div class="modal fade" id="deleteAssociation${association.subjectId}" tabindex="-1" aria-hidden="true">
                                            <div class="modal-dialog modal-dialog-centered">
                                                <div class="modal-content rounded-12 border-0">
                                                    <div class="modal-header border-neutral-30">
                                                        <h5 class="modal-title text-18 fw-semibold">Delete Association</h5>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                    </div>
                                                    <div class="modal-body">
                                                        <p class="text-14 text-neutral-600 mb-0">This action removes the association if it has no dependencies.</p>
                                                    </div>
                                                    <div class="modal-footer border-neutral-30">
                                                        <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                        <form action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/subjects/${association.subjectId}/delete" method="post" class="m-0">
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
                            <c:if test="${empty courseSubjects}">
                                <tr>
                                    <td colspan="5" class="py-32 px-20 text-center text-14 text-neutral-500">No subjects associated with this course.</td>
                                </tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24 mb-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h3 class="text-18 fw-medium text-neutral-700 mb-4">Class Groups</h3>
                            <span class="text-14 text-neutral-500">Groups created for this course and its subjects.</span>
                        </div>
                        <div class="d-flex align-items-center gap-12 flex-wrap">
                            <a href="${pageContext.request.contextPath}/learning/class-groups?courseId=${course.id}" class="border-main-600 border px-16 py-8 fw-semibold rounded-12 hover-bg-main-50 transition-03">Open Groups</a>
                            <c:if test="${not course.archived and canManageCourseChildren}">
                                <a href="${pageContext.request.contextPath}/learning/class-groups/new?courseId=${course.id}" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                    <i class="ph ph-plus-circle me-8"></i>New Group
                                </a>
                            </c:if>
                        </div>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Group</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Subject</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Capacity</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Blocks</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Action</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="classGroup" items="${classGroups}">
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20">
                                        <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                            <c:out value="${classGroup.code}"/>
                                        </a>
                                        <span class="d-block text-12 text-neutral-500"><c:out value="${classGroup.modalityLabel}"/> | <c:out value="${classGroup.shift}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${classGroup.subjectName}"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${classGroup.activeEnrollmentCount}"/> enrolled | <c:out value="${classGroup.capacityLabel}"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${classGroup.blockCount}"/></td>
                                    <td class="py-20 px-20">
                                        <span class="${classGroup.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                            <c:out value="${classGroup.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20 text-end">
                                        <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                                            <i class="ph ph-eye"></i>
                                        </a>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty classGroups}">
                                <tr>
                                    <td colspan="6" class="py-32 px-20 text-center text-14 text-neutral-500">No class groups created for this course.</td>
                                </tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>

                <c:if test="${not course.archived and canModifyCourse}">
                    <div class="bg-white rounded-10 px-24 py-24">
                        <h3 class="text-16 fw-medium text-neutral-700 mb-16">Critical Actions</h3>
                        <div class="d-flex align-items-center gap-16 flex-wrap">
                            <button type="button" class="gape-action-button gape-action-archive px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#archiveCourse">Archive</button>
                            <button type="button" class="gape-action-button gape-action-delete px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#deleteCourse">Delete</button>
                        </div>
                    </div>
                </c:if>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>

<c:if test="${not course.archived and canModifyCourse}">
<div class="modal fade" id="archiveCourse" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Archive Course</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">Confirm archiving <strong><c:out value="${course.name}"/></strong>?</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/archive" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="gape-action-button gape-action-archive px-20 py-10 rounded-12 fw-semibold transition-03">Archive</button>
                </form>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="deleteCourse" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Delete Course</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">This action removes the course if the database allows it.</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/delete" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="gape-action-button gape-action-delete px-20 py-10 rounded-12 fw-semibold transition-03">Delete</button>
                </form>
            </div>
        </div>
    </div>
</div>
</c:if>

<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
