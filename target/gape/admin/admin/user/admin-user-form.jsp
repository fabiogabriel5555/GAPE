<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "users");
    }
%>
<c:set var="profileHasPhoto" value="${not empty form.photo}"/>
<c:set var="profilePhotoUrl" value=""/>
<c:if test="${profileHasPhoto}">
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

        .gape-user-form .gape-password-field label {
            display: block;
        }

        .gape-user-form .gape-password-field .form-control {
            min-height: 58px;
        }

        .gape-user-form .gape-document-field {
            display: flex;
            flex-direction: column;
        }

        .gape-user-form .gape-document-field label {
            display: block;
            line-height: 1.5;
            min-height: 24px;
        }

        .gape-user-form .gape-document-field .form-control,
        .gape-user-form .gape-document-field .select2-container--default .select2-selection--single {
            min-height: 58px !important;
        }

        .gape-user-form .gape-document-field .select2-container--default .select2-selection--single .select2-selection__rendered {
            padding-block: 14px !important;
            padding-inline-start: 24px !important;
        }

        .gape-initial-password-rule .rule-indicator {
            background-color: #94a3b8;
        }

        .gape-initial-password-rule.is-valid .rule-indicator {
            background-color: #16a34a;
        }

        .gape-initial-password-rule.is-valid .rule-text {
            color: #15803d !important;
        }

        .gape-initial-password-rule.is-invalid .rule-indicator {
            background-color: #dc2626;
        }

        .gape-initial-password-rule.is-invalid .rule-text {
            color: #b91c1c !important;
        }

        .gape-user-form .avatar-edit input[type="file"] {
            height: 1px;
            opacity: 0;
            overflow: hidden;
            position: absolute;
            width: 1px;
        }

        .gape-admin-permission-card {
            min-height: 112px;
        }

        .gape-admin-permission-tree-card {
            border-inline-start: 3px solid var(--main-600) !important;
        }

        .gape-admin-permission-context[data-disabled="true"] {
            opacity: .55;
            pointer-events: none;
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

                    <div class="border-bottom-dashed pb-24 mb-24">
                        <h4 class="text-18 fw-normal text-neutral-700 mb-16">Profile Photo</h4>
                        <div class="avatar-upload">
                            <div class="d-flex align-items-center gap-40 flex-wrap">
                                <div class="avatar-preview flex-shrink-0">
                                    <c:choose>
                                        <c:when test="${profileHasPhoto}">
                                            <div id="imagePreview"
                                                 class="gape-user-photo-preview gape-photo-placeholder gape-photo-placeholder--user gape-photo-placeholder--user-form is-image"
                                                 data-current-image="<c:out value='${profilePhotoUrl}'/>"
                                                 data-has-current-image="true"
                                                 style="background-image: url('<c:out value='${profilePhotoUrl}'/>');">
                                                <i class="ph ph-user-circle d-none" data-photo-placeholder-icon></i>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div id="imagePreview"
                                                 class="gape-user-photo-preview gape-photo-placeholder gape-photo-placeholder--user gape-photo-placeholder--user-form"
                                                 data-current-image=""
                                                 data-has-current-image="false">
                                                <i class="ph ph-user-circle" data-photo-placeholder-icon></i>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
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
                            <div class="col-lg-6 gape-password-field">
                                <label for="password" class="fw-medium text-base text-neutral-800 mb-12">Initial Password</label>
                                <div class="position-relative">
                                    <input id="password"
                                           name="password"
                                           type="password"
                                           required
                                           minlength="8"
                                           pattern="(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}"
                                           autocomplete="new-password"
                                           aria-describedby="initialPasswordValidationMessage"
                                           class="form-control ps-24 pe-60 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                    <span class="toggle-password text-16 position-absolute inset-inline-end-3-percent inset-block-start-18-px ph-bold ph-eye-closed" id="#password"></span>
                                </div>
                                <ul id="initialPasswordRules" class="list-unstyled mt-12 mb-0 ps-0">
                                    <li class="gape-initial-password-rule d-flex align-items-center gap-12 mb-8" data-rule="length">
                                        <span class="rule-indicator w-6 h-6 rounded-circle"></span>
                                        <span class="rule-text fw-normal text-13 text-neutral-700">At least 8 characters</span>
                                    </li>
                                    <li class="gape-initial-password-rule d-flex align-items-center gap-12 mb-8" data-rule="lower">
                                        <span class="rule-indicator w-6 h-6 rounded-circle"></span>
                                        <span class="rule-text fw-normal text-13 text-neutral-700">At least 1 lower letter (a-z)</span>
                                    </li>
                                    <li class="gape-initial-password-rule d-flex align-items-center gap-12 mb-8" data-rule="upper">
                                        <span class="rule-indicator w-6 h-6 rounded-circle"></span>
                                        <span class="rule-text fw-normal text-13 text-neutral-700">At least 1 uppercase letter (A-Z)</span>
                                    </li>
                                    <li class="gape-initial-password-rule d-flex align-items-center gap-12 mb-8" data-rule="number">
                                        <span class="rule-indicator w-6 h-6 rounded-circle"></span>
                                        <span class="rule-text fw-normal text-13 text-neutral-700">At least 1 number (0-9)</span>
                                    </li>
                                    <li class="gape-initial-password-rule d-flex align-items-center gap-12 mb-0" data-rule="special">
                                        <span class="rule-indicator w-6 h-6 rounded-circle"></span>
                                        <span class="rule-text fw-normal text-13 text-neutral-700">At least 1 special character</span>
                                    </li>
                                </ul>
                                <p id="initialPasswordValidationMessage" class="text-13 text-danger-600 mt-8 mb-0 d-none"></p>
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
                        <div class="col-lg-3 gape-select-field gape-document-field">
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
                        <div class="col-lg-3 gape-document-field">
                            <label for="documentNumber" class="fw-medium text-base text-neutral-800 mb-12">Document Number</label>
                            <input id="documentNumber" name="documentNumber" type="text" value="<c:out value='${form.documentNumber}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                    </div>

                    <div class="mt-32">
                        <h3 class="text-16 fw-medium text-neutral-700 mb-16">Access Profiles</h3>
                        <div class="row gy-4">
                            <div class="col-xl-3 col-lg-3 col-md-6">
                                <div class="border border-neutral-30 rounded-12 px-20 py-20 h-100">
                                    <div class="form-check common-check">
                                        <input class="form-check-input" type="checkbox" id="administratorProfile" name="administratorProfile" ${form.administratorProfile ? 'checked' : ''}>
                                        <label class="form-check-label fw-medium" for="administratorProfile">Administrator</label>
                                    </div>
                                </div>
                            </div>
                            <div class="col-xl-3 col-lg-3 col-md-6">
                                <div class="border border-neutral-30 rounded-12 px-20 py-20 h-100">
                                    <div class="form-check common-check">
                                        <input class="form-check-input" type="checkbox" id="coordinatorProfile" name="coordinatorProfile" ${form.coordinatorProfile ? 'checked' : ''}>
                                        <label class="form-check-label fw-medium" for="coordinatorProfile">Coordinator</label>
                                    </div>
                                </div>
                            </div>
                            <div class="col-xl-3 col-lg-3 col-md-6">
                                <div class="border border-neutral-30 rounded-12 px-20 py-20 h-100">
                                    <div class="form-check common-check">
                                        <input class="form-check-input" type="checkbox" id="teacherProfile" name="teacherProfile" ${form.teacherProfile ? 'checked' : ''}>
                                        <label class="form-check-label fw-medium" for="teacherProfile">Teacher</label>
                                    </div>
                                </div>
                            </div>
                            <div class="col-xl-3 col-lg-3 col-md-6">
                                <div class="border border-neutral-30 rounded-12 px-20 py-20 h-100">
                                    <div class="form-check common-check">
                                        <input class="form-check-input" type="checkbox" id="studentProfile" name="studentProfile" ${form.studentProfile ? 'checked' : ''}>
                                        <label class="form-check-label fw-medium" for="studentProfile">Student</label>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="mt-24">
                        <c:if test="${not empty teacherContextOptions}">
                            <div class="gape-profile-context mt-24 ${form.teacherProfile ? '' : 'd-none'}"
                                 data-profile-context-section
                                 data-profile-type="TEACHER">
                                <h4 class="text-16 fw-medium text-neutral-700 mb-16">Teacher Context</h4>
                                <div class="d-flex flex-column gap-12">
                                    <c:forEach var="option" items="${teacherContextOptions}">
                                        <div style="margin-left: ${option.hierarchyIndent}px;">
                                            <div class="border border-neutral-30 rounded-12 px-20 py-16 bg-neutral-10">
                                                <c:choose>
                                                    <c:when test="${option.selectable}">
                                                        <div class="form-check common-check">
                                                            <input class="form-check-input"
                                                                   type="checkbox"
                                                                   id="${option.elementId}"
                                                                   name="profileContextAssignments"
                                                                   value="${option.value}"
                                                                   data-profile-context-input
                                                                   data-profile-type="${option.profileType}"
                                                                   ${form.hasProfileContextAssignment(option.profileType, option.contextType, option.contextId, option.parentContextIdValue) ? 'checked' : ''}>
                                                            <label class="form-check-label fw-medium" for="${option.elementId}">
                                                                <c:out value="${option.label}"/>
                                                                <span class="d-block text-12 text-neutral-500"><c:out value="${option.detail}"/></span>
                                                            </label>
                                                        </div>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="fw-medium text-14 text-neutral-700"><c:out value="${option.label}"/></span>
                                                        <span class="d-block text-12 text-neutral-500"><c:out value="${option.detail}"/></span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </div>
                        </c:if>

                    </div>

                    <div class="mt-32 ${form.administratorProfile ? '' : 'd-none'}"
                         id="administratorPermissionAssignments"
                         data-admin-permission-wrapper>
                        <h3 class="text-16 fw-medium text-neutral-700 mb-16">Administrator Permissions</h3>
                        <div class="row gy-4">
                            <div class="col-lg-3 col-md-6">
                                <div class="gape-admin-permission-card border border-neutral-30 rounded-12 px-20 py-20 h-100" data-admin-permission-card>
                                    <div class="form-check common-check">
                                        <input class="form-check-input"
                                               type="checkbox"
                                               id="adminManageAll"
                                               name="adminPermissionAssignments"
                                               value="${manageAllPermissionCode}:GLOBAL:0"
                                               data-admin-permission-input
                                               data-admin-permission-choice
                                               ${form.hasAdminPermissionAssignment(manageAllPermissionCode, 'GLOBAL', 0) ? 'checked' : ''}>
                                        <label class="form-check-label fw-medium" for="adminManageAll">
                                            Full Access
                                            <span class="d-block text-11 text-neutral-400">MANAGE_ALL</span>
                                            <span class="d-block text-12 text-neutral-500">Full system access</span>
                                        </label>
                                    </div>
                                </div>
                            </div>
                            <div class="col-lg-3 col-md-6">
                                <div class="gape-admin-permission-card border border-neutral-30 rounded-12 px-20 py-20 h-100" data-admin-permission-card>
                                    <div class="form-check common-check">
                                        <input class="form-check-input"
                                               type="checkbox"
                                               id="adminManageOrganizationStructure"
                                               data-admin-permission-toggle
                                               data-admin-permission-choice
                                               data-admin-permission-section="${manageOrganizationStructurePermissionCode}"
                                               ${form.hasAdminPermissionCode(manageOrganizationStructurePermissionCode) ? 'checked' : ''}>
                                        <label class="form-check-label fw-medium" for="adminManageOrganizationStructure">
                                            Organization Structure
                                            <span class="d-block text-11 text-neutral-400">MANAGE_ORGANIZATION_STRUCTURE</span>
                                            <span class="d-block text-12 text-neutral-500">Organizations and units</span>
                                        </label>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <c:if test="${not empty organizationStructureContextOptions}">
                            <div class="gape-admin-permission-context mt-24 ${form.hasAdminPermissionCode(manageOrganizationStructurePermissionCode) ? '' : 'd-none'}"
                                 data-admin-permission-context-section
                                 data-admin-permission-section="${manageOrganizationStructurePermissionCode}">
                                <h4 class="text-16 fw-medium text-neutral-700 mb-16">Organization Structure Context</h4>
                                <div class="d-flex flex-column gap-12">
                                    <c:forEach var="option" items="${organizationStructureContextOptions}">
                                        <div data-admin-permission-tree-item
                                             data-permission-section="${manageOrganizationStructurePermissionCode}"
                                             data-tree-node-key="${option.nodeKey}"
                                             data-tree-parent-key="${option.parentKey}"
                                             style="margin-left: ${option.hierarchyIndent}px;">
                                            <div class="gape-admin-permission-tree-card border border-neutral-30 rounded-12 px-20 py-16 bg-neutral-10" data-admin-permission-card>
                                                <div class="form-check common-check">
                                                    <input class="form-check-input"
                                                           type="checkbox"
                                                           id="${option.elementId}"
                                                           name="adminPermissionAssignments"
                                                           value="${option.value}"
                                                           data-admin-permission-input
                                                           data-admin-permission-context
                                                           data-permission-section="${manageOrganizationStructurePermissionCode}"
                                                           data-tree-node-key="${option.nodeKey}"
                                                           data-tree-parent-key="${option.parentKey}"
                                                           ${form.hasAdminPermissionAssignment(option.permissionCode, option.contextType, option.contextId) ? 'checked' : ''}>
                                                    <label class="form-check-label fw-medium" for="${option.elementId}">
                                                        <c:out value="${option.label}"/>
                                                        <span class="d-block text-12 text-neutral-500"><c:out value="${option.detail}"/></span>
                                                    </label>
                                                </div>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </div>
                        </c:if>

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
        const userForm = document.getElementById('userForm');
        const initialPassword = document.getElementById('password');
        const passwordValidationMessage = document.getElementById('initialPasswordValidationMessage');
        const passwordRules = document.querySelectorAll('#initialPasswordRules [data-rule]');
        const administratorProfile = document.getElementById('administratorProfile');
        const coordinatorProfile = document.getElementById('coordinatorProfile');
        const teacherProfile = document.getElementById('teacherProfile');
        const studentProfile = document.getElementById('studentProfile');
        const profileToggles = [administratorProfile, coordinatorProfile, teacherProfile, studentProfile].filter(Boolean);
        const profileContextSections = document.querySelectorAll('[data-profile-context-section]');
        const adminPermissionWrapper = document.querySelector('[data-admin-permission-wrapper]');
        const adminPermissionToggles = document.querySelectorAll('[data-admin-permission-toggle]');
        const adminPermissionChoices = document.querySelectorAll('[data-admin-permission-choice]');
        const adminPermissionControls = document.querySelectorAll('[data-admin-permission-input], [data-admin-permission-toggle]');
        const adminPermissionContextInputs = document.querySelectorAll('[data-admin-permission-context]');
        const adminPermissionContextSections = document.querySelectorAll('[data-admin-permission-context-section]');
        const adminPermissionTreeItems = document.querySelectorAll('[data-admin-permission-tree-item]');

        function placeholderIcon() {
            return imagePreview ? imagePreview.querySelector('[data-photo-placeholder-icon]') : null;
        }

        function showPreviewImage(url) {
            if (!imagePreview || !url) {
                return;
            }
            imagePreview.style.backgroundImage = "url('" + url + "')";
            imagePreview.classList.add('is-image');
            const icon = placeholderIcon();
            if (icon) {
                icon.classList.add('d-none');
            }
        }

        function showPreviewPlaceholder() {
            if (!imagePreview) {
                return;
            }
            imagePreview.style.backgroundImage = '';
            imagePreview.classList.remove('is-image');
            const icon = placeholderIcon();
            if (icon) {
                icon.classList.remove('d-none');
            }
        }

        function restoreCurrentPhoto() {
            if (!imagePreview) {
                return;
            }
            const currentImage = imagePreview.dataset.currentImage;
            if (imagePreview.dataset.hasCurrentImage === 'true' && currentImage) {
                showPreviewImage(currentImage);
                return;
            }
            showPreviewPlaceholder();
        }

        function passwordChecks(value) {
            return {
                length: value.length >= 8,
                lower: /[a-z]/.test(value),
                upper: /[A-Z]/.test(value),
                number: /[0-9]/.test(value),
                special: /[^A-Za-z0-9]/.test(value)
            };
        }

        function setPasswordMessage(message) {
            if (!passwordValidationMessage) {
                return;
            }
            passwordValidationMessage.textContent = message || '';
            passwordValidationMessage.classList.toggle('d-none', !message);
        }

        function syncInitialPasswordValidation() {
            if (!initialPassword) {
                return true;
            }
            const value = initialPassword.value;
            const started = value.length > 0;
            const checks = passwordChecks(value);
            const rulesValid = Object.keys(checks).every(function (rule) {
                return checks[rule];
            });

            passwordRules.forEach(function (item) {
                const valid = checks[item.dataset.rule] === true;
                item.classList.toggle('is-valid', started && valid);
                item.classList.toggle('is-invalid', started && !valid);
            });

            if (!started) {
                initialPassword.setCustomValidity('');
                setPasswordMessage('');
                return false;
            }
            if (!rulesValid) {
                initialPassword.setCustomValidity('The initial password does not meet all requirements.');
                setPasswordMessage('The initial password does not meet all requirements.');
                return false;
            }
            initialPassword.setCustomValidity('');
            setPasswordMessage('');
            return true;
        }

        function syncAdminPermissionInputs() {
            if (!administratorProfile || adminPermissionControls.length === 0) {
                return;
            }
            normalizeAdminPermissionChoices();
            const enabled = administratorProfile.checked;
            if (adminPermissionWrapper) {
                adminPermissionWrapper.classList.toggle('d-none', !enabled);
            }
            adminPermissionControls.forEach(function (control) {
                control.disabled = !enabled;
                const card = control.closest('[data-admin-permission-card]');
                if (card) {
                    card.classList.toggle('opacity-50', !enabled);
                }
            });
            syncAdminPermissionSections(enabled);
            syncAdminPermissionTree();
        }

        function profileToggle(profileType) {
            if (profileType === 'COORDINATOR') {
                return coordinatorProfile;
            }
            if (profileType === 'TEACHER') {
                return teacherProfile;
            }
            if (profileType === 'STUDENT') {
                return studentProfile;
            }
            return null;
        }

        function syncProfileContextSections() {
            profileContextSections.forEach(function (section) {
                const profileType = section.dataset.profileType;
                const toggle = profileToggle(profileType);
                const enabled = toggle && toggle.checked;
                section.classList.toggle('d-none', !enabled);
                section.querySelectorAll('[data-profile-context-input]').forEach(function (input) {
                    input.disabled = !enabled;
                    if (!enabled) {
                        input.checked = false;
                    }
                });
            });
        }

        function permissionCodeForChoice(choice) {
            if (choice.dataset.adminPermissionSection) {
                return choice.dataset.adminPermissionSection;
            }
            const value = choice.value || '';
            return value.split(':')[0] || '';
        }

        function clearPermissionContext(permissionCode) {
            if (!permissionCode) {
                return;
            }
            sectionContextInputs(permissionCode).forEach(function (input) {
                input.checked = false;
                input.indeterminate = false;
            });
        }

        function normalizeAdminPermissionChoices() {
            let selectedChoice = null;
            adminPermissionChoices.forEach(function (choice) {
                if (!choice.checked) {
                    return;
                }
                if (selectedChoice === null) {
                    selectedChoice = choice;
                    return;
                }
                choice.checked = false;
                clearPermissionContext(permissionCodeForChoice(choice));
            });
        }

        function selectSingleAdminPermission(choice) {
            if (!choice.checked) {
                clearPermissionContext(permissionCodeForChoice(choice));
                return;
            }
            adminPermissionChoices.forEach(function (candidate) {
                if (candidate === choice) {
                    return;
                }
                candidate.checked = false;
                clearPermissionContext(permissionCodeForChoice(candidate));
            });
        }

        function sectionToggle(permissionCode) {
            return Array.from(adminPermissionToggles).find(function (toggle) {
                return toggle.dataset.adminPermissionSection === permissionCode;
            }) || null;
        }

        function sectionContextInputs(permissionCode) {
            return Array.from(adminPermissionContextInputs).filter(function (input) {
                return input.dataset.permissionSection === permissionCode;
            });
        }

        function childrenOf(input) {
            const permissionCode = input.dataset.permissionSection;
            const nodeKey = input.dataset.treeNodeKey;
            return sectionContextInputs(permissionCode).filter(function (candidate) {
                return candidate.dataset.treeParentKey === nodeKey;
            });
        }

        function parentOf(input) {
            const permissionCode = input.dataset.permissionSection;
            const parentKey = input.dataset.treeParentKey;
            if (!parentKey) {
                return null;
            }
            return sectionContextInputs(permissionCode).find(function (candidate) {
                return candidate.dataset.treeNodeKey === parentKey;
            }) || null;
        }

        function treeModeFor(input) {
            const section = input.closest('[data-admin-permission-context-section]');
            return section ? section.dataset.treeMode || '' : '';
        }

        function subtreeHasSelection(input) {
            if (input.checked || input.indeterminate) {
                return true;
            }
            return childrenOf(input).some(subtreeHasSelection);
        }

        function setDescendantsChecked(input, checked) {
            childrenOf(input).forEach(function (child) {
                child.checked = checked;
                child.indeterminate = false;
                setDescendantsChecked(child, checked);
            });
        }

        function syncAncestors(input) {
            const parent = parentOf(input);
            if (!parent) {
                return;
            }
            const children = childrenOf(parent);
            const allChecked = children.length > 0 && children.every(function (child) {
                return child.checked && !child.indeterminate;
            });
            const partiallyChecked = children.some(subtreeHasSelection);
            parent.checked = allChecked;
            parent.indeterminate = !allChecked && partiallyChecked;
            syncAncestors(parent);
        }

        function expandCheckedContextParents() {
            adminPermissionContextInputs.forEach(function (input) {
                if (treeModeFor(input) === 'free') {
                    return;
                }
                if (input.checked) {
                    setDescendantsChecked(input, true);
                }
            });
        }

        function syncAllAncestors() {
            adminPermissionContextInputs.forEach(function (input) {
                if (treeModeFor(input) === 'free') {
                    return;
                }
                syncAncestors(input);
            });
        }

        function contextNodeIsVisible(input) {
            const parent = parentOf(input);
            if (!parent) {
                return true;
            }
            return parent.checked || parent.indeterminate || input.checked || subtreeHasSelection(input);
        }

        function syncAdminPermissionSections(adminEnabled) {
            adminPermissionContextSections.forEach(function (section) {
                const permissionCode = section.dataset.adminPermissionSection;
                const toggle = sectionToggle(permissionCode);
                const sectionEnabled = adminEnabled && toggle && toggle.checked;
                section.classList.toggle('d-none', !sectionEnabled);
                section.dataset.disabled = sectionEnabled ? 'false' : 'true';
                sectionContextInputs(permissionCode).forEach(function (input) {
                    input.disabled = !sectionEnabled;
                });
            });
        }

        function syncAdminPermissionTree() {
            syncAllAncestors();
            adminPermissionTreeItems.forEach(function (item) {
                const input = item.querySelector('[data-admin-permission-context]');
                if (!input) {
                    return;
                }
                const section = input.closest('[data-admin-permission-context-section]');
                const sectionEnabled = section && section.dataset.disabled !== 'true' && !section.classList.contains('d-none');
                const freeTree = section && section.dataset.treeMode === 'free';
                const visible = sectionEnabled && (freeTree || contextNodeIsVisible(input));
                item.classList.toggle('d-none', !visible);
                input.disabled = !visible || !sectionEnabled;
                const card = input.closest('[data-admin-permission-card]');
                if (card) {
                    card.classList.toggle('opacity-50', !sectionEnabled);
                }
            });
        }

        if (imageInput && imagePreview && cancelButton) {
            imageInput.addEventListener('change', function () {
                const file = imageInput.files && imageInput.files[0];
                if (!file) {
                    restoreCurrentPhoto();
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
                restoreCurrentPhoto();
            });
        }

        if (initialPassword) {
            initialPassword.addEventListener('input', syncInitialPasswordValidation);
            syncInitialPasswordValidation();
        }

        profileToggles.forEach(function (toggle) {
            toggle.addEventListener('change', syncProfileContextSections);
        });
        syncProfileContextSections();

        adminPermissionChoices.forEach(function (choice) {
            choice.addEventListener('change', function () {
                selectSingleAdminPermission(choice);
                syncAdminPermissionInputs();
            });
        });

        adminPermissionContextInputs.forEach(function (input) {
            input.addEventListener('change', function () {
                if (treeModeFor(input) === 'free') {
                    syncAdminPermissionInputs();
                    return;
                }
                input.indeterminate = false;
                setDescendantsChecked(input, input.checked);
                syncAncestors(input);
                syncAdminPermissionInputs();
            });
        });

        if (administratorProfile && adminPermissionControls.length > 0) {
            administratorProfile.addEventListener('change', syncAdminPermissionInputs);
            expandCheckedContextParents();
            syncAdminPermissionInputs();
        }

        if (userForm) {
            userForm.addEventListener('submit', function (event) {
                syncInitialPasswordValidation();
                if (!userForm.checkValidity()) {
                    event.preventDefault();
                    userForm.reportValidity();
                }
            });
        }
    })();
</script>
</body>
</html>
