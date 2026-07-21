<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:forEach var="subject" items="${subjects}" varStatus="subjectLoop">
    <c:set var="canModifySubjectRow" value="${canModifySubjectById[subject.id]}" />
    <c:set var="subjectCourses" value="${subjectCoursesBySubject[subject.id]}"/>
    <c:set var="primarySubjectCourse" value="${empty subjectCourses ? null : subjectCourses[0]}"/>
    <c:set var="subjectGroupCourseId" value="none"/>
    <c:set var="subjectGroupCourseName" value="No course"/>
    <c:set var="subjectGroupOrganicUnitId" value="none"/>
    <c:set var="subjectGroupOrganicUnitName" value="No organic unit"/>
    <c:if test="${not empty primarySubjectCourse}">
        <c:set var="subjectGroupCourseId" value="${primarySubjectCourse.courseId}"/>
        <c:set var="subjectGroupCourseName" value="${primarySubjectCourse.courseName}"/>
        <c:set var="subjectGroupOrganicUnitId" value="${empty primarySubjectCourse.course.organicUnitId ? 'none' : primarySubjectCourse.course.organicUnitId}"/>
        <c:set var="subjectGroupOrganicUnitName" value="${primarySubjectCourse.course.organicUnitLabel}"/>
    </c:if>
    <c:set var="subjectPhotoUrl" value=""/>
    <c:if test="${subject.hasPhoto}">
        <c:set var="subjectPhotoUrl" value="${pageContext.request.contextPath}/media/${subject.photo}?v=${mediaCacheVersion}"/>
    </c:if>
    <tr class="hover-bg-neutral-20 border-bottom transition-03"
        data-subject-row
        data-sort-index="${subjectListOffset + subjectLoop.index}"
        data-sort-name="${fn:escapeXml(subject.name)}"
        data-sort-date="${subject.id}"
        data-sort-status="${fn:escapeXml(subject.stateLabel)}"
        data-subject-association-count="${associationCountBySubject[subject.id]}"
        data-group-organization-id="${subject.organizationId}"
        data-group-organization="${fn:escapeXml(subject.organizationName)}"
        data-group-organic-unit-id="${subjectGroupOrganicUnitId}"
        data-group-organic-unit="${fn:escapeXml(subjectGroupOrganicUnitName)}"
        data-group-course-id="${subjectGroupCourseId}"
        data-group-course="${fn:escapeXml(subjectGroupCourseName)}">
        <td class="py-20 px-20">
            <div class="d-flex align-items-center gap-12">
                <c:choose>
                    <c:when test="${subject.hasPhoto}">
                        <img src="${subjectPhotoUrl}"
                             alt=""
                             class="gape-learning-table-photo flex-shrink-0"
                             onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                        <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table d-none" aria-label="No subject photo">
                            <i class="ph ph-image"></i>
                        </span>
                    </c:when>
                    <c:otherwise>
                        <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table" aria-label="No subject photo">
                            <i class="ph ph-image"></i>
                        </span>
                    </c:otherwise>
                </c:choose>
                <div>
                    <a href="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                        <c:out value="${subject.name}"/>
                    </a>
                    <span class="d-block text-12 text-neutral-500">
                        <span class="gape-acronym-token" tabindex="0" title="<c:out value='${subject.name}'/>"><c:out value="${subject.acronym}"/></span>
                    </span>
                </div>
            </div>
        </td>
        <td class="py-20 px-20 text-14 text-neutral-500" title="<c:out value='${subject.organizationName}'/>" data-subject-context-column><c:out value="${subject.organizationContextHtml}" escapeXml="false"/></td>
        <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${subject.ectsLabel}"/></td>
        <td class="py-20 px-20 text-14 text-neutral-500" data-subject-association-column><c:out value="${associationCountBySubject[subject.id]}"/></td>
        <td class="py-20 px-20">
            <span class="${subject.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                <c:out value="${subject.stateLabel}"/>
            </span>
        </td>
        <td class="py-20 px-20">
            <div class="d-flex align-items-center gap-12 justify-content-end">
                <a href="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                    <i class="ph ph-eye"></i>
                </a>
                <c:if test="${canModifySubjectRow}">
                    <a href="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/edit" class="text-22 text-neutral-500 hover-text-main-600" title="Edit">
                        <i class="ph ph-pencil-simple-line"></i>
                    </a>
                    <button type="button" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" aria-label="Delete" data-bs-toggle="modal" data-bs-target="#deleteSubject${subject.id}">
                        <i class="ph ph-trash"></i>
                    </button>
                </c:if>
            </div>
            <c:if test="${canModifySubjectRow}">
                <div class="modal fade" id="deleteSubject${subject.id}" tabindex="-1" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-centered">
                        <div class="modal-content rounded-12 border-0">
                            <div class="modal-header border-neutral-30">
                                <h5 class="modal-title text-18 fw-semibold">Delete Subject</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                            </div>
                            <div class="modal-body">
                                <p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${subject.name}"/></strong> if it has no dependencies.</p>
                            </div>
                            <div class="modal-footer border-neutral-30">
                                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                <form action="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/delete" method="post" class="m-0">
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
<c:if test="${empty subjects}">
    <tr>
        <td colspan="6" class="py-32 px-20 text-center text-14 text-neutral-500">No managed subjects found.</td>
    </tr>
</c:if>
<tr hidden
    data-subject-load-more-meta
    data-has-more="${subjectListHasMore}"
    data-next-offset="${subjectListNextOffset}"
    data-current-page="${subjectListCurrentPage}"
    data-load-all="${subjectListLoadAll}"></tr>
