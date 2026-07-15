<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "organizations");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Organizations</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-action-button {
            align-items: center;
            border: 1px solid transparent;
            cursor: pointer;
            display: inline-flex;
            justify-content: center;
            line-height: 1.25;
            text-decoration: none;
        }

        .gape-action-button:hover,
        .gape-action-button:focus-visible {
            text-decoration: none;
            transform: translateY(-1px);
        }

        .gape-action-delete {
            background-color: #dc2626 !important;
            border-color: #dc2626 !important;
            color: #fff !important;
        }

        .gape-organization-table-photo {
            border-radius: 12px;
            height: 44px;
            object-fit: cover;
            width: 44px;
        }

        .gape-hierarchy-node {
            border-inline-start: 3px solid var(--main-600);
        }

        .gape-organization-card {
            background-color: #fff;
            border: 1px solid #d9e2ef;
            border-radius: 10px;
            overflow: hidden;
            transition: border-color 0.2s ease, box-shadow 0.2s ease;
        }

        .gape-organization-card:hover {
            background-color: #f8fbff;
            border-color: #b8c7dc;
        }

        .gape-organization-summary {
            display: grid;
            gap: 16px;
            grid-template-columns: minmax(280px, 1.4fr) minmax(150px, 0.7fr) minmax(90px, 0.4fr) minmax(120px, 0.5fr) minmax(150px, auto);
            padding: 20px;
        }

        .gape-organization-list-header {
            display: grid;
            gap: 16px;
            grid-template-columns: minmax(280px, 1.4fr) minmax(150px, 0.7fr) minmax(90px, 0.4fr) minmax(120px, 0.5fr) minmax(150px, auto);
            padding: 0 20px 12px;
        }

        .gape-tree-toggle {
            align-items: center;
            background: transparent;
            border: 0;
            border-radius: 8px;
            cursor: pointer;
            display: inline-flex;
            height: 32px;
            justify-content: center;
            padding: 0;
            width: 32px;
        }

        .gape-tree-toggle:hover,
        .gape-tree-toggle:focus-visible {
            background-color: var(--main-50);
            text-decoration: none;
        }

        .gape-tree-toggle i {
            transition: transform 0.2s ease;
        }

        .gape-tree-toggle[aria-expanded="true"] i {
            transform: rotate(180deg);
        }

        .gape-organization-units-panel {
            background-color: #f8fbff;
            border-top: 1px solid #d9e2ef;
            padding: 20px;
        }

        .gape-filter-toggle.is-active {
            background-color: var(--main-600) !important;
            border-color: var(--main-600) !important;
            color: #fff !important;
        }

        .gape-filter-toggle.is-active:hover,
        .gape-filter-toggle.is-active:focus-visible {
            background-color: var(--main-700) !important;
            border-color: var(--main-700) !important;
            color: #fff !important;
        }

        .gape-filter-toggle.is-active i {
            color: inherit;
        }

        .gape-sort-option {
            background: transparent;
            border: 0;
            text-align: start;
            width: 100%;
        }

        .gape-sort-option.is-active {
            background-color: var(--main-50);
            color: var(--main-600);
        }

        .gape-sort-arrows {
            min-width: 42px;
        }

        .gape-sort-arrow {
            color: #94a3b8;
            opacity: 0.55;
            transition: color 0.2s ease, opacity 0.2s ease;
        }

        .gape-sort-option[data-sort-state="normal"] .gape-sort-arrow--normal,
        .gape-sort-option[data-sort-state="reverse"] .gape-sort-arrow--reverse {
            color: var(--main-600);
            opacity: 1;
        }

        @media (max-width: 1199.98px) {
            .gape-organization-summary,
            .gape-organization-list-header {
                grid-template-columns: 1fr;
            }

            .gape-organization-list-header {
                display: none;
            }
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
                <div class="row gy-4 mb-24">
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Total</span>
                            <h2 class="text-32 fw-semibold text-neutral-700 mb-0">${organizationCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Active</span>
                            <h2 class="text-32 fw-semibold text-success-600 mb-0">${activeOrganizations}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Inactive</span>
                            <h2 class="text-32 fw-semibold text-danger-600 mb-0">${inactiveOrganizations}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Organic Units</span>
                            <h2 class="text-32 fw-semibold text-main-600 mb-0">${unitTotal}</h2>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Organization Management</h2>
                            <span class="text-14 text-neutral-500">Organizations, institutions, hierarchy and administrator context.</span>
                        </div>
                        <div class="d-flex align-items-center gap-12 flex-wrap">
                            <div class="dropdown">
                                <button type="button"
                                        class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                        data-bs-toggle="dropdown"
                                        data-bs-auto-close="outside"
                                        data-organization-sort-toggle
                                        aria-expanded="false">
                                    <i class="ph ph-sort-ascending"></i>Sort by
                                </button>
                                <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                    <li>
                                        <button type="button"
                                                class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10"
                                                data-organization-sort-option
                                                data-sort-field="name"
                                                data-sort-normal="asc"
                                                data-sort-state="none"
                                                aria-pressed="false">
                                            <span>Name</span>
                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true">
                                                <i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i>
                                                <i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i>
                                            </span>
                                        </button>
                                    </li>
                                    <li>
                                        <button type="button"
                                                class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10"
                                                data-organization-sort-option
                                                data-sort-field="date"
                                                data-sort-normal="desc"
                                                data-sort-state="none"
                                                aria-pressed="false">
                                            <span>Date</span>
                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true">
                                                <i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--normal"></i>
                                                <i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--reverse"></i>
                                            </span>
                                        </button>
                                    </li>
                                    <li>
                                        <button type="button"
                                                class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10"
                                                data-organization-sort-option
                                                data-sort-field="status"
                                                data-sort-normal="asc"
                                                data-sort-state="none"
                                                aria-pressed="false">
                                            <span>State</span>
                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true">
                                                <i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i>
                                                <i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i>
                                            </span>
                                        </button>
                                    </li>
                                </ul>
                            </div>
                            <c:if test="${canCreateOrganizations}">
                                <a href="${pageContext.request.contextPath}/admin/organizations/new" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                    <i class="ph ph-plus-circle me-8"></i>New Organization
                                </a>
                            </c:if>
                        </div>
                    </div>
                    <div class="gape-organization-list">
                        <div class="gape-organization-list-header">
                            <span class="text-14 fw-medium text-neutral-600">Organization</span>
                            <span class="text-14 fw-medium text-neutral-600">Type</span>
                            <span class="text-14 fw-medium text-neutral-600">Units</span>
                            <span class="text-14 fw-medium text-neutral-600">State</span>
                            <span class="text-14 fw-medium text-neutral-600 text-end">Actions</span>
                        </div>
                        <div class="d-flex flex-column gap-14" data-organization-list>
                            <c:forEach var="organization" items="${organizations}" varStatus="organizationLoop">
                                <c:set var="canCreateOrganicUnits" value="${canCreateOrganicUnitsByOrganizationId[organization.id]}" />
                                <c:set var="organizationPhotoUrl" value=""/>
                                <c:if test="${organization.hasPhoto}">
                                    <c:set var="organizationPhotoUrl" value="${pageContext.request.contextPath}/media/${organization.photo}?v=${mediaCacheVersion}"/>
                                </c:if>
                                <section class="gape-organization-card"
                                         data-organization-card
                                         data-sort-index="${organizationLoop.index}"
                                         data-sort-name="${fn:escapeXml(organization.name)}"
                                         data-sort-date="${organization.id}"
                                         data-sort-status="${fn:escapeXml(organization.stateLabel)}"
                                         data-sort-type="${fn:escapeXml(organization.typeLabel)}">
                                    <div class="gape-organization-summary">
                                        <div class="d-flex align-items-center gap-12">
                                            <c:choose>
                                                <c:when test="${organization.hasPhoto}">
                                                    <img src="${organizationPhotoUrl}"
                                                         alt=""
                                                         class="gape-organization-table-photo flex-shrink-0"
                                                         onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table d-none" aria-label="No organization photo">
                                                        <i class="ph ph-image"></i>
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table" aria-label="No organization photo">
                                                        <i class="ph ph-image"></i>
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                            <div>
                                                <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                    <c:out value="${organization.name}"/>
                                                </a>
                                                <span class="d-block text-12 text-neutral-500">
                                                    <span class="gape-acronym-token" tabindex="0" title="<c:out value='${organization.name}'/>"><c:out value="${organization.acronym}"/></span>
                                                </span>
                                            </div>
                                        </div>
                                        <div class="text-14 text-neutral-500 d-flex align-items-center"><c:out value="${organization.typeLabel}"/></div>
                                        <div class="text-14 text-neutral-500 d-flex align-items-center"><c:out value="${organization.organicUnitCount}"/></div>
                                        <div class="d-flex align-items-center">
                                            <span class="${organization.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                                <c:out value="${organization.stateLabel}"/>
                                            </span>
                                        </div>
                                        <div class="d-flex align-items-center gap-12 justify-content-end">
                                            <button type="button"
                                                    class="gape-tree-toggle text-22 text-neutral-500 hover-text-main-600"
                                                    title="Show organic units"
                                                    aria-label="Show organic units"
                                                    aria-expanded="false"
                                                    aria-controls="organizationUnits${organization.id}"
                                                    data-gape-tree-toggle="organizationUnits${organization.id}"
                                                    data-gape-open-title="Hide organic units"
                                                    data-gape-closed-title="Show organic units">
                                                <i class="ph ph-caret-down"></i>
                                            </button>
                                            <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                                                <i class="ph ph-eye"></i>
                                            </a>
                                            <c:if test="${canModifyOrganizations}">
                                                <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}/edit" class="text-22 text-neutral-500 hover-text-main-600" title="Edit">
                                                    <i class="ph ph-pencil-simple-line"></i>
                                                </a>
                                            </c:if>
                                            <c:if test="${canModifyOrganizations}">
                                                <button type="button" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteOrganization${organization.id}">
                                                    <i class="ph ph-trash"></i>
                                                </button>
                                            </c:if>
                                        </div>
                                    </div>

                                    <div id="organizationUnits${organization.id}" class="gape-organization-units-panel d-none">
                                        <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-16">
                                            <div>
                                                <h3 class="text-16 fw-medium text-neutral-700 mb-4">Organic Units</h3>
                                                <span class="text-13 text-neutral-500">
                                                    <c:out value="${organization.name}"/> &middot; <c:out value="${organization.organicUnitCount}"/> units
                                                </span>
                                            </div>
                                            <c:if test="${canCreateOrganicUnits}">
                                                <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}?unitModal=create" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                                    <i class="ph ph-plus-circle me-8"></i>New Unit
                                                </a>
                                            </c:if>
                                        </div>

                                        <div class="d-flex flex-column gap-12">
                                            <c:forEach var="unit" items="${organization.organicUnits}">
                                                <c:set var="canModifyUnit" value="${canModifyOrganicUnitById[unit.id]}" />
                                                <div class="gape-hierarchy-node border border-neutral-30 rounded-12 px-20 py-16 bg-white" style="margin-left: ${unit.hierarchyIndent}px;">
                                                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap">
                                                        <div class="d-flex align-items-start gap-12">
                                                            <span class="text-22 text-main-600 line-height-1"><i class="ph ph-tree-structure"></i></span>
                                                            <div>
                                                                <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}?unitDetail=${unit.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                                    <c:out value="${unit.code}"/> - <c:out value="${unit.name}"/>
                                                                </a>
                                                                <span class="d-block text-12 text-neutral-500">Parent: <c:out value="${unit.parentLabel}"/></span>
                                                            </div>
                                                        </div>
                                                        <div class="d-flex align-items-center gap-12 flex-wrap">
                                                            <span class="text-13 text-neutral-500"><c:out value="${unit.typeLabel}"/></span>
                                                            <span class="${unit.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13">
                                                                <c:out value="${unit.stateLabel}"/>
                                                            </span>
                                                            <c:if test="${canCreateOrganicUnits}">
                                                                <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}?unitModal=create&amp;parentUnitId=${unit.id}" class="text-21 text-neutral-500 hover-text-main-600" title="New child unit" aria-label="New child unit">
                                                                    <i class="ph ph-plus-circle"></i>
                                                                </a>
                                                            </c:if>
                                                            <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}?unitDetail=${unit.id}" class="text-21 text-neutral-500 hover-text-main-600" title="Detail" aria-label="Detail">
                                                                <i class="ph ph-eye"></i>
                                                            </a>
                                                            <c:if test="${canModifyUnit}">
                                                                <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}?unitEdit=${unit.id}" class="text-21 text-neutral-500 hover-text-main-600" title="Edit" aria-label="Edit">
                                                                    <i class="ph ph-pencil-simple-line"></i>
                                                                </a>
                                                                <button type="button" class="text-21 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" aria-label="Delete" data-bs-toggle="modal" data-bs-target="#deleteListUnit${organization.id}_${unit.id}">
                                                                    <i class="ph ph-trash"></i>
                                                                </button>
                                                            </c:if>
                                                        </div>
                                                    </div>
                                                </div>

                                                <c:if test="${canModifyUnit}">
                                                    <div class="modal fade" id="deleteListUnit${organization.id}_${unit.id}" tabindex="-1" aria-hidden="true">
                                                        <div class="modal-dialog modal-dialog-centered">
                                                            <div class="modal-content rounded-12 border-0">
                                                                <div class="modal-header border-neutral-30">
                                                                    <h5 class="modal-title text-18 fw-semibold">Delete Organic Unit</h5>
                                                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                                </div>
                                                                <div class="modal-body">
                                                                    <p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${unit.name}"/></strong> if it has no dependencies.</p>
                                                                </div>
                                                                <div class="modal-footer border-neutral-30">
                                                                    <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                                    <form action="${pageContext.request.contextPath}/admin/organizations/${organization.id}/units/${unit.id}/delete" method="post" class="m-0">
                                                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                        <button type="submit" class="gape-action-button gape-action-delete px-20 py-10 rounded-12 fw-semibold transition-03">Delete</button>
                                                                    </form>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </c:if>
                                            </c:forEach>
                                            <c:if test="${empty organization.organicUnits}">
                                                <div class="border border-neutral-30 rounded-8 px-18 py-24 text-center text-13 text-neutral-500 bg-white">
                                                    No organic units found for this organization.
                                                </div>
                                            </c:if>
                                        </div>
                                    </div>

                                    <c:if test="${canModifyOrganizations}">
                                        <div class="modal fade" id="deleteOrganization${organization.id}" tabindex="-1" aria-hidden="true">
                                            <div class="modal-dialog modal-dialog-centered">
                                                <div class="modal-content rounded-12 border-0">
                                                    <div class="modal-header border-neutral-30">
                                                        <h5 class="modal-title text-18 fw-semibold">Delete Organization</h5>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                    </div>
                                                    <div class="modal-body">
                                                        <p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${organization.name}"/></strong> if it has no dependencies.</p>
                                                    </div>
                                                    <div class="modal-footer border-neutral-30">
                                                        <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                        <form action="${pageContext.request.contextPath}/admin/organizations/${organization.id}/delete" method="post" class="m-0">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                            <button type="submit" class="gape-action-button gape-action-delete px-20 py-10 rounded-12 fw-semibold transition-03">Delete</button>
                                                        </form>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </c:if>
                                </section>
                            </c:forEach>
                            <c:if test="${empty organizations}">
                                <div class="border border-neutral-30 rounded-12 px-20 py-32 text-center text-14 text-neutral-500 bg-white">
                                    No managed organizations found.
                                </div>
                            </c:if>
                        </div>
                    </div>
                </div>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script>
    (function () {
        document.querySelectorAll('[data-gape-tree-toggle]').forEach(function (toggle) {
            var panel = document.getElementById(toggle.dataset.gapeTreeToggle);
            if (!panel) {
                return;
            }
            toggle.addEventListener('click', function () {
                var expanded = toggle.getAttribute('aria-expanded') === 'true';
                toggle.setAttribute('aria-expanded', String(!expanded));
                panel.classList.toggle('d-none', expanded);
                var nextTitle = expanded ? toggle.dataset.gapeClosedTitle : toggle.dataset.gapeOpenTitle;
                if (nextTitle) {
                    toggle.setAttribute('title', nextTitle);
                    toggle.setAttribute('aria-label', nextTitle);
                }
            });
        });
    })();

    (function () {
        var list = document.querySelector('[data-organization-list]');
        if (!list) {
            return;
        }

        var cards = Array.prototype.slice.call(list.querySelectorAll('[data-organization-card]'));
        var options = Array.prototype.slice.call(document.querySelectorAll('[data-organization-sort-option]'));
        var sortToggle = document.querySelector('[data-organization-sort-toggle]');
        if (!cards.length || !options.length) {
            return;
        }

        var collator = new Intl.Collator(document.documentElement.lang || undefined, {
            numeric: true,
            sensitivity: 'base'
        });
        var activeField = null;
        var activeDirection = null;

        function oppositeDirection(direction) {
            return direction === 'asc' ? 'desc' : 'asc';
        }

        function datasetKey(field) {
            return 'sort' + field.charAt(0).toUpperCase() + field.slice(1);
        }

        function numericValue(card, field) {
            return Number(card.dataset[datasetKey(field)] || '0');
        }

        function textValue(card, field) {
            return card.dataset[datasetKey(field)] || '';
        }

        function originalIndex(card) {
            return numericValue(card, 'index');
        }

        function compareCards(field, direction, first, second) {
            var result;
            if (field === 'date') {
                result = numericValue(first, field) - numericValue(second, field);
            } else {
                result = collator.compare(textValue(first, field), textValue(second, field));
            }

            if (result === 0) {
                result = originalIndex(first) - originalIndex(second);
            }

            return direction === 'desc' ? -result : result;
        }

        function renderCards() {
            var sorted = cards.slice();
            if (activeField && activeDirection) {
                sorted.sort(function (first, second) {
                    return compareCards(activeField, activeDirection, first, second);
                });
            } else {
                sorted.sort(function (first, second) {
                    return originalIndex(first) - originalIndex(second);
                });
            }
            sorted.forEach(function (card) {
                list.appendChild(card);
            });
        }

        function updateOptionStates() {
            options.forEach(function (option) {
                var field = option.dataset.sortField;
                var normalDirection = option.dataset.sortNormal;
                var state = 'none';
                if (activeField === field) {
                    state = activeDirection === normalDirection ? 'normal' : 'reverse';
                }

                option.dataset.sortState = state;
                option.classList.toggle('is-active', state !== 'none');
                option.setAttribute('aria-pressed', String(state !== 'none'));
            });
            if (sortToggle) {
                sortToggle.classList.toggle('is-active', activeField !== null);
            }
        }

        function closeDropdown(toggle) {
            if (!toggle) {
                return;
            }
            if (window.bootstrap && window.bootstrap.Dropdown) {
                window.bootstrap.Dropdown.getOrCreateInstance(toggle).hide();
            }
        }

        if (sortToggle) {
            sortToggle.addEventListener('click', function (event) {
                if (!activeField) {
                    return;
                }
                event.preventDefault();
                event.stopPropagation();
                activeField = null;
                activeDirection = null;
                updateOptionStates();
                renderCards();
                closeDropdown(sortToggle);
            });
        }

        options.forEach(function (option) {
            option.addEventListener('click', function () {
                var field = option.dataset.sortField;
                var normalDirection = option.dataset.sortNormal;
                var currentState = option.dataset.sortState;

                if (activeField !== field || currentState === 'none') {
                    activeField = field;
                    activeDirection = normalDirection;
                } else if (currentState === 'normal') {
                    activeDirection = oppositeDirection(normalDirection);
                } else {
                    activeField = null;
                    activeDirection = null;
                }

                updateOptionStates();
                renderCards();
            });
        });

        updateOptionStates();
    })();
</script>
</body>
</html>
