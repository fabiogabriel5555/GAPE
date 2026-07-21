(function (window, document) {
    'use strict';
    var modalId = 'gapeAssessmentDialog';

    function ensureStyles() {
        if (document.getElementById('gapeAssessmentModalStyles')) return;
        document.head.insertAdjacentHTML('beforeend', '<style id="gapeAssessmentModalStyles">'
            + '.modal.gape-assessment-modal-root{overflow:hidden}.modal-dialog.gape-assessment-modal-dialog{max-width:min(1040px,calc(100vw - 32px));margin:24px auto}.modal-dialog.gape-assessment-modal-dialog .modal-content{height:min(860px,calc(100dvh - 48px));max-height:calc(100dvh - 48px);display:flex;overflow:hidden}.modal-dialog.gape-assessment-modal-dialog .modal-body{min-height:0;flex:1 1 auto;overflow:hidden}.gape-assessment-modal-frame{border:0;display:block;height:100%;width:100%;background:#fff}.gape-assessment-modal-loading{align-items:center;display:flex;gap:10px;justify-content:center;min-height:180px;color:#64748b;font-size:14px}.gape-assessment-modal-spinner{align-items:center;display:inline-flex;height:1em;justify-content:center;width:1em}.gape-assessment-modal-spinner .ph-circle-notch{animation:gape-assessment-modal-spinner-rotation .8s linear infinite;display:inline-block;transform-origin:center}@keyframes gape-assessment-modal-spinner-rotation{to{transform:rotate(360deg)}}@media(max-width:575.98px){.modal-dialog.gape-assessment-modal-dialog{max-width:calc(100vw - 16px);margin:8px auto}.modal-dialog.gape-assessment-modal-dialog .modal-content{height:calc(100dvh - 16px);max-height:calc(100dvh - 16px)}}</style>');
    }

    function modalElement() {
        ensureStyles();
        var modal = document.getElementById(modalId);
        if (modal) return modal;
        document.body.insertAdjacentHTML('beforeend', '<div class="modal fade gape-assessment-modal-root" id="' + modalId + '" tabindex="-1" aria-hidden="true"><div class="modal-dialog modal-dialog-centered gape-assessment-modal-dialog"><div class="modal-content"><div class="modal-header border-neutral-30"><h5 class="modal-title text-18 fw-semibold" data-gape-assessment-modal-title>Create Assessment</h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div><div class="modal-body p-0"><div class="gape-assessment-modal-loading" data-gape-assessment-loading><i class="ph ph-circle-notch animate-spin"></i><span>Loading assessment form…</span></div><iframe class="gape-assessment-modal-frame d-none" title="Create Assessment" data-gape-assessment-modal-frame></iframe></div></div></div></div>');
        modal = document.getElementById(modalId);
        modal.addEventListener('hidden.bs.modal', function () {
            var frame = modal.querySelector('[data-gape-assessment-modal-frame]');
            setTriggerLoading(modal._gapeAssessmentTrigger, false);
            modal._gapeAssessmentTrigger = null;
            if (frame) {
                frame.onload = null;
                frame.onerror = null;
                frame.removeAttribute('src');
            }
        });
        return modal;
    }

    function withCurrentSessionUrl(target) {
        var match = window.location.pathname.match(/^(\/[^/]+)(;jsessionid=[^/]+)/i);
        if (match && target.origin === window.location.origin && target.pathname.indexOf(match[2]) === -1
                && target.pathname.indexOf(match[1] + '/') === 0) {
            target.pathname = match[1] + match[2] + target.pathname.substring(match[1].length);
        }
        return target;
    }

    function labelFor(trigger) {
        if (trigger && trigger.dataset.assessmentModalTitle) {
            return trigger.dataset.assessmentModalTitle;
        }
        if (trigger && trigger.getAttribute('title')) {
            return trigger.getAttribute('title');
        }
        return 'Assessment';
    }

    function setTriggerLoading(trigger, busy) {
        if (!trigger) return;
        if (busy) {
            if (trigger.dataset.assessmentOriginalHtml === undefined) {
                trigger.dataset.assessmentOriginalHtml = trigger.innerHTML;
            }
            if (trigger.dataset.assessmentOriginalAriaLabel === undefined) {
                trigger.dataset.assessmentOriginalAriaLabel = trigger.getAttribute('aria-label') || '';
            }
            if (trigger.dataset.assessmentOriginalDimensions === undefined) {
                var assessmentRect = trigger.getBoundingClientRect();
                trigger.dataset.assessmentOriginalDimensions = JSON.stringify({
                    width: trigger.style.width,
                    minWidth: trigger.style.minWidth,
                    height: trigger.style.height,
                    minHeight: trigger.style.minHeight
                });
                trigger.style.width = assessmentRect.width + 'px';
                trigger.style.minWidth = assessmentRect.width + 'px';
                trigger.style.height = assessmentRect.height + 'px';
                trigger.style.minHeight = assessmentRect.height + 'px';
            }
            trigger.disabled = true;
            trigger.setAttribute('aria-busy', 'true');
            trigger.setAttribute('aria-label', labelFor(trigger));
            trigger.innerHTML = '<span class="gape-assessment-modal-spinner" role="status" aria-label="Loading"><i class="ph ph-circle-notch animate-spin" aria-hidden="true"></i></span>';
            return;
        }
        if (trigger.dataset.assessmentOriginalHtml !== undefined) {
            trigger.innerHTML = trigger.dataset.assessmentOriginalHtml;
            delete trigger.dataset.assessmentOriginalHtml;
        }
        if (trigger.dataset.assessmentOriginalAriaLabel !== undefined) {
            if (trigger.dataset.assessmentOriginalAriaLabel) {
                trigger.setAttribute('aria-label', trigger.dataset.assessmentOriginalAriaLabel);
            } else {
                trigger.removeAttribute('aria-label');
            }
            delete trigger.dataset.assessmentOriginalAriaLabel;
        }
        if (trigger.dataset.assessmentOriginalDimensions !== undefined) {
            var assessmentDimensions = JSON.parse(trigger.dataset.assessmentOriginalDimensions);
            trigger.style.width = assessmentDimensions.width;
            trigger.style.minWidth = assessmentDimensions.minWidth;
            trigger.style.height = assessmentDimensions.height;
            trigger.style.minHeight = assessmentDimensions.minHeight;
            delete trigger.dataset.assessmentOriginalDimensions;
        }
        trigger.disabled = false;
        trigger.removeAttribute('aria-busy');
    }

    function open(rawUrl, title, trigger) {
        if (!rawUrl || !window.bootstrap || !window.bootstrap.Modal) return;
        var target = withCurrentSessionUrl(new URL(rawUrl, window.location.href)); target.searchParams.set('modal', '1');
        var modal = modalElement(), frame = modal.querySelector('[data-gape-assessment-modal-frame]'), loading = modal.querySelector('[data-gape-assessment-loading]'), heading = modal.querySelector('[data-gape-assessment-modal-title]');
        modal._gapeAssessmentTrigger = trigger || null;
        if (heading) heading.textContent = title || 'Create Assessment';
        if (loading) loading.classList.remove('d-none');
        setTriggerLoading(trigger, true);
        if (frame) {
            frame.classList.add('d-none');
            frame.onload = function () {
                if (loading) loading.classList.add('d-none');
                frame.classList.remove('d-none');
                setTriggerLoading(modal._gapeAssessmentTrigger, false);
                window.bootstrap.Modal.getOrCreateInstance(modal).show();
            };
            frame.onerror = function () {
                setTriggerLoading(modal._gapeAssessmentTrigger, false);
            };
            frame.src = target.toString();
        }
    }

    document.addEventListener('click', function (event) {
        var trigger = event.target.closest('[data-assessment-modal-url]');
        if (trigger) { event.preventDefault(); open(trigger.dataset.assessmentModalUrl, trigger.dataset.assessmentModalTitle || 'Create Assessment', trigger); return; }
        var close = event.target.closest('[data-gape-assessment-modal-close]');
        if (close && window.parent !== window) { event.preventDefault(); window.parent.postMessage({ type: 'gape:assessment:close' }, window.location.origin); }
    });
    window.addEventListener('message', function (event) {
        if (event.origin !== window.location.origin || !event.data) return;
        var modal = document.getElementById(modalId);
        if (event.data.type === 'gape:assessment:changed' || event.data.type === 'gape:assessment:result') {
            if (modal && window.bootstrap && window.bootstrap.Modal) {
                var reloadScheduled = false;
                var reload = function () {
                    if (reloadScheduled) return;
                    reloadScheduled = true;
                    if (modal.parentNode) modal.parentNode.removeChild(modal);
                    document.body.classList.remove('modal-open');
                    document.querySelectorAll('.modal-backdrop').forEach(function (backdrop) { backdrop.remove(); });
                    window.location.reload();
                };
                modal.addEventListener('hidden.bs.modal', reload, { once: true });
                window.bootstrap.Modal.getOrCreateInstance(modal).hide();
                window.setTimeout(reload, 500);
            } else {
                window.location.reload();
            }
        }
        if (event.data.type === 'gape:assessment:close' && modal) window.bootstrap.Modal.getOrCreateInstance(modal).hide();
    });
    window.GapeAssessmentModal = { open: open };
}(window, document));
