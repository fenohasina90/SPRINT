package  main.java.com.annotation;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ScannerRacineProjet {
    
    /**
     * Scanner à partir du répertoire racine du projet
     */
    public static void scannerDepuisRacine() {
        System.out.println("=== Scan depuis la racine du projet ===");
        
        // Obtenir le répertoire racine du projet
        File racineProjet = obtenirRacineProjet();
        if (racineProjet == null) {
            System.out.println("Impossible de déterminer la racine du projet");
            return;
        }
        
        System.out.println("Racine du projet: " + racineProjet.getAbsolutePath());
        
        List<Class<?>> classesAnnotees = new ArrayList<>();
        scannerRepertoireRecursif(racineProjet, racineProjet, classesAnnotees);
        
        afficherClassesTrouvees(classesAnnotees);
    }
    
    private static File obtenirRacineProjet() {
        try {
            // Méthode 1: Via le classloader
            // URL url = ScanProjectAvecReflections.class.getResource("");
            URL url = ScannerRacineProjet.class.getResource("");
            if (url != null) {
                String chemin = url.getPath();
                File fichier = new File(chemin);
                
                // Remonter jusqu'à la racine du projet
                while (fichier != null && !contientFichierRacine(fichier)) {
                    fichier = fichier.getParentFile();
                }
                return fichier;
            }
            
            // Méthode 2: Via le répertoire de travail
            return new File(System.getProperty("user.dir"));
            
        } catch (Exception e) {
            return new File(System.getProperty("user.dir"));
        }
    }
    
    private static boolean contientFichierRacine(File repertoire) {
        String[] fichiersRacine = {"pom.xml", "build.gradle", ".git", "src"};
        for (String nomFichier : fichiersRacine) {
            if (new File(repertoire, nomFichier).exists()) {
                return true;
            }
        }
        return false;
    }
    
    private static void scannerRepertoireRecursif(File racine, File repertoire, List<Class<?>> classesAnnotees) {
        File[] fichiers = repertoire.listFiles();
        if (fichiers == null) return;
        
        for (File fichier : fichiers) {
            if (fichier.isDirectory()) {
                // Ignorer certains répertoires
                if (!fichier.getName().equals("target") && 
                    !fichier.getName().equals("build") &&
                    !fichier.getName().equals(".git") &&
                    !fichier.getName().equals(".idea")) {
                    scannerRepertoireRecursif(racine, fichier, classesAnnotees);
                }
            } else if (fichier.getName().endsWith(".class")) {
                traiterFichierClass(racine, fichier, classesAnnotees);
            }
        }
    }
    
    private static void traiterFichierClass(File racine, File fichierClass, List<Class<?>> classesAnnotees) {
        try {
            String cheminRelatif = racine.toURI().relativize(fichierClass.toURI()).getPath();
            String nomClasse = cheminRelatif.replace('/', '.')
                                           .replace('\\', '.')
                                           .replace(".class", "");
            
            // Nettoyer le nom de classe
            nomClasse = nettoyerNomClasse(nomClasse);
            
            if (nomClasse != null) {
                Class<?> classe = Class.forName(nomClasse);
                if (classe.isAnnotationPresent(AnnotationClasse.class)) {
                    classesAnnotees.add(classe);
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs de chargement
        }
    }
    
    private static String nettoyerNomClasse(String nomClasse) {
        // Supprimer les préfixes indésirables
        if (nomClasse.startsWith("bin.")) nomClasse = nomClasse.substring(4);
        if (nomClasse.startsWith("target.classes.")) nomClasse = nomClasse.substring(15);
        if (nomClasse.startsWith("build.classes.")) nomClasse = nomClasse.substring(14);
        
        // Ignorer les classes anonymes et internes
        if (nomClasse.contains("$")) return null;
        
        return nomClasse;
    }
    
    private static void afficherClassesTrouvees(List<Class<?>> classes) {
        if (classes.isEmpty()) {
            System.out.println("Aucune classe annotée trouvée.");
        } else {
            System.out.println("Classes annotées trouvées (" + classes.size() + ") :");
            for (Class<?> classe : classes) {
                // AnnotationClasse annotation = classe.getAnnotation(AnnotationClasse.class);
                System.out.println("✓ " + classe.getSimpleName() + " (" + classe.getPackage().getName() + ")");
                // System.out.println("  Valeur: " + annotation.value());
                // System.out.println("  Description: " + annotation.description());
                System.out.println();
            }
        }
    }
    
    // public static void main(String[] args) {
    //     scannerDepuisRacine();
    // }
}