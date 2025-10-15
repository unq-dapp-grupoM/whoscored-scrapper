package com.dapp.whoscored_scrapper.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public abstract class AbstractWebService {

    private static final Logger log = LoggerFactory.getLogger(AbstractWebService.class);
    protected static final String BASE_URL = "https://es.whoscored.com/";
    protected static final String NOT_FOUND = "Not found";

    protected Page createPage(Playwright playwright) {
        System.setProperty("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
        
        Browser browser = playwright.chromium()
                .launch(new BrowserType.LaunchOptions()
                        .setHeadless(true)
                        .setArgs(List.of(
                                "--no-sandbox",
                                "--disable-setuid-sandbox",
                                "--disable-blink-features=AutomationControlled",
                                "--disable-dev-shm-usage",
                                "--disable-gpu",
                                "--single-process",
                                "--no-zygote",
                                "--disable-web-security",
                                "--disable-features=VizDisplayCompositor",
                                "--disable-blink-features=AutomationControlled",
                                "--disable-features=TranslateUI",
                                "--disable-ipc-flooding-protection",
                                "--no-first-run",
                                "--disable-default-apps",
                                "--disable-popup-blocking",
                                "--disable-hang-monitor"
                        )));

        // Contexto con más opciones de stealth
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setViewportSize(1920, 1080)
                .setLocale("es-ES")
                .setTimezoneId("Europe/Madrid")
                .setJavaScriptEnabled(true)
                .setIgnoreHTTPSErrors(true)
                .setExtraHTTPHeaders(Map.of(
                        "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
                        "Accept-Language", "es-ES,es;q=0.9,en;q=0.8",
                        "Accept-Encoding", "gzip, deflate, br",
                        "Cache-Control", "no-cache",
                        "DNT", "1"
                )));

        Page page = context.newPage();

        // Al inicio de createPage, después de crear el context
        page.onResponse(response -> {
            if (response.status() >= 400) {
                log.warn("HTTP {} for: {}", response.status(), response.url());
            }
        });
        
        page.onRequestFailed(request -> {
            log.warn("Request failed: {} {}", request.failure(), request.url());
        });
        
        // Configurar timeouts
        page.setDefaultTimeout(60000);
        page.setDefaultNavigationTimeout(60000);

        // Navegar con múltiples estrategias
        try {
            log.info("Navigating to: {}", BASE_URL);

            // Opción 1: Intentar navegación simple primero
            page.navigate(BASE_URL, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(45000));

            // Esperar un poco más
            page.waitForTimeout(3000);

            // Verificar si la página cargó
            String title = page.title();
            log.info("Page title: {}", title);

            // Intentar diferentes selectores para cookies
            handleCookiesWithMultipleSelectors(page);

        } catch (Exception e) {
            log.warn("First navigation attempt failed: {}", e.getMessage());

            // Opción 2: Reintentar con load state diferente
            try {
                page.waitForLoadState(LoadState.LOAD, new Page.WaitForLoadStateOptions().setTimeout(30000));
                handleCookiesWithMultipleSelectors(page);
            } catch (Exception e2) {
                log.warn("Second attempt also failed: {}", e2.getMessage());
                // Continuar sin cookies aceptadas
            }
        }

        return page;
    }

    private void handleCookiesWithMultipleSelectors(Page page) {
        // Múltiples estrategias para encontrar el botón de cookies
        String[] cookieSelectors = {
            "button:has-text('Aceptar todo')",
            "button:has-text('Accept All')", 
            "button:has-text('Aceptar')",
            "button[aria-label*='cookie']",
            "button[class*='cookie']",
            ".cookie-banner button",
            "#cookie-banner button",
            "[data-testid*='cookie'] button"
        };

        for (String selector : cookieSelectors) {
            try {
                Locator cookieButton = page.locator(selector).first();
                if (cookieButton.isVisible(new Locator.IsVisibleOptions())) {
                    cookieButton.click(new Locator.ClickOptions().setTimeout(5000));
                    log.info("Cookie banner accepted using selector: {}", selector);
                    page.waitForTimeout(2000);
                    return;
                }
            } catch (Exception e) {
                log.debug("Cookie selector {} failed: {}", selector, e.getMessage());
            }
        }
        log.warn("No cookie button found with any selector");
    }

    protected void performSearch(Page page, String searchTerm) {
        log.info("Searching for: {}", searchTerm);
        
        try {
            // Múltiples estrategias para encontrar el campo de búsqueda
            Locator searchInput = findSearchInput(page);

            if (searchInput == null) {
                throw new RuntimeException("Search input not found");
            }

            // Limpiar y escribir
            searchInput.click(new Locator.ClickOptions().setTimeout(10000));
            page.waitForTimeout(1000);
            searchInput.fill("");
            page.waitForTimeout(500);
            searchInput.fill(searchTerm);
            page.waitForTimeout(500);

            // Presionar Enter
            searchInput.press("Enter");

            // Esperar a que los resultados carguen
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(45000));
            page.waitForTimeout(3000);

            log.info("Search completed successfully");

        } catch (Exception e) {
            log.error("Search operation failed: {}", e.getMessage());
            throw new RuntimeException("Search operation failed: " + e.getMessage(), e);
        }
    }

    private Locator findSearchInput(Page page) {
        String[] searchSelectors = {
            "input[placeholder*='Buscar campeonatos']",
            "input[placeholder*='Search']",
            "input[name*='search']",
            "input[type='search']",
            ".search-input",
            "#search-input",
            "input[class*='search']"
        };

        for (String selector : searchSelectors) {
            try {
                Locator input = page.locator(selector).first();
                if (input.isVisible(new Locator.IsVisibleOptions())) {
                    log.info("Found search input using selector: {}", selector);
                    return input;
                }
            } catch (Exception e) {
                log.debug("Search selector {} failed: {}", selector, e.getMessage());
            }
        }

        log.error("No search input found with any selector");
        return null;
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