/**
 * Created by nrobison on 1/17/17.
 */
"use strict";

const helper = require("./helpers");
const webpack = require("webpack");
const HtmlWebpackPlugin = require("html-webpack-plugin");

var options = {
    resolve: {
        extensions: [".webpack.js", ".web.js", ".ts", ".tsx", ".js"]
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
                test: /\.(jpe?g|png|gif)$/i,
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
        new webpack.optimize.CommonsChunkPlugin({
            name: ["app", "vendor", "polyfills"]
        }),
        new HtmlWebpackPlugin({
            template: "./src/main/webapp/app/index.html"
        }),
        new webpack.ContextReplacementPlugin(
            /(.+)?angular(\\|\/)core(.+)?/,
            helper.root("/src/main/webapp/")
        )
    ]
};
module.exports = options;
