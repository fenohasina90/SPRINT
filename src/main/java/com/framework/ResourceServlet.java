package main.java.com.framework;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@WebServlet(name = "ResourceServlet", urlPatterns = "/static/*")
public class ResourceServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String contextPath = req.getContextPath();                 // e.g. /test_framework
        String uri = req.getRequestURI();                          // e.g. /test_framework/static/page.html
        String staticPrefix = contextPath + "/static";            // e.g. /test_framework/static
        String resourcePath = uri.substring(staticPrefix.length()); // e.g. /page.html ou /

        // Normalize and default to index.html for root
        if (resourcePath == null || resourcePath.isEmpty() || "/".equals(resourcePath)) {
            resourcePath = "/index.html";
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

        // Not found: you can choose to send 404 or delegate elsewhere. For now, send 404.
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
