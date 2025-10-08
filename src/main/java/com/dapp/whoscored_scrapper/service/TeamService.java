package com.dapp.whoscored_scrapper.service;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dapp.whoscored_scrapper.model.dto.TeamDTO;
import com.dapp.whoscored_scrapper.model.dto.TeamPlayerDTO;

@Service
public class TeamService extends AbstractWebService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    public TeamDTO getTeamInfoByName(String teamName) {
        try (Playwright playwright = Playwright.create()) { // Bean of Spring
            Page page = createPage(playwright); // Injected into Service
            // Search for the team
            performSearch(page, teamName);

            // Click the first team result
            Locator firstResult = page
                    .locator("div.search-result:has(h2:text('Equipos:')) >> tbody tr:nth-child(2) >> a")
                    .first();
            try {
                firstResult.waitFor(new Locator.WaitForOptions().setTimeout(15000)); // Esperar hasta 15 segundos
            } catch (Exception e) {
                log.warn("Team '{}' not found in the second result block, trying the first one.", teamName);
                firstResult = page
                        .locator("div.search-result:has(h2:text('Equipos:')) >> tbody tr:nth-child(1) >> a")
                        .first();
                // If it also fails here, it means the team was not found.
                firstResult.waitFor(new Locator.WaitForOptions().setTimeout(5000)); 
            }

            firstResult.click();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            // Extract team and squad data
            TeamDTO teamDTO = new TeamDTO();
            teamDTO.setName(extractText(page, "h1.team-header"));
            teamDTO.setSquad(scrapeSquadData(page));

            return teamDTO;
        } catch (com.microsoft.playwright.TimeoutError e) {
            log.error("Team '{}' not found in search results or timed out.", teamName, e);
            throw new IllegalArgumentException("Team with name '" + teamName + "' not found.");
        } catch (Exception e) {
            log.error("An error occurred during scraping for team: {}", teamName, e);
            // For other unexpected errors, throw a generic RuntimeException.
            throw new RuntimeException("An unexpected error occurred while fetching team data.", e);
        }
    }

    List<TeamPlayerDTO> scrapeSquadData(Page page) {
        List<TeamPlayerDTO> squad = new ArrayList<>();

        // Get squad statistics table body
        Locator squadTableBody = page.locator("tbody#player-table-statistics-body");
        try {
            // Wait for the element to be attached to the DOM, not necessarily visible.
            squadTableBody.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        } catch (Exception e) {
            log.warn("Squad stats table body not found or not visible. Returning empty list.");
            return squad; // Return empty list if table body doesn't exist or is empty.
        }

        List<Locator> playerRows = squadTableBody.locator("tr").all();
        for (Locator row : playerRows) {
            String name = row.locator("td:nth-child(1) a.player-link span.iconize-icon-left").innerText();
            String age = row.locator("td:nth-child(1) span.player-meta-data:nth-of-type(1)").innerText();
            String position = row.locator("td:nth-child(1) span.player-meta-data:nth-of-type(2)").innerText();

            TeamPlayerDTO player = TeamPlayerDTO.builder()
                    .name(name).age(age).position(position.replace(",", "").trim())
                    .height(row.locator("td:nth-child(3)").innerText())
                    .weight(row.locator("td:nth-child(4)").innerText())
                    .apps(row.locator("td:nth-child(5)").innerText())
                    .minsPlayed(row.locator("td:nth-child(6)").innerText())
                    .goals(row.locator("td:nth-child(7)").innerText())
                    .assists(row.locator("td:nth-child(8)").innerText())
                    .yellowCards(row.locator("td:nth-child(9)").innerText())
                    .redCards(row.locator("td:nth-child(10)").innerText())
                    .shotsPerGame(row.locator("td:nth-child(11)").innerText())
                    .passSuccess(row.locator("td:nth-child(12)").innerText())
                    .aerialsWonPerGame(row.locator("td:nth-child(13)").innerText())
                    .manOfTheMatch(row.locator("td:nth-child(14)").innerText())
                    .rating(row.locator("td:nth-child(15)").innerText())
                    .build();
            squad.add(player);
        }
        return squad;
    }
}
