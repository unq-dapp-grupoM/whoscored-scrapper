package com.dapp.whoscored_scrapper.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class AbstractWebService {

    private static final Logger log = LoggerFactory.getLogger(AbstractWebService.class);
    protected static final String BASE_URL = "https://es.whoscored.com/";
    protected static final String NOT_FOUND = "Not found";

    protected Page createPage(Playwright playwright) {
        // Configuration to make headless mode less detectable.
        Browser browser = playwright.chromium()
                .launch(new BrowserType.LaunchOptions()
                        .setHeadless(true) // Run in headless mode
                        .setArgs(List.of(
                                "--no-sandbox", // Necessary for Render
                                "--disable-setuid-sandbox", // Necessary for Render
                                "--disable-blink-features=AutomationControlled" // Hide automation
                        ))); // Hide automation

        // Create a browser context with options that simulate a real user.
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .setViewportSize(1920, 1080) // Common window size
                .setLocale("es-ES") // Set locale
                .setTimezoneId("America/Argentina/Buenos_Aires")); // Set timezone

        Page page = context.newPage();

        // Accept cookies before each navigation
        page.navigate(BASE_URL);
        try {
            // Using getByRole is more robust for finding the cookie button.
            Locator acceptButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Aceptar todo"));
            acceptButton.waitFor(new Locator.WaitForOptions().setTimeout(60000)); // Increase wait time
            acceptButton.click();
            log.info("Cookie banner accepted.");
        } catch (Exception e) {
            log.warn("Cookie button not found or could not be clicked. Continuing...");
        }
        return page;
    }

    protected void performSearch(Page page, String searchTerm) {
        log.info("Searching for: {}", searchTerm);
        // Using getByPlaceholder is more robust than a generic CSS selector.
        Locator searchInput = page.getByPlaceholder("Buscar campeonatos, equipos y jugadores");
        searchInput.waitFor(new Locator.WaitForOptions().setTimeout(60000)); // Increase wait time
        // Click first to ensure the input has focus.
        searchInput.click();
        searchInput.fill(searchTerm);
        searchInput.press("Enter");
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    protected String extractText(Page page, String selector) {
        try {
            return page.locator(selector).first().innerText();
        } catch (Exception e) {
            log.warn("Could not find element with selector: {}", selector);
            return NOT_FOUND;
        }
    }

    protected String extractText(Locator context, String selector) {
        try {
            // Playwright will automatically wait for the element to appear.
            return context.locator(selector).first().innerText();
        } catch (Exception e) {
            log.warn("Could not find element with selector '{}' in the given context", selector);
            return NOT_FOUND;
        }
    }

    protected String extractAttribute(Page page, String selector, String attribute) {
        try {
            String value = page.locator(selector).first().getAttribute(attribute);
            if (value == null) {
                log.warn("Attribute '{}' not found for element with selector: {}", attribute, selector);
                return NOT_FOUND;
            }
            return value;
        } catch (Exception e) {
            log.warn("Could not find attribute '{}' for element with selector: {}", attribute, selector);
            return NOT_FOUND;
        }
    }
}