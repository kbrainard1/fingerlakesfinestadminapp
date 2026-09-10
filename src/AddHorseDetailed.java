import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class AddHorseDetailed extends AddHorseShared {
    private DataFieldComponent name;
    private DataFieldComponent year;
    private DataFieldComponent color;
    private DataFieldComponent sex;
    private DataFieldComponent height;
    private JTextArea bio;
    private DataFieldComponent price;
    private DataFieldComponent contact;
    private DataFieldComponent equibaseLink;
    private DataFieldComponent pedigreeLink;
    private VideoPanel videos;

    public AddHorseDetailed() {
        setLayout(new BorderLayout());
        EquibaseConnector.precacheConnection();

        JLabel firstPageLabel = new JLabel("First, enter the horse stats");
        firstPageLabel.setFont(CreateListingFrontend.DEFAULT_FONT);
        JPanel header = CreateListingFrontend.wrapButton(firstPageLabel);

        // enter stats
        JButton autofill = new CustomButton("Auto-fill based on name");
        autofill.addActionListener(e -> {
            if (!name.getData().isBlank()) {
                CreateListingFrontend.showSpinner();
                CreateListingFrontend.threadPool.submit(() -> {
                    try {
                        attemptToFill(name.getData());
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
            if (validateForm()) {
                loadNextScreen();
            }
        });
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new GridLayout(0, 1));
        name = new DataFieldComponent("Horse Name");
        center.add(name);
        year = new DataFieldComponent("Year");
        center.add(year);
        color = new DataFieldComponent("Color");
        center.add(color);
        sex = new DataFieldComponent("Sex");
        center.add(sex);
        height = new DataFieldComponent("Height");
        center.add(height);
        price = new DataFieldComponent("Price");
        center.add(price);
        contact = new DataFieldComponent("Contact Info");
        center.add(contact);
        equibaseLink = new DataFieldComponent("Equibase link");
        center.add(equibaseLink);
        pedigreeLink = new DataFieldComponent("Pedigree link");
        center.add(pedigreeLink);
        add(center, BorderLayout.CENTER);

        // then enter bio, photos, and videos
    }


    private void loadNextScreen() {
        remove(((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.CENTER));
        remove(((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.NORTH));

        JButton next = new CustomButton("Create Horse Webpage");
        JPanel header = CreateListingFrontend.wrapButton(next);
        
        header.add(next);
        next.addActionListener(e -> {

            CreateListingFrontend.showSpinner();

            CreateListingFrontend.threadPool.submit(() -> {
                try {
                    List<String> imageFiles = prepImageFiles();

                    List<String> bioPlusBoilerplate = Arrays.asList(bio.getText().split("\n"));
                    bioPlusBoilerplate.add(contact.getData());
                    bioPlusBoilerplate.add(price.getData());
                    bioPlusBoilerplate.add("A PPE is always recommended. For information about vet practices available to do PPEs, and other "
                            + "important information about the buying process, please see the <a href=\"../howtobuy.html\">How to Buy</a> page.");

                    List<String> videoLinks = videos.getYoutubeLinks();
                    String title = name.getData().toUpperCase() + ", " + year.getData() + ", " + height.getData() + " " + color.getData() + " " + sex.getData();
                    CreateListing.createListingPage(name.getData(), title, "profile.jpg",
                            imageFiles, equibaseLink.getData(), pedigreeLink.getData(), videoLinks,
                            bioPlusBoilerplate);
                    createFbPost();
                } catch (Exception e1) {
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
        bio = new JTextArea(10, 70);
        bio.setBackground(CreateListingFrontend.ADMIN_BACKGROUND);
        JScrollPane scrollBio = new JScrollPane(bio, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollBio.setPreferredSize(new Dimension(CreateListingFrontend.OVERALL_WIDTH - 100, 200));
        bioPanel.add(scrollBio);
        center.add(bioPanel);
        center.add(photosPanel);
        
        addVideoPanel(center);
        add(center, BorderLayout.CENTER);
        
        revalidate();
        repaint();
    }

    private void createFbPost() {
        // TODO Auto-generated method stub
        
    }


    private void addVideoPanel(JPanel center) {
        videos = new VideoPanel(name.getData());
        center.add(videos);
    }


    private boolean validateForm() {
        return validate(name) &&
                validate(year) &&
                validate(sex) &&
                validate(color) &&
                validate(height) &&
                validate(contact) &&
                validate(price) &&
                validate(equibaseLink) &&
                validate(pedigreeLink);
    }


    private boolean validate(DataFieldComponent comp) {
        if (comp.getData().isBlank()) {
            comp.setError("Required field");
            return false;
        }
        return true;
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
        equibaseLink.setText(url);
        color.setText(expandColor(horseDeets[1]));
        sex.setText(expandSex(horseDeets[2]));
        year.setText(horseDeets[horseDeets.length - 1]);
        pedigreeLink.setText(horsePage.select("a[href*=equineline.com/Free]").get(0).attr("href"));

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
