import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EditHorseComponent extends JPanel {
    
    private JPanel horses;
    
    private JTextField title;
    private JTextArea bio;
    private VideoPanel videos;
    private PhotosPanel photos;
    private Document htmlDoc;
    private Set<String> prevImages = new HashSet<>();
    private String prevProfile;

    public EditHorseComponent() {
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        setLayout(new BorderLayout());

        horses = new JPanel();
        horses.setLayout(new GridLayout(0, 2, 3, 3));
        JScrollPane comp = new JScrollPane(horses,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        AvailableHorsesLoader.loadHorses().forEach(horse -> {
            horses.add(new EditHorseListingComponent(horse.page(), horse.title(), horse.thumbnail(),
                    href -> editHorse(href)));
        });
        add(comp, BorderLayout.CENTER);
    }
    
    public EditHorseComponent(String horsePage) {
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        setLayout(new BorderLayout());
        editHorse(horsePage);
    }

    private void editHorse(String horsePage) {
        Component centerComp = ((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (centerComp != null) {
            remove(centerComp);
        }
        
        Component northComp = ((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.NORTH);
        if (northComp != null) {
            remove(northComp);
        }

        JButton next = new CustomButton("Update Horse Webpage");
        JPanel header = CreateListingFrontend.wrapButton(next);
        StatusLabel success = new StatusLabel();
        header.add(success);
        next.addActionListener(e -> {
            CreateListingFrontend.runWithSpinner(success, () -> {
                Element titleElem = htmlDoc.getElementsByTag("h1").get(0);
                updateTitle(horsePage, titleElem);

                List<String> bioInfo = Arrays.asList(bio.getText().split(System.lineSeparator()));
                updatePageText(titleElem, bioInfo);

                updateImages(horsePage);

                writeVideos();

                MarkPlaced.writePage(htmlDoc);
                GithubConnector.editFile(horsePage, "temp.html");

                updateAvailablePage(horsePage, bioInfo);

                GithubConnector.commitAndPush(GithubConnector.CommitType.EDIT_HORSE);

                showSuccess(horsePage);
            });
        });
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        try {
            htmlDoc = GithubConnector.getHtmlFile(horsePage);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }

        Element titleElem = addTitle(center);
        
        addBio(center, titleElem);

        photos = new PhotosPanel("Loading...");
        CreateListingFrontend.threadPool.submit(() -> {
            List<File> files;
            try {
                Element imageGallery = htmlDoc.getElementById("image_gallery_full");
                files = GithubConnector.getImageDirectory(horsePage.replace(".html", "_files/"));
                // Don't include any photos that were previously removed, or the profile thumbnail 
                // (which will slowly degrade the quality)
                files = files.stream().filter(file -> getIndex(file, imageGallery) >= 0)
                        .sorted((image1, image2) -> Integer.compare(getIndex(image1, imageGallery), getIndex(image2, imageGallery)))
                        .toList();
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }

            photos.addFiles(files);
            prevImages.addAll(photos.getImageFilenames());
            prevProfile = photos.getImageFilenames().get(0);
        });
        center.add(photos.getPhotosComponent());
        
        List<String> urls = new ArrayList<>();
        for (Element elem : htmlDoc.getElementsByTag("iframe")) {
            urls.add(elem.attr("src"));
        }
        videos = new VideoPanel(urls);
        center.add(videos);
        add(center, BorderLayout.CENTER);
        
        revalidate();
        repaint();
    }

    private void showSuccess(String horsePage) {
        remove(((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.CENTER));
        remove(((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.NORTH));
        
        add(new SuccessHorseComponent(horsePage, new SuccessHorseComponent.FacebookCallback() {
            
            @Override
            public void postToFb() {
                updateFbPost();
            }
            
            @Override
            public String getButtonText() {
                return "Update Facebook Post";
            }
        }), BorderLayout.CENTER);
        
    }

    protected void updateFbPost() {
        String text = title.getText();
        text += "\n";
        text += "\n";
        
        text += removeHrefs(bio.getText());
        text += "\n";
        
        List<String> videoLinks = videos.getYoutubeLinks();
        for (String video : videoLinks) {
            text += "Video: " + video + "\n";
        }
        
        try {
            String horseName = title.getText().substring(0, title.getText().indexOf(","));
            String postId = FbConnector.findPost(horseName).id();
            if (postId == null) {
                FbConnector.createPagePost(text, photos.getImageFilenames());
            } else {
                // Open question: do we need to be able to edit the photos?
                // For now, let's say no
                FbConnector.updatePostText(text, postId);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }   
    }

    private String removeHrefs(String text) {
        String openTag = "<a ";
        String closeTag = "</a>";
        int startIndex = text.indexOf(openTag);
        while (startIndex >= 0) {
            int hrefIndex = text.indexOf("href", startIndex) + 6;
            int hrefEnd = text.indexOf("\"", hrefIndex);
            int textStart = text.indexOf(">", startIndex);
            int endIndex = text.indexOf(closeTag, startIndex);
            text = text.substring(0, startIndex) + 
                    text.substring(textStart, endIndex) + ": " +
                    text.substring(hrefIndex, hrefEnd) +
                    text.substring(endIndex + 4);
            startIndex = text.indexOf(openTag);
        }
        
        return text;
    }

    private void writeVideos() {
        Element appendAfter = htmlDoc.getElementById("gallery_dot_progress");
        
        while (true) {
            Elements jogVideos = htmlDoc.getElementsByClass("jog_video");
            if (jogVideos.isEmpty()) {
                break;
            }
            jogVideos.get(0).remove();
        }
        
        for (String video : videos.getYoutubeLinks()) {
            String videoId = CreateListing.extractId(video);
            
            String toWrite = "        <div class=\"jog_video\">" + 
                    "            <iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/" + videoId + "\""+ 
                    "                title=\"YouTube video player\" frameborder=\"0\""+ 
                    "                allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\""+ 
                    "                referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen></iframe>" + 
                    "        </div>";

            appendAfter.after(toWrite);
        }
    }


    private int getIndex(File f, Element imageGallery) {
        String name = f.getPath().substring(f.getPath().lastIndexOf(File.separator) + 1);
       for (int i = 0; i < imageGallery.childrenSize(); i++) {
           Element elem = imageGallery.child(i);
           if (elem.attr("src").endsWith(name)) {
               return i;
           }
       }
       return -1;
    }

    private void updateImages(String horsePage) throws IOException {
        String imageFilePrefix = horsePage.replace(".html", "_files/");
       
        List<String> imageFiles = photos.getImageFilenames();
        if (!photos.getImageFilenames().get(0).equals(prevProfile)) {
            // Update images - profile pic can change, support that
            try {
                String thumbName = photos.prepThumbnail().get();
                GithubConnector.editFile(imageFilePrefix + thumbName, thumbName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } 
        
        Element imageGallery = htmlDoc.getElementById("image_gallery_full");
        // clear previous images
        while (imageGallery.childrenSize() > 0) {
            imageGallery.child(0).remove();
        }
        for (String fullPath : imageFiles) {
            String image = fullPath.substring(fullPath.lastIndexOf(File.separator) + 1);
            // no-op if image already there - this does mean we don't support name swapping of local image files,
            // but that seems fine
            if (!prevImages.contains(fullPath)) {
                GithubConnector.editFile(imageFilePrefix + image, fullPath);
            }
            imageGallery.append( "<img src=\"../" + imageFilePrefix + image + "\" full_size=\"../" + imageFilePrefix + image + "\">");
        }
    }

    private void updateAvailablePage(String horsePage, List<String> bioInfo) throws IOException {
        String snippet = CreateListing.buildSnippet(bioInfo);
           MarkPlaced.updatePage("available.html", page -> {
               for (Element elem : page.getElementsByClass("available_title")) {
                   if (elem.attr("href").equals(horsePage)) {
                       elem.text(title.getText());
                       for (Element sib : elem.siblingElements()) {
                           if (sib.tag().getName().equalsIgnoreCase("div")) {
                               sib.getElementsByTag("p").get(0).html(snippet 
                                       +  "<a href=\"" + horsePage + "\">Continue Reading...</a>");
                           }
                       }
                   }
               }
            });
    }

    private List<String> updatePageText(Element titleElem, List<String> bioInfo) {
        for (Element deets : titleElem.nextElementSiblings()) {
            if (deets.tag().getName().equalsIgnoreCase("p")) {
                deets.remove();
            }
        }
        
        Element addAfter = titleElem;
        
        for (String bioPara : bioInfo) {
            if (!bioPara.isBlank()) {
                addAfter.after("<p>" + bioPara + "</p>");
                addAfter = addAfter.nextElementSibling();
            }
        }
        return bioInfo;
    }

    private void updateTitle(String horsePage, Element titleElem) throws IOException {
        if (!titleElem.ownText().equals(title.getText())) {
            titleElem.text(title.getText());
            MarkPlaced.updatePage("index.html", page -> {
               Elements mobileList = page.getElementById("full_available_list").children();
               for (Element elem : mobileList) {
                   if (elem.attr("href").equals(horsePage)) {
                       elem.attr("horse_title", title.getText());
                   }
               }
               Element ul = page.select(".recent_adds").get(0).getElementsByTag("ul").get(0);
               for (Element elem : ul.children()) {
                   Element link = elem.getElementsByTag("a").get(0);
                   if (link.attr("href").equals(horsePage)) {
                       link.text(title.getText());
                   }
               }
            });
        }
    }

    private void addBio(JPanel center, Element titleElem) {
        JLabel bioLabel = new JLabel("Horse Bio: ");
        bioLabel.setFont(CreateListingFrontend.DEFAULT_FONT);
        bio = new JTextArea(10, 70);
        for (Element deets : titleElem.nextElementSiblings()) {
            if (deets.tag().getName().equalsIgnoreCase("p")) {
                // yes, we're putting raw html in here. The user is already an admin who has write access
                // to this page's html anyway
                if (!deets.html().isBlank()) {
                    bio.append(deets.html() + System.lineSeparator() + System.lineSeparator());
                }
            }
        }
        bio.setWrapStyleWord(true);
        bio.setFont(CreateListingFrontend.DEFAULT_FONT);
        bio.setLineWrap(true);
        bio.setBackground(CreateListingFrontend.ADMIN_BACKGROUND);
        
        JScrollPane bioScroll = new JScrollPane(bio, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        bioScroll.setPreferredSize(new Dimension(CreateListingFrontend.OVERALL_WIDTH - 100, 200));

        JPanel bioPanel = CreateListingFrontend.wrapButton(bioLabel, new FlowLayout(FlowLayout.LEFT));
        bioPanel.add(bioScroll);
        center.add(bioPanel);
    }

    private Element addTitle(JPanel center) {
        JLabel titleLabel = new JLabel("Title: ");
        titleLabel.setFont(CreateListingFrontend.DEFAULT_FONT);
        title = new JTextField();
        Element titleElem = htmlDoc.getElementsByTag("h1").get(0);
        title.setText(titleElem.ownText());
        JPanel titlePanel = CreateListingFrontend.wrapButton(titleLabel, new FlowLayout(FlowLayout.LEFT));
        titlePanel.add(title);
        center.add(titlePanel);
        return titleElem;
    }
}
