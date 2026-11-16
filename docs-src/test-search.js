const assert = require('node:assert/strict');
const { indexPage, plainText } = require('./lib/search-index');
const fixture = '<html lang="zh-CN"><nav>noise</nav><article class="aj-text"><p>前言</p>'
    + '<h1>MCP &amp; Java</h1><p>初始化</p><h2>工具</h2><p>调用 callTool()。</p>'
    + '<h2>工具</h2><p>重复标题</p><h3 id="custom">保留锚点</h3><p>说明</p></article><footer>noise</footer></html>';
const indexed = indexPage(fixture, '/client/tool-cn/');
assert.equal(indexed.entries.length, 5);
assert.equal(indexed.entries[0].text, '前言');
assert.equal(indexed.entries[1].title, 'MCP & Java');
assert.ok(indexed.entries.every(entry => entry.language === 'zh' && entry.project === 'aj-mcp'));
assert.ok(indexed.html.includes('id="search-工具-2"'));
assert.ok(indexed.entries.some(entry => entry.url.endsWith('#custom')));
assert.ok(!indexed.entries.some(entry => entry.text.includes('noise')));
assert.ok(indexed.entries.some(entry => entry.text.includes('callTool()')));
assert.deepEqual(indexPage('<article class="version">landing</article>', '/').entries, []);
assert.equal(indexPage(fixture.replace('zh-CN', 'en'), '/client/tool/').entries[0].language, 'en');
assert.equal(plainText('text<script>bad</script><!--bad--> &lt;x&gt;'), 'text <x>');
console.log('AJ-MCP search indexing, languages, duplicate/preserved anchors and document-only extraction verified.');
