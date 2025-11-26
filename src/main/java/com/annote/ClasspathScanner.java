package main.java.com.annote;

import main.java.com.annote.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClasspathScanner {

    public static List<RouteInfo> scanClasspath() {
        List<RouteInfo> routes = new ArrayList<>();

        System.out.println("=== 🔍 DÉBUT DU SCAN CLASSPATH ===");

        try {
            // Récupère tous les chemins du classpath (ex: WEB-INF/classes/)
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = loader.getResources("");

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File rootDir = new File(resource.toURI());
                System.out.println("📂 Scan du dossier : " + rootDir.getAbsolutePath());
                scanDirectory(rootDir, "", routes);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("=== ✅ FIN DU SCAN CLASSPATH ===");
        System.out.println("Nombre total de routes détectées : " + routes.size());
        routes.forEach(r ->
            System.out.println("➡ " + r.getType() + " " + r.getUrl()
                    + " -> " + r.getNomClasse() + "." + r.getNomMethode())
        );

        return routes;
    }

    private static void scanDirectory(File directory, String packageName, List<RouteInfo> routes) {
        if (directory == null || !directory.exists()) return;

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                String subPackage = packageName.isEmpty() ? file.getName() : packageName + "." + file.getName();
                scanDirectory(file, subPackage, routes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);

                    if (clazz.isAnnotationPresent(Controllera.class)) {
                        System.out.println("\n🧩 Classe contrôleur trouvée : " + clazz.getName());

                        // Préfixe d'URL de la classe via @RequestMapping (optionnel)
                        String basePath = "";
                        if (clazz.isAnnotationPresent(RequestMapping.class)) {
                            RequestMapping rm = clazz.getAnnotation(RequestMapping.class);
                            basePath = rm.value();
                        }

                        for (Method method : clazz.getDeclaredMethods()) {
                            if (method.isAnnotationPresent(GETY.class)) {
                                GETY get = method.getAnnotation(GETY.class);
                                String fullPath = normalizePath(basePath, get.value());
                                System.out.println("   🔹 Méthode GETY : " + method.getName() + " -> " + fullPath);
                                routes.add(new RouteInfo(clazz.getName(), method.getName(), fullPath, "GET"));
                            } else if (method.isAnnotationPresent(POSTA.class)) {
                                POSTA post = method.getAnnotation(POSTA.class);
                                String fullPath = normalizePath(basePath, post.value());
                                System.out.println("   🔸 Méthode POSTA : " + method.getName() + " -> " + fullPath);
                                routes.add(new RouteInfo(clazz.getName(), method.getName(), fullPath, "POST"));
                            }
                        }
                    }
                } catch (Throwable ignored) {
                    // Certaines classes (ex: internes) peuvent ne pas être chargées correctement, on ignore.
                }
            }
        }
    }

    // Concatène proprement un préfixe de classe et un chemin de méthode en gérant les '/'
    private static String normalizePath(String base, String methodPath) {
        if (base == null) base = "";
        if (methodPath == null) methodPath = "";

        String b = base.trim();
        String m = methodPath.trim();

        if (!b.isEmpty() && !b.startsWith("/")) b = "/" + b;
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);

        if (!m.isEmpty() && !m.startsWith("/")) m = "/" + m;

        String result = b + m;
        if (result.isEmpty()) {
            return "/";
        }
        return result;
    }
}