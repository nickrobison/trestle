/**
 * Created by nrobison on 1/17/17.
 */
const webpackMerge = require("webpack-merge");
const ExtractTextPlugin = require("extract-text-webpack-plugin");
const commonConfig = require("./webpack.common");
const helpers = require("./helpers");

var devOptions = {
    devtool: "source-map",
    output: {
        path: helpers.root("src/main/resources/build"),
        publicPath: "admin/",
        filename: "[name].bundle.js",
        sourceMapFilename: "[name].map",
        chunkFilename: "[id].chunk.js"
    },
    plugins: [
        new ExtractTextPlugin("[name].css")
    ]
};

module.exports = webpackMerge(commonConfig, devOptions);
