/**
 * Created by nrobison on 1/17/17.
 */
const webpackMerge = require("webpack-merge");
const ExtractTextPlugin = require("extract-text-webpack-plugin");
const DefinePlugin = require('webpack/lib/DefinePlugin');
const CSPWebpackPlugin = require("csp-webpack-plugin");
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const commonConfig = require("./webpack.common");
const helpers = require("./helpers");
const env = require("./env");

var devOptions = {
    entry: {
        "polyfills": "./src/main/webapp/polyfills.ts",
        "vendor": "./src/main/webapp/vendor.ts",
        "workspace": "./src/main/webapp/workspace/workspace.bootstrap.ts",
        "evaluation": "./src/main/webapp/evaluation/evaluation.bootstrap.ts"

    },
    devtool: "source-map",
    output: {
        path: helpers.root("target/classes/build"),
        filename: "[name].bundle.js",
        sourceMapFilename: "[name].map",
        chunkFilename: "[id].chunk.js"
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: [
                    {
                        loader: "awesome-typescript-loader"
                    },
                    {
                        loader: "angular2-template-loader",
                        options: {
                            keepUrl: true
                        }
                    },
                    {
                        loader: "angular-router-loader"
                    }
                ],
                exclude: [/\.(spec|e2e)\.ts$/]
            }
        ]
    },
    plugins: [
        new ExtractTextPlugin("[name].css"),
        new DefinePlugin({
            ENV: JSON.stringify("development")
        }),
        // Merge the common CSP configuration along with the script settings to allow dynamic execution
        new CSPWebpackPlugin(Object.assign(env.csp, {
            "script-src": ["'self'", "'unsafe-inline'", "'unsafe-eval'", "blob:"]
        })),
        new BundleAnalyzerPlugin()
    ]
};

module.exports = webpackMerge(commonConfig, devOptions);
