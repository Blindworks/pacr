package com.trainingsplan.dto.ladv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LadvStartstelle {
    public String startstelle;
    public String strasse;
    public String ort;
    public String plz;
}
