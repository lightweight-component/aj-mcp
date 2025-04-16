const pluginLess = require("eleventy-plugin-less");

module.exports = function (eleventyConfig) {
    eleventyConfig.addPlugin(pluginLess);
    eleventyConfig.addPassthroughCopy('src/asset');
    // eleventyConfig.addPassthroughCopy("**/*.jpg");
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