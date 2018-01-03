/**
 * Created by nrobison on 6/10/17.
 */
var configure = function () {
    this.setDefaultTimeout(60 * 1000);
};

var cspData = {
    "default-src": ["'self'"],
    "style-src": ["'self'", "'unsafe-inline'", "https://fonts.googleapis.com"],
    "img-src": ["'self'", "data:", "blob:", "https://www.gravatar.com"],
    "font-src": ["'self'", "https://fonts.gstatic.com"],
    "connect-src": ["'self'", "https://*.mapbox.com"],
    "worker-src": ["'self'","blob:"]
};

exports.configure = configure;
exports.csp = cspData;
