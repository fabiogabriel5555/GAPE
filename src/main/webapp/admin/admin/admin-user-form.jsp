<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "users");
    }
%>
<c:set var="profilePhotoUrl" value="${pageContext.request.contextPath}/assets/images/thumbs/student-dashbord-profile-photo-img1.png"/>
<c:if test="${not empty form.photo}">
    <c:set var="profilePhotoUrl" value="${pageContext.request.contextPath}/media/${form.photo}"/>
</c:if>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - User</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-user-photo-preview {
            background-position: center center;
            background-repeat: no-repeat;
            background-size: cover;
            border-radius: 50%;
            height: 88px;
            width: 88px;
        }

        .gape-user-form .avatar-edit input[type="file"] {
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
                <form id="userForm" action="${formAction}" method="post" enctype="multipart/form-data" class="gape-user-form bg-white rounded-10 px-40 py-40">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <input type="hidden" id="photo" name="photo" value="<c:out value='${form.photo}'/>">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">${creating ? 'Create User' : 'Edit User'}</h2>
                            <span class="text-14 text-neutral-500">Email and document data are validated before saving.</span>
                        </div>
                        <a href="${pageContext.request.contextPath}/admin/users" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                    </div>

                    <c:if test="${not creating}">
                        <div class="border-bottom-dashed pb-24 mb-24">
                            <h4 class="text-18 fw-normal text-neutral-700 mb-16">Profile Photo</h4>
                            <div class="avatar-upload">
                                <div class="d-flex align-items-center gap-40 flex-wrap">
                                    <div class="avatar-preview flex-shrink-0">
                                        <div id="imagePreview"
                                             class="gape-user-photo-preview"
                                             data-current-image="<c:out value='${profilePhotoUrl}'/>"
                                             style="background-image: url('<c:out value='${profilePhotoUrl}'/>');">
                                        </div>
                                    </div>
                                    <div class="avatar-edit">
                                        <input type="file" id="imageUpload" name="profileImage" accept="image/*">
                                    </div>
                                    <div class="d-flex align-items-center gap-16 flex-wrap">
                                        <label for="imageUpload" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-16 text-white transition-04 hover-bg-main-700">Upload Image</label>
                                        <button type="button" id="cancelProfileImage" class="border-main-600 border px-24 py-12 rounded-12 fw-semibold text-16 text-main-600 hover-bg-main-50 transition-04">Cancel</button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </c:if>

                    <div class="row gy-4">
                        <div class="col-lg-6">
                            <label for="name" class="fw-medium text-base text-neutral-800 mb-12">Name</label>
                            <input id="name" name="name" type="text" value="<c:out value='${form.name}'/>" required class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-6">
                            <label for="email" class="fw-medium text-base text-neutral-800 mb-12">Email</label>
                            <input id="email" name="email" type="email" value="<c:out value='${form.email}'/>" required class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <c:if test="${creating}">
                            <div class="col-lg-6">
                                <label for="password" class="fw-medium text-base text-neutral-800 mb-12">Initial Password</label>
                                <input id="password" name="password" type="password" required class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                            </div>
                        </c:if>
                        <div class="col-lg-3 gape-select-field">
                            <label for="state" class="fw-medium text-base text-neutral-800 mb-12">State</label>
                            <select id="state" name="state" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <option value="ACTIVE" ${form.state == 'ACTIVE' ? 'selected' : ''}>Active</option>
                                <option value="INACTIVE" ${form.state == 'INACTIVE' ? 'selected' : ''}>Inactive</option>
                                <option value="BLOCKED" ${form.state == 'BLOCKED' ? 'selected' : ''}>Blocked</option>
                            </select>
                        </div>
                        <div class="col-lg-3 gape-select-field">
                            <label for="language" class="fw-medium text-base text-neutral-800 mb-12">Language</label>
                            <select id="language" name="language" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <c:forEach var="languageOption" items="${languageOptions}">
                                    <option value="${languageOption.code}" ${form.language == languageOption.code ? 'selected' : ''}>
                                        <c:out value="${languageOption.label}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-lg-3 gape-select-field">
                            <label for="documentType" class="fw-medium text-base text-neutral-800 mb-12">Document Type</label>
                            <select id="documentType" name="documentType" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <option value="" ${empty form.documentType ? 'selected' : ''}>Select document type</option>
                                <c:forEach var="documentTypeOption" items="${documentTypeOptions}">
                                    <option value="${documentTypeOption.code}" ${form.documentType == documentTypeOption.code ? 'selected' : ''}>
                                        <c:out value="${documentTypeOption.label}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-lg-3">
                            <label for="documentNumber" class="fw-medium text-base text-neutral-800 mb-12">Document Number</label>
                            <input id="documentNumber" name="documentNumber" type="text" value="<c:out value='${form.documentNumber}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                    </div>

                    <div class="mt-32">
                        <h3 class="text-16 fw-medium text-neutral-700 mb-16">Access Profiles</h3>
                        <div class="row gy-4">
                            <div class="col-lg-3 col-md-6">
                                <div class="border border-neutral-30 rounded-12 px-20 py-20 h-100">
                                    <div class="form-check common-check">
                                        <input class="form-check-input" type="checkbox" id="administratorProfile" name="administratorProfile" ${form.administratorProfile ? 'checked' : ''}>
                                        <label class="form-check-label fw-medium" for="administratorProfile">Administrator</label>
                                    </div>
                                </div>
                            </div>
                            <div class="col-lg-3 col-md-6">
                                <div class="border border-neutral-30 rounded-12 px-20 py-20 h-100">
                                    <div class="form-check common-check">
                                        <input class="form-check-input" type="checkbox" id="coordinatorProfile" name="coordinatorProfile" ${form.coordinatorProfile ? 'checked' : ''}>
                                        <label class="form-check-label fw-medium" for="coordinatorProfile">Coordinator</label>
                                    </div>
                                </div>
                            </div>
                            <div class="col-lg-3 col-md-6">
                                <div class="border border-neutral-30 rounded-12 px-20 py-20 h-100">
                                    <div class="form-check common-check">
                                        <input class="form-check-input" type="checkbox" id="teacherProfile" name="teacherProfile" ${form.teacherProfile ? 'checked' : ''}>
                                        <label class="form-check-label fw-medium" for="teacherProfile">Teacher</label>
                                    </div>
                                </div>
                            </div>
                            <div class="col-lg-3 col-md-6">
                                <div class="border border-neutral-30 rounded-12 px-20 py-20 h-100">
                                    <div class="form-check common-check">
                                        <input class="form-check-input" type="checkbox" id="studentProfile" name="studentProfile" ${form.studentProfile ? 'checked' : ''}>
                                        <label class="form-check-label fw-medium" for="studentProfile">Student</label>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="d-flex align-items-center gap-16 flex-wrap mt-32">
                        <button type="submit" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                            Save Change
                        </button>
                        <a href="${pageContext.request.contextPath}/admin/users" class="border-main-600 border px-24 py-12 fw-semibold rounded-12 hover-bg-main-50 transition-03">Cancel</a>
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
        const imageInput = document.getElementById('imageUpload');
        const imagePreview = document.getElementById('imagePreview');
        const cancelButton = document.getElementById('cancelProfileImage');
        if (!imageInput || !imagePreview || !cancelButton) {
            return;
        }

        const currentImage = imagePreview.dataset.currentImage;
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
