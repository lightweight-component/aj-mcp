# Documents for AJ-MCP

Powered by [Eleventy](https://www.11ty.dev/).

This is a Node.js project. Run `npm install` to install dependencies and `npm run dev` to start the development server.

Easy to write HTML content in this environment, just pure HTML/CSS/JS is enough, plus Markdown to HTML is supported.

## Documentation search

Documentation pages use the shared browser component from:

- `https://aj-util.ajaxjs.com/asset/js/static-search.js`
- `https://aj-util.ajaxjs.com/asset/static-search.css`

The two document layouts load these resources before the local `/asset/search.js` adapter. No component source is copied into this repository. The adapter selects `/search/en.json` or `/search/zh.json` on the AJ-MCP site, not AJ-Util; `/asset/search.css` only integrates the site's theme and mobile navigation.

`lib/search-index.js` and `eleventy.config.js` extract sections from rendered documentation articles, preserve existing heading IDs, add missing ones, and generate version-1 JSON indexes. Client, server, architecture, Next and other document pages are included; the marketing landing page and navigation/footer text are excluded. Update source pages rather than editing generated JSON.

From `docs-src`, run:

```sh
npm test
npm run build
```

Tests build into `.test-dist`, check both languages, shared resource load order and every indexed heading anchor, then remove that temporary output. The normal build writes `dist/search/en.json` and `dist/search/zh.json` alongside the pages and local adapter. Deploy these together; a local build does not publish the site. Use HTTP/HTTPS, not `file://`, for search.

Optional real-browser validation after building: `node test-search-browser.js`. It requires Playwright, a browser, and network access to the shared resources; `AJ_DOCS_PLAYWRIGHT` and `AJ_DOCS_BROWSER` can override the module/browser locations. It checks real shared-resource loading, local-only index requests, Chinese/English matching, highlights, chapter navigation, mobile/dark layout and failed-index retry. If the shared component cannot load, the adapter displays a localized unavailable message instead of breaking the documentation page.

## Less.js support

Install https://www.npmjs.com/package/eleventy-plugin-less
