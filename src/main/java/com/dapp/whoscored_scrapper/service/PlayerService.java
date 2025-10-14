package com.dapp.whoscored_scrapper.service;

import com.dapp.whoscored_scrapper.model.dto.PlayerDTO;
import com.dapp.whoscored_scrapper.model.dto.PlayerMatchStatsDTO;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerService extends AbstractWebService {

    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);

    public PlayerDTO getPlayerInfoByName(String playerName) {
        try (Playwright playwright = Playwright.create()) {
            Page page = createPage(playwright);

            // Search for the player
            performSearch(page, playerName);

            // Click on the first player result
            Locator firstResult = page
                    .locator("div.search-result:has(h2:text('Jugadores:')) >> tbody tr:nth-child(2) >> a")
                    .first();
            try {
                firstResult.waitFor(new Locator.WaitForOptions().setTimeout(15000));
            } catch (Exception e) {
                log.error("Player '{}' not found in search results or timed out.", playerName);
                throw new IllegalArgumentException("Player with name '" + playerName + "' not found.");
            }
            firstResult.click();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            // Scrape player data
            PlayerDTO playerDTO = scrapePlayerData(page);

            // Navigate to Match Statistics and scrape data
            page.getByText("Estadísticas del Partido").click();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            playerDTO.setMatchStats(scrapePlayerMatchStats(page));

            return playerDTO;
        } catch (IllegalArgumentException e) {
            log.error("An error occurred during scraping for player: {}", playerName, e);
            throw new IllegalArgumentException("Player with name 'Unknown Player' not found.", e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during scraping for player: {}", playerName, e);
            throw new RuntimeException("An unexpected error occurred while fetching player data.", e);
        }
    }

    public PlayerDTO scrapePlayerData(Page page) {
        PlayerDTO player = new PlayerDTO();

        // Get player info container
        Locator playerInfoContainer = page.locator("div.col12-lg-10.col12-m-10.col12-s-9.col12-xs-8");
        playerInfoContainer.waitFor(new Locator.WaitForOptions().setTimeout(10000));

        // Set player basic info
        player.setName(extractValueFromPlayerInfo(playerInfoContainer, "Nombre"));
        player.setCurrentTeam(extractValueFromPlayerInfo(playerInfoContainer, "Equipo Actual"));
        player.setShirtNumber(extractValueFromPlayerInfo(playerInfoContainer, "Número de Dorsal"));
        player.setAge(extractValueFromPlayerInfo(playerInfoContainer, "Edad").split(" ")[0].trim());
        player.setHeight(extractValueFromPlayerInfo(playerInfoContainer, "Altura"));
        player.setNationality(extractValueFromPlayerInfo(playerInfoContainer, "Nacionalidad"));
        player.setPositions(extractPlayerPositionsFromPlayerInfo(playerInfoContainer));

        return player;
    }

    private String extractValueFromPlayerInfo(Locator context, String label) {
        try {
            String selector = String.format("div.col12-lg-6:has(span.info-label:text-is('%s:'))", label);
            String fullText = context.locator(selector).first().innerText();
            return fullText.replace(label + ":", "").trim();
        } catch (Exception e) {
            log.warn("Could not extract value for label '{}'", label);
            return NOT_FOUND;
        }
    }

    private String extractPlayerPositionsFromPlayerInfo(Locator playerInfoContainer) {
        Locator positionsContainer = playerInfoContainer
                .locator("div:has(span.info-label:text-is('Posiciones:')) > span:not(.info-label)");
        if (positionsContainer.isVisible()) {
            String positions = positionsContainer.locator("span").all().stream()
                    .map(Locator::innerText)
                    .collect(Collectors.joining(" "));
            return positions;
        } else {
            return NOT_FOUND;
        }
    }

    List<PlayerMatchStatsDTO> scrapePlayerMatchStats(Page page) {
        List<PlayerMatchStatsDTO> matchStats = new ArrayList<>();

        // Get match statistics table body
        Locator statsTableBody = page.locator("tbody#player-table-statistics-body");
        try {
            // Wait for the element to be attached to the DOM, not necessarily visible.
            statsTableBody.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        } catch (Exception e) {
            log.warn("Match stats table body not found. Returning empty list.");
            return matchStats; // Return empty list if table body doesn't even exist.
        }

        List<Locator> matchRows = statsTableBody.locator("tr").all();
        for (Locator row : matchRows) {
            Locator opponentLink = row.locator("td:nth-child(1) a.player-match-link");
            String fullOpponentText = opponentLink.innerText();
            String score = opponentLink.locator("span.scoreline").innerText();
            String opponent = fullOpponentText.replace(score, "").trim();

            PlayerMatchStatsDTO match = PlayerMatchStatsDTO.builder()
                    .opponent(opponent)
                    .score(score)
                    .date(row.locator("td:nth-child(3)").innerText())
                    .position(row.locator("td:nth-child(4)").innerText())
                    .minsPlayed(row.locator("td:nth-child(5)").innerText())
                    .goals(row.locator("td:nth-child(6)").innerText())
                    .assists(row.locator("td:nth-child(7)").innerText())
                    .yellowCards(row.locator("td:nth-child(8)").innerText())
                    .redCards(row.locator("td:nth-child(9)").innerText())
                    .shots(row.locator("td:nth-child(10)").innerText())
                    .passSuccess(row.locator("td:nth-child(11)").innerText())
                    .aerialsWon(row.locator("td:nth-child(12)").innerText())
                    .rating(row.locator("td:nth-child(13)").innerText())
                    .build();
            matchStats.add(match);
        }
        return matchStats;
    }

}
