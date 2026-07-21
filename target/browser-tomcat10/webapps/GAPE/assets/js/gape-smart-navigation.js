(function () {
    function ready(callback) {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', callback);
            return;
        }
        callback();
    }

    ready(function () {
        if (!document.querySelector('.dashbord')) {
            return;
        }

        var contextPath = contextPathFromBase();
        var currentPath = relativePathWithoutReturnTo(window.location);
        var inheritedReturnTo = new URLSearchParams(window.location.search).get('returnTo');
        var safeInheritedReturnTo = safeReturnPath(inheritedReturnTo) ? inheritedReturnTo : '';

        if (safeInheritedReturnTo) {
            document.querySelectorAll('a[href]').forEach(function (anchor) {
                if (isBackAnchor(anchor)) {
                    anchor.href = contextPath + safeInheritedReturnTo;
                }
            });
            document.querySelectorAll('form[method="post"], form[method="POST"]').forEach(function (form) {
                if (!form.querySelector('input[name="returnTo"]')) {
                    var input = document.createElement('input');
                    input.type = 'hidden';
                    input.name = 'returnTo';
                    input.value = safeInheritedReturnTo;
                    form.appendChild(input);
                }
            });
        }

        document.addEventListener('click', function (event) {
            var anchor = event.target.closest('a[href]');
            if (!anchor || event.defaultPrevented || anchor.target || anchor.hasAttribute('download')) {
                return;
            }
            var url;
            try {
                url = new URL(anchor.href, window.location.href);
            } catch (ignored) {
                return;
            }
            if (url.origin !== window.location.origin || url.searchParams.has('returnTo')) {
                return;
            }
            var relativePath = stripContextPath(url.pathname);
            if (!shouldCarryReturnTo(relativePath)) {
                return;
            }
            url.searchParams.set('returnTo', currentPath);
            anchor.href = url.toString();
        }, true);

        function contextPathFromBase() {
            var base = document.querySelector('base[href]');
            if (!base) {
                return '';
            }
            try {
                var baseUrl = new URL(base.href, window.location.href);
                return baseUrl.pathname.replace(/\/$/, '');
            } catch (ignored) {
                return '';
            }
        }

        function stripContextPath(pathname) {
            if (contextPath && pathname.indexOf(contextPath + '/') === 0) {
                return pathname.substring(contextPath.length);
            }
            return pathname;
        }

        function relativePathWithoutReturnTo(locationLike) {
            var pathname = stripContextPath(locationLike.pathname);
            var params = new URLSearchParams(locationLike.search);
            params.delete('returnTo');
            var query = params.toString();
            return pathname + (query ? '?' + query : '');
        }

        function safeReturnPath(path) {
            return !!path
                    && path.charAt(0) === '/'
                    && path.indexOf('//') !== 0
                    && path.indexOf('\\') < 0
                    && path.indexOf(':') < 0;
        }

        function isBackAnchor(anchor) {
            var text = (anchor.textContent || '').replace(/\s+/g, ' ').trim().toLowerCase();
            return text === 'back' || text.indexOf('back ') === 0;
        }

        function shouldCarryReturnTo(relativePath) {
            return /\/(new|edit)$/.test(relativePath)
                    || /^\/learning\/(lessons|rooms|class-groups)\/[^/?#]+$/.test(relativePath)
                    || /^\/student\/lessons\/[^/?#]+$/.test(relativePath)
                    || /^\/admin\/(users|organizations|courses|subjects)\/[^/?#]+$/.test(relativePath)
                    || /^\/admin\/organizations\/[^/?#]+\/units\/[^/?#]+$/.test(relativePath);
        }
    });
})();
