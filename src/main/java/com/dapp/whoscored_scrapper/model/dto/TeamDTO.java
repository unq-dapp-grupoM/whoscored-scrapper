package com.dapp.whoscored_scrapper.model.dto;

import java.util.List;
import lombok.Data;

@Data
public class TeamDTO {
    private String name;
    private List<TeamPlayerDTO> squad;
}
