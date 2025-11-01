package main.java.com.framework;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.com.annotation.ClasspathScanner;
import main.java.com.annotation.RouteInfo;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "FrontServlet", urlPatterns = "/front")
public class FrontServlet extends HttpServlet {
    private List<RouteInfo> routes;

    @Override
    public void init() throws ServletException {
        routes = ClasspathScanner.scanClasspath();
        routes.forEach(r ->
            System.out.println(" - " + r.getType() + " " + r.getUrl()
                    + " -> " + r.getNomClasse() + "." + r.getNomMethode())
        );
        System.out.println("=== Fin du scan ===");
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        // Check if we were forwarded from ResourceServlet with the original path
        Object requestedPath = req.getAttribute("__requestedPath");
        String url = requestedPath != null ? "/" + requestedPath : req.getRequestURI().replace(req.getContextPath(), "");
        String method = req.getMethod();

        // Recherche de la route correspondante
        RouteInfo found = routes.stream()
            .filter(r -> r.getUrl().equals(url) && r.getType().equalsIgnoreCase(method))
            .findFirst()
            .orElse(null);

        res.setContentType("text/html");

        if (found != null) {
            // === Affichage dans le terminal ===
            System.out.println("➡ Requête reçue : " + method + " " + url);
            System.out.println("   ↳ Classe : " + found.getNomClasse());
            System.out.println("   ↳ Méthode : " + found.getNomMethode());
            System.out.println("---------------------------------------");

            // === Affichage dans le navigateur ===
            res.getWriter().write("<h1>URL : " + found.getUrl() + "</h1>");
            res.getWriter().write("<h2>Méthode : " + found.getNomMethode() + "</h2>");
            res.getWriter().write("<h3>Classe : " + found.getNomClasse() + "</h3>");
        } else {
            System.out.println("❌ Aucune correspondance trouvée pour " + method + " " + url);
            res.getWriter().write("<h1>Aucune correspondance trouvée</h1>");
        }
    }
}
