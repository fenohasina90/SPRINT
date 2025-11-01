package main.java.com.annotation;

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

                        for (Method method : clazz.getDeclaredMethods()) {
                            if (method.isAnnotationPresent(GETY.class)) {
                                GETY get = method.getAnnotation(GETY.class);
                                System.out.println("   🔹 Méthode GETY : " + method.getName() + " -> " + get.value());
                                routes.add(new RouteInfo(clazz.getSimpleName(), method.getName(), get.value(), "GET"));
                            } else if (method.isAnnotationPresent(POSTA.class)) {
                                POSTA post = method.getAnnotation(POSTA.class);
                                System.out.println("   🔸 Méthode POSTA : " + method.getName() + " -> " + post.value());
                                routes.add(new RouteInfo(clazz.getSimpleName(), method.getName(), post.value(), "POST"));
                            }
                        }
                    }
                } catch (Throwable ignored) {
                    // Certaines classes (ex: internes) peuvent ne pas être chargées correctement, on ignore.
                }
            }
        }
    }
}
