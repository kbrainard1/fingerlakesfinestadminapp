import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class CreateListing {

    public static boolean createListingPage(List<String> data, Future<String> thumbnail,
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
        return createListingPage(name, title, thumbnail, imagePaths, EquibaseConnector.loadEquibaseUrl(shortName), pedigreeLink, videoLinks, bio);
    }
    
    public static boolean createListingPage(String name, String title, Future<String> thumbnail,
            List<String> imagePaths, String raceRecordLink, String pedigreeLink, List<String> videoLinks,
            List<String> bio) throws Exception {
        String shortNameBuilder = "";
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLetter(name.charAt(i))) {
                shortNameBuilder += Character.toLowerCase(name.charAt(i));
            }
        }
        String shortName = shortNameBuilder;
        
        Future<?> storeImageFiles = CreateListingFrontend.threadPool.submit(() -> {
            try {
                storeImageFiles(imagePaths, thumbnail, shortName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        Document page = GithubConnector.getHtmlFile("horsePages/availTemplate.html");
        page.getElementsByTag("head").get(0).append("<title>" + nameCase(name) + " | Finger Lakes Finest Thoroughbreds, Inc</title>");
        
        Element firstMainChild = page.getElementById("image_gallery_full");
        firstMainChild.before("    <img class=\"listing_thumb\" src=\"" + shortName + "_files/" + PhotosPanel.THUMBNAIL_NAME + "\">");
        firstMainChild.before("    <h1>" + title + "</h1>");

        for (String bioPara : bio) {
            firstMainChild.before("<p>" + bioPara + "</p>");
        }

        firstMainChild.before("<p><a href=\"" + raceRecordLink + "\" target=\"_blank\" rel=\"noreferrer noopener\">Race Record</a></p>");
        firstMainChild.before("<p><a rel=\"noreferrer noopener\" href=\"" + pedigreeLink + "\" target=\"_blank\">Pedigree</a></p>");
        
        writeImages(page, shortName, imagePaths);
        
        writeVideos(page, videoLinks);
        
        MarkPlaced.writePage(page);
       
        if (GithubConnector.getFile("horsePages/" + shortName + ".html").exists()) {
            // already created! Don't double-post to the metadata
            return false;
        }
        GithubConnector.editFile("horsePages/" + shortName + ".html", "temp.html");
        String snippet = buildSnippet(bio);

        updateMetadata(title,  "horsePages/" + shortName + "_files/" + PhotosPanel.THUMBNAIL_NAME, 
                "horsePages/" + shortName + ".html", snippet);
        storeImageFiles.get(); 
        GithubConnector.commitAndPush(GithubConnector.CommitType.ADD_HORSE);
        return true;
    }

    private static String nameCase(String name) {
        String result = "";
        boolean shouldCapsNext = true;
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLetter(name.charAt(i))) {
                if (shouldCapsNext) {
                    result += Character.toUpperCase(name.charAt(i));
                } else {
                    result += Character.toLowerCase(name.charAt(i));
                }
                shouldCapsNext = false;
            } else {
                result += name.charAt(i);
            }
                
            if (Character.isWhitespace(name.charAt(i))) {
                shouldCapsNext = true;
            }
        }
        return result;
    }

    private static void storeImageFiles(List<String> imagePaths, Future<String> thumbnail, String shortName) throws IOException {
        GithubConnector.getFile("horsePages/" + shortName + "_files/").mkdir();
        for (String fullPath : imagePaths) {
            String image = fullPath.substring(fullPath.lastIndexOf(File.separator) + 1);
            GithubConnector.editFile("horsePages/" + shortName + "_files/" + image, fullPath);
        }
        
        String thumbnailName;
        try {
            thumbnailName = thumbnail.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        GithubConnector.editFile("horsePages/" + shortName + "_files/" + thumbnailName, thumbnailName);
    }

    private static void writeImages(Document page, String shortName,
            List<String> imageFullPaths) throws Exception {
        Element imageGallery = page.getElementById("image_gallery_full");
        for (String fullPath : imageFullPaths) {
            String image = fullPath.substring(fullPath.lastIndexOf(File.separator) + 1);
            imageGallery.append( "<img src=\"" + shortName + "_files/" + image + "\" full_size=\"" + shortName + "_files/" + image + "\">");
        }
    }

    public static String buildSnippet(List<String> data) {
        String snippet = "";
        int dataIndex = 0;
        int snippetLen = 400;
        boolean trimmed = false;
        while (snippet.length() < snippetLen && !trimmed && dataIndex < data.size()) {
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
                    "        <div class=\"jog_video\">" +
                    "            <iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/" + videoId + "\""+ 
                    "                title=\"YouTube video player\" frameborder=\"0\""+ 
                    "                allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\""+ 
                    "                referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen></iframe>" + 
                    "        </div>";

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
                    "    <div class=\"available_snippet_div\">" +
                    "        <a class=\"available_title\" href=\"" + pageUrl + "\">" + title + "</a>" +
                    "        <div class=\"available_snippet_inner_div\">" +
                    "        <img class=\"snippet_thumb\" src=\"" + thumbPath + "\">" + 
                    "        <p>" + snippet + 
                    "        <a href=\"" + pageUrl + "\">Continue Reading...</a></p> " + 
                    "        </div>" +
                    "    </div>";
            titleElem.after(toAdd);
        });
        
        MarkPlaced.updatePage("index.html", page -> {
            page.getElementById("full_available_list").prepend("        <img src=\"" + thumbPath + "\" href=\"" + pageUrl + "\"" +
                    "            horse_title=\"" + title + "\">");
            Element ul = page.select(".recent_adds").get(0).getElementsByTag("ul").get(0);
            ul.prepend("<li><a class=\"available_title\" href=\"" + pageUrl + "\">" + title + "</a></li>");
        });
    }
}
