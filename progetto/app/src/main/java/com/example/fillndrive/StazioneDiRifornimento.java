package com.example.fillndrive;

public class StazioneDiRifornimento {

    private int idImpianto;
    private String bandiera;
    private String comune;
    private double prezzo;
    private String latitudine;
    private String longitudine;

    public StazioneDiRifornimento(int idImpianto, String bandiera, String comune, double prezzo, String latitudine, String longitudine) {
        this.idImpianto = idImpianto;
        this.bandiera = bandiera;
        this.comune = comune;
        this.prezzo = prezzo;
        this.latitudine = latitudine;
        this.longitudine = longitudine;
    }

    public int getIdImpianto() {
        return idImpianto;
    }

    public String getBandiera() {
        return bandiera;
    }

    public String getComune() {
        return comune;
    }

    public double getPrezzo() {
        return prezzo;
    }

    public String getLatitudine() {
        return latitudine;
    }

    public String getLongitudine() {
        return longitudine;
    }

    public void setIdImpianto(int idImpianto) {
        this.idImpianto = idImpianto;
    }

    public void setBandiera(String bandiera) {
        this.bandiera = bandiera;
    }

    public void setComune(String comune) {
        this.comune = comune;
    }

    public void setPrezzo(double prezzo) {
        this.prezzo = prezzo;
    }

    public void setLatitudine(String latitudine) {
        this.latitudine = latitudine;
    }

    public void setLongitudine(String longitudine) {
        this.longitudine = longitudine;
    }

}
