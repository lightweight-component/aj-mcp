const markdown = require('markdown-it')();

/** Extract searchable text from rendered Markdown, excluding non-document code. */
function plainText(html) {
    return markdown.utils.unescapeAll(html
        .replace(/<!--[\s\S]*?-->/g, '')
        .replace(/<(script|style)\b[^>]*>[\s\S]*?<\/\1\s*>/gi, '')
        .replace(/<[^>]+>/g, ' ')).replace(/\s+/g, ' ').trim();
}

/** Add stable heading anchors and derive sections from the same HTML we publish. */
function indexPage(html, url) {
    const project = 'aj-mcp';
    const article = /<article\b[^>]*class=["'][^"']*\baj-text\b[^"']*["'][^>]*>([\s\S]*?)<\/article>/i.exec(html);
    if (!article)
        return { html, entries: [] };

    const language = /<html\b[^>]*lang=["']zh/i.test(html) ? 'zh' : 'en';
    const used = new Set(Array.from(html.matchAll(/\bid=["']([^"']+)["']/g), match => match[1]));
    const body = article[1].replace(/<h([1-6])\b([^>]*)>([\s\S]*?)<\/h\1>/gi, (tag, level, attrs, text) => {
        if (/\bid\s*=/.test(attrs))
            return tag;

        const base = 'search-' + (plainText(text).toLowerCase().replace(/[^\p{L}\p{N}_-]+/gu, '-').replace(/^-|-$/g, '') || 'section');
        let id = base;
        let number = 2;
        while (used.has(id))
            id = base + '-' + number++;

        used.add(id);
        return `<h${level}${attrs} id="${id}">${text}</h${level}>`;
    });
    const headings = Array.from(body.matchAll(/<h([1-6])\b[^>]*\bid=["']([^"']+)["'][^>]*>([\s\S]*?)<\/h\1>/gi));
    const title = headings.length ? plainText(headings[0][3]) : project;
    const entries = [];
    const introduction = plainText(body.slice(0, headings.length ? headings[0].index : body.length));
    if (introduction)
        entries.push({ project, language, title, heading: title, url, text: introduction });

    headings.forEach((heading, i) => {
        const text = plainText(body.slice(heading.index + heading[0].length, i + 1 < headings.length ? headings[i + 1].index : body.length));
        entries.push({ project, language, title, heading: plainText(heading[3]),
            url: url + '#' + encodeURIComponent(heading[2]), text });
    });
    return { html: html.replace(article[0], article[0].replace(article[1], body)), entries };
}

module.exports = { indexPage, plainText };
