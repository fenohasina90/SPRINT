package main.java.com.annote;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClasspathScanner {

    public static List<RouteInfo> scanClasspath() {
        List<RouteInfo> routes = new ArrayList<>();
        try {
            // Récupère tous les chemins du classpath (ex: WEB-INF/classes/)
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = loader.getResources("");

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File rootDir = new File(resource.toURI());
                scanDirectory(rootDir, "", routes);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (method.isAnnotationPresent(GETY.class)) {
                                GETY get = method.getAnnotation(GETY.class);
                                routes.add(new RouteInfo(clazz.getName(), method.getName(), get.value(), "GET"));
                            } else if (method.isAnnotationPresent(POSTA.class)) {
                                POSTA post = method.getAnnotation(POSTA.class);
                                routes.add(new RouteInfo(clazz.getName(), method.getName(), post.value(), "POST"));
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