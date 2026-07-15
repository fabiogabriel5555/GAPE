/* Lesson creation, edition and detail are deliberately hosted in one fixed
 * dialog.  The form keeps its established contextual validation inside the
 * frame, while the dashboard never navigates to a standalone lesson page. */
(function (window, document) {
    'use strict';

    var modalId = 'gapeLessonDialog';

    function ensureStyles() {
        if (document.getElementById('gapeLessonModalStyles')) {
            return;
        }
        document.head.insertAdjacentHTML('beforeend',
            '<style id="gapeLessonModalStyles">'
            + '.modal.gape-lesson-modal-root{overflow:hidden}'
            + '.modal-dialog.gape-lesson-modal-dialog{max-width:min(980px,calc(100vw - 48px));margin:32px auto}'
            + '.modal-dialog.gape-lesson-modal-dialog .modal-content{height:min(780px,calc(100dvh - 64px));max-height:calc(100dvh - 64px);display:flex;overflow:hidden}'
            + '.modal-dialog.gape-lesson-modal-dialog .modal-header{padding:16px 20px}'
            + '.modal-dialog.gape-lesson-modal-dialog .modal-body{min-height:0;flex:1 1 auto;overflow:hidden}'
            + '.gape-lesson-modal-frame{border:0;display:block;height:100%;width:100%;background:#fff}'
            + '@media(max-width:575.98px){.modal-dialog.gape-lesson-modal-dialog{max-width:calc(100vw - 20px);margin:10px auto}.modal-dialog.gape-lesson-modal-dialog .modal-content{height:calc(100dvh - 20px);max-height:calc(100dvh - 20px)}.modal-dialog.gape-lesson-modal-dialog .modal-header{padding:14px 16px}}'
            + '</style>'
        );
    }

    function withCurrentSessionUrl(rawUrl) {
        var target = new URL(rawUrl, window.location.href);
        var match = window.location.pathname.match(/^(\/[^/]+)(;jsessionid=[^/]+)/i);
        if (match && target.origin === window.location.origin && target.pathname.indexOf(match[2]) === -1
                && target.pathname.indexOf(match[1] + '/') === 0) {
            target.pathname = match[1] + match[2] + target.pathname.substring(match[1].length);
        }
        return target.toString();
    }

    function modalElement() {
        ensureStyles();
        var modal = document.getElementById(modalId);
        if (modal) {
            return modal;
        }
        document.body.insertAdjacentHTML('beforeend',
            '<div class="modal fade gape-subject-form-modal-root gape-lesson-modal-root" id="' + modalId + '" tabindex="-1" aria-hidden="true">'
            + '<div class="modal-dialog modal-dialog-centered gape-subject-form-modal-dialog gape-lesson-modal-dialog">'
            + '<div class="modal-content">'
            + '<div class="modal-header border-neutral-30">'
            + '<h5 class="modal-title text-18 fw-semibold" data-gape-lesson-modal-title>Lesson</h5>'
            + '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>'
            + '</div>'
            + '<div class="modal-body p-0"><iframe class="gape-lesson-modal-frame" title="Lesson" data-gape-lesson-modal-frame></iframe></div>'
            + '</div></div></div>'
        );
        modal = document.getElementById(modalId);
        modal.addEventListener('hidden.bs.modal', function () {
            var frame = modal.querySelector('[data-gape-lesson-modal-frame]');
            if (frame) {
                frame.removeAttribute('src');
            }
        });
        return modal;
    }

    function modalUrl(rawUrl) {
        var target = new URL(rawUrl, window.location.href);
        target.searchParams.set('modal', '1');
        return withCurrentSessionUrl(target);
    }

    function labelFor(trigger) {
        if (trigger && trigger.dataset.lessonModalTitle) {
            return trigger.dataset.lessonModalTitle;
        }
        if (trigger && trigger.getAttribute('title')) {
            return trigger.getAttribute('title');
        }
        return 'Lesson';
    }

    function open(rawUrl, title) {
        if (!rawUrl || !window.bootstrap || !window.bootstrap.Modal) {
            return;
        }
        var modal = modalElement();
        var frame = modal.querySelector('[data-gape-lesson-modal-frame]');
        var heading = modal.querySelector('[data-gape-lesson-modal-title]');
        if (heading) {
            heading.textContent = title || 'Lesson';
        }
        if (frame) {
            frame.title = title || 'Lesson';
            frame.src = modalUrl(rawUrl);
        }
        window.bootstrap.Modal.getOrCreateInstance(modal).show();
    }

    function openRequestedModal() {
        var url = new URL(window.location.href);
        var requested = url.searchParams.get('lessonModal');
        if (!requested) {
            return;
        }
        var match = /^(new|detail|edit)(?::(\d+))?$/.exec(requested);
        url.searchParams.delete('lessonModal');
        window.history.replaceState(null, '', url.toString());
        if (!match) {
            return;
        }
        var kind = match[1];
        var id = match[2];
        // Direct lesson routes first redirect to the lesson-list host.  Keep the
        // deployed application context here: an absolute /learning URL would
        // otherwise escape a non-root deployment (for example /GAPE) and leave
        // the dialog iframe on a 404 page.
        var learningPathIndex = window.location.pathname.indexOf('/learning/');
        var contextPath = learningPathIndex >= 0
            ? window.location.pathname.slice(0, learningPathIndex)
            : '';
        var target = new URL(contextPath + '/learning/lessons/'
            + (kind === 'new' ? 'new' : id + (kind === 'edit' ? '/edit' : '')), window.location.origin);
        url.searchParams.forEach(function (value, key) {
            if (key !== 'lessonModal' && key !== 'modal') {
                target.searchParams.set(key, value);
            }
        });
        if (kind === 'new') {
            open(target.toString(), 'Create Lesson');
        } else if (id) {
            open(target.toString(),
                kind === 'edit' ? 'Edit Lesson' : 'Lesson Details');
        }
    }

    document.addEventListener('click', function (event) {
        var trigger = event.target.closest('[data-lesson-modal-url]');
        if (!trigger) {
            return;
        }
        event.preventDefault();
        open(trigger.dataset.lessonModalUrl, labelFor(trigger));
    });

    window.addEventListener('message', function (event) {
        if (event.origin !== window.location.origin || !event.data) {
            return;
        }
        var modal = document.getElementById(modalId);
        if (event.data.type === 'gape:lesson:changed') {
            if (modal && window.bootstrap && window.bootstrap.Modal) {
                window.bootstrap.Modal.getOrCreateInstance(modal).hide();
            }
            window.location.reload();
        } else if (event.data.type === 'gape:lesson:close' && modal && window.bootstrap && window.bootstrap.Modal) {
            window.bootstrap.Modal.getOrCreateInstance(modal).hide();
        }
    });

    document.addEventListener('click', function (event) {
        var close = event.target.closest('[data-gape-lesson-modal-close]');
        if (!close || window.parent === window) {
            return;
        }
        event.preventDefault();
        window.parent.postMessage({ type: 'gape:lesson:close' }, window.location.origin);
    });

    window.GapeLessonModal = { open: open };
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', openRequestedModal);
    } else {
        openRequestedModal();
    }
}(window, document));
