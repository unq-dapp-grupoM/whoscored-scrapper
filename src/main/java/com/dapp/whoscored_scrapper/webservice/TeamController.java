package com.dapp.whoscored_scrapper.webservice;

import com.dapp.whoscored_scrapper.model.dto.TeamDTO;
import com.dapp.whoscored_scrapper.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teamInfo")
@Tag(name = "Team Info", description = "Endpoints for team information.")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "Search and get team info", description = "Find a team and your players. AUTHENTICATION REQUIRED!")
    @GetMapping("/teamName")
    public ResponseEntity<TeamDTO> getTeamInfoByName(
            @Parameter(description = "Name of the team to search for.", example = "Real Madrid") @RequestParam("teamName") String teamName) {
        TeamDTO team = teamService.getTeamInfoByName(teamName);
        return ResponseEntity.ok(team);
    }
}
