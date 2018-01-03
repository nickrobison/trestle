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
const commonConfig = require("./webpack.common");
const helpers = require("./helpers");
const env = require("./env");

const AOT = process.env.AOT;

var prodOptions = {
    entry: {
        "polyfills": "./src/main/webapp/polyfills.ts",
        "vendor": "./src/main/webapp/vendor.ts",
        "app": "./src/main/webapp/bootstrap.ts"
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
            entryModule: helpers.root("src/main/webapp/app/app.module#AppModule")
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
                warnings: true,
                mangle: true, // debug false
                output: {
                    comments: false,
                    beautify: false  // debug true
                },
                compress: {
                    unused: true,
                    dead_code: true,
                    drop_debugger: true,
                    comparisons: true
                }
            }
        }),
        // Merge the common CSP configuration along with the script settings that disallow inline execution, since we're all AOT now
        new CSPWebpackPlugin(Object.assign(env.csp, {
            "script-src": ["'self'", "'nonce-YLMZop38Ktla8/hmmA=='"]
        }))
    ]
};

module.exports = webpackMerge(commonConfig, prodOptions);