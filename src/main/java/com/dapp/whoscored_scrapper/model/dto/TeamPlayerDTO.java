package com.dapp.whoscored_scrapper.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamPlayerDTO {
    private String name;
    private String age;
    private String position;
    private String height;
    private String weight;
    private String apps;
    private String minsPlayed;
    private String goals;
    private String assists;
    private String yellowCards;
    private String redCards;
    private String shotsPerGame;
    private String passSuccess;
    private String aerialsWonPerGame;
    private String manOfTheMatch;
    private String rating;
}