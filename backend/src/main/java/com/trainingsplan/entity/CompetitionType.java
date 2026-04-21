package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CompetitionType {
    FIVE_K("5K"),
    TEN_K("10K"),
    TWENTY_K("20K"),
    HALF_MARATHON("Halbmarathon"),
    THIRTY_K("30K"),
    FORTY_K("40K"),
    MARATHON("Marathon"),
    FIFTY_K("50K"),
    HUNDRED_K("100K"),
    BACKYARD_ULTRA("Backyard Ultra"),
    CATCHER_CAR("Catcher car"),
    OTHER("Sonstige");

    private final String displayName;

    CompetitionType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static CompetitionType fromString(String value) {
        if (value == null) return null;
        for (CompetitionType t : values()) {
            if (t.displayName.equalsIgnoreCase(value) || t.name().equalsIgnoreCase(value)) {
                return t;
            }
        }
        return null;
    }
}
