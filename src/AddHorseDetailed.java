import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class AddHorseDetailed extends AddHorseShared {
    private HorseDetailsRecord horseData;

    public AddHorseDetailed() {
        setLayout(new BorderLayout());
        EquibaseConnector.precacheConnection();

        JLabel firstPageLabel = new JLabel("First, enter the horse stats");
        firstPageLabel.setFont(CreateListingFrontend.DEFAULT_FONT);
        JPanel header = CreateListingFrontend.wrapButton(firstPageLabel);

        // enter stats
        JButton autofill = new CustomButton("Auto-fill based on name");
        autofill.addActionListener(e -> {
            if (!horseData.getName().isBlank()) {
                CreateListingFrontend.showSpinner();
                CreateListingFrontend.threadPool.submit(() -> {
                    try {
                        attemptToFill(horseData.getName());
                    } finally {
                        CreateListingFrontend.hideSpinner();
                    }
                });
            }
        });
        header.add(CreateListingFrontend.wrapButton(autofill));
        JButton next = new CustomButton("Next - Add Details");
        header.add(next);
        next.addActionListener(e -> {
            if (horseData.validateStats()) {
                loadNextScreen();
            }
        });
        add(header, BorderLayout.NORTH);

        horseData = new HorseDetailsRecord();
        add(horseData.createStatsComponent(), BorderLayout.CENTER);
    }


    private void loadNextScreen() {
        remove(((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.CENTER));
        remove(((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.NORTH));

        JButton next = new CustomButton("Create Horse Webpage");
        JPanel header = CreateListingFrontend.wrapButton(next);
        
        header.add(next);
        JLabel success = new JLabel();
        success.setForeground(CreateListingFrontend.ERROR_COLOR);
        header.add(success);
        next.addActionListener(e -> {
            success.setText("");
            CreateListingFrontend.showSpinner();

            CreateListingFrontend.threadPool.submit(() -> {
                try {
                    List<String> imageFiles = photosPanel.prepImageFiles();

                    List<String> bioPlusBoilerplate = new ArrayList<>(Arrays.asList(horseData.getBio().split("\n")));
                    bioPlusBoilerplate.add(horseData.getContact());
                    bioPlusBoilerplate.add(horseData.getPrice());
                    bioPlusBoilerplate.add("A PPE is always recommended. For information about vet practices available to do PPEs, and other "
                            + "important information about the buying process, please see the <a href=\"../howtobuy.html\">How to Buy</a> page.");

                    List<String> videoLinks = horseData.getVideos().getYoutubeLinks();
                    String title = horseData.getName().toUpperCase() + ", " + horseData.getYear() + ", " + horseData.getHeight() + " " + horseData.getColor() + " " + horseData.getSex();
                    CreateListing.createListingPage(horseData.getName(), title, "profile.jpg",
                            imageFiles, horseData.getEquibase(), horseData.getPedigree(), videoLinks,
                            bioPlusBoilerplate);
                    
                    showSuccess();
                } catch (Exception e1) {
                    success.setText("Unexpected error: " + e1.getMessage());
                    throw new RuntimeException(e1);
                } finally {
                    CreateListingFrontend.hideSpinner();
                }
            });
        });
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        JLabel bioLabel = new JLabel("Horse Bio: ");
        bioLabel.setFont(CreateListingFrontend.DEFAULT_FONT);
        JPanel bioPanel = CreateListingFrontend.wrapButton(bioLabel, new FlowLayout(FlowLayout.LEFT));
        bioPanel.add(horseData.createBioComponent());
        center.add(bioPanel);
        center.add(photosPanel.getPhotosComponent());
        
        addVideoPanel(center);
        add(center, BorderLayout.CENTER);
        
        revalidate();
        repaint();
    }

    private void showSuccess() {
        remove(((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.CENTER));
        remove(((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.NORTH));
        
        String shortNameBuilder = "";
        for (int i = 0; i < horseData.getName().length(); i++) {
            if (Character.isLetter(horseData.getName().charAt(i))) {
                shortNameBuilder += Character.toLowerCase(horseData.getName().charAt(i));
            }
        }
        String shortName = shortNameBuilder;
        String pageUrl = "horsePages/" + shortName + ".html";
        
        add(new SuccessHorseComponent(pageUrl, new SuccessHorseComponent.FacebookCallback() {
            
            @Override
            public void postToFb() {
                createFbPost();
            }
            
            @Override
            public String getButtonText() {
                return "Create Facebook Post";
            }
        }), BorderLayout.CENTER);
        
    }


    private void createFbPost() {
        String text = horseData.getName().toUpperCase() + ", " + horseData.getYear() + ", " + horseData.getHeight() + " " + horseData.getColor() + " " + horseData.getSex();
        text += "\n";
        text += "\n";
        
        text += horseData.getBio();
        text += "\n";
        text += horseData.getContact();
        text += "\n";
        text += horseData.getPrice();
        text += "\n";
        
        text += "Race Record: " + horseData.getEquibase();
        text += "\n";
        text += "Pedigree: " + horseData.getPedigree();
        text += "\n";
        
        List<String> videoLinks = horseData.getVideos().getYoutubeLinks();
        for (String video : videoLinks) {
            text += "Video: " + video + "\n";
        }
        
        text += "A PPE is always recommended. For information about vet practices available to do PPEs, and other important "
                + "information about the buying process, please see the How to Buy page on our website.";
        
        try {
            FbConnector.createPagePost(text, photosPanel.getImageFilenames());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }   
    }


    private void addVideoPanel(JPanel center) {
        center.add(horseData.createVideoComponent());
    }

    private void attemptToFill(String name) {
        StringBuilder sanitized = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLetter(name.charAt(i)) || name.charAt(i) == ' ') {
                sanitized.append(Character.toLowerCase(name.charAt(i)));
            }
        }

        EquibaseConnector.HorsePage horseInfo = EquibaseConnector.loadHorsePage(sanitized.toString());
        String url = horseInfo.url();
        Document horsePage = Jsoup.parse(horseInfo.contents());
        Elements elems = horsePage.select(".horse-profile-top-bar-headings");
        String[] horseDeets = elems.get(0).ownText().split(",");
        horseData.setEquibase(url);
        horseData.setColor(expandColor(horseDeets[1]));
        horseData.setSex(expandSex(horseDeets[2]));
        horseData.setYear(horseDeets[horseDeets.length - 1]);
        horseData.setPedigree(horsePage.select("a[href*=equineline.com/Free]").get(0).attr("href"));
    }

    private String expandSex(String sex) {
        sex = sex.trim();
        if (sex.equalsIgnoreCase("g")) {
            return "gelding";
        }
        if (sex.equalsIgnoreCase("m")) {
            return "mare";
        }
        if (sex.equalsIgnoreCase("f")) {
            return "filly";
        }
        if (sex.equalsIgnoreCase("c")) {
            return "colt";
        }
        if (sex.equalsIgnoreCase("h")) {
            return "stallion";
        }
        return sex;
    }


    private String expandColor(String color) {
        color = color.trim();
        if (color.equalsIgnoreCase("ch")) {
            return "chestnut";
        }
        if (color.equalsIgnoreCase("DK B/BR") || color.equalsIgnoreCase("DK B")) {
            return "dark bay";
        }
        if (color.equalsIgnoreCase("b")) {
            return "bay";
        }
        if (color.equalsIgnoreCase("ro")) {
            return "grey";
        }
        if (color.equalsIgnoreCase("gr/ro")) {
            return "grey";
        }
        if (color.equalsIgnoreCase("gr")) {
            return "grey";
        }
        return color;
    }

}
