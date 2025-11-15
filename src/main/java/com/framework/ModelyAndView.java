package main.java.com.framework;

public class ModelyAndView {
    private String nomDeFichier;

    public ModelyAndView(String nomDeFichier) {
        this.nomDeFichier = nomDeFichier;
    }

    public String getNomDeFichier() {
        return nomDeFichier;
    }

    public void setNomDeFichier(String nomDeFichier) {
        this.nomDeFichier = nomDeFichier;
    }

    @Override
    public String toString() {
        return "ModelyAndView{" +
                "nomDeFichier='" + nomDeFichier + '\'' +
                '}';
    }
}
