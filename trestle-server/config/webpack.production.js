/**
 * Created by nrobison on 1/20/17.
 */
const ngtools = require("@ngtools/webpack");
const webpackMerge = require("webpack-merge");
const ExtractTextPlugin = require("extract-text-webpack-plugin");
const OptimizeJsPlugin = require('optimize-js-plugin');
const UglifyJsPlugin = require('uglifyjs-webpack-plugin');
const DefinePlugin = require('webpack/lib/DefinePlugin');
const CSPWebpackPlugin = require("csp-webpack-plugin");
const CompressionPlugin = require("compression-webpack-plugin");
const BrotliPlugin = require("brotli-webpack-plugin");
const commonConfig = require("./webpack.common");
const helpers = require("./helpers");
const env = require("./env");

var prodOptions = {
    entry: {
        "polyfills": "./src/main/webapp/polyfills.ts",
        "vendor": "./src/main/webapp/vendor.ts",
        "workspace": "./src/main/webapp/workspace/workspace.bootstrap.ts",
        "evaluation": "./src/main/webapp/evaluation/evaluation.bootstrap.ts"
    },
    devtool: "source-map",
    output: {
        path: helpers.root("src/main/resources/build"),
        filename: "[name].[chunkhash].bundle.js",
        sourceMapFilename: "[file].map",
        chunkFilename: "[name].[chunkhash].chunk.js"
    },
    module: {
        // If we optimize comparisons, mapbox will fail.
        // https://github.com/mapbox/mapbox-gl-js/issues/4359#issuecomment-288001933
        noParse: /(mapbox-gl)\.js$/,
        rules: [
            {
                test: /(?:\.ngfactory\.js|\.ngstyle\.js|\.ts)$/,
                loader: "@ngtools/webpack"
            }
        ]
    },
    plugins: [
        new ngtools.AngularCompilerPlugin({
            tsConfigPath: helpers.root("tsconfig.json"),
            entryModule: helpers.root("src/main/webapp/workspace/workspace.module#WorkspaceModule"),
            skipCodeGeneration: true
        }),
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
                mangle: true, // debug false
                output: {
                    comments: false,
                    beautify: false  // debug true
                },
                // Below are the only options we change from their default settings
                compress: {
                    drop_console: true,
                    comparisons: true
                }
            }
        }),
        // Merge the common CSP configuration along with the script settings that disallow inline execution, since we're all AOT now
        new CSPWebpackPlugin(Object.assign(env.csp, {
            "script-src": ["'self'", "'unsafe-inline'", "'unsafe-eval'", "blob:"]
        })),
        // new CSPWebpackPlugin(Object.assign(env.csp, {
        //     "script-src": ["'self'"]
        // }))
        new CompressionPlugin({
            asset: "[path].gz[query]",
            algorithm: "gzip",
            test: /\.js$|\.css$|\.html$/,
            threshold: 10240,
            minRatio: 0.8
        }),
        new BrotliPlugin({
            asset: "[path].br[query]",
            test: /\.js$|\.css$|\.html$/,
            threshold: 10240,
            minRatio: 0.8
        })
    ]
};

module.exports = webpackMerge(commonConfig, prodOptions);