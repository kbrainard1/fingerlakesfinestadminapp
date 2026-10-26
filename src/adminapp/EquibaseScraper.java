// The irony. The anti-scraping features of the site make on-demand user-driven
// queries slow and flaky, to the point that to deliver a good UX, we need to do
// dramatically *more* scraping.
package adminapp;



import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

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

    private static final long MAX_NUM = 13_000_000;
    private static final long BATCH_SIZE = 100;
    
    public static WebDriver makeDriver() {
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

        // Disable 2D/3D canvas rendering pipelines (major RAM consumer)
        options.addArguments("--disable-accelerated-2d-canvas");
        options.addArguments("--disable-canvas-aa"); // Antialiasing
        options.addArguments("--disable-2d-canvas-clip-utils");
        
        // Map content settings (2 = Block)
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.managed_default_content_settings.images", 2);
        prefs.put("profile.managed_default_content_settings.stylesheet", 2);
        prefs.put("profile.managed_default_content_settings.fonts", 2);
        options.setExperimentalOption("prefs", prefs);
        
        ChromeDriver driver = new ChromeDriver(options);
        
        driver.executeCdpCommand("Network.enable", new HashMap<>());

        Map<String, Object> params = new HashMap<>();
        params.put("urls", List.of("*.css", "*.svg", "*.gif", "*.png", "*.jpg", "*.jpeg", 
                "*.xyz/*", "*cdn.statically.io/*", "*githack.com/*", "*html-load.cc/*", "*cdn.cookielaw.org/*",
                "*cdn.jsdeliver.net/*", "*fonts.googleapis.com*",
                "*.js" // doesn't disable inline scripts, which is important
                ));
        driver.executeCdpCommand("Network.setBlockedURLs", params);
        
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        return driver;
    }
    
    public static void main(String[] args) throws Exception {
        consolidateFiles();
        //cleanUpConsolidated();
        long currentNum = getCurrentNum();
        int numThreads = 4;
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        ArrayBlockingQueue<Future<?>> queue = new ArrayBlockingQueue<>(numThreads + 1);
        List<Long> retryBatch = new ArrayList<>();
        for (long i = currentNum; i < MAX_NUM; i+= BATCH_SIZE) {
            long finalI = i;
            synchronized (retryBatch) {
                for (long start : retryBatch) {
                    threadPool.submit(() -> {
                        try {
                            horseThread(start);
                        } catch (Exception e) {
                            System.out.println("Error with chunk " + finalI);
                            synchronized (retryBatch) {
                                retryBatch.add(finalI);
                            }

                        }
                    });
                }
                retryBatch.clear();
            }
            Future<?> submitted = threadPool.submit(() -> {
                try {
                    horseThread(finalI);
                } catch (Exception e) {
                    System.out.println("Error with chunk " + finalI);
                    synchronized (retryBatch) {
                        retryBatch.add(finalI);
                    }
                   
                }
            });
            if (!queue.offer(submitted)) {
                queue.poll(1, TimeUnit.DAYS).get();
                queue.offer(submitted);
            }
        }
    }
    
    private static ThreadLocal<WebDriver> threadDriver = ThreadLocal.withInitial(() -> makeDriver());
    
    private static void horseThread(long start) throws IOException {
        WebDriver driver = threadDriver.get();
        BufferedWriter out = new BufferedWriter(new FileWriter(getNextFile(start)));
        try {
            for (long i = 0; i < BATCH_SIZE; i++) {
                try {
                    writeHorse(i + start, driver, out);
                } catch (Exception e) {
                    driver.quit();
                    driver = makeDriver();
                    threadDriver.set(driver);
                    writeHorse(i, driver, out);
                }
            }
        } finally {
            out.close();
        }
    }

    private static void consolidateFiles() throws IOException {
        try (OutputStream out = new FileOutputStream("consolidated.csv")) {
            for (File f : new File("scrapedDb").listFiles()) {
                Files.copy(f, out);
            }
        }
    }
    
    // https://www.equineline.com/Free-5X-Pedigree.cfm?page_state=ORDER_AND_CONFIRM&include_sire_line=N&include_truenick=N&reference_number=10998815
    private static void writeHorse(long horseNum, WebDriver driver, BufferedWriter out) throws IOException {
        String url = "https://www.equibase.com/profiles/Results.cfm?type=Horse&refno=" + horseNum + "&registry=T&rbt=TB";
  
        driver.get(url);
        driver.findElement(By.id("addThis")); // check that the page has loaded


        Document horsePage = Jsoup.parse(driver.getPageSource());
        if (horsePage.select(".horse-profile-top-bar-headings").size() > 0) {

            String name = horsePage.select(".horse-name-header").get(0).child(0).ownText();
            name = name.substring(0, (name.indexOf("(") >= 0 ? name.indexOf("(") : name.length()));
            if (name.isBlank()) {
                System.out.println("Invalid horse page " + horseNum);
                return;
            }

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
                synchronized (out) {
                    out.write(line);
                    out.newLine();
                }
            }
        } else {
            if (horsePage.getElementById("c-horse-profile") == null) {
                System.out.println("hmm " + horseNum);
            }
        }
    }

    private static String getNextFile(long currentNum) {
        long fileNum = currentNum/BATCH_SIZE;
        return "scrapedDb/" + fileNum + ".csv";
    }

    private static long getCurrentNum() {
       // return 10898812;
        return 11196500;
    }
    
    private static void cleanUpConsolidated() throws IOException {
        // remove all state names
        WebDriver driver = makeDriver();
        List<String> allLines = java.nio.file.Files.readAllLines(Path.of("consolidated.csv"));
        try (BufferedWriter out = new BufferedWriter(new FileWriter("consolidated.csv"))) {
            for (String line : allLines) {
                String[] data = line.split(",");
                if (data[0].length() < 3) {
                    writeHorse(Long.parseLong(data[1].trim()), driver, out);
                } else {
                    out.write(line);
                    out.newLine();
                }
            }
        } finally {
            driver.quit();
        }
    }
}

