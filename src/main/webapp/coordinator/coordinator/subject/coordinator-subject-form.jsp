<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "subjects");
    }
%>
<c:if test="${empty subjectBasePath}">
    <c:set var="subjectBasePath" value="/coordinator/subjects"/>
</c:if>
<c:if test="${empty subjectCourseBasePath}">
    <c:set var="subjectCourseBasePath" value="/courses"/>
</c:if>
<c:set var="subjectBackHref" value="${pageContext.request.contextPath}${subjectBasePath}"/>
<c:if test="${not creating}">
    <c:set var="subjectBackHref" value="${pageContext.request.contextPath}${subjectBasePath}/${form.id}"/>
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
                <form action="${formAction}" method="post" enctype="multipart/form-data" class="bg-white rounded-10 px-40 py-40" data-subject-context-form>
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <input type="hidden" id="photo" name="photo" value="<c:out value='${form.photo}'/>">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">${creating ? 'Create Subject' : 'Edit Subject'}</h2>
                            <span class="text-14 text-neutral-500">Subject and organic unit must belong to the same organization.</span>
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
                        <div class="col-lg-6 gape-select-field gape-course-context-field" data-subject-organization-context>
                            <label for="organizationId" class="fw-medium text-base text-neutral-800 mb-12">Organization</label>
                            <select id="organizationId" name="organizationId" required ${creating ? '' : 'disabled'} class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-subject-organization>
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
                        <div class="col-lg-6 gape-select-field gape-course-context-field${selectedOrganizationId == 0 ? ' opacity-75 is-disabled' : ''}" data-subject-organic-unit-context>
                            <label for="organicUnitId" class="fw-medium text-base text-neutral-800 mb-12">Organic Unit</label>
                            <select id="organicUnitId" name="organicUnitId" ${selectedOrganizationId == 0 ? 'disabled' : ''} class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-subject-organic-unit>
                                <option value="">No organic unit</option>
                                <c:forEach var="unit" items="${organicUnitOptions}">
                                    <option value="${unit.id}" data-organization-id="${unit.organizationId}" ${selectedOrganizationId != unit.organizationId ? 'hidden disabled' : ''} ${form.organicUnitId == unit.id ? 'selected' : ''}>
                                        <c:out value="${unit.code}"/> - <c:out value="${unit.name}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-lg-6 gape-select-field">
                            <label for="state" class="fw-medium text-base text-neutral-800 mb-12">State</label>
                            <select id="state" name="state" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <option value="ACTIVE" ${form.state == 'ACTIVE' ? 'selected' : ''}>Active</option>
                                <option value="INACTIVE" ${form.state == 'INACTIVE' ? 'selected' : ''}>Inactive</option>
                            </select>
                        </div>
                        <div class="col-lg-7">
                            <label for="name" class="fw-medium text-base text-neutral-800 mb-12">Name</label>
                            <input id="name" name="name" type="text" value="<c:out value='${form.name}'/>" required pattern="[^\|]*" title="Names cannot contain |" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-2">
                            <label for="acronym" class="fw-medium text-base text-neutral-800 mb-12">Acronym</label>
                            <input id="acronym" name="acronym" type="text" value="<c:out value='${form.acronym}'/>" required pattern="[^\|]*" title="Acronyms cannot contain |" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-3">
                            <label for="ects" class="fw-medium text-base text-neutral-800 mb-12">ECTS</label>
                            <input id="ects" name="ects" type="number" min="0.01" step="0.01" required value="<c:out value='${form.ects}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-3">
                            <label for="finalGradeMax" class="fw-medium text-base text-neutral-800 mb-12">Max final grade</label>
                            <input id="finalGradeMax" name="finalGradeMax" type="number" min="0.01" step="0.01" required value="<c:out value='${form.finalGradeMax}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-3">
                            <label for="workloadHours" class="fw-medium text-base text-neutral-800 mb-12">Workload Hours</label>
                            <input id="workloadHours" name="workloadHours" type="number" min="0" value="<c:out value='${form.workloadHours}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
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

            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script src="${pageContext.request.contextPath}/assets/js/gape-course-context.js?v=20260714-subject-context"></script>
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
