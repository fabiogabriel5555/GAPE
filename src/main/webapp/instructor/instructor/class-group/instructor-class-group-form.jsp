<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "class-groups");
    }
%>
<c:set var="classGroupBackHref" value="${pageContext.request.contextPath}/learning/class-groups"/>
<c:if test="${not creating}">
    <c:set var="classGroupBackHref" value="${pageContext.request.contextPath}/learning/class-groups/${form.id}"/>
</c:if>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Class Group</title>
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
                <form action="${formAction}" method="post" class="bg-white rounded-10 px-40 py-40">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">${creating ? 'Create Class Group' : 'Edit Class Group'}</h2>
                            <span class="text-14 text-neutral-500">Class groups must use a course-subject association already active in the system.</span>
                        </div>
                        <a href="${classGroupBackHref}" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                    </div>

                    <div class="row gy-4">
                        <c:choose>
                            <c:when test="${creating}">
                                <div class="col-lg-5 gape-select-field">
                                    <label for="courseId" class="fw-medium text-base text-neutral-800 mb-12">Course</label>
                                    <select id="courseId" name="courseId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                        <option value="">Select course</option>
                                        <c:forEach var="course" items="${courseOptions}">
                                            <option value="${course.id}" ${form.courseId == course.id ? 'selected' : ''}>
                                                <c:out value="${course.name}"/>
                                            </option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div class="col-lg-5 gape-select-field">
                                    <label for="subjectId" class="fw-medium text-base text-neutral-800 mb-12">Subject</label>
                                    <select id="subjectId" name="subjectId" required data-dependent-select data-parent-select="#courseId" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                        <option value="">Select subject</option>
                                        <c:forEach var="association" items="${courseSubjectOptions}">
                                            <option value="${association.subjectId}" data-parent-value="${association.courseId}" ${form.subjectId == association.subjectId and form.courseId == association.courseId ? 'selected' : ''}>
                                                <c:out value="${association.subjectName}"/> - <c:out value="${association.curricularPositionLabel}"/>
                                            </option>
                                        </c:forEach>
                                    </select>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="col-lg-5">
                                    <label for="courseContext" class="fw-medium text-base text-neutral-800 mb-12">Course</label>
                                    <input id="courseContext" type="text" value="<c:out value='${classGroup.courseName}'/>" readonly class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14">
                                    <input type="hidden" name="courseId" value="${form.courseId}">
                                </div>
                                <div class="col-lg-5">
                                    <label for="subjectContext" class="fw-medium text-base text-neutral-800 mb-12">Subject</label>
                                    <input id="subjectContext" type="text" value="<c:out value='${classGroup.subjectName}'/>" readonly class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14">
                                    <input type="hidden" name="subjectId" value="${form.subjectId}">
                                </div>
                            </c:otherwise>
                        </c:choose>
                        <div class="col-lg-2">
                            <label for="code" class="fw-medium text-base text-neutral-800 mb-12">Code</label>
                            <input id="code" name="code" type="text" value="<c:out value='${form.code}'/>" required pattern="[^|]*" title="Codes cannot contain |" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-4 gape-select-field">
                            <label for="modality" class="fw-medium text-base text-neutral-800 mb-12">Modality</label>
                            <select id="modality" name="modality" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <option value="ONSITE" ${form.modality == 'ONSITE' ? 'selected' : ''}>On-site</option>
                                <option value="ONLINE" ${form.modality == 'ONLINE' ? 'selected' : ''}>Online</option>
                                <option value="HYBRID" ${form.modality == 'HYBRID' ? 'selected' : ''}>Hybrid</option>
                            </select>
                        </div>
                        <div class="col-lg-4">
                            <label for="shift" class="fw-medium text-base text-neutral-800 mb-12">Shift</label>
                            <input id="shift" name="shift" type="text" value="<c:out value='${form.shift}'/>" pattern="[^|]*" title="Shifts cannot contain |" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-4 gape-select-field">
                            <label for="state" class="fw-medium text-base text-neutral-800 mb-12">State</label>
                            <select id="state" name="state" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <option value="ACTIVE" ${form.state == 'ACTIVE' ? 'selected' : ''}>Active</option>
                                <option value="INACTIVE" ${form.state == 'INACTIVE' ? 'selected' : ''}>Inactive</option>
                                <option value="CLOSED" ${form.state == 'CLOSED' ? 'selected' : ''}>Closed</option>
                            </select>
                        </div>
                        <div class="col-lg-3">
                            <label for="minStudents" class="fw-medium text-base text-neutral-800 mb-12">Min Students</label>
                            <input id="minStudents" name="minStudents" type="number" min="0" step="1" value="<c:out value='${form.minStudents}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-3">
                            <label for="maxStudents" class="fw-medium text-base text-neutral-800 mb-12">Max Students</label>
                            <input id="maxStudents" name="maxStudents" type="number" min="0" step="1" value="<c:out value='${form.maxStudents}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-3">
                            <label for="startsAt" class="fw-medium text-base text-neutral-800 mb-12">Start Date</label>
                            <input id="startsAt" name="startsAt" type="date" value="<c:out value='${form.startsAt}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-3">
                            <label for="endsAt" class="fw-medium text-base text-neutral-800 mb-12">End Date</label>
                            <input id="endsAt" name="endsAt" type="date" value="<c:out value='${form.endsAt}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                    </div>

                    <div class="d-flex align-items-center gap-16 flex-wrap mt-32">
                        <button type="submit" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Save</button>
                        <a href="${classGroupBackHref}" class="border-main-600 border px-24 py-12 fw-semibold rounded-12 hover-bg-main-50 transition-03">Cancel</a>
                    </div>
                </form>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
