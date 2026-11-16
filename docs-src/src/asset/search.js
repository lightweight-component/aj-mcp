/* Site integration only: the reusable component is loaded from AJ-Util. */
(function () {
    'use strict';
    function mount() {
        document.querySelectorAll('[data-mcp-search]').forEach(function (container) {
            if (container.staticSearch)
                return;
            var chinese = container.dataset.language === 'zh';
            if (typeof StaticSearch !== 'function') {
                var message = document.createElement('p');
                message.setAttribute('role', 'status');
                message.textContent = chinese ? '搜索组件暂不可用，请刷新页面重试。' : 'Search is unavailable. Please reload to retry.';
                container.appendChild(message);
                return;
            }
            var navigation = container.closest('.docs-navigation');
            container.addEventListener('static-search:querychange', function (event) {
                if (navigation)
                    navigation.classList.toggle('is-searching', Boolean(event.detail.query));
            });
            container.staticSearch = new StaticSearch(container, {
                indexUrl: '/search/' + (chinese ? 'zh' : 'en') + '.json',
                language: chinese ? 'zh' : 'en',
                theme: 'light',
                limit: 20
            });
        });
    }
    if (document.readyState === 'loading')
        document.addEventListener('DOMContentLoaded', mount, { once: true });
    else
        mount();
})();
