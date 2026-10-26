package adminapp;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.google.common.collect.Maps;

public class EquibaseConnector {


    public static synchronized String loadEquibaseUrl(String title) throws InterruptedException {        
        WebDriver driver = makeDriver();
        try {
            driver.get("https://www.equibase.com");
            navigateToHorsePage(title, driver);
            return driver.getCurrentUrl();
        } catch (Exception e) {
            return "";
        } finally {
            driver.quit();
        }
    }

    private static void navigateToHorsePage(String title, WebDriver driver) throws InterruptedException {       
        WebElement searchBox = driver.findElement(By.className("header-search-form"));
        WebElement input = searchBox.findElement(By.className("input"));
        input.sendKeys(title);
        input.sendKeys(Keys.ENTER);
        
        // make sure the page loaded
        try {
            driver.findElement(By.className("horse-profile-top-bar-headings"));
        } catch (NoSuchElementException e) {
            // Possible the page needs to disambiguate - this will throw if something
            // else went wrong
            driver.findElement(By.id("profiles-results"));
            
            // look for the tb
            List<WebElement> possibleMatches = driver.findElement(By.tagName("table"))
                    .findElements(By.tagName("a"));
            for (WebElement match : possibleMatches) {
                if (match.getAttribute("href").contains("rbt=TB")
                        && match.getText().substring(0, 3).equalsIgnoreCase(title.substring(0,3))) {
                    driver.get(match.getAttribute("href"));
                    break;
                }
            }
            // load the disambiguated page
            driver.findElement(By.className("horse-profile-top-bar-headings"));
        }
    }

    private static WebDriver makeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless=new",          // new headless mode (Chrome ≥ 112)
                "--disable-gpu",
                "--no-sandbox",
                "--disable-logging",
                "--disable-crash-reporter",
                "--disable-crashpad-for-testing",
                "--disable-software-rasterizer",
                "--disable-extensions",
                "--disable-sync",
                "--disable-default-apps",
                "--disable-background-networking",
                "--disable-dev-shm-usage", // avoids crashes in Docker / CI
                "--window-size=1280,900",
                "--js-flags=--max-old-space-size=512",
                "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/124.0.0.0 Safari/537.36"
                );
        ChromeDriver driver = new ChromeDriver(options);
        
        driver.executeCdpCommand("Network.enable", new HashMap<>());

        Map<String, Object> params = new HashMap<>();
        params.put("urls", List.of("*.css", "*.svg", "*.gif", "*.png", "*.jpg", "*.jpeg", 
                "*.xyz/*", "*cdn.statically.io/*", "*githack.com/*", "*html-load.cc/*", "*cdn.cookielaw.org/*",
                "*cdn.jsdeliver.net/*", "*fonts.googleapis.com*"
                ));
        driver.executeCdpCommand("Network.setBlockedURLs", params);
        
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        return driver;
    }

    public static record HorseInfo(String name, String equibaseUrl, String pedigreeUrl, 
            String shortSex, String shortColor, String year) {}
    
    private static final Map<String, String[]> equibaseCache = loadHorseCache();
    
    
    public static Map<String, String[]> loadHorseCache() {
        Map<String, String[]> cache = Maps.newHashMapWithExpectedSize(100_000);
        try {
            java.nio.file.Files.readAllLines(GithubConnector.getFile("resources/consolidated.csv").toPath()).forEach(line -> {
                String[] data = line.split(",");
                String[] overwrite = cache.put(data[0], data);
                if (overwrite != null) {
                   // System.out.println("Overwriting cache for " + data[0]);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
        return cache;
    }
    
    public static synchronized HorseInfo loadHorsePage(String horse) {
        HorseInfo cached = cacheLookup(horse);
        if (cached != null) {
            return cached;
        }
        
        WebDriver driver = makeDriver();
        try {
            driver.get("https://www.equibase.com/");
            navigateToHorsePage(horse, driver);
            String url = driver.getCurrentUrl();
            Document horsePage = Jsoup.parse(driver.getPageSource());
            Elements elems = horsePage.select(".horse-profile-top-bar-headings");
            String name = horsePage.select(".horse-name-header").get(0).getElementsByTag("div").get(0).ownText();
            name = name.substring(0, (name.indexOf("(") >= 0 ? name.indexOf("(") : name.length()));
            String[] horseDeets = elems.get(0).ownText().split(",");
            if (Thread.interrupted()) {
                throw new RuntimeException("Interrupted");
            }
            return new HorseInfo(name, url,
                    horsePage.select("a[href*=equineline.com/Free]").get(0).attr("href"),
                    horseDeets[2].trim(),
                    horseDeets[1].trim(),
                    horseDeets[horseDeets.length - 1].trim());
        } catch (Exception e) {
            e.printStackTrace();
            if (Thread.interrupted()) {
                throw new RuntimeException("Interrupted");
            }
            throw new RuntimeException(e);
        } finally {
            driver.quit();
        }
    }

    private static EquibaseConnector.HorseInfo cacheLookup(String horse) {        
        String shortName = "";
        for (int i = 0; i < horse.length(); i++) {
            if (Character.isLetter(horse.charAt(i))) {
                shortName += Character.toLowerCase(horse.charAt(i));
            }
        }
        //sweetrefuge,10900506, 2019, B, M
        String[] horseInfo = equibaseCache.get(shortName);
        if (horseInfo != null) {
            String equibaseUrl = "https://www.equibase.com/profiles/Results.cfm?type=Horse&refno=" + horseInfo[1] + "&registry=T&rbt=TB";
            String pedigreeUrl = "https://www.equineline.com/Free-5X-Pedigree.cfm?page_state=ORDER_AND_CONFIRM&include_sire_line=N&include_truenick=N&reference_number=" + horseInfo[1];
            String year = horseInfo[2].trim();
            String color = horseInfo[3].trim();
            String sex = horseInfo[4].trim();
            return new HorseInfo(horse, equibaseUrl, pedigreeUrl, sex, color, year);
        }
        return null;
    }
}
