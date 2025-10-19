package main.java.com.framework;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FrontServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String url = req.getRequestURI();
        res.setContentType("text/html");
        res.getWriter().write("<h1>HELLO WORLD</h1>");
        res.getWriter().write("<h1>URL  : " + url + "</h1>");
    }
}
