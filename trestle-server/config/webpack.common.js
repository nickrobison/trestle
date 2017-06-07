/**
 * Created by nrobison on 1/17/17.
 */
"use strict";

const helper = require("./helpers");
const webpack = require("webpack");
const ExtractTextPlugin = require("extract-text-webpack-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");

var options = {
    resolve: {
        extensions: [".webpack.js", ".web.js", ".ts", ".tsx", ".js"]
    },
    module: {
        loaders: [
            {
                test: /\.tsx?$/,
                loaders: ["awesome-typescript-loader", "angular2-template-loader?keepUrl=true", "angular2-router-loader"],
                exclude: [/\.(spec|e2e)\.ts$/]
            },
            {
                test: /\.html$/,
                loader: "html-loader"
            },
            {
                test: /\.(jpe?g|png|gif|svg)$/i,
                loaders: [
                    "file-loader?hash=sha512&digest=hex&name=[hash].[ext]",
                    "image-webpack-loader?bypassOnDebug&optimizationLevel=7&interlaced=false"
                ]
            },
            // {
            //     test: /\.(png|jpe?g|gif|svg|woff|woff2|ttf|eot|ico)$/,
            //     loader: "file-loader?name=assets/[name].[hash].[ext]"
            // },
            {
                test: /\.css$/,
                loader: "raw-loader",
                exclude: /\.async\.(html|css)$/
            },
            {
                test: /\.async\.(html|css)$/,
                loaders: ['file?name=[name].[hash].[ext]', 'extract']
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
