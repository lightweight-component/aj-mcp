const pluginLess = require("eleventy-plugin-less");
const fs = require('node:fs/promises');
const path = require('node:path');
const { indexPage } = require('./lib/search-index');

module.exports = function (eleventyConfig) {
    eleventyConfig.addPlugin(pluginLess);
    const searchPages = new Map();
    eleventyConfig.on('eleventy.before', ({ incremental }) => {
        if (!incremental)
            searchPages.clear();
    });
    eleventyConfig.addTransform('documentation-search', function (content) {
        if (!this.page.outputPath || !this.page.outputPath.endsWith('.html'))
            return content;
        const result = indexPage(content, this.page.url);
        if (result.entries.length)
            searchPages.set(this.page.url, result.entries);
        return result.html;
    });
    eleventyConfig.on('eleventy.after', async ({ directories, outputMode }) => {
        if (outputMode !== 'fs')
            return;
        const destination = path.join(directories.output, 'search');
        const entries = Array.from(searchPages.values()).flat().sort((a, b) => a.url.localeCompare(b.url));
        await fs.mkdir(destination, { recursive: true });
        await Promise.all(['en', 'zh'].map(language => fs.writeFile(path.join(destination, language + '.json'),
            JSON.stringify({ version: 1, entries: entries.filter(entry => entry.language === language) }))));
    });
    // Re-scan templates when Markdown files change. This also picks up newly added
    // templates when using a non-default input directory (`src`) with Eleventy 3.1.x.
    eleventyConfig.addWatchTarget('src/**/*.md', { resetConfig: true });
    eleventyConfig.addPassthroughCopy('src/asset');
    // Restrict image discovery to sources so generated dist files are never copied back into dist.
    eleventyConfig.addPassthroughCopy("src/**/*.jpg");
    eleventyConfig.addPassthroughCopy("src/**/*.png");
    eleventyConfig.addPassthroughCopy('src/style/reset.css');
    // eleventyConfig.addWatchTarget('src/style');
    // eleventyConfig.addPassthroughCopy("bundle.css");

    	// the default is "copy"
	// eleventyConfig.setServerPassthroughCopyBehavior("passthrough");

    return {
        dir: {
            input: "src",
            output: "dist"
        }
    }
};
