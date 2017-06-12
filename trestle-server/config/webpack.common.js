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
                test: /\.(jpe?g|png|gif)$/i,
                loaders: [
                    "file-loader?hash=sha512&digest=hex&name=[hash].[ext]",
                    {
                        loader: "image-webpack-loader",
                        query: {
                            bypassOnDebug: true,
                            progressive: true,
                            optimizationLevel: 7,
                            optipng: {
                                optimizationLevel: 7
                            }
                        }
                    }
                ]
            },
            {
                test: /\.(woff|woff2|ttf|eot|ico)$/,
                loader: "url-loader",
                options: {
                    limit: 50000,
                    name: "fonts/[name].[hash].[ext]"
                }
            },
            {
                test: /\.css$/,
                loaders: ["to-string-loader", "css-loader"],
                exclude: /\.async\.(html|css)$/
            },
            {
                test: /\.async\.(html|css)$/,
                loaders: ['file?name=[name].[hash].[ext]', 'extract']
            },
            {
                test: /\.scss$/,
                loaders: ["to-string-loader", "css-loader", "resolve-url-loader", "sass-loader?sourceMap"]
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
