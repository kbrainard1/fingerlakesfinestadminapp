import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AddHorseDetailed extends AddHorseShared {
    
    private static final List<String> htmlPrefix = new ArrayList<>();
    private static final List<String> htmlSuffix = new ArrayList<>();
    private static final List<String> fbPrefix = new ArrayList<>();
    private static final List<String> fbSuffix = new ArrayList<>();
    static {
        try {
            htmlPrefix.addAll(Files.readAllLines(GithubConnector.getFile("resources/htmlPrefix.txt").toPath()).stream().filter(line -> !line.isBlank()).toList());
            htmlSuffix.addAll(Files.readAllLines(GithubConnector.getFile("resources/htmlSuffix.txt").toPath()).stream().filter(line -> !line.isBlank()).toList());
            fbPrefix.addAll(Files.readAllLines(GithubConnector.getFile("resources/fbPrefix.txt").toPath()).stream().filter(line -> !line.isBlank()).toList());
            fbSuffix.addAll(Files.readAllLines(GithubConnector.getFile("resources/fbSuffix.txt").toPath()).stream().filter(line -> !line.isBlank()).toList());
        } catch (IOException e) {
            htmlSuffix.add("A PPE is always recommended. For information about vet practices available to do PPEs, and other "
                    + "important information about the buying process, please see the <a href=\"../howtobuy.html\">How to Buy</a> page.");
            fbSuffix.add("A PPE is always recommended. For information about vet practices available to do PPEs, and other "
                    + "important information about the buying process, please see the How to Buy page on our website.");
        }
    }
    
    private HorseDetailsRecord horseData;

    public AddHorseDetailed() {
        setLayout(new BorderLayout());

        JButton next = new CustomButton("Next - Add Details");
        next.setEnabled(false);
        JPanel header = CreateListingFrontend.wrapButton(next);

        next.addActionListener(e -> {
            if (horseData.validateStats()) {
                loadNextScreen();
            }
        });
        add(header, BorderLayout.NORTH);

        JPanel addMargins = new JPanel();
        addMargins.setBackground(Color.WHITE);
        addMargins.setLayout(new BorderLayout());
        addMargins.add(MarkPlaceableComponent.createSpacer(), BorderLayout.WEST);
        addMargins.add(MarkPlaceableComponent.createSpacer(), BorderLayout.EAST);
        addMargins.add(MarkPlaceableComponent.createSpacer(), BorderLayout.SOUTH);
        
        horseData = new HorseDetailsRecord();
        addMargins.add(horseData.createStatsComponent(name -> {
            try {
                next.setEnabled(true);
                attemptToFill(name);
                return true;
            } catch (Exception e) {
                return false;
            }
            
        }), BorderLayout.CENTER);
        
        add(addMargins, BorderLayout.CENTER);
    }


    private void loadNextScreen() {
        remove(((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.CENTER));
        remove(((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.NORTH));

        JButton next = new CustomButton("Create Horse Webpage");
        JPanel header = CreateListingFrontend.wrapButton(next);
        
        header.add(next);
        StatusLabel success = new StatusLabel();
        header.add(success);
        next.addActionListener(e -> {
            CreateListingFrontend.runWithSpinner(success, () -> {
                List<String> imageFiles = photosPanel.getImageFilenames();
                Future<String> thumnail = photosPanel.prepThumbnail();

                List<String> bioPlusBoilerplate = new ArrayList<>();
                bioPlusBoilerplate.addAll(htmlPrefix);
                bioPlusBoilerplate.addAll(Arrays.asList(horseData.getBio().split("\n")));
                bioPlusBoilerplate.add("Contact: " + horseData.getContact());
                bioPlusBoilerplate.add("Price: " + horseData.getPrice());
                bioPlusBoilerplate.addAll(htmlSuffix);

                List<String> videoLinks = horseData.getVideos().getYoutubeLinks();
                String title = horseData.getName().toUpperCase() + ", " + horseData.getYear() + ", " + horseData.getHeight() + " " + horseData.getColor() + " " + horseData.getSex();
                CreateListing.createListingPage(horseData.getName(), title, thumnail,
                        imageFiles, horseData.getEquibase(), horseData.getPedigree(), videoLinks,
                        bioPlusBoilerplate);

                showSuccess();
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
        StringBuilder text = new StringBuilder(horseData.getName().toUpperCase() + ", " + horseData.getYear() + ", " + horseData.getHeight() + " " + horseData.getColor() + " " + horseData.getSex());
        text.append("\n");
        text.append("\n");
        
        fbPrefix.forEach(line -> text.append(line + "\n"));
        
        text.append(horseData.getBio());
        text.append("\n");
        text.append("Contact: " + horseData.getContact());
        text.append("\n");
        text.append("Price: " + horseData.getPrice());
        text.append("\n");
        
        text.append("Race Record: " + horseData.getEquibase());
        text.append("\n");
        text.append("Pedigree: " + horseData.getPedigree());
        text.append("\n");
        
        List<String> videoLinks = horseData.getVideos().getYoutubeLinks();
        for (String video : videoLinks) {
            text.append("Video: " + video + "\n");
        }
        
        fbSuffix.forEach(line -> text.append(line + "\n"));
        
        try {
            FbConnector.createPagePost(text.toString(), photosPanel.getImageFilenames());
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

        EquibaseConnector.HorseInfo horseInfo = EquibaseConnector.loadHorsePage(sanitized.toString());
        String url = horseInfo.equibaseUrl();
        horseData.setEquibase(url);
        horseData.setColor(expandColor(horseInfo.shortColor()));
        horseData.setSex(expandSex(horseInfo.shortSex()));
        horseData.setYear(horseInfo.year());
        horseData.setPedigree(horseInfo.pedigreeUrl());
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
        if (color.equalsIgnoreCase("DK B/BR") 
                || color.equalsIgnoreCase("DK B") 
                || color.equalsIgnoreCase("br")) {
            return "dark bay";
        }
        if (color.equalsIgnoreCase("b") 
                || color.equalsIgnoreCase("b/br")) {
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
