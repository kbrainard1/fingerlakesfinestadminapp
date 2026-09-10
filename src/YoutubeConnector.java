import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.Video;

public class YoutubeConnector {
    private static final String TOKEN_KEY = "YOUTUBE_API_KEY";
    private static final String channelId = "UCoFDUQqK-KWncEVDwiUIWDg"; // FLFinests
    private static final String APPLICATION_NAME = "FLF-Admin-App";
    
    static String token;

    static {
        try {
            token = Files.readAllLines(Path.of("env")).stream().filter(line -> line.contains(TOKEN_KEY))
                    .map(line -> line.substring(line.indexOf("=") + 1)).toList().get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static record YoutubeResult(String title, Thumbnail thumb, String videoId) {}
    
    public static List<YoutubeResult> getMatchingVideos(String horseName) throws Exception {
     
        YouTube youtubeService = new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                null)
                .setApplicationName(APPLICATION_NAME)
                .build();
     
        YouTube.Search.List search = youtubeService.search().list(Collections.singletonList("snippet"));
        search.setKey(token);
        search.setChannelId(channelId);
        search.setType(Collections.singletonList("video")); // Only retrieve video results
        search.setOrder("date"); // Sort by upload date (newest first)
        search.setMaxResults(10L);
        search.setQ(horseName);

     
        SearchListResponse response = search.execute();
        List<SearchResult> searchResults = response.getItems();
        return searchResults.stream().map(sr -> new YoutubeResult(sr.getSnippet().getTitle(), 
                sr.getSnippet().getThumbnails().getMedium(),
                sr.getId().getVideoId())).toList();
    }

    public static YoutubeConnector.YoutubeResult lookupByLink(String youtubeLink) throws Exception {
        String videoId = CreateListing.extractId(youtubeLink);
        
        YouTube youtubeService = new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                null)
                .setApplicationName(APPLICATION_NAME)
                .build();
        
     
        YouTube.Videos.List request = youtubeService.videos()
                .list(Collections.singletonList("snippet"));
        request.setKey(token);
        request.setId(Collections.singletonList(videoId));

     
        Video video = request.execute().getItems().get(0);
        return new YoutubeResult(video.getSnippet().getTitle(), video.getSnippet().getThumbnails().getMedium(), video.getId());
    }
}
