/**
 * Created by nrobison on 1/17/17.
 */
const webpackMerge = require("webpack-merge");
const ExtractTextPlugin = require("extract-text-webpack-plugin");
const DefinePlugin = require('webpack/lib/DefinePlugin');
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
        filename: "[name].bundle.js",
        sourceMapFilename: "[name].map",
        chunkFilename: "[id].chunk.js"
    },
    module: {
        loaders: [
            {
                test: /\.tsx?$/,
                loaders: ["awesome-typescript-loader", "angular2-template-loader?keepUrl=true", "angular2-router-loader"],
                exclude: [/\.(spec|e2e)\.ts$/]
            },
        ]
    },
    plugins: [
        new ExtractTextPlugin("[name].css"),
        new DefinePlugin({
            ENV: JSON.stringify("development")
        })
    ]
};

module.exports = webpackMerge(commonConfig, devOptions);
