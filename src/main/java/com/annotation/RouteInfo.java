package main.java.com.annotation;

public class RouteInfo {
    private String nomClasse;
    private String nomMethode;
    private String url;
    private String type;

    public RouteInfo(String nomClasse, String nomMethode, String url, String type) {
        this.nomClasse = nomClasse;
        this.nomMethode = nomMethode;
        this.url = url;
        this.type = type;
    }

    public String getNomClasse() { return nomClasse; }
    public String getNomMethode() { return nomMethode; }
    public String getUrl() { return url; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return "[" + type + "] " + url + " => " + nomClasse + "." + nomMethode;
    }
}
