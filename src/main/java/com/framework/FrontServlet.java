package main.java.com.framework;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.com.annote.ClasspathScanner;
import main.java.com.annote.RouteInfo;
import main.java.com.framework.ModelyAndView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@WebServlet(name = "FrontServlet", urlPatterns = "/")
public class FrontServlet extends HttpServlet {
    private List<RouteInfo> routes;

    @Override
    public void init() throws ServletException {
        System.out.println("=== Initialisation du mini framework ===");
        routes = ClasspathScanner.scanClasspath();
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


            // === Invocation par réflexion de la méthode trouvée ===
            try {
                Class<?> controllerClass = Class.forName(found.getNomClasse());
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                Method target = controllerClass.getDeclaredMethod(found.getNomMethode());
                target.setAccessible(true);
                Object result = target.invoke(controllerInstance);

                // Si la méthode retourne un ModelyAndView, afficher la page si elle existe
                if (result instanceof ModelyAndView) {
                    ModelyAndView mv = (ModelyAndView) result;

                    // Injecter le modèle comme attributs de requête
                    for (Map.Entry<String, Object> entry : mv.getModel().entrySet()) {
                        req.setAttribute(entry.getKey(), entry.getValue());
                    }

                    String view = mv.getNomDeFichier();
                    if (view == null || view.isEmpty()) {
                        res.getWriter().write("<em>vue vide</em>");
                        return;
                    }
                    if (!view.startsWith("/")) view = "/" + view;
                    String[] candidates;
                    if (view.endsWith(".jsp") || view.endsWith(".html")) {
                        candidates = new String[]{view};
                    } else {
                        candidates = new String[]{view + ".jsp", view + ".html"};
                    }

                    ServletContext context = getServletContext();
                    boolean served = false;
                    for (String path : candidates) {
                        try (InputStream is = context.getResourceAsStream(path)) {
                            if (is != null) {
                                if (path.endsWith(".jsp")) {
                                    // Laisser le container exécuter la JSP
                                    req.getRequestDispatcher(path).forward(req, res);
                                } else {
                                    String mime = context.getMimeType(path);
                                    if (mime == null) mime = "text/html";
                                    res.setContentType(mime);
                                    try (OutputStream os = res.getOutputStream()) {
                                        byte[] buffer = new byte[8192];
                                        int len;
                                        while ((len = is.read(buffer)) != -1) {
                                            os.write(buffer, 0, len);
                                        }
                                    }
                                }
                                served = true;
                                break;
                            }
                        }
                    }

                    if (!served) {
                        String nameForMsg = view.startsWith("/") ? view.substring(1) : view;
                        res.getWriter().write(nameForMsg + " non trouve");
                    }
                    return; // ne pas écrire d'autres contenus après avoir servi la vue
                }

                // Optionnel: afficher aussi dans la réponse HTTP
                res.getWriter().write("<h4>Résultat: " + String.valueOf(result) + "</h4>");
            } catch (Throwable t) {
                System.out.println("❗ Erreur lors de l'invocation: " + t.getClass().getName() + " - " + t.getMessage());
                t.printStackTrace();
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
}
