package com.dapp.whoscored_scrapper.webservice;

import com.dapp.whoscored_scrapper.model.dto.PlayerDTO;
import com.dapp.whoscored_scrapper.service.PlayerService;

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
@RequestMapping("/api/searchPlayer")
@Tag(name = "Player Info", description = "Endpoints to get player information.")
@RequiredArgsConstructor
public class PlayerController {

  private final PlayerService playerService;

  @Operation(summary = "Search and get player information by name", description = "Searches for a player by name on WhoScored and extracts their details. AUTHENTICATION REQUIRED!")
  @GetMapping("/playerName")
  public ResponseEntity<PlayerDTO> getPlayerInfoByName(
      @Parameter(description = "Name of the player to search for.", example = "Lionel Messi") @RequestParam("playerName") String playerName) {
    PlayerDTO player = playerService.getPlayerInfoByName(playerName);
    return ResponseEntity.ok(player);
  }
}
