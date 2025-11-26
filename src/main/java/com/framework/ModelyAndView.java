package main.java.com.framework;

import java.util.HashMap;
import java.util.Map;

public class ModelyAndView {
    private String nomDeFichier;
    private Map<String, Object> model = new HashMap<>();

    public ModelyAndView(String nomDeFichier) {
        this.nomDeFichier = nomDeFichier;
    }

    public String getNomDeFichier() {
        return nomDeFichier;
    }

    public void setNomDeFichier(String nomDeFichier) {
        this.nomDeFichier = nomDeFichier;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public ModelyAndView addObject(String name, Object value) {
        this.model.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return "ModelyAndView{" +
                "nomDeFichier='" + nomDeFichier + '\'' +
                ", model=" + model +
                '}';
    }
}
