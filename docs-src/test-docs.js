const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const output = path.join(__dirname, '.test-dist');
const sharedJs = 'https://aj-util.ajaxjs.com/asset/js/static-search.js';
const sharedCss = 'https://aj-util.ajaxjs.com/asset/static-search.css';
try {
    for (const language of ['en', 'zh']) {
        const index = JSON.parse(fs.readFileSync(path.join(output, 'search', language + '.json'), 'utf8'));
        assert.equal(index.version, 1);
        assert.ok(index.entries.length > 0);
        const pages = new Set(index.entries.map(entry => entry.url.split('#')[0]));
        for (const section of ['client', 'server', 'architecture', 'next'])
            assert.ok(Array.from(pages).some(url => url.startsWith('/' + section + '/')), 'missing ' + section);
        assert.ok(!pages.has('/'), 'marketing landing page must not enter the documentation index');
        for (const entry of index.entries) {
            assert.equal(entry.language, language);
            assert.equal(entry.project, 'aj-mcp');
            const [url, anchor] = entry.url.split('#');
            const html = fs.readFileSync(path.join(output, url, 'index.html'), 'utf8');
            if (anchor)
                assert.ok(html.includes('id="' + decodeURIComponent(anchor) + '"'), 'missing anchor ' + entry.url);
            assert.ok(html.includes('src="' + sharedJs + '"'));
            assert.ok(html.includes('href="' + sharedCss + '"'));
            assert.ok(html.indexOf('src="' + sharedJs + '"') < html.indexOf('src="/asset/search.js"'));
            assert.ok(html.includes('data-mcp-search data-language="' + language + '"'));
        }
        console.log(language + ': ' + pages.size + ' documentation pages, ' + index.entries.length + ' search sections verified.');
    }
    assert.ok(fs.existsSync(path.join(output, 'asset', 'search.js')));
    assert.ok(fs.existsSync(path.join(output, 'asset', 'search.css')));
} finally {
    fs.rmSync(output, { recursive: true, force: true });
}
