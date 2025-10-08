package com.dapp.whoscored_scrapper.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMatchDTO {
    private String cup;
    private String date;
    private String redCardsActualTeam;
    private String redCardsRivalTeam;
    private String actualTeam;
    private String rivalTeam;
    private String score;
    private String result;
}