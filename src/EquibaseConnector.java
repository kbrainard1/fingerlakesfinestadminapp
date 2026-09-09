import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
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
        driver.findElement(By.className("horse-profile-top-bar-headings"));
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

    private static WebDriver preCachedDriver;
    
    public static synchronized void precacheConnection() {
        CreateListingFrontend.threadPool.submit(() -> {
            synchronousCacheDriver();
        });
        
        CreateListingFrontend.threadPool.submit(() -> {
            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            cleanUpCachedConnection();
        });
    }
    
    private static synchronized void synchronousCacheDriver() {
        if (preCachedDriver == null) {
            preCachedDriver = makeDriver();
            preCachedDriver.get("https://www.equibase.com");
        }
    }

    private static synchronized void cleanUpCachedConnection() {
        if (preCachedDriver != null) {
            preCachedDriver.quit();
            preCachedDriver = null;
        }
    }
    
    public static synchronized HorsePage loadHorsePage(String horse) {
        synchronousCacheDriver(); // no-op if already cached
        try {
            navigateToHorsePage(horse, preCachedDriver);
            return new HorsePage(preCachedDriver.getCurrentUrl(), preCachedDriver.getPageSource());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            preCachedDriver.quit();
            preCachedDriver = null;
        }
    }
}
