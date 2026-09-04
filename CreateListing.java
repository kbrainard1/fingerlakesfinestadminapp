import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;


public class CreateListing {
    
    // Note: if this gets too complex, rewrite with JSoup

    // pass in a structured file
    public static void main(String[] args) throws Exception {
        List<String> data = Files.readAllLines(Path.of(args[0]));
        String pathToCheckout = args[1];
        
        createListingPage(data, pathToCheckout, "side.jpg");
    }
    
    // No, it's not ideal
    public static String getShortName(String title) {
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
        return shortNameBuilder;
    }

    public static void createListingPage(List<String> data, String pathToCheckout, String thumbName)
            throws FileNotFoundException, IOException, Exception {
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

        String detailPage = pathToCheckout + "/horsePages/" + shortName + ".html";
        FileOutputStream out = new FileOutputStream(detailPage);
        Files.copy(Paths.get(pathToCheckout + "/horsePages/availTemplate.html"), out);
        out.close();

        insertTextAfter("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">", 
                detailPage, "<title>" + name + " | Finger Lakes Finest Thoroughbreds, Inc</title>");

        List<String> videos = new ArrayList<>();
        insertTextAfter("<main>", detailPage, fileOut -> {
            fileOut.write("    <img class=\"listing_thumb\" src=\"" + shortName + "_files/" + thumbName + "\">");
            fileOut.newLine();
            fileOut.write("    <h1>" + title + "</h1>");

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
                fileOut.write("<p>" + data.get(i) + "</p>");
                fileOut.newLine();
            }
            
            fileOut.write("<p><a href=\"" + getEquibaseLink(shortName) + "\" target=\"_blank\" rel=\"noreferrer noopener\">Race Record</a></p>");
            fileOut.newLine();
            
            fileOut.write("<p><a rel=\"noreferrer noopener\" href=\"" + pedigreeLink + "\" target=\"_blank\">Pedigree</a></p>");
            fileOut.newLine();

            return 0;
        });

        witeImages(pathToCheckout, shortName, detailPage);
        
        writeVideos(videos, detailPage);
        
        String snippet = buildSnippet(data);

        updateMetadata(pathToCheckout, title,  "horsePages/" + shortName + "_files/" + thumbName, 
                "horsePages/" + shortName + ".html", snippet);
    }



    private static void witeImages(String pathToCheckout, String shortName, String detailPage) throws Exception {
        File imageDir = new File(pathToCheckout + "/horsePages/" + shortName + "_files/");
        if (imageDir.exists() && imageDir.isDirectory() && imageDir.listFiles().length > 0) {
            insertTextAfter("<div id=\"image_gallery_full\">", detailPage, fileOut -> {
                for (String image : imageDir.list()) {
                    fileOut.write( "<img src=\"" + shortName + "_files/" + image + "\" full_size=\"" + shortName + "_files/" + image + "\">");
                    fileOut.newLine();
                }

                return 0;
            });

        } else { // whoops, forgot to download the images first, do a best effort
            
            // Requires audit after generation, but this saves more time than it doesn't
            insertTextAfter("<div id=\"image_gallery_full\">", detailPage, 
                    "<img src=\"" + shortName + "_files/side.jpg\" full_size=\"" + shortName + "_files/side.jpg\">",
                    "<img src=\"" + shortName + "_files/side2.jpg\" full_size=\"" + shortName + "_files/side2.jpg\">",
                    "<img src=\"" + shortName + "_files/face.jpg\" full_size=\"" + shortName + "_files/face.jpg\">",
                    "<img src=\"" + shortName + "_files/front.jpg\" full_size=\"" + shortName + "_files/front.jpg\">",
                    "<img src=\"" + shortName + "_files/rear.jpg\" full_size=\"" + shortName + "_files/rear.jpg\">");
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

    private static void writeVideos(List<String> videos, String detailPage) throws Exception {
        insertTextAfter("Video Section", detailPage, fileOut -> {
            for (String video : videos) {
                int idStart = video.lastIndexOf("/") + 1;
                int idEnd = video.indexOf("?");
                if (idEnd < 0) {
                    idEnd = video.length();
                }
                String[] toWrite = {
                        "        <div class=\"jog_video\">",
                        "            <iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/" + video.substring(idStart, idEnd) + "\"",
                        "                title=\"YouTube video player\" frameborder=\"0\"",
                        "                allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\"",
                        "                referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen></iframe>",
                        "        </div>"
                        };
                for (String add : toWrite) {
                    fileOut.write(add);
                    fileOut.newLine();
                }
            }

            return 0;
        });
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

    public static void insertTextAfter(String lineBefore, String fileName, String...toAdd) throws Exception {
        insertTextAfter(lineBefore, fileName, out -> {
            for (String add : toAdd) {
                out.write(add);
                out.newLine();
            }
            return 0;
        });
    }
    
 // updates index & available.html
    public static void updateMetadata(String pathToCheckout, String title, String thumbPath, String pageUrl, String snippet) throws Exception {
        insertTextAfter("<h1>Available Horses</h1>", pathToCheckout + "/available.html", out -> {
            String[] toAdd = {"<div class=\"available_snippet_div\">",
                    "<a class=\"available_title\" href=\"" + pageUrl + "\">" + title + "</a>",
                    "    <div class=\"available_snippet_inner_div\">",
                    "   <img class=\"snippet_thumb\" src=\"" + thumbPath + "\">",
                    "   <p>" + snippet,
                    "       <a href=\"" + pageUrl + "\">Continue Reading...</a></p> ",
                    "</div>",
                    "</div>"
            };

            for (String add : toAdd) {
                out.write(add);
                out.newLine();
            }
            return 0;
        });

        insertTextAfter("<div id=\"full_available_list\">", pathToCheckout + "/index.html", out -> {
            String[] toAdd = {"<img src=\"" + thumbPath + "\" href=\"" + pageUrl + "\"",
                    "        horse_title=\"" + title + "\">"
            };

            for (String add : toAdd) {
                out.write(add);
                out.newLine();
            }
            return 0;
        });

        insertTextAfter("<h3>Recent Additions</h3>", pathToCheckout + "/index.html", out -> {
            String[] toAdd = {"<ul>",
                    "<li><a class=\"available_title\" href=\"" + pageUrl + "\">" + title + "</a></li>"
            };

            for (String add : toAdd) {
                out.write(add);
                out.newLine();
            }
            return 1;
        });


    }
    
    public static interface AddLines {
        int apply(BufferedWriter out) throws Exception;
    }

    public static void insertTextAfter(String lineBefore, String fileName, AddLines addition) throws Exception {
        try (BufferedWriter temp = new BufferedWriter(new FileWriter("temp.html"));
                BufferedReader in = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = in.readLine()) != null) {
                temp.write(line);
                temp.newLine();
                if (line.contains(lineBefore)) {
                    int linesToSkip = addition.apply(temp);
                    for (int i = 0; i < linesToSkip; i++) {
                        in.readLine();
                    }
                }
            }
        }
        new File(fileName).delete();
        new File("temp.html").renameTo(new File(fileName));
    }
}
