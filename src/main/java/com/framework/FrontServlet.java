package main.java.com.framework;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.com.annote.ClasspathScanner;
import main.java.com.annote.RouteInfo;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

@WebServlet(name = "FrontServlet", urlPatterns = "/front")
public class FrontServlet extends HttpServlet {
    private List<RouteInfo> routes;

    @Override
    public void init() throws ServletException {
        System.out.println("=== Initialisation du mini framework ===");
        routes = ClasspathScanner.scanClasspath();
        System.out.println("Routes détectées :");
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

            // === Invocation par réflexion de la méthode trouvée ===
            try {
                Class<?> controllerClass = Class.forName(found.getNomClasse());
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                Method target = controllerClass.getDeclaredMethod(found.getNomMethode());
                target.setAccessible(true);
                Object result = target.invoke(controllerInstance);
                System.out.println("   ↳ Résultat de l'exécution: " + String.valueOf(result));
                // Optionnel: afficher aussi dans la réponse HTTP
                res.getWriter().write("<h4>Résultat: " + String.valueOf(result) + "</h4>");
            } catch (Throwable t) {
                System.out.println("❗ Erreur lors de l'invocation: " + t.getClass().getName() + " - " + t.getMessage());
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                res.getWriter().write("<pre>Erreur d'exécution: " + t.getMessage() + "</pre>");
                return;
            }

            // === Affichage dans le navigateur ===
            res.getWriter().write("<h1>URL : " + found.getUrl() + "</h1>");
            res.getWriter().write("<h2>Méthode : " + found.getNomMethode() + "</h2>");
            res.getWriter().write("<h3>Classe : " + found.getNomClasse() + "</h3>");
        } else {
            System.out.println("❌ Aucune correspondance trouvée pour " + method + " " + url);
            res.getWriter().write("<h1>Aucune correspondance trouvée</h1>");
        }
    }

    // @Override
    // protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    //     Object requestedPath = req.getAttribute("__requestedPath");
    //     String url = requestedPath != null ? requestedPath.toString() : req.getRequestURI();
    //     res.setContentType("text/html");
    //     res.getWriter().write("<h1>HELLO WORLD</h1>");
    //     res.getWriter().write("<h1>URL  : " + url + "</h1>");
    // }
    // @Override
    // protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    //     String path = req.getRequestURI().replace(req.getContextPath(), "");
    //     String methodType = req.getMethod();
        
    //     RouteInfo matched = routes.stream()
    //         .filter(r -> r.getUrl().equals(path) && r.getType().equalsIgnoreCase(methodType))
    //         .findFirst()
    //         .orElse(null);

    //     res.setContentType("text/html");
    //     if (matched != null) {
    //         res.getWriter().write("<h1>URL : " + path + "</h1>");
    //         res.getWriter().write("<h2>Méthode : " + matched.getNomMethode() + "</h2>");
    //         res.getWriter().write("<h3>Classe : " + matched.getNomClasse() + "</h3>");
    //     } else {
    //         res.getWriter().write("<h1>Aucune route correspondante</h1>");
    //     }
    // }
}
