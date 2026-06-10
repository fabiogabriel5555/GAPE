<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "organizations");
    }
%>
<c:set var="organizationBackHref" value="${pageContext.request.contextPath}/admin/organizations"/>
<c:if test="${not creating}">
    <c:set var="organizationBackHref" value="${pageContext.request.contextPath}/admin/organizations/${form.id}"/>
</c:if>
<c:set var="organizationPhotoUrl" value="${pageContext.request.contextPath}/assets/images/thumbs/student-dashbord-profile-photo-img1.png"/>
<c:if test="${not empty form.photo}">
    <c:set var="organizationPhotoUrl" value="${pageContext.request.contextPath}/media/${form.photo}?v=${mediaCacheVersion}"/>
</c:if>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Organization</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-organization-photo-preview {
            background-position: center center;
            background-repeat: no-repeat;
            background-size: cover;
            border-radius: 16px;
            height: 88px;
            width: 88px;
        }

        .gape-organization-form .avatar-edit input[type="file"] {
            height: 1px;
            opacity: 0;
            overflow: hidden;
            position: absolute;
            width: 1px;
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
                <form id="organizationForm" action="${formAction}" method="post" enctype="multipart/form-data" class="gape-organization-form bg-white rounded-10 px-40 py-40">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <input type="hidden" id="photo" name="photo" value="<c:out value='${form.photo}'/>">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">${creating ? 'Create Organization' : 'Edit Organization'}</h2>
                            <span class="text-14 text-neutral-500">Active organizations require at least one active administrator.</span>
                        </div>
                        <a href="${organizationBackHref}" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                    </div>

                    <div class="border-bottom-dashed pb-24 mb-24">
                        <h4 class="text-18 fw-normal text-neutral-700 mb-16">Organization Photo</h4>
                        <div class="avatar-upload">
                            <div class="d-flex align-items-center gap-40 flex-wrap">
                                <div class="avatar-preview flex-shrink-0">
                                    <div id="organizationImagePreview"
                                         class="gape-organization-photo-preview"
                                         data-current-image="<c:out value='${organizationPhotoUrl}'/>"
                                         data-fallback-image="${pageContext.request.contextPath}/assets/images/thumbs/student-dashbord-profile-photo-img1.png"
                                         style="background-image: url('<c:out value='${organizationPhotoUrl}'/>');">
                                    </div>
                                </div>
                                <div class="avatar-edit">
                                    <input type="file" id="organizationImageUpload" name="organizationImage" accept="image/jpeg,image/png,image/gif,image/bmp,image/webp">
                                </div>
                                <div class="d-flex align-items-center gap-16 flex-wrap">
                                    <label for="organizationImageUpload" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-16 text-white transition-04 hover-bg-main-700">Upload Image</label>
                                    <button type="button" id="cancelOrganizationImage" class="border-main-600 border px-24 py-12 rounded-12 fw-semibold text-16 text-main-600 hover-bg-main-50 transition-04">Cancel</button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="row gy-4">
                        <div class="col-lg-6">
                            <label for="name" class="fw-medium text-base text-neutral-800 mb-12">Name</label>
                            <input id="name" name="name" type="text" value="<c:out value='${form.name}'/>" required class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-3">
                            <label for="acronym" class="fw-medium text-base text-neutral-800 mb-12">Acronym</label>
                            <input id="acronym" name="acronym" type="text" value="<c:out value='${form.acronym}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-3 gape-select-field">
                            <label for="state" class="fw-medium text-base text-neutral-800 mb-12">State</label>
                            <select id="state" name="state" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <option value="ACTIVE" ${form.state == 'ACTIVE' ? 'selected' : ''}>Active</option>
                                <option value="INACTIVE" ${form.state == 'INACTIVE' ? 'selected' : ''}>Inactive</option>
                            </select>
                        </div>
                        <div class="col-lg-6 gape-select-field">
                            <label for="type" class="fw-medium text-base text-neutral-800 mb-12">Type</label>
                            <select id="type" name="type" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <option value="EDUCATIONAL_INSTITUTION" ${form.type == 'EDUCATIONAL_INSTITUTION' ? 'selected' : ''}>Educational institution</option>
                                <option value="TRAINING_COMPANY" ${form.type == 'TRAINING_COMPANY' ? 'selected' : ''}>Training company</option>
                                <option value="COMPANY" ${form.type == 'COMPANY' ? 'selected' : ''}>Company</option>
                                <option value="OTHER" ${form.type == 'OTHER' ? 'selected' : ''}>Other</option>
                            </select>
                        </div>
                    </div>

                    <c:if test="${creating}">
                        <div class="mt-32">
                            <h3 class="text-16 fw-medium text-neutral-700 mb-16">Organization Administrators</h3>
                            <div class="row gy-4">
                                <c:forEach var="administrator" items="${administratorOptions}">
                                    <div class="col-lg-4 col-md-6">
                                        <div class="border border-neutral-30 rounded-12 px-20 py-20 h-100">
                                            <div class="form-check common-check">
                                                <input class="form-check-input" type="checkbox" id="administrator${administrator.id}" name="administratorUserIds" value="${administrator.id}" ${administrator.selected ? 'checked' : ''}>
                                                <label class="form-check-label fw-medium" for="administrator${administrator.id}">
                                                    <c:out value="${administrator.name}"/>
                                                    <span class="d-block text-12 text-neutral-500"><c:out value="${administrator.email}"/></span>
                                                </label>
                                            </div>
                                        </div>
                                    </div>
                                </c:forEach>
                                <c:if test="${empty administratorOptions}">
                                    <div class="col-12">
                                        <div class="alert alert-warning rounded-12 border-0 mb-0" role="alert">No active administrators are available for assignment.</div>
                                    </div>
                                </c:if>
                            </div>
                        </div>
                    </c:if>

                    <div class="d-flex align-items-center gap-16 flex-wrap mt-32">
                        <button type="submit" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Save Change</button>
                        <a href="${organizationBackHref}" class="border-main-600 border px-24 py-12 fw-semibold rounded-12 hover-bg-main-50 transition-03">Cancel</a>
                    </div>
                </form>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script>
    (function () {
        const imageInput = document.getElementById('organizationImageUpload');
        const imagePreview = document.getElementById('organizationImagePreview');
        const cancelButton = document.getElementById('cancelOrganizationImage');
        if (!imageInput || !imagePreview || !cancelButton) {
            return;
        }

        const currentImage = imagePreview.dataset.currentImage;
        const fallbackImage = imagePreview.dataset.fallbackImage;
        const preloadCurrentImage = new Image();
        preloadCurrentImage.onerror = function () {
            imagePreview.style.backgroundImage = "url('" + fallbackImage + "')";
        };
        preloadCurrentImage.src = currentImage;

        imageInput.addEventListener('change', function () {
            const file = imageInput.files && imageInput.files[0];
            if (!file) {
                return;
            }
            const reader = new FileReader();
            reader.onload = function (event) {
                imagePreview.style.backgroundImage = "url('" + event.target.result + "')";
            };
            reader.readAsDataURL(file);
        });

        cancelButton.addEventListener('click', function () {
            imageInput.value = '';
            imagePreview.style.backgroundImage = "url('" + currentImage + "')";
        });
    })();
</script>
</body>
</html>
