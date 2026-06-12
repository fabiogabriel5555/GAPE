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
    <title>GAPE - Associate Subject</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-association-form .form-control,
        .gape-association-form .form-select,
        .gape-association-form .select2-container .select2-selection--single {
            min-height: 52px;
        }

        .gape-association-form .select2-container .select2-selection--single {
            align-items: center;
            display: flex;
        }

        .gape-association-form .select2-container--default .select2-selection--single .select2-selection__arrow {
            height: 52px;
        }

        .gape-association-check {
            align-items: center;
            display: flex;
            min-height: 52px;
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

                <div class="bg-white rounded-10 px-40 py-40 mb-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Associate Subject</h2>
                            <span class="text-14 text-neutral-500"><c:out value="${course.name}"/></span>
                        </div>
                        <a href="${pageContext.request.contextPath}/admin/courses/${course.id}" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                    </div>

                    <h3 class="text-16 fw-medium text-neutral-700 mb-16">Current Associations</h3>
                    <div class="d-flex flex-column gap-16 mb-32">
                        <c:forEach var="association" items="${courseSubjects}">
                            <form action="${pageContext.request.contextPath}/admin/courses/${course.id}/subjects/${association.subjectId}" method="post" class="gape-association-form border border-neutral-30 rounded-12 px-20 py-20">
                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                <div class="row gy-4 align-items-start">
                                    <div class="col-xl-3 col-lg-6">
                                        <span class="text-13 text-neutral-500 d-block mb-8">Subject</span>
                                        <a href="${pageContext.request.contextPath}/admin/subjects/${association.subjectId}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                            <c:out value="${association.subjectName}"/>
                                        </a>
                                        <span class="d-block text-12 text-neutral-500">
                                            <span class="gape-acronym-token" tabindex="0" title="<c:out value='${association.subjectName}'/>"><c:out value="${association.subjectAcronym}"/></span>
                                            | <c:out value="${association.subjectEctsLabel}"/>
                                        </span>
                                    </div>
                                    <div class="col-xl-2 col-lg-3 col-md-6">
                                        <label for="curricularYear${association.subjectId}" class="fw-medium text-base text-neutral-800 mb-12">Year</label>
                                        <input id="curricularYear${association.subjectId}" name="curricularYear" type="number" min="1" value="<c:out value='${association.curricularYear}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                    </div>
                                    <div class="col-xl-2 col-lg-3 col-md-6 gape-select-field">
                                        <label for="term${association.subjectId}" class="fw-medium text-base text-neutral-800 mb-12">Period</label>
                                        <select id="term${association.subjectId}" name="term" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                            <option value="" ${empty association.term ? 'selected' : ''}>No period</option>
                                            <option value="ANNUAL" ${association.term == 'ANNUAL' ? 'selected' : ''}>Annual</option>
                                            <option value="SEMESTER_1" ${association.term == 'SEMESTER_1' ? 'selected' : ''}>1st semester</option>
                                            <option value="SEMESTER_2" ${association.term == 'SEMESTER_2' ? 'selected' : ''}>2nd semester</option>
                                            <option value="TRIMESTER_1" ${association.term == 'TRIMESTER_1' ? 'selected' : ''}>1st trimester</option>
                                            <option value="TRIMESTER_2" ${association.term == 'TRIMESTER_2' ? 'selected' : ''}>2nd trimester</option>
                                            <option value="TRIMESTER_3" ${association.term == 'TRIMESTER_3' ? 'selected' : ''}>3rd trimester</option>
                                        </select>
                                    </div>
                                    <div class="col-xl-2 col-lg-3 col-md-6 gape-select-field">
                                        <label for="state${association.subjectId}" class="fw-medium text-base text-neutral-800 mb-12">State</label>
                                        <select id="state${association.subjectId}" name="state" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                            <option value="ACTIVE" ${association.state == 'ACTIVE' ? 'selected' : ''}>Active</option>
                                            <option value="INACTIVE" ${association.state == 'INACTIVE' ? 'selected' : ''}>Inactive</option>
                                        </select>
                                    </div>
                                    <div class="col-xl-2 col-lg-6">
                                        <span class="d-block fw-medium text-base text-neutral-800 mb-12 invisible" aria-hidden="true">Mandatory</span>
                                        <div class="form-check common-check gape-association-check mb-0">
                                            <input class="form-check-input" type="checkbox" id="mandatory${association.subjectId}" name="mandatory" value="true" ${association.mandatory ? 'checked' : ''}>
                                            <label class="form-check-label fw-medium" for="mandatory${association.subjectId}">Mandatory</label>
                                        </div>
                                    </div>
                                    <div class="col-xl-1 col-lg-3 d-flex flex-column">
                                        <span class="d-block fw-medium text-base text-neutral-800 mb-12 invisible" aria-hidden="true">Action</span>
                                        <button type="submit" class="bg-main-600 px-20 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03 w-100 min-w-86-px">Save</button>
                                    </div>
                                </div>
                            </form>
                        </c:forEach>
                        <c:if test="${empty courseSubjects}">
                            <p class="text-14 text-neutral-500 mb-0">No associated subjects found.</p>
                        </c:if>
                    </div>

                    <form action="${pageContext.request.contextPath}/admin/courses/${course.id}/subjects" method="post" class="gape-association-form border border-neutral-30 rounded-12 px-20 py-20">
                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                        <h3 class="text-16 fw-medium text-neutral-700 mb-20">Add Subject</h3>
                        <div class="row gy-4 align-items-start">
                            <div class="col-xl-4 col-lg-6 gape-select-field">
                                <label for="subjectId" class="fw-medium text-base text-neutral-800 mb-12">Subject</label>
                                <select id="subjectId" name="subjectId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                    <option value="">Select subject</option>
                                    <c:forEach var="subject" items="${availableSubjectOptions}">
                                        <option value="${subject.id}">
                                            <c:out value="${subject.name}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-xl-2 col-lg-3 col-md-6">
                                <label for="curricularYear" class="fw-medium text-base text-neutral-800 mb-12">Year</label>
                                <input id="curricularYear" name="curricularYear" type="number" min="1" value="<c:out value='${form.curricularYear}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                            </div>
                            <div class="col-xl-2 col-lg-3 col-md-6 gape-select-field">
                                <label for="term" class="fw-medium text-base text-neutral-800 mb-12">Period</label>
                                <select id="term" name="term" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                    <option value="" ${empty form.term ? 'selected' : ''}>No period</option>
                                    <option value="ANNUAL" ${form.term == 'ANNUAL' ? 'selected' : ''}>Annual</option>
                                    <option value="SEMESTER_1" ${form.term == 'SEMESTER_1' ? 'selected' : ''}>1st semester</option>
                                    <option value="SEMESTER_2" ${form.term == 'SEMESTER_2' ? 'selected' : ''}>2nd semester</option>
                                    <option value="TRIMESTER_1" ${form.term == 'TRIMESTER_1' ? 'selected' : ''}>1st trimester</option>
                                    <option value="TRIMESTER_2" ${form.term == 'TRIMESTER_2' ? 'selected' : ''}>2nd trimester</option>
                                    <option value="TRIMESTER_3" ${form.term == 'TRIMESTER_3' ? 'selected' : ''}>3rd trimester</option>
                                </select>
                            </div>
                            <div class="col-xl-2 col-lg-3 col-md-6 gape-select-field">
                                <label for="state" class="fw-medium text-base text-neutral-800 mb-12">State</label>
                                <select id="state" name="state" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                    <option value="ACTIVE" ${form.state == 'ACTIVE' ? 'selected' : ''}>Active</option>
                                    <option value="INACTIVE" ${form.state == 'INACTIVE' ? 'selected' : ''}>Inactive</option>
                                </select>
                            </div>
                            <div class="col-xl-2 col-lg-6">
                                <span class="d-block fw-medium text-base text-neutral-800 mb-12 invisible" aria-hidden="true">Mandatory</span>
                                <div class="form-check common-check gape-association-check mb-0">
                                    <input class="form-check-input" type="checkbox" id="mandatory" name="mandatory" value="true" ${form.mandatory ? 'checked' : ''}>
                                    <label class="form-check-label fw-medium" for="mandatory">Mandatory subject</label>
                                </div>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-16 flex-wrap mt-24">
                            <button type="submit" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Add Subject</button>
                            <a href="${pageContext.request.contextPath}/admin/courses/${course.id}" class="border-main-600 border px-24 py-12 fw-semibold rounded-12 hover-bg-main-50 transition-03">Done</a>
                        </div>
                    </form>
                </div>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
