<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:forEach var="course" items="${courses}" varStatus="courseLoop">
    <c:set var="canModifyCourseRow" value="${canModifyCourseById[course.id]}" />
    <c:set var="coursePhotoUrl" value=""/>
    <c:if test="${course.hasPhoto}">
        <c:set var="coursePhotoUrl" value="${pageContext.request.contextPath}/media/${course.photo}?v=${mediaCacheVersion}"/>
    </c:if>
    <tr class="hover-bg-neutral-20 border-bottom transition-03"
        data-course-row
        data-sort-index="${courseListOffset + courseLoop.index}"
        data-sort-name="${fn:escapeXml(course.name)}"
        data-sort-date="${course.id}"
        data-sort-status="${fn:escapeXml(course.stateLabel)}"
        data-group-organization-id="${course.organizationId}"
        data-group-organization="${fn:escapeXml(course.organizationName)}"
        data-group-organic-unit-id="${course.organicUnitId}"
        data-group-organic-unit="${fn:escapeXml(course.organicUnitLabel)}">
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
                    <a href="${pageContext.request.contextPath}${courseBasePath}/${course.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                        <c:out value="${course.name}"/>
                    </a>
                    <span class="d-block text-12 text-neutral-500">
                        <span class="gape-acronym-token" tabindex="0" title="<c:out value='${course.name}'/>"><c:out value="${course.acronym}"/></span>
                        | <c:out value="${course.ectsLabel}"/>
                    </span>
                </div>
            </div>
        </td>
        <td class="py-20 px-20 text-14 text-neutral-500" title="<c:out value='${course.courseManagementContextTitle}'/>" data-course-context-column><c:out value="${course.courseManagementContextHtml}" escapeXml="false"/></td>
        <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${course.subjectCount}"/></td>
        <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${activeEnrollmentCountByCourse[course.id]}"/></td>
        <td class="py-20 px-20">
            <span class="${course.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                <c:out value="${course.stateLabel}"/>
            </span>
        </td>
        <td class="py-20 px-20">
            <div class="d-flex align-items-center gap-12 justify-content-end">
                <a href="${pageContext.request.contextPath}${courseBasePath}/${course.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                    <i class="ph ph-eye"></i>
                </a>
                <c:if test="${canModifyCourseRow}">
                    <a href="${pageContext.request.contextPath}${courseBasePath}/${course.id}/edit" class="text-22 text-neutral-500 hover-text-main-600" title="Edit">
                        <i class="ph ph-pencil-simple-line"></i>
                    </a>
                    <button type="button" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" aria-label="Delete" data-bs-toggle="modal" data-bs-target="#deleteCourse${course.id}">
                        <i class="ph ph-trash"></i>
                    </button>
                </c:if>
            </div>
            <c:if test="${canModifyCourseRow}">
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
                                <form action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/delete" method="post" class="m-0">
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
<tr hidden
    data-course-load-more-meta
    data-has-more="${courseListHasMore}"
    data-next-offset="${courseListNextOffset}"
    data-current-page="${courseListCurrentPage}"
    data-load-all="${courseListLoadAll}"></tr>
