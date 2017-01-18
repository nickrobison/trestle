/**
 * Created by nrobison on 1/17/17.
 */
"use strict";

const helper = require("./helpers");
const webpack = require("webpack");
const ExtractTextPlugin = require("extract-text-webpack-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");

var options = {
    entry: {
        "polyfills": "./src/main/webapp/polyfills.ts",
        "vendor": "./src/main/webapp/vendor.ts",
        "app": "./src/main/webapp/bootstrap.ts"
    },
    resolve: {
        extensions: [".webpack.js", ".web.js", ".ts", ".tsx", ".js"]
    },
    module: {
        loaders: [
            {
                test: /\.tsx?$/,
                loaders: ["awesome-typescript-loader", "angular2-template-loader?keepUrl=true"]
            },
            {
                test: /\.html$/,
                loader: "html-loader"
            },
            {
                test: /\.(png|jpe?g|gif|svg|woff|woff2|ttf|eot|ico)$/,
                loader: "file?name=assets/[name].[hash].[ext]"
            },
            {
                test: /\.css$/,
                exclude: helper.root("src", "app"),
                loader: ExtractTextPlugin.extract(["style-loader", "css-loader?sourceMap"])
            },
            {
                test: /\.css$/,
                include: helper.root("src, app"),
                loader: "raw-loader"
            },
            {
                test: /\.scss$/,
                loaders: ["raw-loader", "sass-loader"]
            }
        ]
    },
    plugins: [
        new webpack.optimize.CommonsChunkPlugin({
            name: ["app", "vendor", "polyfills"]
        }),
        new HtmlWebpackPlugin({
            template: "./src/main/webapp/app/index.html"
        })
    ]
};
module.exports = options;
