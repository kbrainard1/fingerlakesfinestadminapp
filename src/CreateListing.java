import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class CreateListing {

    public static boolean createListingPage(List<String> data, String thumbName,
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
        String pedigreeLink = "";
       
        List<String> videoLinks = new ArrayList<>();
        List<String> bio = new ArrayList<>();
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
                videoLinks.add(data.get(i).substring(data.get(i).lastIndexOf(" ")).trim());
                continue;
            }
            bio.add(data.get(i));
        }
        return createListingPage(name, title, thumbName, imagePaths, EquibaseConnector.loadEquibaseUrl(shortName), pedigreeLink, videoLinks, bio);
    }
    
    public static boolean createListingPage(String name, String title, String thumbName,
            List<String> imagePaths, String raceRecordLink, String pedigreeLink, List<String> videoLinks,
            List<String> bio) throws Exception {
        String shortNameBuilder = "";
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLetter(name.charAt(i))) {
                shortNameBuilder += Character.toLowerCase(name.charAt(i));
            }
        }
        String shortName = shortNameBuilder;

        Document page = Jsoup.parse(GithubConnector.getString(GithubConnector.getRetriably("horsePages/availTemplate.html")));
        page.getElementsByTag("head").get(0).append("<title>" + name + " | Finger Lakes Finest Thoroughbreds, Inc</title>");
        
        Element firstMainChild = page.getElementById("image_gallery_full");
        firstMainChild.before("    <img class=\"listing_thumb\" src=\"" + shortName + "_files/" + thumbName + "\">");
        firstMainChild.before("    <h1>" + title + "</h1>");

        for (String bioPara : bio) {
            firstMainChild.before("<p>" + bioPara + "</p>");
        }

        firstMainChild.before("<p><a href=\"" + raceRecordLink + "\" target=\"_blank\" rel=\"noreferrer noopener\">Race Record</a></p>");
        firstMainChild.before("<p><a rel=\"noreferrer noopener\" href=\"" + pedigreeLink + "\" target=\"_blank\">Pedigree</a></p>");
        
        GithubConnector.commitNew("horsePages/" + shortName + "_files/" + thumbName, thumbName);
        writeImages(page, shortName, imagePaths);
        
        writeVideos(page, videoLinks);
        
        page.outputSettings(page.outputSettings().prettyPrint(false));
        try (BufferedWriter out = new BufferedWriter(new FileWriter("temp.html"))) {
            out.write(page.outerHtml());
            out.newLine();
        }
       boolean newPage = GithubConnector.commitNew("horsePages/" + shortName + ".html", "temp.html");
       if (!newPage) {
           // Generally, semantic merge conflict, horse has already been posted
           return false;
       }

        String snippet = buildSnippet(bio);

        updateMetadata(title,  "horsePages/" + shortName + "_files/" + thumbName, 
                "horsePages/" + shortName + ".html", snippet);
        return true;
    }

    private static void writeImages(Document page, String shortName,
            List<String> imageFullPaths) throws Exception {
        Element imageGallery = page.getElementById("image_gallery_full");
        for (String fullPath : imageFullPaths) {
            String image = fullPath.substring(fullPath.lastIndexOf(File.separator) + 1);
            GithubConnector.commitNew("horsePages/" + shortName + "_files/" + image, fullPath);
            imageGallery.append( "<img src=\"" + shortName + "_files/" + image + "\" full_size=\"" + shortName + "_files/" + image + "\">");
        }
    }

    public static String buildSnippet(List<String> data) {
        String snippet = "";
        int dataIndex = 0;
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
            String videoId = extractId(video);
            
            String toWrite = 
                    "        <div class=\"jog_video\">" + System.lineSeparator() +
                    "            <iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/" + videoId + "\""+ System.lineSeparator() +
                    "                title=\"YouTube video player\" frameborder=\"0\""+ System.lineSeparator() +
                    "                allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\""+ System.lineSeparator() +
                    "                referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen></iframe>" + System.lineSeparator() +
                    "        </div>"  + System.lineSeparator();

            appendAfter.after(toWrite);
        }
    }

    // formats supported:
    // https://www.youtube.com/watch?v=Zm4zWUoRQpo
    // https://youtu.be/x1TOTwSb0xc?is=o7EoG8GKI_7k6qh0
    // https://youtu.be/x1TOTwSb0xc
    public static String extractId(String video) {
        int idStart, idEnd;
        if (video.contains("/watch")) {
            idStart = video.indexOf("=") + 1;
            idEnd = video.indexOf("&");
            if (idEnd < 0) {
                idEnd = video.length();
            }
        } else {
            idStart = video.lastIndexOf("/") + 1;
            idEnd = video.indexOf("?");
            if (idEnd < 0) {
                idEnd = video.length();
            }
        }
        return video.substring(idStart, idEnd);
    }
    
 // updates index & available.html
    public static void updateMetadata(String title, String thumbPath, String pageUrl, String snippet) throws Exception {
        MarkPlaced.updatePage("available.html", page -> {
            Element titleElem = page.getElementsByTag("h1").get(0);
            String toAdd = 
                    "    <div class=\"available_snippet_div\">" + System.lineSeparator() +
                    "        <a class=\"available_title\" href=\"" + pageUrl + "\">" + title + "</a>" + System.lineSeparator() +
                    "        <div class=\"available_snippet_inner_div\">" + System.lineSeparator() +
                    "        <img class=\"snippet_thumb\" src=\"" + thumbPath + "\">" + System.lineSeparator() +
                    "        <p>" + snippet + System.lineSeparator() +
                    "        <a href=\"" + pageUrl + "\">Continue Reading...</a></p> " + System.lineSeparator() +
                    "        </div>" + System.lineSeparator() +
                    "    </div>" + System.lineSeparator();
            titleElem.after(toAdd);
        });
        
        MarkPlaced.updatePage("index.html", page -> {
            page.getElementById("full_available_list").prepend("        <img src=\"" + thumbPath + "\" href=\"" + pageUrl + "\"" +
                    "            horse_title=\"" + title + "\">" + System.lineSeparator());
            Element ul = page.select(".recent_adds").get(0).getElementsByTag("ul").get(0);
            ul.prepend("<li><a class=\"available_title\" href=\"" + pageUrl + "\">" + title + "</a></li>" + System.lineSeparator());
        });
    }
}
