package com.trainingsplan.dto;

import java.time.LocalDate;

public class BioWeatherDto {
    private Integer regionId;
    private String regionName;
    private Integer pollenBirch;
    private Integer pollenGrasses;
    private Integer pollenMugwort;
    private Integer pollenRagweed;
    private Integer pollenHazel;
    private Integer pollenAlder;
    private Integer pollenAsh;
    private Double temperature;
    private Integer humidity;
    private Double pm25;
    private Double ozone;
    private Integer asthmaRiskIndex;
    private String biowetterRisk;
    private LocalDate validDate;
    private String dataSource;

    public BioWeatherDto() {}

    public Integer getRegionId() { return regionId; }
    public void setRegionId(Integer regionId) { this.regionId = regionId; }

    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }

    public Integer getPollenBirch() { return pollenBirch; }
    public void setPollenBirch(Integer pollenBirch) { this.pollenBirch = pollenBirch; }

    public Integer getPollenGrasses() { return pollenGrasses; }
    public void setPollenGrasses(Integer pollenGrasses) { this.pollenGrasses = pollenGrasses; }

    public Integer getPollenMugwort() { return pollenMugwort; }
    public void setPollenMugwort(Integer pollenMugwort) { this.pollenMugwort = pollenMugwort; }

    public Integer getPollenRagweed() { return pollenRagweed; }
    public void setPollenRagweed(Integer pollenRagweed) { this.pollenRagweed = pollenRagweed; }

    public Integer getPollenHazel() { return pollenHazel; }
    public void setPollenHazel(Integer pollenHazel) { this.pollenHazel = pollenHazel; }

    public Integer getPollenAlder() { return pollenAlder; }
    public void setPollenAlder(Integer pollenAlder) { this.pollenAlder = pollenAlder; }

    public Integer getPollenAsh() { return pollenAsh; }
    public void setPollenAsh(Integer pollenAsh) { this.pollenAsh = pollenAsh; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Integer getHumidity() { return humidity; }
    public void setHumidity(Integer humidity) { this.humidity = humidity; }

    public Double getPm25() { return pm25; }
    public void setPm25(Double pm25) { this.pm25 = pm25; }

    public Double getOzone() { return ozone; }
    public void setOzone(Double ozone) { this.ozone = ozone; }

    public Integer getAsthmaRiskIndex() { return asthmaRiskIndex; }
    public void setAsthmaRiskIndex(Integer asthmaRiskIndex) { this.asthmaRiskIndex = asthmaRiskIndex; }

    public String getBiowetterRisk() { return biowetterRisk; }
    public void setBiowetterRisk(String biowetterRisk) { this.biowetterRisk = biowetterRisk; }

    public LocalDate getValidDate() { return validDate; }
    public void setValidDate(LocalDate validDate) { this.validDate = validDate; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
}
