package com.dapp.whoscored_scrapper.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
                                "--disable-web-security", // Deshabilitar políticas CORS
                                "--disable-features=VizDisplayCompositor" // Mejorar rendimiento
                        )));

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36") // User Agent más genérico
                .setViewportSize(1920, 1080)
                .setLocale("es-ES")
                .setTimezoneId("Europe/Madrid") // Cambiar a zona horaria europea
                .setJavaScriptEnabled(true)
                .setIgnoreHTTPSErrors(true)); // Ignorar errores SSL

        Page page = context.newPage();
        
        // Configurar timeouts más largos para entornos cloud
        page.setDefaultTimeout(60000); // 60 segundos
        page.setDefaultNavigationTimeout(60000);

        // Navegar con más opciones de espera
        try {
            page.navigate(BASE_URL, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE) // Esperar a que la red esté inactiva
                    .setTimeout(60000));

            // Esperar adicionalmente a que ciertos elementos críticos carguen
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(60000));

        } catch (Exception e) {
            log.warn("Initial navigation failed, but continuing: {}", e.getMessage());
        }

        // Manejar cookies con más tolerancia
        try {
            Locator acceptButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Aceptar todo"));
            acceptButton.waitFor(new Locator.WaitForOptions().setTimeout(10000));
            acceptButton.click();
            log.info("Cookie banner accepted.");

            // Esperar después de aceptar cookies
            page.waitForTimeout(2000);
        } catch (Exception e) {
            log.warn("Cookie button not found or could not be clicked. Continuing...");
        }

        return page;
    }

    protected void performSearch(Page page, String searchTerm) {
        log.info("Searching for: {}", searchTerm);

        try {
            // Esperar a que el input de búsqueda esté disponible con más tolerancia
            Locator searchInput = page.getByPlaceholder("Buscar campeonatos, equipos y jugadores").first();
                    searchInput.waitFor(new Locator.WaitForOptions().setTimeout(30000));

            // Hacer click con más intentos
            searchInput.click(new Locator.ClickOptions().setTimeout(15000));

            // Limpiar y llenar con delays
            searchInput.fill("");
            page.waitForTimeout(500);
            searchInput.fill(searchTerm);
            page.waitForTimeout(500);

            // Presionar Enter
            searchInput.press("Enter");

            // Esperar con diferentes estrategias
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(45000));
            page.waitForTimeout(2000); // Espera adicional

        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage());
            throw new RuntimeException("Search operation failed: " + e.getMessage(), e);
        }
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