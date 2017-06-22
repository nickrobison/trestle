package com.nickrobison.trestle.server;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by nrobison on 6/22/17.
 */
public class URLRewriter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest servletRequest = (HttpServletRequest) request;
        final HttpServletResponse servletResponse = (HttpServletResponse) response;
        final String requestURI = servletRequest.getRequestURI();
        if (shouldRewrite(requestURI)) {
            servletResponse.sendRedirect("/workspace/");
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }

    private static boolean shouldRewrite(String requestURI) {
        if (!requestURI.endsWith("workspace/") && !requestURI.contains("static")) {
            return true;
        }
        return false;
    }
}
