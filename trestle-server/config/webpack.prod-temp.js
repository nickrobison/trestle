/**
 * Created by nrobison on 1/17/17.
 */
const webpackMerge = require("webpack-merge");
const ExtractTextPlugin = require("extract-text-webpack-plugin");
const DefinePlugin = require('webpack/lib/DefinePlugin');
const CSPWebpackPlugin = require("csp-webpack-plugin");
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const CompressionPlugin = require("compression-webpack-plugin");
const BrotliPlugin = require("brotli-webpack-plugin");
const OptimizeJsPlugin = require('optimize-js-plugin');
const UglifyJsPlugin = require('uglifyjs-webpack-plugin');
const commonConfig = require("./webpack.common");
const helpers = require("./helpers");
const env = require("./env");

var plugins = [
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
            // Below are the only options we change from their default settings
            compress: {
                drop_console: true,
                comparisons: true
            }
        }
    }),
    // Merge the common CSP configuration along with the script settings that disallow inline execution, since we're all AOT now
    // We also need to allow Cloudflare to do its thing
    new CSPWebpackPlugin(Object.assign(env.csp, {
        "script-src": ["'self'", "'unsafe-inline'", "'unsafe-eval'", "blob:", "https://ajax.cloudflare.com"]
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
];

// If analyze is enabled, enable the bundle analyzer and Jarvis
if (env.analyze) {
    plugins.push([
        new BundleAnalyzerPlugin()
    ]);
}

var devOptions = {
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
                test: /\.tsx?$/,
                use: [
                    {
                        loader: "cache-loader"
                    },
                    {
                        loader: "thread-loader",
                        options: {
                            workers: require("os").cpus().length - 2
                        }
                    },
                    {
                        loader: "ts-loader",
                        options: {
                            happyPackMode: true,
                            transpileOnly: true
                        }
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
    plugins: plugins
};

module.exports = webpackMerge(commonConfig, devOptions);
