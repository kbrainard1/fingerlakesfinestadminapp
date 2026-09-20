

// The irony. The anti-scraping features of the site make on-demand user-driven
// queries slow and flaky, to the point that to deliver a good UX, we need to do
// dramatically *more* scraping.

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.google.common.collect.Maps;
import com.google.common.io.Files;

public class EquibaseScraper {

    public static enum Color {
        CHESTNUT(1),
        DARK_BAY(2),
        GREY(3);
        
        private final int id;

        private Color(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
        
        public static Color getById(int id) {
            for (Color c : values()) {
                if (c.getId() == id) {
                    return c;
                }
            }
            return null;
        }
    }
    
    public static enum Sex {
        GELDING(1),
        MARE(2),
        FILLY(3),
        COLT(4),
        STALLION(5);
        
        private final int id;

        private Sex(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
        
        public static Sex getById(int id) {
            for (Sex c : values()) {
                if (c.getId() == id) {
                    return c;
                }
            }
            return null;
        }
    }

    private static final long MAX_NUM = 11_000_000;
    private static final long BATCH_SIZE = 100;
    
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
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        return driver;
    }
    
    public static void main(String[] args) throws Exception {
        consolidateFiles();
        long currentNum = getCurrentNum();
        String nextFile = getNextFile(currentNum);
        WebDriver driver = makeDriver();
        BufferedWriter out = new BufferedWriter(new FileWriter(nextFile));
        try {
            for (long i = currentNum; i < MAX_NUM; i++) {
                if (i % BATCH_SIZE == 0) {
                   out.close();
                   out = new BufferedWriter(new FileWriter(getNextFile(i)));
                }
                try {
                    writeHorse(i, driver, out);
                } catch (Exception e) {
                    driver.quit();
                    driver = makeDriver();
                    writeHorse(i, driver, out);
                }
            }
        } finally {
            out.close();
            driver.quit();
        }
    }

    private static void consolidateFiles() throws IOException {
        try (OutputStream out = new FileOutputStream("consolidated.csv")) {
            for (File f : new File("scrapedDb").listFiles()) {
                Files.copy(f, out);
            }
        }
    }
    
    public static Map<String, String[]> loadHorseCache() {
        Map<String, String[]> cache = Maps.newHashMapWithExpectedSize(100_000);
        try {
            java.nio.file.Files.readAllLines(Path.of("consolidated.csv")).forEach(line -> {
                String[] data = line.split(",");
                cache.put(data[0], data);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return cache;
    }
    
    // https://www.equineline.com/Free-5X-Pedigree.cfm?page_state=ORDER_AND_CONFIRM&include_sire_line=N&include_truenick=N&reference_number=10998815
    private static void writeHorse(long horseNum, WebDriver driver, BufferedWriter out) throws IOException {
        String url = "https://www.equibase.com/profiles/Results.cfm?type=Horse&refno=" + horseNum + "&registry=T&rbt=TB";
  
        driver.get(url);
        driver.findElement(By.id("addThis")); // check that the page has loaded


        Document horsePage = Jsoup.parse(driver.getPageSource());
        if (horsePage.select(".horse-profile-top-bar-headings").size() > 0) {

            String name = horsePage.select(".horse-name-header").get(0).child(0).ownText();
            name = name.substring(0, (name.indexOf("(") > 0 ? name.indexOf("(") : name.length()));

            String shortNameBuilder = "";
            for (int i = 0; i < name.length(); i++) {
                if (Character.isLetter(name.charAt(i))) {
                    shortNameBuilder += Character.toLowerCase(name.charAt(i));
                }
            }

            Elements elems = horsePage.select(".horse-profile-top-bar-headings");
            String[] horseDeets = elems.get(0).ownText().split(",");
            if (horseDeets.length <= 2) {
                System.out.println("Invalid horse page " + horseNum);
            } else {
                // name, number, year, color, sex
                String line = shortNameBuilder + "," + horseNum + "," + horseDeets[horseDeets.length - 1] + "," + horseDeets[1] + "," + horseDeets[2];
                out.write(line);
                out.newLine();
            }
        }
    }

    private static String getNextFile(long currentNum) {
        long fileNum = currentNum/BATCH_SIZE;
        return "scrapedDb/" + fileNum + ".csv";
    }

    private static long getCurrentNum() {
       // return 10898812;
        return 10917900;
    }
}

