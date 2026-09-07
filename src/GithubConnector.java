import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

public class GithubConnector {
    
    private static final String GITHUB_REPO = "kbrainard1/fingerlakesfinest";
    private static final String EDIT_BRANCH = "staging";
    private static final String TOKEN_KEY = "GH_TOKEN";
    
    private static GHRepository repo;
    private static String token;
    
    static {
        try {
           token = Files.readAllLines(Path.of("env")).stream().filter(line -> line.contains(TOKEN_KEY))
           .map(line -> line.substring(line.indexOf("=") + 1)).toList().get(0);
           // TODO: device flow auth if needed
            GitHub github = new GitHubBuilder().withOAuthToken(token).build();
            repo = github.getRepository(GITHUB_REPO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static GHContent getRetriably(String file) throws IOException {
        for (int i = 0; i < 3; i++) {
            try {
                return repo.getFileContent(file, EDIT_BRANCH);
            } catch (IOException e) {
               // ignore and retry
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                }
            }
        }
        return repo.getFileContent(file, EDIT_BRANCH);
    }
    
    public static List<String> readFile(GHContent fileContent) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader buff = new BufferedReader(new InputStreamReader(fileContent.read()))) {
            String line;
            while ((line = buff.readLine()) != null) {
                result.add(line);
            }
        }
        return result;
     }

    public static void commitChange(String repoFile, String locationOfContents) throws IOException {
        GHContent content = getRetriably(repoFile);
        repo.createContent()
        .content(Files.readAllBytes(Path.of(locationOfContents)))
        .message("Content created")
        .branch(EDIT_BRANCH)
        .sha(content.getSha())
        .path(repoFile)
        .commit();
    }
    
    public static boolean commitNew(String repoFile, String locationOfContents) throws IOException {
        try {
            repo.createContent()
            .content(Files.readAllBytes(Path.of(locationOfContents)))
            .message("Content created")
            .branch(EDIT_BRANCH)
            .path(repoFile)
            .commit();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void cloneStaging() {
        try {
            repo.readZip(is -> {
                Files.copy(is, Path.of("staging.zip"), StandardCopyOption.REPLACE_EXISTING);
                return null;
            }, EDIT_BRANCH);


            if (new File("staging").exists()) {
                deleteDirectory(new File("staging"));
            }

           
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream("staging.zip"))) {
                // outer folder is the repo name, skip that
                zis.getNextEntry();
                ZipEntry entry;
                byte[] buffer = new byte[1024];
                while ((entry = zis.getNextEntry()) != null) {
                    File newFile = new File("staging" + File.separator + removeFirstFolder(entry.getName()));
                    if (entry.isDirectory()) {
                        newFile.mkdirs();
                    } else {
                        new File(newFile.getParent()).mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            int length;
                            while ((length = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, length);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String removeFirstFolder(String name) {
       return name.substring(name.indexOf("/") + 1);
    }

    private static void deleteDirectory(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                deleteDirectory(f);
            }
        }
        file.delete();
    }

    public static void mergeStaging() {
        try {
            repo.getBranch("main").merge(repo.getBranch(EDIT_BRANCH), "deploy site");
            repo.getBranch(EDIT_BRANCH).merge(repo.getBranch("main"), "avoid conflicts");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() {
        // just does the static init
    }

}
