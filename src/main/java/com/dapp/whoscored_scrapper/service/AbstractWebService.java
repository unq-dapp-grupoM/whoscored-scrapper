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
                                "--disable-hang-monitor",
                                "--disable-background-timer-throttling",
                                "--disable-renderer-backgrounding",
                                "--disable-backgrounding-occluded-windows"
                        )));

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
                        "DNT", "1",
                        "Sec-Fetch-Dest", "document",
                        "Sec-Fetch-Mode", "navigate",
                        "Sec-Fetch-Site", "none",
                        "Upgrade-Insecure-Requests", "1"
                )));

        Page page = context.newPage();
        page.setDefaultTimeout(120000); // 2 minutos
        page.setDefaultNavigationTimeout(120000);

        try {
            log.info("Navigating to: {}", BASE_URL);

            // Navegar y manejar Cloudflare
            page.navigate(BASE_URL, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE)
                    .setTimeout(90000));

            // Verificar si estamos en Cloudflare
            String title = page.title();
            log.info("Initial page title: {}", title);

            if (isCloudflarePage(page)) {
                log.warn("Cloudflare challenge detected, attempting to bypass...");
                boolean bypassed = handleCloudflareChallenge(page);

                if (!bypassed) {
                    throw new RuntimeException("Cloudflare challenge could not be bypassed");
                }
            }

            // Esperar a que la página real cargue
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(30000));

            // Manejar cookies después de pasar Cloudflare
            handleCookiesWithMultipleSelectors(page);

            log.info("Successfully bypassed Cloudflare and loaded WhoScored");

        } catch (Exception e) {
            log.error("Failed to load page: {}", e.getMessage());
            // No cerrar el browser inmediatamente, podría estar en medio de un challenge
            throw new RuntimeException("Failed to load WhoScored: " + e.getMessage(), e);
        }

        return page;
    }

    private boolean isCloudflarePage(Page page) {
        try {
            String title = page.title();
            String url = page.url();

            return title.contains("Cloudflare") || 
                   title.contains("Attention Required") ||
                   title.contains("Just a moment") ||
                   url.contains("challenge") ||
                   page.locator("div#cf-content").isVisible() ||
                   page.locator("div.cf-browser-verification").isVisible() ||
                   page.locator("[id*='challenge']").isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean handleCloudflareChallenge(Page page) {
        try {
            log.info("Waiting for Cloudflare challenge to resolve...");

            // Estrategia 1: Esperar a que Cloudflare redirija automáticamente
            try {
                page.waitForURL("**://es.whoscored.com/**", 
                    new Page.WaitForURLOptions()
                        .setTimeout(120000)
                        .setWaitUntil(WaitUntilState.NETWORKIDLE));
                log.info("Cloudflare auto-redirect successful");
                return true;
            } catch (Exception e) {
                log.debug("Auto-redirect failed: {}", e.getMessage());
            }

            // Estrategia 2: Esperar a que el título cambie
            try {
                page.waitForFunction(
                    "() => !document.title.includes('Cloudflare') && " +
                    "!document.title.includes('Attention Required') && " +
                    "!document.title.includes('Just a moment')",
                    new Page.WaitForFunctionOptions().setTimeout(120000)
                );
                log.info("Cloudflare title change detected");
                return true;
            } catch (Exception e) {
                log.debug("Title change wait failed: {}", e.getMessage());
            }

            // Estrategia 3: Intentar interactuar con elementos de Cloudflare
            try {
                // Buscar y hacer click en el botón de verificación de Cloudflare
                String[] challengeSelectors = {
                    "input[type='submit'][value*='Verify']",
                    "button[type*='submit']",
                    ".btn-primary",
                    "#challenge-form input[type='submit']",
                    "a[role='button']",
                    "[data-ray*='submit']"
                };

                for (String selector : challengeSelectors) {
                    try {
                        Locator button = page.locator(selector).first();
                        if (button.isVisible(new Locator.IsVisibleOptions())) {
                            log.info("Clicking Cloudflare challenge button: {}", selector);
                            button.click(new Locator.ClickOptions().setTimeout(10000));

                            // Esperar después del click
                            page.waitForTimeout(5000);
                            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(30000));
                            return true;
                        }
                    } catch (Exception e) {
                        log.debug("Challenge selector {} failed: {}", selector, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.debug("Challenge interaction failed: {}", e.getMessage());
            }

            // Estrategia 4: Espera simple con timeout extendido
            log.info("Waiting extended time for Cloudflare...");
            page.waitForTimeout(30000); // 30 segundos extra

            // Verificar si finalmente cargó WhoScored
            String finalTitle = page.title();
            if (!isCloudflarePage(page) && finalTitle.contains("WhoScored")) {
                log.info("Cloudflare resolved after extended wait");
                return true;
            }

            log.error("All Cloudflare bypass strategies failed");
            return false;

        } catch (Exception e) {
            log.error("Error handling Cloudflare challenge: {}", e.getMessage());
            return false;
        }
    }

    private void handleCookiesWithMultipleSelectors(Page page) {
        // MÁS selectores para cookies - WhoScored podría usar diferentes sistemas
        String[] cookieSelectors = {
            "button:has-text('Aceptar todo')",
            "button:has-text('Accept All')", 
            "button:has-text('Aceptar')",
            "button:has-text('Accept')",
            "button[aria-label*='cookie']",
            "button[class*='cookie']",
            ".cookie-banner button",
            "#cookie-banner button",
            "[data-testid*='cookie'] button",
            ".qc-cmp2-summary-buttons button[mode='primary']",
            "#onetrust-accept-btn-handler",
            ".ot-sdk-row button:has-text('Aceptar')",
            "button.js-cookie-notice-accept",  // Selector común
            "a.cookie-accept",  // A veces es un link
            "div.cookie button" // Selector genérico
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
            // Esperar un poco más después de pasar Cloudflare
            page.waitForTimeout(3000);

            Locator searchInput = findSearchInput(page);

            if (searchInput == null) {
                // Último intento: buscar cualquier input que pueda ser de búsqueda
                log.warn("Trying fallback search strategy...");
                searchInput = findAnyInputForSearch(page);
            }

            if (searchInput == null) {
                throw new RuntimeException("Search input not found");
            }

            // Limpiar y escribir con más delays
            searchInput.click(new Locator.ClickOptions().setTimeout(15000));
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

    private Locator findAnyInputForSearch(Page page) {
        // Estrategia de fallback: buscar cualquier input que parezca de búsqueda
        try {
            Locator allInputs = page.locator("input[type='text'], input[type='search']");
            int inputCount = allInputs.count();
            log.info("Found {} text/search inputs on page", inputCount);
            
            for (int i = 0; i < inputCount; i++) {
                Locator input = allInputs.nth(i);
                try {
                    if (input.isVisible() && input.isEnabled()) {
                        String placeholder = input.getAttribute("placeholder");
                        String name = input.getAttribute("name");
                        String id = input.getAttribute("id");
                        
                        // Verificar si alguno de los atributos contiene "search" o "buscar"
                        boolean isSearchInput = false;
                        
                        if (placeholder != null && 
                            (placeholder.toLowerCase().contains("search") || 
                             placeholder.toLowerCase().contains("buscar"))) {
                            isSearchInput = true;
                        }
                        
                        if (name != null && name.toLowerCase().contains("search")) {
                            isSearchInput = true;
                        }
                        
                        if (id != null && id.toLowerCase().contains("search")) {
                            isSearchInput = true;
                        }
                        
                        if (isSearchInput) {
                            log.info("Found potential search input: placeholder='{}', name='{}', id='{}'", 
                                    placeholder, name, id);
                            return input;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Input {} check failed: {}", i, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Fallback search strategy failed: {}", e.getMessage());
        }
        return null;
    }

    private Locator findSearchInput(Page page) {
        // MÁS selectores para el campo de búsqueda
        String[] searchSelectors = {
            "input[placeholder*='Buscar campeonatos']",
            "input[placeholder*='Search']",
            "input[name*='search']",
            "input[type='search']",
            "input[class*='search']",
            ".search-input",
            "#search-input",
            "input#search",
            "[data-testid*='search']",
            "form[role='search'] input",
            "input[aria-label*='search']",
            "header input[type='text']",  // Buscar en el header
            "nav input",  // Buscar en la navegación
            ".header-search input",
            "#header-search input"
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

        // DIAGNÓSTICO: Ver qué elementos hay en la página
        logDiagnosticInfo(page);

        log.error("No search input found with any selector");
        return null;
    }

    private void logDiagnosticInfo(Page page) {
        try {
            log.info("=== PAGE DIAGNOSTIC INFO ===");
            log.info("Page URL: {}", page.url());
            log.info("Page title: {}", page.title());

            // Verificar elementos comunes en WhoScored
            String[] diagnosticSelectors = {
                "header", "nav", "main", ".header", "#header", 
                "input", "button", "form", "[role='search']"
            };

            for (String selector : diagnosticSelectors) {
                try {
                    int count = page.locator(selector).count();
                    if (count > 0) {
                        log.info("Found {} elements with selector: {}", count, selector);
                    }
                } catch (Exception e) {
                    log.debug("Diagnostic selector {} failed: {}", selector, e.getMessage());
                }
            }

            // Verificar si hay algún formulario
            try {
                Locator forms = page.locator("form");
                int formCount = forms.count();
                log.info("Total forms on page: {}", formCount);

                for (int i = 0; i < formCount; i++) {
                    Locator form = forms.nth(i);
                    String formHtml = form.innerHTML();
                    if (formHtml.contains("search") || formHtml.contains("buscar")) {
                        log.info("Form {} might be search form: {}", i, formHtml.substring(0, Math.min(100, formHtml.length())));
                    }
                }
            } catch (Exception e) {
                log.debug("Form diagnostic failed: {}", e.getMessage());
            }

            log.info("=== END DIAGNOSTIC ===");

        } catch (Exception e) {
            log.error("Diagnostic failed: {}", e.getMessage());
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