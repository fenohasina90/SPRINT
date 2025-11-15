package main.java.com.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // Disponible à l'exécution
@Target(ElementType.METHOD)
public @interface Andrana {
    String url();
    String message() default "URL affichée :";
}
