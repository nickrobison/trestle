/**
 * Created by nrobison on 1/17/17.
 */
"use strict";

const helpers = require("./helpers");
const webpack = require("webpack");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const rxPaths = require("rxjs/_esm5/path-mapping");
const Jarvis = require("webpack-jarvis");

var options = {
    resolve: {
        extensions: [".webpack.js", ".web.js", ".ts", ".tsx", ".js"],
        alias: rxPaths()
    },
    output: {
        publicPath: "/static/"
    },
    module: {
        rules: [
            {
                test: /\.html$/,
                loader: "html-loader"
            },
            {
                test: /\.(jpe?g|png|gif|svg)$/i,
                use: [
                    {
                        loader: "file-loader",
                        options: {
                            hash: "sha512",
                            digest: "hex",
                            name: "[hash].[ext]"
                        }
                    },
                    {
                        loader: "image-webpack-loader",
                        options: {
                            bypassOnDebug: true,
                            optpng: {
                                optimizationLevel: 7
                            },
                            mozjpeg: {
                                progressive: true
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
                use: [
                    {
                        loader: "to-string-loader"
                    },
                    {
                        loader: "css-loader"
                    }
                ],
                exclude: /\.async\.(html|css)$/
            },
            {
                test: /\.async\.(html|css)$/,
                use:[
                    {
                        loader: "file-loader",
                        options: {
                            name: "[name].[hash].[ext]"
                        }
                    },
                    {
                        loader: "extract"
                    }
                ]
            },
            {
                test: /\.scss$/,
                use: [
                    {
                        loader: "to-string-loader"
                    },
                    {
                        loader: "css-loader"
                    },
                    {
                        loader: "resolve-url-loader"
                    },
                    {
                        loader: "sass-loader",
                        options: {
                            sourceMap: true
                        }
                    }
                ]
            }
        ]
    },
    plugins: [
        // Extract the common code from both main application
        new webpack.optimize.CommonsChunkPlugin({
            name: "common",
            chunks: ["workspace", "evaluation"]
        }),
        new webpack.optimize.ModuleConcatenationPlugin(),
        new HtmlWebpackPlugin({
            inject: true,
            chunks: ["polyfills", "vendor", "common", "workspace"],
            chunksSortMode: "manual",
            template: helpers.root("src/main/webapp/workspace/workspace.index.html"),
            filename: "workspace.index.html"
        }),
        new HtmlWebpackPlugin({
            inject: true,
            chunks: ["vendor", "polyfills", "common", "evaluation"],
            chunksSortMode: "manual",
            template: helpers.root("src/main/webapp/evaluation/evaluation.index.html"),
            filename: "evaluation.index.html"
        })
        // new Jarvis()
    ]
};
module.exports = options;
