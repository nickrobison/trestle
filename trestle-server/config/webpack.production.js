/**
 * Created by nrobison on 1/20/17.
 */
const webpackMerge = require("webpack-merge");
const ExtractTextPlugin = require("extract-text-webpack-plugin");
const commonConfig = require("./webpack.common");
const helpers = require("./helpers");

var prodOptions = {
    entry: {
        "polyfills": "./src/main/webapp/polyfills.ts",
        "vendor": "./src/main/webapp/vendor.ts",
        "app": "./src/main/webapp/bootstrap.ts"
    },
    devtool: "source-map",
    output: {
        path: helpers.root("src/main/resources/build"),
        publicPath: "static/",
        filename: "[name].bundle.js",
        sourceMapFilename: "[name].map",
        chunkFilename: "[id].chunk.js"
    },
    plugins: [
        new ExtractTextPlugin("[name].css")
    ]
};

module.exports = webpackMerge(commonConfig, prodOptions);