package main.java.com.annotation;

import java.lang.reflect.Method;

public class AnnotationProcessor {
    public static void processAnnotations(Object instance) {
        System.out.println("=== Traitement des annotations Andrana ===");
        
        Class<?> clazz = instance.getClass();
        
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Andrana.class)) {
                Andrana annotation = method.getAnnotation(Andrana.class);
                String url = annotation.url();
                
                System.out.println("📌 Méthode: " + method.getName());
                System.out.println("🔗 URL: " + url);
                
                try {
                    // Exécuter la méthode
                    method.invoke(instance);
                } catch (Exception e) {
                    System.err.println("Erreur: " + e.getMessage());
                }
                System.out.println("----------");
            }
        }
    }
    
    public static void processAnnotations(Class<?> clazz) {
        try {
            processAnnotations(clazz.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            System.err.println("Erreur lors de la création de l'instance: " + e.getMessage());
        }
    }
}
