/**
 * Created by nrobison on 1/20/17.
 */
const webpackMerge = require("webpack-merge");
const ExtractTextPlugin = require("extract-text-webpack-plugin");
const OptimizeJsPlugin = require('optimize-js-plugin');
const UglifyJsPlugin = require('uglifyjs-webpack-plugin');
const DefinePlugin = require('webpack/lib/DefinePlugin');
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
        publicPath: "/static/",
        filename: "[name].[chunkhash].bundle.js",
        sourceMapFilename: "[file].map",
        chunkFilename: "[name].[chunkhash].chunk.js"
    },
    module: {
        noParse: /(mapbox-gl)\.js$/
    },
    plugins: [
        new ExtractTextPlugin("[name].css"),
        new DefinePlugin({
            ENV: JSON.stringify("production")
        }),
        new OptimizeJsPlugin({
            sourceMap: false
        }),
        new UglifyJsPlugin({
            parallel: true,
            uglifyOptions: {
                ie8: false,
                ecma: 5,
                warnings: true,
                mangle: true, // debug false
                output: {
                    comments: false,
                    beautify: false  // debug true
                },
                // If we optimize comparisons, mapbox will fail.
                // https://github.com/mapbox/mapbox-gl-js/issues/4359#issuecomment-288001933
                compress: {
                    comparisons: false
                }
            }
        })
    ]
};

module.exports = webpackMerge(commonConfig, prodOptions);