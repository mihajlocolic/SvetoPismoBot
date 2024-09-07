package org.mihajlo.models;

public class DataModel {
    private String stihovi;
    private String knjiga;
    private String glava;
    private String pocetniStih;
    private String zavrsniStih;
    private String prevod;

    public DataModel() {
    }

    public DataModel(String stihovi, String knjiga, String glava, String pocetniStih, String zavrsniStih, String prevod) {
        this.stihovi = stihovi;
        this.knjiga = knjiga;
        this.glava = glava;
        this.pocetniStih = pocetniStih;
        this.zavrsniStih = zavrsniStih;
        this.prevod = prevod;
    }

    public String getStihovi() {
        return stihovi;
    }

    public void setStihovi(String stihovi) {
        this.stihovi = stihovi;
    }

    public String getKnjiga() {
        return knjiga;
    }

    public void setKnjiga(String knjiga) {
        this.knjiga = knjiga;
    }

    public String getGlava() {
        return glava;
    }

    public void setGlava(String glava) {
        this.glava = glava;
    }

    public String getPocetniStih() {
        return pocetniStih;
    }

    public void setPocetniStih(String pocetniStih) {
        this.pocetniStih = pocetniStih;
    }

    public String getZavrsniStih() {
        return zavrsniStih;
    }

    public void setZavrsniStih(String zavrsniStih) {
        this.zavrsniStih = zavrsniStih;
    }

    public String getPrevod() {
        return prevod;
    }

    public void setPrevod(String prevod) {
        this.prevod = prevod;
    }

    @Override
    public String toString() {
        return "DataModel{" +
                "stihovi='" + stihovi + '\'' +
                ", knjiga='" + knjiga + '\'' +
                ", glava='" + glava + '\'' +
                ", pocetniStih='" + pocetniStih + '\'' +
                ", zavrsniStih='" + zavrsniStih + '\'' +
                ", prevod='" + prevod + '\'' +
                '}';
    }
}
