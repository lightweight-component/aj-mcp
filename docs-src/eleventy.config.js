const pluginLess = require("eleventy-plugin-less");

module.exports = function (eleventyConfig) {
    eleventyConfig.addPlugin(pluginLess);
    // Re-scan templates when Markdown files change. This also picks up newly added
    // templates when using a non-default input directory (`src`) with Eleventy 3.1.x.
    eleventyConfig.addWatchTarget('src/**/*.md', { resetConfig: true });
    eleventyConfig.addPassthroughCopy('src/asset');
    eleventyConfig.addPassthroughCopy("**/*.jpg");
    eleventyConfig.addPassthroughCopy("**/*.png");
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
