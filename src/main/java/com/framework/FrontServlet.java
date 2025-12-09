package main.java.com.framework;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.com.annote.ClasspathScanner;
import main.java.com.annote.RouteInfo;
import main.java.com.annote.RequestParam;
import main.java.com.annote.PathVariable;
import main.java.com.framework.ModelyAndView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;

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

        // Recherche de la route correspondante (supporte les variables de chemin /produits/{id})
        RouteInfo found = null;
        Map<String, String> pathVariables = new HashMap<>();
        for (RouteInfo r : routes) {
            if (!r.getType().equalsIgnoreCase(method)) continue;
            Map<String, String> vars = matchPath(url, r.getUrl());
            if (vars != null) {
                found = r;
                pathVariables = vars;
                break;
            }
        }

        res.setContentType("text/html");

        if (found != null) {


            // === Invocation par réflexion de la méthode trouvée ===
            try {
                Class<?> controllerClass = Class.forName(found.getNomClasse());
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

                // Résoudre la méthode par nom, quelle que soit sa signature
                Method target = null;
                for (Method m : controllerClass.getDeclaredMethods()) {
                    if (m.getName().equals(found.getNomMethode())) {
                        target = m;
                        break;
                    }
                }
                if (target == null) {
                    throw new NoSuchMethodException("Méthode " + found.getNomMethode() + " introuvable dans " + controllerClass.getName());
                }

                target.setAccessible(true);

                // Préparer les arguments à partir des paramètres de requête (@RequestParam), de chemin (@PathVariable)
                // ou d'objets métier (entités/POJO) et Map<String,Object>
                Parameter[] params = target.getParameters();
                Object[] args = new Object[params.length];
                for (int i = 0; i < params.length; i++) {
                    Parameter p = params[i];
                    RequestParam rp = p.getAnnotation(RequestParam.class);
                    PathVariable pv = p.getAnnotation(PathVariable.class);
                    String raw = null;
                    if (rp != null) {
                        raw = req.getParameter(rp.value());
                        args[i] = convertValue(raw, p.getType());
                    } else if (pv != null) {
                        raw = pathVariables.get(pv.value());
                        args[i] = convertValue(raw, p.getType());
                    } else if (isEntityType(p.getType())) {
                        // Liaison d'une entité (POJO) à partir des paramètres de requête, y compris listes imbriquées
                        try {
                            Object bean = p.getType().getDeclaredConstructor().newInstance();

                            // 1) Remplir d'abord les propriétés "simples" avec BeanUtils.populate
                            //    On exclut les champs indexés de type liste (ex: typeClient[0].ecole)
                            Map<String, String[]> simpleParams = new HashMap<>();
                            for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                                String key = entry.getKey();
                                if (key.contains("[")) continue; // laissé aux listes imbriquées
                                simpleParams.put(key, entry.getValue());
                            }
                            BeanUtils.populate(bean, simpleParams);

                            // 2) Puis gère les propriétés de type List<Entite>
                            populateNestedLists(bean, req.getParameterMap());
                            args[i] = bean;
                        } catch (Exception e) {
                            e.printStackTrace();
                            args[i] = null;
                        }
                    } else if (Map.class.isAssignableFrom(p.getType())) {
                        // Injection d'une Map<String,Object> contenant tous les paramètres de requête
                        Map<String, Object> mapArg = new HashMap<>();
                        for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                            String key = entry.getKey();
                            String[] values = entry.getValue();
                            if (values == null) {
                                mapArg.put(key, null);
                            } else if (values.length == 1) {
                                mapArg.put(key, values[0]);
                            } else {
                                mapArg.put(key, values);
                            }
                        }
                        args[i] = mapArg;
                    } else {
                        // Pas d'annotation et pas de type spécial géré: laisser null par défaut
                        args[i] = null;
                    }
                }

                Object result = target.invoke(controllerInstance, args);

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

    private Object convertValue(String raw, Class<?> type) {
        // Gestion des LocalDate explicitement (format par défaut yyyy-MM-dd)
        if (type == LocalDate.class) {
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(raw);
            } catch (Exception e) {
                return null;
            }
        }

        // Gestion des LocalDateTime pour les inputs HTML datetime-local (yyyy-MM-dd'T'HH:mm)
        if (type == LocalDateTime.class) {
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            try {
                // datetime-local envoie typiquement: 2025-11-26T13:05
                DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                return LocalDateTime.parse(raw, fmt);
            } catch (Exception e) {
                return null;
            }
        }

        // Laisser BeanUtils gérer les autres conversions courantes
        if (raw == null) {
            if (!type.isPrimitive()) return null;
            // Pour les primitifs, BeanUtils fournira déjà des valeurs par défaut
            // mais on gère le cas null de manière sûre
            return ConvertUtils.convert("", type);
        }

        try {
            return ConvertUtils.convert(raw, type);
        } catch (Exception e) {
            return null;
        }
    }

    // Retourne les variables de chemin extraites si le chemin correspond au template, sinon null.
    // Exemple: requestPath="/produits/42" et pattern="/produits/{id}" -> {id=42}
    private Map<String, String> matchPath(String requestPath, String pattern) {
        if (requestPath == null || pattern == null) return null;

        String req = requestPath;
        String pat = pattern;

        if (!req.startsWith("/")) req = "/" + req;
        if (!pat.startsWith("/")) pat = "/" + pat;

        String[] reqSeg = req.split("/");
        String[] patSeg = pat.split("/");
        if (reqSeg.length != patSeg.length) return null;

        Map<String, String> vars = new HashMap<>();
        for (int i = 0; i < patSeg.length; i++) {
            String ps = patSeg[i];
            String rs = reqSeg[i];

            if (ps.isEmpty() && rs.isEmpty()) continue; // leading segment

            if (ps.startsWith("{") && ps.endsWith("}")) {
                String name = ps.substring(1, ps.length() - 1).trim();
                if (!name.isEmpty()) {
                    vars.put(name, rs);
                }
            } else {
                if (!ps.equals(rs)) {
                    return null; // segment fixe qui ne correspond pas
                }
            }
        }

        return vars;
    }

    // Remplit les propriétés de type List<POJO> d'un bean à partir des paramètres de requête
    // Convention de nommage: fieldName[0].property, fieldName[1].property, ...
    private void populateNestedLists(Object bean, Map<String, String[]> paramMap) {
        Class<?> beanClass = bean.getClass();

        // Pour chaque propriété List<T> déclarée dans le bean
        for (java.lang.reflect.Field field : beanClass.getDeclaredFields()) {
            Type genericType = field.getGenericType();
            if (!(genericType instanceof ParameterizedType)) continue;

            ParameterizedType pt = (ParameterizedType) genericType;
            if (!(field.getType().isAssignableFrom(List.class))) continue;

            Type[] args = pt.getActualTypeArguments();
            if (args.length != 1 || !(args[0] instanceof Class<?>)) continue;

            Class<?> elementType = (Class<?>) args[0];
            if (!isEntityType(elementType)) continue;

            String fieldName = field.getName();
            // Pattern: fieldName[<index>].property
            Pattern pattern = Pattern.compile(Pattern.quote(fieldName) + "\\[(\\d+)]\\.(.+)");

            // Regrouper les valeurs par index
            Map<Integer, Map<String, String[]>> perIndex = new HashMap<>();
            for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                Matcher m = pattern.matcher(entry.getKey());
                if (!m.matches()) continue;
                int idx = Integer.parseInt(m.group(1));
                String prop = m.group(2);
                perIndex.computeIfAbsent(idx, k -> new HashMap<>())
                        .put(prop, entry.getValue());
            }

            if (perIndex.isEmpty()) continue;

            List<Object> list = new ArrayList<>();
            // Reconstruire la liste en ordre d'index croissant
            perIndex.keySet().stream().sorted().forEach(idx -> {
                Map<String, String[]> values = perIndex.get(idx);
                try {
                    Object elem = elementType.getDeclaredConstructor().newInstance();
                    BeanUtils.populate(elem, values);
                    list.add(elem);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            try {
                field.setAccessible(true);
                field.set(bean, list);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    // Détermine si un type peut être traité comme entité métier (POJO) pour binding automatique
    private boolean isEntityType(Class<?> type) {
        if (type.isPrimitive()) return false;
        Package pkg = type.getPackage();
        String pkgName = (pkg != null) ? pkg.getName() : "";
        if (pkgName.startsWith("java.")) return false;
        if (Map.class.isAssignableFrom(type)) return false;
        // On pourrait exclure ici d'autres types spéciaux si besoin
        return true;
    }
}
