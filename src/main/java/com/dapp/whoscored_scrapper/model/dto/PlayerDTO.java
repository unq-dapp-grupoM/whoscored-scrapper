package com.dapp.whoscored_scrapper.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class PlayerDTO {
    private String name;
    private String shirtNumber;
    private String age;
    private String height;
    private String positions;
    private String nationality;
    private String currentTeam;
    private List<PlayerMatchStatsDTO> matchStats;
}