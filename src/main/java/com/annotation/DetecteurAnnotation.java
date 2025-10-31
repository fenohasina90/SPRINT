package main.java.com.annotation;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DetecteurAnnotation {
    
    /**
     * Détecte toutes les classes annotées avec @MonAnnotation dans un package
     */
    public static List<Class<?>> trouverClassesAnnotees(String packageName) {
        List<Class<?>> classesAnnotees = new ArrayList<>();
        
        try {
            // Obtenir le ClassLoader
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String cheminPackage = packageName.replace('.', '/');
            URL resource = classLoader.getResource(cheminPackage);
            
            if (resource != null) {
                File repertoire = new File(resource.getFile());
                if (repertoire.exists()) {
                    // Scanner les fichiers .class
                    scannerRepertoire(packageName, repertoire, classesAnnotees);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return classesAnnotees;
    }
    
    private static void scannerRepertoire(String packageName, File repertoire, List<Class<?>> classesAnnotees) {
        File[] fichiers = repertoire.listFiles();
        
        if (fichiers != null) {
            for (File fichier : fichiers) {
                if (fichier.isDirectory()) {
                    // Scanner récursivement les sous-répertoires
                    scannerRepertoire(packageName + "." + fichier.getName(), fichier, classesAnnotees);
                } else if (fichier.getName().endsWith(".class")) {
                    // Charger la classe
                    String nomClasse = packageName + '.' + fichier.getName().substring(0, fichier.getName().length() - 6);
                    
                    try {
                        Class<?> classe = Class.forName(nomClasse);
                        
                        // Vérifier si la classe a l'annotation
                        if (classe.isAnnotationPresent(AnnotationClasse.class)) {
                            classesAnnotees.add(classe);
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        // Ignorer les erreurs de chargement
                    }
                }
            }
        }
    }
    
    /**
     * Méthode alternative utilisant Reflections (nécessite la librairie Reflections)
     * Plus simple mais nécessite une dépendance externe
     */
    public static void afficherClassesAnnotees(String packageName) {
        System.out.println("=== Recherche des classes annotées avec @MonAnnotation ===");
        
        List<Class<?>> classes = trouverClassesAnnotees(packageName);
        
        if (classes.isEmpty()) {
            System.out.println("Aucune classe annotée trouvée.");
        } else {
            System.out.println("Classes trouvées (" + classes.size() + ") :");
            for (Class<?> classe : classes) {
                System.out.println("✓ " + classe.getSimpleName()); 
                                //  " - valeur: " + annotation.value() + 
                                //  " - description: " + annotation.description());
            }
        }
    }

}