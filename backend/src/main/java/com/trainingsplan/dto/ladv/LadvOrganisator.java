package com.trainingsplan.dto.ladv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LadvOrganisator {
    public String anrede;
    public String vorname;
    public String nachname;
    public String strasse;
    public String ort;
    public String plz;
    public String phone;
    public String email;
}
