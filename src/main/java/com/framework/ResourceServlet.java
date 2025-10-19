package com.myFramework;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@WebServlet(name = "ResourceServlet", urlPatterns = "/*")
public class ResourceServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String contextPath = req.getContextPath();            // e.g. /test_framework
        String uri = req.getRequestURI();                     // e.g. /test_framework/page.html
        String resourcePath = uri.substring(contextPath.length()); // e.g. /page.html

        // Normalize and default to index.html for root
        if (resourcePath == null || resourcePath.isEmpty() || "/".equals(resourcePath)) {
            resourcePath = "/index.html";
        }

        // If requesting a JSP, forward so the container compiles/executes it
        if (resourcePath.endsWith(".jsp")) {
            req.getRequestDispatcher(resourcePath).forward(req, resp);
            return;
        }

        ServletContext context = getServletContext();
        try (InputStream is = context.getResourceAsStream(resourcePath)) {
            if (is != null) {
                String mime = context.getMimeType(resourcePath);
                if (mime == null) mime = "text/plain";
                resp.setContentType(mime);

                try (OutputStream os = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                }
                return;
            }
        }

        // Not found: forward to FrontServlet fallback
        // Pass the requested path without leading slash for display
        String displayPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        req.setAttribute("__requestedPath", displayPath);
        req.getRequestDispatcher("/front").forward(req, resp);
    }
}
