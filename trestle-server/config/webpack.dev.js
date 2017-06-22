/**
 * Created by nrobison on 1/17/17.
 */
const webpackMerge = require("webpack-merge");
const ExtractTextPlugin = require("extract-text-webpack-plugin");
const commonConfig = require("./webpack.common");
const helpers = require("./helpers");

var devOptions = {
    entry: {
        "polyfills": "./src/main/webapp/polyfills.ts",
        "vendor": "./src/main/webapp/vendor.ts",
        "app": "./src/main/webapp/bootstrap.ts"
    },
    devtool: "source-map",
    output: {
        path: helpers.root("target/classes/build"),
        publicPath: "/static/",
        filename: "[name].bundle.js",
        sourceMapFilename: "[name].map",
        chunkFilename: "[id].chunk.js"
    },
    plugins: [
        new ExtractTextPlugin("[name].css")
    ]
};

module.exports = webpackMerge(commonConfig, devOptions);
