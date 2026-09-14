import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JComponent;
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

    public JPanel createStatsComponent() {
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
        return center;
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
