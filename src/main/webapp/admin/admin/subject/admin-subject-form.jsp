<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "subjects");
    }
%>
<c:set var="subjectBackHref" value="${pageContext.request.contextPath}/admin/subjects"/>
<c:if test="${not creating}">
    <c:set var="subjectBackHref" value="${pageContext.request.contextPath}/admin/subjects/${form.id}"/>
</c:if>
<c:set var="subjectHasPhoto" value="${not empty form.photo}"/>
<c:set var="subjectPhotoUrl" value=""/>
<c:if test="${subjectHasPhoto}">
    <c:set var="subjectPhotoUrl" value="${pageContext.request.contextPath}/media/${form.photo}?v=${mediaCacheVersion}"/>
</c:if>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Subject</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-subject-photo-preview {
            background-position: center center;
            background-repeat: no-repeat;
            background-size: cover;
            border-radius: 16px;
            height: 132px;
            width: 176px;
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
                <form action="${formAction}" method="post" enctype="multipart/form-data" class="bg-white rounded-10 px-40 py-40">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <input type="hidden" id="photo" name="photo" value="<c:out value='${form.photo}'/>">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">${creating ? 'Create Subject' : 'Edit Subject'}</h2>
                            <span class="text-14 text-neutral-500">Subjects are scoped to one organization.</span>
                        </div>
                        <a href="${subjectBackHref}" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                    </div>

                    <div class="border-bottom-dashed pb-24 mb-24">
                        <h4 class="text-18 fw-normal text-neutral-700 mb-16">Subject Photo</h4>
                        <div class="avatar-upload">
                            <div class="d-flex align-items-center gap-32 flex-wrap">
                                <div class="avatar-preview flex-shrink-0">
                                    <c:choose>
                                        <c:when test="${subjectHasPhoto}">
                                            <div id="subjectImagePreview"
                                                 class="gape-subject-photo-preview gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--image-form is-image"
                                                 data-current-image="<c:out value='${subjectPhotoUrl}'/>"
                                                 data-has-current-image="true"
                                                 style="background-image: url('<c:out value='${subjectPhotoUrl}'/>');">
                                                <i class="ph ph-image d-none" data-photo-placeholder-icon></i>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div id="subjectImagePreview"
                                                 class="gape-subject-photo-preview gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--image-form"
                                                 data-current-image=""
                                                 data-has-current-image="false">
                                                <i class="ph ph-image" data-photo-placeholder-icon></i>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                                <div class="avatar-edit">
                                    <input type="file" id="subjectImageUpload" name="subjectImage" accept="image/jpeg,image/png,image/gif,image/bmp,image/webp">
                                </div>
                                <div class="d-flex align-items-center gap-16 flex-wrap">
                                    <label for="subjectImageUpload" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-16 text-white transition-04 hover-bg-main-700">Upload Image</label>
                                    <button type="button" id="cancelSubjectImage" class="border-main-600 border px-24 py-12 rounded-12 fw-semibold text-16 text-main-600 hover-bg-main-50 transition-04">Cancel</button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="row gy-4">
                        <div class="col-lg-6 gape-select-field">
                            <label for="organizationId" class="fw-medium text-base text-neutral-800 mb-12">Organization</label>
                            <select id="organizationId" name="organizationId" required ${creating ? '' : 'disabled'} class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <option value="">Select organization</option>
                                <c:forEach var="organization" items="${organizationOptions}">
                                    <option value="${organization.id}" ${form.organizationId == organization.id or selectedOrganizationId == organization.id ? 'selected' : ''}>
                                        <c:out value="${organization.name}"/>
                                    </option>
                                </c:forEach>
                            </select>
                            <c:if test="${not creating}">
                                <input type="hidden" name="organizationId" value="${form.organizationId}">
                            </c:if>
                        </div>
                        <div class="col-lg-6 gape-select-field">
                            <label for="state" class="fw-medium text-base text-neutral-800 mb-12">State</label>
                            <select id="state" name="state" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <option value="ACTIVE" ${form.state == 'ACTIVE' ? 'selected' : ''}>Active</option>
                                <option value="INACTIVE" ${form.state == 'INACTIVE' ? 'selected' : ''}>Inactive</option>
                            </select>
                        </div>
                        <c:if test="${creating}">
                            <div class="col-lg-6 gape-select-field">
                                <label for="initialCourseIds" class="fw-medium text-base text-neutral-800 mb-12">Initial Courses</label>
                                <select id="initialCourseIds" name="initialCourseIds" multiple required data-dependent-select data-parent-select="#organizationId" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                    <c:forEach var="course" items="${courseOptions}">
                                        <option value="${course.id}" data-parent-value="${course.organizationId}" title="<c:out value='${course.courseManagementContextTitle}'/>" ${form.isInitialCourseSelected(course.id) ? 'selected' : ''}>
                                            <c:out value="${course.name}"/> | <c:out value="${course.courseManagementContextLabel}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-lg-2">
                                <label for="initialCurricularYear" class="fw-medium text-base text-neutral-800 mb-12">Year</label>
                                <input id="initialCurricularYear" name="initialCurricularYear" type="number" min="1" value="<c:out value='${form.initialCurricularYear}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                            </div>
                            <div class="col-lg-4 gape-select-field">
                                <label for="initialTerm" class="fw-medium text-base text-neutral-800 mb-12">Period</label>
                                <select id="initialTerm" name="initialTerm" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                    <option value="" ${empty form.initialTerm ? 'selected' : ''}>No period</option>
                                    <option value="ANNUAL" ${form.initialTerm == 'ANNUAL' ? 'selected' : ''}>Annual</option>
                                    <option value="SEMESTER_1" ${form.initialTerm == 'SEMESTER_1' ? 'selected' : ''}>1st semester</option>
                                    <option value="SEMESTER_2" ${form.initialTerm == 'SEMESTER_2' ? 'selected' : ''}>2nd semester</option>
                                    <option value="TRIMESTER_1" ${form.initialTerm == 'TRIMESTER_1' ? 'selected' : ''}>1st trimester</option>
                                    <option value="TRIMESTER_2" ${form.initialTerm == 'TRIMESTER_2' ? 'selected' : ''}>2nd trimester</option>
                                    <option value="TRIMESTER_3" ${form.initialTerm == 'TRIMESTER_3' ? 'selected' : ''}>3rd trimester</option>
                                </select>
                            </div>
                            <div class="col-lg-6 d-flex align-items-end">
                                <div class="form-check common-check">
                                    <input class="form-check-input" type="checkbox" id="initialMandatory" name="initialMandatory" value="true" ${form.initialMandatory ? 'checked' : ''}>
                                    <label class="form-check-label fw-medium" for="initialMandatory">Mandatory subject</label>
                                </div>
                            </div>
                        </c:if>
                        <div class="col-lg-7">
                            <label for="name" class="fw-medium text-base text-neutral-800 mb-12">Name</label>
                            <input id="name" name="name" type="text" value="<c:out value='${form.name}'/>" required pattern="[^|]*" title="Names cannot contain |" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-2">
                            <label for="acronym" class="fw-medium text-base text-neutral-800 mb-12">Acronym</label>
                            <input id="acronym" name="acronym" type="text" value="<c:out value='${form.acronym}'/>" required pattern="[^|]*" title="Acronyms cannot contain |" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-3">
                            <label for="ects" class="fw-medium text-base text-neutral-800 mb-12">ECTS</label>
                            <input id="ects" name="ects" type="number" min="0" step="0.01" value="<c:out value='${form.ects}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-4">
                            <label for="workloadHours" class="fw-medium text-base text-neutral-800 mb-12">Workload Hours</label>
                            <input id="workloadHours" name="workloadHours" type="number" min="0" value="<c:out value='${form.workloadHours}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <c:if test="${creating}">
                            <div class="col-lg-8 gape-select-field">
                                <label for="coordinatorUserId" class="fw-medium text-base text-neutral-800 mb-12">Initial Coordinator</label>
                                <select id="coordinatorUserId" name="coordinatorUserId" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                    <option value="">No coordinator</option>
                                    <c:forEach var="coordinator" items="${coordinatorOptions}">
                                        <option value="${coordinator.id}" ${form.coordinatorUserId == coordinator.id ? 'selected' : ''}>
                                            <c:out value="${coordinator.name}"/> - <c:out value="${coordinator.email}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                        </c:if>
                        <div class="col-12">
                            <label for="description" class="fw-medium text-base text-neutral-800 mb-12">Description</label>
                            <textarea id="description" name="description" rows="4" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600"><c:out value="${form.description}"/></textarea>
                        </div>
                    </div>

                    <div class="d-flex align-items-center gap-16 flex-wrap mt-32">
                        <button type="submit" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Save Change</button>
                        <a href="${subjectBackHref}" class="border-main-600 border px-24 py-12 fw-semibold rounded-12 hover-bg-main-50 transition-03">Cancel</a>
                    </div>
                </form>

                <c:if test="${not creating}">
                    <div class="bg-white rounded-10 px-40 py-40 mt-24">
                        <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                            <div>
                                <h3 class="text-18 fw-medium text-neutral-700 mb-4">Coordinator Assignment</h3>
                                <span class="text-14 text-neutral-500">Assign an active coordinator to this subject.</span>
                            </div>
                        </div>
                        <form action="${pageContext.request.contextPath}/admin/subjects/${form.id}/assign-coordinator" method="post">
                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                            <div class="row gy-4">
                                <div class="col-lg-6 gape-select-field">
                                    <label for="editCoordinatorUserId" class="fw-medium text-base text-neutral-800 mb-12">Coordinator</label>
                                    <select id="editCoordinatorUserId" name="coordinatorUserId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                        <option value="">Select coordinator</option>
                                        <c:forEach var="coordinator" items="${coordinatorOptions}">
                                            <option value="${coordinator.id}"><c:out value="${coordinator.name}"/> - <c:out value="${coordinator.email}"/></option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div class="col-lg-2">
                                    <label for="editStartDate" class="fw-medium text-base text-neutral-800 mb-12">Start Date</label>
                                    <input id="editStartDate" name="startDate" type="date" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                </div>
                                <div class="col-lg-2">
                                    <label for="editEndDate" class="fw-medium text-base text-neutral-800 mb-12">End Date</label>
                                    <input id="editEndDate" name="endDate" type="date" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                </div>
                                <div class="col-lg-2 d-flex align-items-end justify-content-lg-end">
                                    <button type="submit" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Assign</button>
                                </div>
                            </div>
                        </form>
                    </div>

                    <div class="bg-white rounded-10 px-40 py-40 mt-24">
                        <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                            <div>
                                <h3 class="text-18 fw-medium text-neutral-700 mb-4">Course Associations</h3>
                                <span class="text-14 text-neutral-500">Courses linked to this subject.</span>
                            </div>
                        </div>

                        <div class="overflow-x-auto mb-24">
                            <table class="table mb-0">
                                <thead>
                                <tr>
                                    <th class="py-14 px-16 text-14 fw-medium text-neutral-600">Course</th>
                                    <th class="py-14 px-16 text-14 fw-medium text-neutral-600">Position</th>
                                    <th class="py-14 px-16 text-14 fw-medium text-neutral-600">Requirement</th>
                                    <th class="py-14 px-16 text-14 fw-medium text-neutral-600">State</th>
                                    <th class="py-14 px-16 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="association" items="${subjectCourseAssociations}">
                                    <tr class="border-bottom">
                                        <td class="py-16 px-16">
                                            <a href="${pageContext.request.contextPath}/admin/courses/${association.courseId}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                <c:out value="${association.courseName}"/>
                                            </a>
                                            <span class="d-block text-12 text-neutral-500" title="<c:out value='${association.courseContextTitle}'/>"><c:out value="${association.courseContextHtml}" escapeXml="false"/></span>
                                        </td>
                                        <td class="py-16 px-16 text-14 text-neutral-500"><c:out value="${association.curricularPositionLabel}"/></td>
                                        <td class="py-16 px-16 text-14 text-neutral-500"><c:out value="${association.mandatoryLabel}"/></td>
                                        <td class="py-16 px-16">
                                            <span class="${association.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-14">
                                                <c:out value="${association.stateLabel}"/>
                                            </span>
                                        </td>
                                        <td class="py-16 px-16">
                                            <div class="d-flex align-items-center justify-content-end">
                                                <c:choose>
                                                    <c:when test="${not association.archived}">
                                                        <form action="${pageContext.request.contextPath}/admin/subjects/${form.id}/courses/${association.courseId}/delete" method="post" class="m-0">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                            <button type="submit" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Remove">
                                                                <i class="ph ph-trash"></i>
                                                            </button>
                                                        </form>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="text-14 text-neutral-500">-</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty subjectCourseAssociations}">
                                    <tr>
                                        <td colspan="5" class="py-24 px-16 text-center text-14 text-neutral-500">No associated courses found.</td>
                                    </tr>
                                </c:if>
                                </tbody>
                            </table>
                        </div>

                        <form action="${pageContext.request.contextPath}/admin/subjects/${form.id}/courses" method="post" class="border border-neutral-30 rounded-12 px-20 py-20">
                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                            <div class="row gy-4">
                                <div class="col-lg-6 gape-select-field">
                                    <label for="courseId" class="fw-medium text-base text-neutral-800 mb-12">Course</label>
                                    <select id="courseId" name="courseId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                        <option value="">Select course</option>
                                        <c:forEach var="course" items="${availableCourseOptions}">
                                            <option value="${course.id}" title="<c:out value='${course.courseManagementContextTitle}'/>">
                                                <c:out value="${course.name}"/> | <c:out value="${course.courseManagementContextLabel}"/>
                                            </option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div class="col-lg-2">
                                    <label for="curricularYear" class="fw-medium text-base text-neutral-800 mb-12">Year</label>
                                    <input id="curricularYear" name="curricularYear" type="number" min="1" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                </div>
                                <div class="col-lg-4 gape-select-field">
                                    <label for="term" class="fw-medium text-base text-neutral-800 mb-12">Period</label>
                                    <select id="term" name="term" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                        <option value="">No period</option>
                                        <option value="ANNUAL">Annual</option>
                                        <option value="SEMESTER_1">1st semester</option>
                                        <option value="SEMESTER_2">2nd semester</option>
                                        <option value="TRIMESTER_1">1st trimester</option>
                                        <option value="TRIMESTER_2">2nd trimester</option>
                                        <option value="TRIMESTER_3">3rd trimester</option>
                                    </select>
                                </div>
                                <div class="col-lg-8 d-flex align-items-end">
                                    <div class="form-check common-check">
                                        <input class="form-check-input" type="checkbox" id="mandatory" name="mandatory" value="true" checked>
                                        <label class="form-check-label fw-medium" for="mandatory">Mandatory subject in this course</label>
                                    </div>
                                </div>
                                <div class="col-lg-4 d-flex align-items-end justify-content-lg-end">
                                    <button type="submit" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Add Course</button>
                                </div>
                            </div>
                        </form>
                    </div>
                </c:if>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script>
    (function () {
        const imageInput = document.getElementById('subjectImageUpload');
        const imagePreview = document.getElementById('subjectImagePreview');
        const cancelButton = document.getElementById('cancelSubjectImage');
        if (!imageInput || !imagePreview || !cancelButton) {
            return;
        }

        function placeholderIcon() {
            return imagePreview.querySelector('[data-photo-placeholder-icon]');
        }

        function showPreviewImage(url) {
            imagePreview.style.backgroundImage = "url('" + url + "')";
            imagePreview.classList.add('is-image');
            const icon = placeholderIcon();
            if (icon) {
                icon.classList.add('d-none');
            }
        }

        function showPreviewPlaceholder() {
            imagePreview.style.backgroundImage = '';
            imagePreview.classList.remove('is-image');
            const icon = placeholderIcon();
            if (icon) {
                icon.classList.remove('d-none');
            }
        }

        function restoreCurrentImage() {
            const currentImage = imagePreview.dataset.currentImage;
            if (imagePreview.dataset.hasCurrentImage === 'true' && currentImage) {
                showPreviewImage(currentImage);
                return;
            }
            showPreviewPlaceholder();
        }

        imageInput.addEventListener('change', function () {
            const file = imageInput.files && imageInput.files[0];
            if (!file) {
                return;
            }
            const reader = new FileReader();
            reader.onload = function (event) {
                showPreviewImage(event.target.result);
            };
            reader.readAsDataURL(file);
        });

        cancelButton.addEventListener('click', function () {
            imageInput.value = '';
            restoreCurrentImage();
        });
    })();
</script>
</body>
</html>
