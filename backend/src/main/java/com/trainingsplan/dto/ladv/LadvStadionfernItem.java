package com.trainingsplan.dto.ladv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Top-level shape of one item in the LADV {@code /stadionfern} array.
 * See <a href="https://ladv.de/entwickler">LADV PUBLIC API v2.014</a>.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LadvStadionfernItem {
    public Long id;
    public String veranstaltungsnummer;
    public String name;
    /** Time of day, e.g. "10:00" — LADV gives this as a free string. */
    public String beginn;
    /** Unix epoch milliseconds — LADV uses 0:00 Europe/Berlin for the start of the day. */
    public Long datum;
    public String datumText;
    public Long endeDatum;
    public String endeDatumText;
    public String veranstalter;
    public Boolean abgesagt;
    public String kategorie;
    public String homepage;
    public Boolean bestenlistenfaehig;
    public LadvOrganisator organisator;
    public LadvStartstelle startstelle;
    public List<LadvLaufstrecke> laufstrecken;
}
