package com.dapp.whoscored_scrapper.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerMatchStatsDTO {
    private String opponent;
    private String score;
    private String date;
    private String position;
    private String minsPlayed;
    private String goals;
    private String assists;
    private String yellowCards;
    private String redCards;
    private String shots;
    private String passSuccess;
    private String aerialsWon;
    private String rating;
}