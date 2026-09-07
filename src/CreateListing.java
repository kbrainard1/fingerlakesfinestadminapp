import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class CreateListing {

    public static void createListingPage(List<String> data, String thumbName,
            List<String> imagePaths) throws Exception {
        String title = data.get(0);
        // sometimes it's name, birth year, sometimes name birth year, height
        // This should work for the next 900 years or so
        int nameEndPos = Math.min(title.indexOf(','), title.indexOf('2'));
        String name = title.substring(0, nameEndPos).trim();
        
        String shortNameBuilder = "";
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLetter(name.charAt(i))) {
                shortNameBuilder += Character.toLowerCase(name.charAt(i));
            }
        }
        String shortName = shortNameBuilder;

        Document page = Jsoup.parse(MarkPlaced.getString(GithubConnector.getRetriably("horsePages/availTemplate.html")));
        page.getElementsByTag("head").getFirst().append("<title>" + name + " | Finger Lakes Finest Thoroughbreds, Inc</title>");
        
        Element firstMainChild = page.getElementById("image_gallery_full");
        firstMainChild.before("    <img class=\"listing_thumb\" src=\"" + shortName + "_files/" + thumbName + "\">");
        firstMainChild.before("    <h1>" + title + "</h1>");
        
        List<String> videos = new ArrayList<>();
        String pedigreeLink = "";

        for (int i = 1; i < data.size(); i++) {
            String filterLine = data.get(i).toLowerCase().trim();
            if (filterLine.contains("equibase.com")) {
                continue;
            }
            if (filterLine.contains("pedigreequery.com")) {
                pedigreeLink = filterLine.substring(filterLine.lastIndexOf(" ")).trim();
                continue;
            }
            // youtube.com or youtu.be
            if (filterLine.contains("youtu")) {
                // youtube ids are case sensitive
                videos.add(data.get(i).substring(data.get(i).lastIndexOf(" ")).trim());
                continue;
            }
            firstMainChild.before("<p>" + data.get(i) + "</p>");
        }

        firstMainChild.before("<p><a href=\"" + getEquibaseLink(shortName) + "\" target=\"_blank\" rel=\"noreferrer noopener\">Race Record</a></p>");
        firstMainChild.before("<p><a rel=\"noreferrer noopener\" href=\"" + pedigreeLink + "\" target=\"_blank\">Pedigree</a></p>");
        
        writeImages(page, shortName, imagePaths);
        
        writeVideos(page, videos);
        
        page.outputSettings(page.outputSettings().prettyPrint(false));
        try (BufferedWriter out = new BufferedWriter(new FileWriter("temp.html"))) {
            out.write(page.outerHtml());
            out.newLine();
        }
       boolean newPage = GithubConnector.commitNew("horsePages/" + shortName + ".html", "temp.html");
       if (!newPage) {
           // Generally, semantic merge conflict, horse has already been posted
           return;
       }

        String snippet = buildSnippet(data);

        updateMetadata(title,  "horsePages/" + shortName + "_files/" + thumbName, 
                "horsePages/" + shortName + ".html", snippet);
    }



    private static void writeImages(Document page, String shortName,
            List<String> imageFullPaths) throws Exception {
        Element imageGallery = page.getElementById("image_gallery_full");
        for (String fullPath : imageFullPaths) {
            String image = fullPath.substring(fullPath.lastIndexOf(File.separator) + 1);
            imageGallery.append( "<img src=\"" + shortName + "_files/" + image + "\" full_size=\"" + shortName + "_files/" + image + "\">");
        }
    }

    private static String buildSnippet(List<String> data) {
        String snippet = "";
        int dataIndex = 1;
        int snippetLen = 400;
        boolean trimmed = false;
        while (snippet.length() < snippetLen && !trimmed) {
            String toAdd = data.get(dataIndex);
            while (toAdd.length() > snippetLen - snippet.length() && toAdd.contains(".")) {
                toAdd = toAdd.substring(0, toAdd.lastIndexOf("."));
                trimmed = true;
            }
            snippet += toAdd;
            if (trimmed) {
                snippet += ".";
            }
            snippet += " ";
            dataIndex++;
        }
        return snippet;
    }

    private static void writeVideos(Document page, List<String> videos) throws Exception {
        Element appendAfter = page.getElementById("gallery_dot_progress");
        
        for (String video : videos) {
            int idStart = video.lastIndexOf("/") + 1;
            int idEnd = video.indexOf("?");
            if (idEnd < 0) {
                idEnd = video.length();
            }
            String toWrite = 
                    "        <div class=\"jog_video\">" + System.lineSeparator() +
                    "            <iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/" + video.substring(idStart, idEnd) + "\""+ System.lineSeparator() +
                    "                title=\"YouTube video player\" frameborder=\"0\""+ System.lineSeparator() +
                    "                allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\""+ System.lineSeparator() +
                    "                referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen></iframe>" + System.lineSeparator() +
                    "        </div>"  + System.lineSeparator();

            appendAfter.after(toWrite);
        }
    }

    private static String getEquibaseLink(String title) throws InterruptedException {
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

        driver.get("https://www.equibase.com");
        Thread.sleep(1000);

        WebElement searchBox = driver.findElement(By.className("header-search-form"));
        WebElement input = searchBox.findElement(By.className("input"));
        input.sendKeys(title);
        input.sendKeys(Keys.ENTER);
        
        Thread.sleep(2000);
        try {
            return driver.getCurrentUrl();
        } catch (Exception e) {
            return "";
        } finally {
            driver.quit();
        }
    }
    
 // updates index & available.html
    public static void updateMetadata(String title, String thumbPath, String pageUrl, String snippet) throws Exception {
        MarkPlaced.updatePage("available.html", page -> {
            Element titleElem = page.getElementsByTag("h1").getFirst();
            String toAdd = 
                    "    <div class=\"available_snippet_div\">" + System.lineSeparator() +
                    "        <a class=\"available_title\" href=\"" + pageUrl + "\">" + title + "</a>" + System.lineSeparator() +
                    "        <div class=\"available_snippet_inner_div\">" + System.lineSeparator() +
                    "        <img class=\"snippet_thumb\" src=\"" + thumbPath + "\">" + System.lineSeparator() +
                    "        <p>" + snippet + System.lineSeparator() +
                    "        <a href=\"" + pageUrl + "\">Continue Reading...</a></p> " + System.lineSeparator() +
                    "        </div>" + System.lineSeparator() +
                    "    </div>";
            titleElem.after(toAdd);
        });
        
        MarkPlaced.updatePage("index.html", page -> {
            page.getElementById("full_available_list").append("        <img src=\"" + thumbPath + "\" href=\"" + pageUrl + "\"" + System.lineSeparator() +
                    "            horse_title=\"" + title + "\">");
            Element ul = page.select(".recent_adds").getFirst().getElementsByTag("ul").getFirst();
            ul.append("<li><a class=\"available_title\" href=\"" + pageUrl + "\">" + title + "</a></li>");
        });
    }
}
