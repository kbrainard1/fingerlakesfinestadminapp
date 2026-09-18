import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

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
                "--disable-dev-shm-usage", // avoids crashes in Docker / CI
                "--window-size=1280,900",
                "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/124.0.0.0 Safari/537.36"
                );
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        return driver;
    }

    public static record HorsePage(String url, String contents) {}
    
    public static synchronized HorsePage loadHorsePage(String horse) {
        WebDriver driver = makeDriver();
        try {
            driver.get("https://www.equibase.com");
            navigateToHorsePage(horse, driver);
            return new HorsePage(driver.getCurrentUrl(), driver.getPageSource());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            driver.quit();
        }
    }
}
