package com.trainingsplan.dto.ladv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One running track (distance offering) within a LADV "stadionfern" event.
 * Mapped from the {@code laufstrecken[]} array in the LADV JSON response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LadvLaufstrecke {
    public String laufname;
    public String klassenZusammenfassung;
    public Integer streckeMeter;
    public String dlvDisziplinName;
    public String dlvDisziplinType;
    public Boolean bestenlistenfaehig;
}
