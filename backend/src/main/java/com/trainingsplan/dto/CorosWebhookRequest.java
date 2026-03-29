package com.trainingsplan.dto;

import java.util.List;

public class CorosWebhookRequest {
    private List<CorosSportData> sportDataList;

    public List<CorosSportData> getSportDataList() { return sportDataList; }
    public void setSportDataList(List<CorosSportData> sportDataList) { this.sportDataList = sportDataList; }
}
