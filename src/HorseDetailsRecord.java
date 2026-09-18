import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class HorseDetailsRecord {

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
    private Future<Boolean> autofillResult;
    private JLabel loading;
    
    public static interface AutofillCallback {
        public boolean autofill(String name);
    }

    public JPanel createStatsComponent(AutofillCallback autofillCallack) {
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        name = new DataFieldComponent("Horse Name");
        name.enable();
        
        JButton autofill = new CustomButton("Next");
        autofill.addActionListener(e -> {
            runAutofill(autofillCallack, autofill);
        });
        name.onEnter(() -> runAutofill(autofillCallack, autofill));
        JPanel autofillPanel = CreateListingFrontend.wrapButton(name, new FlowLayout(FlowLayout.LEFT));
        autofillPanel.add(autofill);
        autofillPanel.setBackground(null);
        center.add(autofillPanel);
        
        JPanel manualFields = new JPanel();
        manualFields.setLayout(new GridLayout(0, 1));
        height = new DataFieldComponent("Height");
        manualFields.add(height);
        price = new DataFieldComponent("Price");
        manualFields.add(price);
        contact = new DataFieldComponent("Contact Info");
        manualFields.add(contact);
        addSpacer(manualFields);
        addSpacer(manualFields);
        addSpacer(manualFields);
        center.add(manualFields);
        
        
        JPanel autofillableFields = new JPanel();
        autofillableFields.setLayout(new GridLayout(0, 1));
        year = new DataFieldComponent("Year");
        autofillableFields.add(year);
        color = new DataFieldComponent("Color");
        autofillableFields.add(color);
        sex = new DataFieldComponent("Sex");
        autofillableFields.add(sex);
        equibaseLink = new DataFieldComponent("Equibase link");
        autofillableFields.add(equibaseLink);
        pedigreeLink = new DataFieldComponent("Pedigree link");
        autofillableFields.add(pedigreeLink);
        addSpacer(autofillableFields);
        addSpacer(autofillableFields);
        addSpacer(autofillableFields);
        
        loading = new JLabel();
        loading.setFont(CreateListingFrontend.DEFAULT_FONT.deriveFont(24f));
        loading.setHorizontalAlignment(JLabel.CENTER);
        loading.setVerticalAlignment(JLabel.TOP);
        
        JPanel combined = new JPanel();
        combined.setLayout(new GridLayout(1, 0));
        combined.add(autofillableFields);
        combined.add(loading);
        
        center.add(combined);
       
        return center;
    }

    private void runAutofill(AutofillCallback autofillCallack, JButton autofill) {
        autofillResult = CreateListingFrontend.threadPool.submit(() -> {
            return autofillCallack.autofill(getName());
        });
        CreateListingFrontend.threadPool.submit(() -> {
            DataFieldComponent[] comps = new DataFieldComponent[] {
                    year,
                    color, 
                    sex,
                    equibaseLink,
                    pedigreeLink,
                    name
            };
            autofill.setEnabled(false);
            try {
                
                for (DataFieldComponent comp : comps) {
                    comp.disable();
                }
                loading.setForeground(Color.BLACK);
                while (true) {
                    // Update the spinner
                    long timeBox = (System.currentTimeMillis() / 1000) % 3;
                    String text = "Loading";
                    for (int i = 0; i <= timeBox; i++) {
                        text += ".";
                    }
                    loading.setText(text);
                    
                    // Check for results
                    try {
                        boolean success = autofillResult.get(1, TimeUnit.SECONDS);
                        if (!success) {
                            loading.setForeground(StatusLabel.ERROR_COLOR);
                            loading.setText("No match found, check horse name");
                        } else {
                            loading.setForeground(StatusLabel.SUCCESS_COLOR);
                            loading.setText("Auto-fill Complete");
                        }
                        break;
                    } catch (TimeoutException e1) {
                        // that's ok, update the spinner again
                    } catch (Exception e1) {
                        loading.setForeground(StatusLabel.ERROR_COLOR);
                        loading.setText("Auto-fill failed");
                        break;
                    }
                    

                }
            } finally {
                for (DataFieldComponent comp : comps) {
                    comp.enable();
                }
                autofill.setEnabled(true);
                autofill.setText("Autofill");
            }
        });
        height.enable();
        height.focus();
        price.enable();
        contact.enable();
    }

    private void addSpacer(JPanel center) {
        center.add(new JLabel(""));
    }

    public String getName() {
        return name.getData();
    }

    public String getContact() {
       return contact.getData();
    }
    
    public String getPrice() {
        return price.getData();
     }
    
    public boolean validateStats() {
        return validate(name) &&
                validate(height) &&
                validate(contact) &&
                validate(price) &&
                validate(year) &&
                validate(sex) &&
                validate(color) &&
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

    public String getYear() {
        return year.getData();
    }

    public String getHeight() {
        return height.getData();
    }

    public String getPedigree() {
        return pedigreeLink.getData();
    }

    public String getEquibase() {
        return equibaseLink.getData();
    }

    public String getColor() {
        return color.getData();
    }

    public String getSex() {
        return sex.getData();
    }

    public void setEquibase(String link) {
       equibaseLink.setText(link);
    }

    public void setColor(String color) {
       this.color.setText(color);
    }

    public void setYear(String year) {
        this.year.setText(year);
    }

    public void setPedigree(String link) {
        pedigreeLink.setText(link);
    }

    public void setSex(String sex) {
        this.sex.setText(sex);
    }

    public String getBio() {
        return bio.getText();
    }

    public VideoPanel getVideos() {
       return videos;
    }

    public JComponent createBioComponent() {
        bio = new JTextArea(10, 70);
        bio.setWrapStyleWord(true);
        bio.setFont(CreateListingFrontend.DEFAULT_FONT);
        bio.setLineWrap(true);
        bio.setBackground(CreateListingFrontend.ADMIN_BACKGROUND);
        JScrollPane scrollBio = new JScrollPane(bio, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollBio.setPreferredSize(new Dimension(CreateListingFrontend.OVERALL_WIDTH - 100, 200));
        return scrollBio;
    }

    public JComponent createVideoComponent() {
        videos = new VideoPanel(name.getData());
        return videos;
    }
    
}
