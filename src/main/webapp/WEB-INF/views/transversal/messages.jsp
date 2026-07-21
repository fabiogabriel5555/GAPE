<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="studentMessagesPage" value="${sessionScope['gape.auth.hasStudentProfile']
        and not sessionScope['gape.auth.hasAdministratorProfile']
        and not sessionScope['gape.auth.hasCoordinatorProfile']
        and not sessionScope['gape.auth.hasTeacherProfile']}"/>
<c:set var="studentName" value="${sessionScope['gape.auth.userName']}"/>
<c:set var="studentId" value="${sessionScope['gape.auth.userId']}"/>
<c:set var="studentEmail" value="${sessionScope['gape.auth.userEmail']}"/>
<c:set var="studentFirstName" value="Student"/>
<c:set var="studentLastName" value="Student"/>
<c:if test="${not empty studentName}">
    <c:set var="studentNameParts" value="${fn:split(fn:trim(studentName), ' ')}"/>
    <c:set var="studentFirstName" value="${studentNameParts[0]}"/>
    <c:set var="studentLastName" value="${studentNameParts[fn:length(studentNameParts) - 1]}"/>
</c:if>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Messages</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        html:has(.messages-student-shell),
        body:has(.messages-student-shell) {
            background: var(--neutral-900);
            overscroll-behavior-y: none;
        }

        .messages-student-hero-band {
            min-height: 97px;
            padding-bottom: 47px !important;
            padding-top: 20px !important;
            z-index: 0 !important;
        }

        .messages-student-hero-band .shape {
            pointer-events: none;
        }

        .messages-student-hero-band .shape.one {
            inset-block-start: 12%;
            inset-inline-start: clamp(132px, 13vw, 214px);
        }

        .messages-student-shell .gape-student-dashboard-layout {
            align-items: flex-start;
            margin-top: -38px !important;
        }

        .messages-student-shell .gape-student-dashboard-main {
            min-width: 0;
            padding-top: 0;
        }

        .messages-student-shell .gape-student-sidebar-avatar {
            align-items: center;
            background: var(--main-25);
            border-radius: 50%;
            color: var(--main-600);
            display: inline-flex;
            height: 112px;
            justify-content: center;
            margin: 0 auto 20px;
            overflow: hidden;
            width: 112px;
        }

        .messages-student-shell .gape-student-sidebar-avatar img {
            height: 100%;
            object-fit: cover;
            width: 100%;
        }

        .messages-student-shell .student-dashboard-sidebar ul li:not(.activePage) > a {
            background: transparent !important;
            color: var(--neutral-500) !important;
        }

        .messages-student-shell .student-dashboard-sidebar ul li:not(.activePage) > a i {
            color: var(--main-600) !important;
        }

        .messages-student-shell .student-dashboard-sidebar ul li:not(.activePage) > a:hover {
            background: var(--main-600) !important;
            color: #fff !important;
        }

        .messages-student-shell .student-dashboard-sidebar ul li:not(.activePage) > a:hover i {
            color: #fff !important;
        }

        .messages-student-shell .gape-student-page-heading {
            align-items: flex-start;
            display: flex;
            margin-block: 0 5px !important;
            min-height: 48px;
            padding-block-start: 3px;
        }

        .messages-student-shell .gape-student-page-heading h6 {
            color: #fff;
            font-size: 22px !important;
            line-height: 1.1;
            margin-bottom: 0;
        }

        .messages-student-shell .gape-student-mobile-menu-bar {
            position: relative;
            z-index: 5;
        }

        @media (min-width: 1200px) {
            .messages-student-shell .student-dashboard-sidebar > .student-dashboard-sidebar-menu {
                overflow: visible !important;
            }

            .messages-student-shell .student-dashboard-sidebar .student-dashbord-scrollbar {
                height: auto !important;
                max-height: none !important;
                overflow: visible !important;
                padding-bottom: 0;
            }

            .messages-student-shell .student-dashboard-sidebar > .position-absolute.inset-block-end-0 {
                margin-top: 28px;
                position: static !important;
            }
        }

        .messages-layout {
            min-height: calc(100vh - 286px);
        }

        .messages-contact-panel {
            flex: 0 0 336px;
            max-width: 336px;
            min-width: 280px;
        }

        .messages-thread-panel {
            min-width: 0;
            flex: 1 1 520px;
        }

        .messages-contact-list {
            max-height: calc(105vh - 474.6px);
            min-height: 273px;
            overflow-x: hidden;
            overflow-y: auto;
        }

        .messages-contact-link {
            color: inherit;
            min-width: 0;
            width: 100%;
        }

        .messages-contact-link > .min-w-0 {
            min-width: 0;
        }

        .messages-contact-preview {
            display: block;
            max-width: 100%;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .messages-avatar {
            width: 44px;
            height: 44px;
            flex: 0 0 44px;
        }

        .messages-avatar--lg {
            width: 52px;
            height: 52px;
            flex-basis: 52px;
        }

        .messages-new-form .common-input {
            height: 48px;
        }

        .messages-user-search {
            position: relative;
        }

        .messages-recipient-results {
            position: absolute;
            inset-inline: 0;
            top: calc(100% + 8px);
            z-index: 30;
            max-height: 286px;
            overflow-x: hidden;
            overflow-y: auto;
            box-shadow: 0 14px 32px rgba(16, 24, 40, .12);
        }

        .messages-recipient-option {
            box-sizing: border-box;
            min-width: 0;
            width: 100%;
            text-align: start;
        }

        .messages-recipient-option > .min-w-0 {
            flex: 1 1 auto;
            max-width: calc(100% - 56px);
        }

        .messages-chat-scroll {
            min-height: 380px;
            max-height: calc(100vh - 424px);
            overflow-y: auto;
        }

        .message-bubble {
            overflow-wrap: anywhere;
        }

        .message-bubble__text {
            white-space: pre-wrap;
        }

        .message-bubble--sent {
            background: #0f8f7f;
            color: #fff;
        }

        .message-bubble--received {
            background: var(--neutral-20);
            color: var(--neutral-600);
        }

        .messages-attachment-chip {
            max-width: 100%;
            min-height: 40px;
        }

        .messages-attachment-chip span {
            min-width: 0;
        }

        .messages-file-button {
            width: 48px;
            height: 48px;
            border: 1px solid transparent;
            transition: background-color .2s ease, border-color .2s ease, color .2s ease, box-shadow .2s ease;
        }

        .messages-file-button:hover,
        .messages-file-button:focus-visible,
        .messages-reply-form.has-attachments .messages-file-button {
            background: var(--main-50) !important;
            border-color: var(--main-600);
            color: var(--main-600) !important;
            box-shadow: 0 0 0 4px rgba(6, 121, 117, .12);
        }

        @media (max-width: 575px) {
            .messages-student-hero-band {
                min-height: 88px;
            }

            .messages-student-hero-band .shape.one {
                inset-block-start: 15%;
                inset-inline-start: 128px;
            }
        }

        .messages-chat-row.is-selected .message-bubble {
            box-shadow: 0 0 0 2px rgba(6, 121, 117, .18);
        }

        .messages-reply-form .common-input {
            min-height: 48px;
            min-width: 0;
            width: auto;
        }

        .messages-reply-form textarea.common-input {
            max-height: 144px;
            line-height: 1.45;
            overflow-y: auto;
            resize: none;
        }

        .messages-reply-form {
            border-top: 1px dashed var(--neutral-100);
        }

        .messages-reply-input {
            flex: 1 1 260px;
        }

        .messages-attachment-name {
            flex-basis: 100%;
            padding-inline-start: 58px;
        }

        .messages-attachment-selection {
            flex-basis: 100%;
            padding-inline-start: 58px;
        }

        .messages-file-chip {
            max-width: min(100%, 260px);
            background: var(--main-50);
            color: var(--main-600);
            border: 1px solid rgba(6, 121, 117, .2);
        }

        .messages-form-feedback {
            flex-basis: 100%;
            padding-inline-start: 58px;
        }

        .messages-media-preview {
            display: block;
            width: min(100%, 360px);
            max-height: 320px;
            object-fit: cover;
            background: #101828;
        }

        .messages-media-preview--video {
            max-height: 360px;
        }

        @media (max-width: 991px) {
            .messages-student-shell .gape-student-dashboard-layout {
                min-height: 420px;
            }

            .messages-student-shell .gape-student-dashboard-main {
                padding-inline-start: 0;
            }

            .messages-student-shell .gape-student-page-heading {
                margin-block: 0 5px !important;
            }

            .messages-student-shell .gape-student-mobile-menu-bar {
                inset-block-start: 0;
                inset-inline-end: 0;
                margin-bottom: 0 !important;
                position: absolute;
            }

            .messages-layout {
                min-height: 0;
            }

            .messages-contact-panel {
                flex-basis: 100%;
                max-width: 100%;
                min-width: 0;
                padding-inline-end: 0 !important;
                border-inline-end: 0 !important;
                border-right: 0 !important;
                border-bottom: 1px dashed var(--neutral-100);
                padding-bottom: 24px;
            }

            .messages-thread-panel {
                flex-basis: 100%;
                padding-inline-start: 0 !important;
                padding-top: 24px;
            }

            .messages-contact-list,
            .messages-chat-scroll {
                max-height: none;
            }
        }

        @media (max-width: 575px) {
            .messages-student-shell .gape-student-page-heading h6 {
                font-size: 20px;
            }

            .messages-reply-input {
                flex-basis: calc(100% - 58px);
            }

            .messages-reply-form .btn {
                flex: 1 1 100%;
            }

            .messages-attachment-name {
                padding-inline-start: 0;
            }

            .messages-attachment-selection,
            .messages-form-feedback {
                padding-inline-start: 0;
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
<c:if test="${studentMessagesPage}">
    <section class="breadcrumb pt-80 pb-187 bg-neutral-900 position-relative z-1 overflow-hidden mb-0 z-n1 gape-student-hero-band messages-student-hero-band">
        <img src="${pageContext.request.contextPath}/assets/images/shapes/shape1.png" alt="" class="shape one animation-rotation">
        <img src="${pageContext.request.contextPath}/assets/images/shapes/shape2.png" alt="" class="shape two animation-scalation">
        <img src="${pageContext.request.contextPath}/assets/images/shapes/shape3.png" alt="" class="shape eight animation-walking">
        <img src="${pageContext.request.contextPath}/assets/images/shapes/shape5.png" alt="" class="shape six animation-walking">
        <img src="${pageContext.request.contextPath}/assets/images/shapes/shape4.png" alt="" class="shape three animation-upDown">
        <img src="${pageContext.request.contextPath}/assets/images/shapes/shape6.png" alt="" class="shape four animation-scalation">
        <img src="${pageContext.request.contextPath}/assets/images/shapes/shape7.png" alt="" class="shape seven animation-walking">
        <img src="${pageContext.request.contextPath}/assets/images/shapes/shape10.png" alt="" class="shape ten animation-upDown">
    </section>
</c:if>
<c:choose>
    <c:when test="${studentMessagesPage}">
<section class="messages-student-shell bg-main-25 pb-80 w-100 h-100"
                 data-messages-page
                 data-csrf-token="${sessionScope['gape.auth.csrfToken']}"
                 data-read-conversation-url="${pageContext.request.contextPath}/messages/read-conversation">
            <div class="container container--xl">
                <div class="d-flex gap-24 mt--120 z-2 position-relative gape-student-dashboard-layout">
                    <div class="student-overlay-sidebar"></div>
                    <%@ include file="/WEB-INF/fragments/student-dashboard-sidebar.jspf" %>
                    <main class="w-100 gape-student-dashboard-main">
                        <%@ include file="/WEB-INF/fragments/flash-messages.jspf" %>
                        <div class="gape-student-mobile-menu-bar d-flex align-items-center justify-content-end mb-16 d-xl-none">
                            <button type="button" class="toggle-student-dashbord-button text-white text-32" aria-label="Open dashboard menu">
                                <i class="ph-bold ph-list"></i>
                            </button>
                        </div>
                        <section class="gape-student-page-heading mb-24">
                            <h6 class="text-28 fw-semibold">Messages</h6>
                        </section>
                        <%@ include file="/WEB-INF/fragments/messages-content.jspf" %>
                    </main>
                </div>
            </div>
        </section>
        <%@ include file="/WEB-INF/fragments/student-template-footer.jspf" %>
    </c:when>
    <c:otherwise>
<div class="dashbord bg-main-25 w-100 overflow-hidden"
     data-messages-page
     data-csrf-token="${sessionScope['gape.auth.csrfToken']}"
     data-read-conversation-url="${pageContext.request.contextPath}/messages/read-conversation">
    <div class="d-flex">
        <%@ include file="/WEB-INF/fragments/dashboard-sidebar.jspf" %>
        <div class="dashbord-body flex-grow-1 d-flex flex-column min-vh-100">
            <%@ include file="/WEB-INF/fragments/dashboard-topbar.jspf" %>
            <div class="px-24 py-32 flex-grow-1">
                <%@ include file="/WEB-INF/fragments/flash-messages.jspf" %>

                <section class="bg-white rounded-10 px-24 py-24">
                    <div class="messages-layout d-flex flex-wrap align-items-stretch">
                        <aside class="messages-contact-panel max-w-420-px w-100 border-right-dashed pe-24 d-flex flex-column">
                            <div class="d-flex align-items-center justify-content-between gap-16 border-bottom-dashed pb-24">
                                <div class="d-flex align-items-center gap-12 min-w-0">
                                    <span class="messages-avatar rounded-circle bg-main-50 text-main-600 d-inline-flex align-items-center justify-content-center text-24">
                                        <i class="ph ph-chats-circle"></i>
                                    </span>
                                    <div class="min-w-0">
                                        <h2 class="text-18 fw-semibold text-neutral-700 mb-2">Conversations</h2>
                                    </div>
                                </div>
                                <span class="w-32 h-32 rounded-circle bg-neutral-20 text-neutral-500 d-inline-flex align-items-center justify-content-center text-18 flex-shrink-0">
                                    <i class="ph ph-chat-dots"></i>
                                </span>
                            </div>

                            <div class="messages-new-form new-message-form border-bottom-dashed py-24">
                                <div class="d-flex align-items-center justify-content-between gap-12 mb-12">
                                    <span class="d-block text-15 fw-semibold text-neutral-700 mb-0">New message</span>
                                    <span class="text-20 text-neutral-500 d-inline-flex">
                                        <i class="ph ph-pencil-simple-line"></i>
                                    </span>
                                </div>
                                <div class="messages-user-search">
                                    <label for="new-message-recipient" class="visually-hidden">Select user</label>
                                    <input id="new-message-recipient"
                                           type="search"
                                           class="common-input"
                                           placeholder="Select user"
                                           autocomplete="off"
                                           role="combobox"
                                           aria-controls="messages-recipient-results"
                                           aria-expanded="false"
                                           data-user-search-input>
                                    <div id="messages-recipient-results"
                                         class="messages-recipient-results bg-white border border-neutral-30 rounded-10 py-8"
                                         role="listbox"
                                         hidden
                                         data-user-search-results>
                                        <c:forEach var="recipient" items="${communicationPage.recipients}">
                                            <button type="button"
                                                    class="messages-recipient-option border-0 bg-white hover-bg-neutral-20 transition-03 px-16 py-10 d-flex align-items-center gap-12"
                                                    role="option"
                                                    data-user-option
                                                    data-user-id="${recipient.userId}"
                                                    data-user-label="<c:out value='${recipient.label}'/>"
                                                    data-user-search="${recipient.userId} <c:out value='${recipient.name}'/> <c:out value='${recipient.email}'/>"
                                                    data-user-url="${pageContext.request.contextPath}/messages?userId=${recipient.userId}">
                                                <span class="messages-avatar rounded-circle bg-main-50 text-main-600 d-inline-flex align-items-center justify-content-center text-14 fw-semibold">
                                                    <c:out value="${recipient.initials}"/>
                                                </span>
                                                <span class="min-w-0">
                                                    <span class="d-block text-14 fw-semibold text-neutral-700 text-line-1"><c:out value="${recipient.name}"/></span>
                                                    <span class="d-block text-12 text-neutral-500 text-line-1">ID ${recipient.userId} - <c:out value="${recipient.email}"/></span>
                                                </span>
                                            </button>
                                        </c:forEach>
                                        <div class="px-16 py-14 text-13 text-neutral-500" data-user-search-empty hidden>
                                            No users found.
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="messages-contact-list pt-24" data-messages-contact-list>
                                <%@ include file="/WEB-INF/fragments/messages-contact-list.jspf" %>
                            </div>
                        </aside>

                        <%@ include file="/WEB-INF/fragments/messages-thread-panel.jspf" %>
                    </div>
                </section>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
    </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script>
    (function () {
        var root = document.querySelector('[data-messages-page]');
        if (!root) {
            return;
        }

        var searchInput = root.querySelector('[data-user-search-input]');
        var results = root.querySelector('[data-user-search-results]');
        var emptyResult = root.querySelector('[data-user-search-empty]');
        var options = Array.prototype.slice.call(root.querySelectorAll('[data-user-option]'));
        var activeIndex = -1;
        var threadCache = Object.create(null);
        var currentThreadCacheKey = null;
        var csrfToken = root.getAttribute('data-csrf-token') || '';
        var readConversationUrl = root.getAttribute('data-read-conversation-url') || '';

        function normalize(value) {
            return String(value || '')
                .normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '')
                .toLowerCase();
        }

        function visibleOptions() {
            return options.filter(function (option) {
                return !option.hidden;
            });
        }

        function setActiveOption(index) {
            var visible = visibleOptions();
            visible.forEach(function (option) {
                option.classList.remove('bg-neutral-20');
                option.setAttribute('aria-selected', 'false');
            });
            if (visible.length === 0) {
                activeIndex = -1;
                return;
            }
            activeIndex = (index + visible.length) % visible.length;
            visible[activeIndex].classList.add('bg-neutral-20');
            visible[activeIndex].setAttribute('aria-selected', 'true');
        }

        function renderResults(showAll) {
            var terms = normalize(searchInput.value).split(/\s+/).filter(Boolean);
            var count = 0;
            options.forEach(function (option) {
                var searchable = normalize(option.getAttribute('data-user-search'));
                var matches = showAll || terms.length === 0 || terms.every(function (term) {
                    return searchable.indexOf(term) !== -1;
                });
                option.hidden = !matches;
                if (matches) {
                    count++;
                }
            });
            emptyResult.hidden = count !== 0;
            results.hidden = false;
            searchInput.setAttribute('aria-expanded', 'true');
            setActiveOption(count > 0 ? 0 : -1);
        }

        function hideResults() {
            results.hidden = true;
            searchInput.setAttribute('aria-expanded', 'false');
            activeIndex = -1;
        }

        function scrollConversationToBottom() {
            var scroller = root.querySelector('[data-messages-chat-scroll]');
            if (scroller) {
                scroller.scrollTop = scroller.scrollHeight;
            }
        }

        function fragmentFromHtml(html, selector) {
            var template = document.createElement('template');
            template.innerHTML = html.trim();
            var element = template.content.querySelector(selector);
            if (!element) {
                throw new Error('Missing messages fragment: ' + selector);
            }
            return element;
        }

        function selectedPeerIdFromUrl(url) {
            return new URL(url, window.location.href).searchParams.get('userId');
        }

        function setActiveConversation(peerUserId) {
            Array.prototype.slice.call(root.querySelectorAll('[data-conversation-link]')).forEach(function (link) {
                link.classList.toggle('bg-neutral-20', peerUserId && link.getAttribute('data-peer-user-id') === peerUserId);
            });
        }

        function conversationLink(peerUserId) {
            var links = Array.prototype.slice.call(root.querySelectorAll('[data-conversation-link]'));
            for (var index = 0; index < links.length; index++) {
                if (links[index].getAttribute('data-peer-user-id') === String(peerUserId)) {
                    return links[index];
                }
            }
            return null;
        }

        function setCountBadge(element, count) {
            if (!element) {
                return;
            }
            var normalized = Number(count || 0);
            element.textContent = normalized;
            element.classList.toggle('d-none', normalized <= 0);
        }

        function updateMessageUnreadSummary(count) {
            Array.prototype.slice.call(document.querySelectorAll('[data-sidebar-message-badge]')).forEach(function (badge) {
                setCountBadge(badge, count);
            });
        }

        function clearConversationUnread(peerUserId, totalUnreadCount, updateThread) {
            var link = conversationLink(peerUserId);
            if (link) {
                link.setAttribute('data-conversation-unread-count', '0');
                var name = link.querySelector('[data-conversation-name]');
                if (name) {
                    name.classList.remove('text-main-600');
                    name.classList.add('text-neutral-700');
                }
                var badge = link.querySelector('[data-conversation-unread-badge]');
                if (badge) {
                    badge.remove();
                }
            }

            var panel = root.querySelector('[data-messages-thread-panel]');
            if (updateThread !== false && panel && panel.getAttribute('data-selected-peer-id') === String(peerUserId)) {
                panel.setAttribute('data-selected-unread-count', '0');
                var threadBadge = panel.querySelector('[data-thread-unread-pill]');
                if (threadBadge) {
                    threadBadge.remove();
                }
                Array.prototype.slice.call(panel.querySelectorAll('[data-message-state-label][data-message-readable="true"]')).forEach(function (label) {
                    label.textContent = 'Read';
                    label.classList.remove('text-main-600');
                    label.classList.add('text-neutral-500');
                    label.setAttribute('data-message-readable', 'false');
                });
                Array.prototype.slice.call(panel.querySelectorAll('[data-message-read-form]')).forEach(function (form) {
                    form.remove();
                });
            }

            updateMessageUnreadSummary(totalUnreadCount);
            if (updateThread !== false) {
                cacheCurrentThread();
            }
        }

        function markConversationRead(peerUserId, options) {
            options = options || {};
            if (!peerUserId || !csrfToken || !readConversationUrl) {
                return Promise.resolve(false);
            }
            var panel = root.querySelector('[data-messages-thread-panel]');
            if (panel && panel.dataset.markingRead === 'true' && panel.getAttribute('data-selected-peer-id') === String(peerUserId)) {
                return Promise.resolve(false);
            }
            if (panel && panel.getAttribute('data-selected-peer-id') === String(peerUserId)) {
                panel.dataset.markingRead = 'true';
            }
            var body = new URLSearchParams();
            body.set('csrfToken', csrfToken);
            body.set('userId', peerUserId);
            return fetch(sessionAwareUrl(readConversationUrl), {
                method: 'POST',
                credentials: 'same-origin',
                keepalive: !!options.keepalive,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest',
                    'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
                },
                body: body.toString()
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error('Could not mark conversation as read');
                    }
                    return response.json();
                })
                .then(function (payload) {
                    clearConversationUnread(peerUserId, Number(payload.messageUnreadCount || 0), options.updateThread);
                    return true;
                })
                .catch(function () {
                    if (panel && panel.getAttribute('data-selected-peer-id') === String(peerUserId)) {
                        panel.dataset.markingRead = 'false';
                    }
                    return false;
                });
        }

        function markSelectedConversationRead(options) {
            var panel = root.querySelector('[data-messages-thread-panel]');
            if (!panel) {
                return Promise.resolve(false);
            }
            var peerUserId = panel.getAttribute('data-selected-peer-id');
            var unreadCount = parseInt(panel.getAttribute('data-selected-unread-count') || '0', 10);
            if (!peerUserId || !Number.isFinite(unreadCount) || unreadCount <= 0) {
                return Promise.resolve(false);
            }
            return markConversationRead(peerUserId, options);
        }

        function markSelectedConversationReadOnPageExit() {
            var panel = root.querySelector('[data-messages-thread-panel]');
            if (!panel || !csrfToken || !readConversationUrl) {
                return;
            }
            var peerUserId = panel.getAttribute('data-selected-peer-id');
            var unreadCount = parseInt(panel.getAttribute('data-selected-unread-count') || '0', 10);
            if (!peerUserId || !Number.isFinite(unreadCount) || unreadCount <= 0) {
                return;
            }
            var body = new URLSearchParams();
            body.set('csrfToken', csrfToken);
            body.set('userId', peerUserId);
            var targetUrl = sessionAwareUrl(readConversationUrl);
            if (navigator.sendBeacon) {
                var blob = new Blob([body.toString()], {
                    type: 'application/x-www-form-urlencoded;charset=UTF-8'
                });
                if (navigator.sendBeacon(targetUrl, blob)) {
                    return;
                }
            }
            fetch(targetUrl, {
                method: 'POST',
                credentials: 'same-origin',
                keepalive: true,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest',
                    'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
                },
                body: body.toString()
            }).catch(function () {});
        }

        function rewriteMessagesPath(url, suffix) {
            var target = new URL(url, window.location.href);
            target.pathname = target.pathname.replace(/\/messages(?:\/thread(?:\/messages)?)?$/, '/messages' + suffix);
            return target.toString();
        }

        function publicConversationUrl(url) {
            return sessionAwareUrl(rewriteMessagesPath(url, ''));
        }

        function threadUrl(url) {
            return sessionAwareUrl(rewriteMessagesPath(url, '/thread'));
        }

        function olderMessagesUrl(peerUserId, beforeMessageId) {
            var target = new URL(rewriteMessagesPath(window.location.href, '/thread/messages'), window.location.href);
            target.search = '?userId=' + encodeURIComponent(peerUserId)
                    + '&beforeMessageId=' + encodeURIComponent(beforeMessageId);
            return sessionAwareUrl(target.toString());
        }

        function replaceThreadPanel(html) {
            var source = fragmentFromHtml(html, '[data-messages-thread-panel]');
            var target = root.querySelector('[data-messages-thread-panel]');
            if (!target) {
                throw new Error('Missing current messages thread panel');
            }
            target.replaceWith(source);
            bindThreadControls();
            syncUnreadSummaryFromThread();
        }

        function replaceConversationList(html) {
            var target = root.querySelector('[data-messages-contact-list]');
            if (!target) {
                return false;
            }
            var template = document.createElement('template');
            template.innerHTML = html.trim();
            var payload = template.content.querySelector('[data-messages-contact-list-payload]');
            if (!payload) {
                return false;
            }
            target.innerHTML = payload.innerHTML;
            bindConversationLinks();
            return true;
        }

        function syncUnreadSummaryFromThread() {
            var panel = root.querySelector('[data-messages-thread-panel]');
            if (!panel) {
                return;
            }
            var count = panel.getAttribute('data-total-unread-count');
            if (count !== null && count !== '') {
                updateMessageUnreadSummary(Number(count || 0));
            }
        }

        function cacheCurrentThread() {
            var panel = root.querySelector('[data-messages-thread-panel]');
            if (panel && currentThreadCacheKey) {
                threadCache[currentThreadCacheKey] = panel.outerHTML;
            }
        }

        function cacheInitialThread() {
            var panel = root.querySelector('[data-messages-thread-panel]');
            var peerUserId = panel ? panel.getAttribute('data-selected-peer-id') : '';
            if (!peerUserId) {
                return;
            }
            var target = new URL(window.location.href);
            target.searchParams.set('userId', peerUserId);
            currentThreadCacheKey = threadUrl(target.toString());
            cacheCurrentThread();
        }

        function bindThreadControls() {
            Array.prototype.slice.call(root.querySelectorAll('[data-load-older-messages]')).forEach(function (button) {
                if (button.dataset.dynamicBound === 'true') {
                    return;
                }
                button.dataset.dynamicBound = 'true';
                button.addEventListener('click', function () {
                    loadOlderMessages(button);
                });
            });
            bindAttachmentControl();
            bindSendForm();
            bindReplyInput();
        }

        function bindAttachmentControl() {
            var input = root.querySelector('[data-message-attachment-input]');
            var list = root.querySelector('[data-message-attachment-name]');
            var form = root.querySelector('[data-message-send-form]');
            var feedback = root.querySelector('[data-message-form-feedback]');
            if (!input || input.dataset.dynamicBound === 'true') {
                return;
            }
            input.dataset.dynamicBound = 'true';
            input.addEventListener('change', function () {
                renderSelectedFiles(input, list, form, feedback);
            });
        }

        function renderSelectedFiles(input, list, form, feedback) {
            var files = Array.prototype.slice.call(input.files || []);
            if (list) {
                list.innerHTML = '';
                files.slice(0, 5).forEach(function (file) {
                    var chip = document.createElement('span');
                    chip.className = 'messages-file-chip rounded-pill px-12 py-6 text-12 fw-semibold d-inline-flex align-items-center gap-6';
                    chip.innerHTML = '<i class="ph ph-file"></i><span class="text-line-1"></span>';
                    chip.querySelector('span').textContent = file.name;
                    list.appendChild(chip);
                });
                if (files.length > 5) {
                    var more = document.createElement('span');
                    more.className = 'text-12 fw-semibold text-danger-600';
                    more.textContent = 'Maximum 5 files per message.';
                    list.appendChild(more);
                }
                list.hidden = files.length === 0;
            }
            if (form) {
                form.classList.toggle('has-attachments', files.length > 0);
            }
            setFormFeedback(feedback, files.length > 5 ? 'Maximum 5 files per message.' : '', true);
        }

        function setFormFeedback(feedback, message, isError) {
            if (!feedback) {
                return;
            }
            feedback.textContent = message || '';
            feedback.hidden = !message;
            feedback.classList.toggle('text-danger-600', !!isError);
            feedback.classList.toggle('text-main-600', !isError && !!message);
        }

        function bindSendForm() {
            var form = root.querySelector('[data-message-send-form]');
            if (!form || form.dataset.dynamicBound === 'true') {
                return;
            }
            form.dataset.dynamicBound = 'true';
            form.addEventListener('submit', function (event) {
                event.preventDefault();
                submitMessageForm(form);
            });
        }

        function bindReplyInput() {
            var input = root.querySelector('[data-message-body-input]');
            if (!input || input.dataset.dynamicBound === 'true') {
                return;
            }
            input.dataset.dynamicBound = 'true';
            var resize = function () {
                input.style.height = 'auto';
                input.style.height = Math.min(input.scrollHeight, 144) + 'px';
            };
            input.addEventListener('input', resize);
            resize();
        }

        function submitMessageForm(form) {
            if (form.dataset.sending === 'true') {
                return;
            }
            var input = form.querySelector('[data-message-attachment-input]');
            var feedback = form.querySelector('[data-message-form-feedback]');
            var files = Array.prototype.slice.call((input && input.files) || []);
            if (files.length > 5) {
                setFormFeedback(feedback, 'Maximum 5 files per message.', true);
                return;
            }
            var bodyInput = form.querySelector('[name="body"]');
            if (files.length === 0 && bodyInput && bodyInput.value.trim() === '') {
                setFormFeedback(feedback, 'Type a message or attach a file.', true);
                return;
            }

            form.dataset.sending = 'true';
            setFormFeedback(feedback, '', false);
            var submitButton = form.querySelector('button[type="submit"]');
            var originalButtonHtml = submitButton ? submitButton.innerHTML : '';
            if (submitButton) {
                submitButton.disabled = true;
                submitButton.innerHTML = '<span>Sending</span><i class="ph ph-spinner-gap"></i>';
            }
            fetch(sessionAwareUrl(form.action), {
                method: 'POST',
                credentials: 'same-origin',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: new FormData(form)
            })
                .then(function (response) {
                    if (!response.ok) {
                        return response.text().then(function (text) {
                            var error = 'The message could not be sent.';
                            try {
                                error = JSON.parse(text).error || error;
                            } catch (ignored) {
                                if (text) {
                                    error = text;
                                }
                            }
                            throw new Error(error);
                        });
                    }
                    return response.text();
                })
                .then(function (html) {
                    replaceThreadPanel(html);
                    replaceConversationList(html);
                    var panel = root.querySelector('[data-messages-thread-panel]');
                    if (panel) {
                        clearConversationUnread(
                                panel.getAttribute('data-selected-peer-id'),
                                Number(panel.getAttribute('data-total-unread-count') || 0),
                                true
                        );
                    }
                    cacheCurrentThread();
                    scrollConversationToBottom();
                })
                .catch(function (error) {
                    setFormFeedback(feedback, error.message || 'The message could not be sent.', true);
                })
                .finally(function () {
                    form.dataset.sending = 'false';
                    if (submitButton) {
                        submitButton.disabled = false;
                        submitButton.innerHTML = originalButtonHtml;
                    }
                });
        }

        function bindConversationLinks() {
            Array.prototype.slice.call(root.querySelectorAll('[data-conversation-link]')).forEach(function (link) {
                if (link.dataset.dynamicBound === 'true') {
                    return;
                }
                link.dataset.dynamicBound = 'true';
                link.addEventListener('click', function (event) {
                    if (event.defaultPrevented || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
                        return;
                    }
                    event.preventDefault();
                    openConversation(link.href, '', true);
                });
            });
        }

        function sessionAwareUrl(url) {
            var target = new URL(url, window.location.href);
            var sessionMatch = window.location.pathname.match(/^(\/[^/]+)(;jsessionid=[^/]+)\//);
            if (sessionMatch && target.pathname.indexOf(sessionMatch[2]) === -1) {
                target.pathname = target.pathname.replace(
                    sessionMatch[1] + '/',
                    sessionMatch[1] + sessionMatch[2] + '/'
                );
            }
            return target.toString();
        }

        function openConversation(url, label, pushHistory) {
            var requestUrl = threadUrl(url);
            var historyUrl = publicConversationUrl(url);
            var peerUserId = selectedPeerIdFromUrl(url);
            var leavingPanel = root.querySelector('[data-messages-thread-panel]');
            var leavingPeerUserId = leavingPanel ? leavingPanel.getAttribute('data-selected-peer-id') : '';
            var leavingUnreadCount = parseInt(leavingPanel ? leavingPanel.getAttribute('data-selected-unread-count') || '0' : '0', 10);
            var beforeOpen = Promise.resolve(false);
            if (leavingPeerUserId && leavingPeerUserId !== String(peerUserId || '')) {
                var leavingCacheKey = currentThreadCacheKey;
                if (Number.isFinite(leavingUnreadCount) && leavingUnreadCount > 0) {
                    beforeOpen = markConversationRead(leavingPeerUserId, {updateThread: false});
                }
                if (leavingCacheKey) {
                    delete threadCache[leavingCacheKey];
                }
            }
            hideResults();
            if (label) {
                searchInput.value = label;
            }
            beforeOpen.finally(function () {
                if (threadCache[requestUrl]) {
                    currentThreadCacheKey = requestUrl;
                    replaceThreadPanel(threadCache[requestUrl]);
                    if (pushHistory) {
                        history.pushState({}, '', historyUrl);
                    }
                    setActiveConversation(peerUserId);
                    scrollConversationToBottom();
                    return;
                }
                root.classList.add('is-loading-conversation');
                fetch(requestUrl, {
                    credentials: 'same-origin',
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                })
                    .then(function (response) {
                        if (!response.ok) {
                            throw new Error('Could not load conversation');
                        }
                        return response.text();
                    })
                    .then(function (html) {
                        currentThreadCacheKey = requestUrl;
                        replaceThreadPanel(html);
                        cacheCurrentThread();
                        if (pushHistory) {
                            history.pushState({}, '', historyUrl);
                        }
                        setActiveConversation(peerUserId);
                        scrollConversationToBottom();
                    })
                    .catch(function () {
                        window.location.assign(historyUrl);
                    })
                    .finally(function () {
                        root.classList.remove('is-loading-conversation');
                    });
            });
        }

        function loadOlderMessages(button) {
            var peerUserId = button.getAttribute('data-peer-user-id');
            var beforeMessageId = button.getAttribute('data-before-message-id');
            var scroller = root.querySelector('[data-messages-chat-scroll]');
            var items = root.querySelector('[data-message-items]');
            if (!peerUserId || !beforeMessageId || !scroller || !items || button.dataset.loading === 'true') {
                return;
            }
            button.dataset.loading = 'true';
            button.disabled = true;
            var previousHeight = scroller.scrollHeight;
            var previousTop = scroller.scrollTop;
            fetch(olderMessagesUrl(peerUserId, beforeMessageId), {
                credentials: 'same-origin',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error('Could not load older messages');
                    }
                    return response.text();
                })
                .then(function (html) {
                    var payload = fragmentFromHtml(html, '[data-messages-older-payload]');
                    var rows = Array.prototype.slice.call(payload.querySelectorAll('[data-message-id]'));
                    if (rows.length === 0) {
                        throw new Error('No older messages were returned');
                    }
                    var fragment = document.createDocumentFragment();
                    rows.forEach(function (row) {
                        fragment.appendChild(row);
                    });
                    items.insertBefore(fragment, items.firstChild);
                    var nextBeforeMessageId = payload.getAttribute('data-next-before-message-id');
                    if (payload.getAttribute('data-has-older-messages') === 'true' && nextBeforeMessageId) {
                        button.setAttribute('data-before-message-id', nextBeforeMessageId);
                        button.disabled = false;
                        button.dataset.loading = 'false';
                    } else {
                        var shell = button.closest('[data-load-older-shell]');
                        if (shell) {
                            shell.remove();
                        }
                    }
                    scroller.scrollTop = scroller.scrollHeight - previousHeight + previousTop;
                    cacheCurrentThread();
                })
                .catch(function () {
                    button.disabled = false;
                    button.dataset.loading = 'false';
                });
        }

        if (!searchInput || !results) {
            return;
        }

        searchInput.addEventListener('focus', function () {
            searchInput.select();
            renderResults(true);
        });
        searchInput.addEventListener('click', function () {
            searchInput.select();
            renderResults(true);
        });
        searchInput.addEventListener('input', function () {
            renderResults(false);
        });
        searchInput.addEventListener('keydown', function (event) {
            var visible = visibleOptions();
            if (event.key === 'Escape') {
                hideResults();
                return;
            }
            if (event.key === 'ArrowDown') {
                event.preventDefault();
                if (results.hidden) {
                    renderResults(false);
                } else {
                    setActiveOption(activeIndex + 1);
                }
                return;
            }
            if (event.key === 'ArrowUp') {
                event.preventDefault();
                setActiveOption(activeIndex - 1);
                return;
            }
            if (event.key === 'Enter' && !results.hidden) {
                event.preventDefault();
                if (visible[activeIndex]) {
                    visible[activeIndex].click();
                }
            }
        });

        options.forEach(function (option) {
            option.addEventListener('click', function () {
                openConversation(option.getAttribute('data-user-url'), option.getAttribute('data-user-label'), true);
            });
        });

        document.addEventListener('click', function (event) {
            if (!event.target.closest('.messages-user-search')) {
                hideResults();
            }
        });

        window.addEventListener('popstate', function () {
            markSelectedConversationRead({updateThread: false});
            openConversation(window.location.href, '', false);
        });
        window.addEventListener('pagehide', markSelectedConversationReadOnPageExit);

        bindConversationLinks();
        bindThreadControls();
        cacheInitialThread();
        scrollConversationToBottom();
        syncUnreadSummaryFromThread();
    }());
</script>
</body>
</html>
