package adminapp;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.jsoup.nodes.Document;

import com.google.common.collect.Lists;

import adminapp.GithubConnector.CommitType;
import wysiwyg.WysiwygPanel;

public class EditSiteComponent extends JPanel {
    
    private static enum WebPage {
        INDEX("index.html", "Home Page"),
        HOWTOBUY("howtobuy.html", "How to Buy"),
        RESOURCES("resources.html", "Resources"),
        DONATE("donate.html", "Donate"),
        CONTACT("contact.html", "Contact Us");
        
        private String page;
        private String name;
        
        private WebPage(String page, String name) {
            this.page = page;
            this.name = name;
        }
    }
    
    private WebPage currentPage = null;
    private JPanel wysiwyg;
    private List<JButton> pageButtons;
    private WysiwygPanel htmlContents;
    private StatusLabel status;

    public EditSiteComponent() {
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        // panel with the pages
        // panel with save, preview, publish
        // wysiwyg panel
        
        JPanel topPanel = CreateListingFrontend.wrapButton(new JPanel());
        for (WebPage page : WebPage.values()) {
            JButton button = new CustomButton(page.name);
            button.addActionListener(e -> loadEditor(page));
            topPanel.add(button);
        }
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
        add(topPanel);
        
        JPanel siteControls = CreateListingFrontend.wrapButton(new JPanel());
        status = new StatusLabel();
        JButton save = new CustomButton("Save");
        save.addActionListener(e -> saveEditor());
        JButton preview = new CustomButton("Preview");
        preview.addActionListener(e -> previewPage());
        JButton publish = new CustomButton("Publish Site");
        publish.addActionListener(e -> {
            CreateListingFrontend.runWithSpinner(status, () -> {
                GithubConnector.mergeStaging();
                status.setSuccess("Site published!");
            });
        });
        pageButtons = Lists.newArrayList(save, preview, publish);
        pageButtons.forEach(button -> button.setEnabled(false));
        siteControls.add(save);
        siteControls.add(preview);
        siteControls.add(publish);
        siteControls.add(status);
        siteControls.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
        add(siteControls);
        
        wysiwyg = CreateListingFrontend.wrapButton(new JPanel());
        wysiwyg.setPreferredSize(new Dimension(CreateListingFrontend.OVERALL_WIDTH, 700));
        add(wysiwyg);
    }

    private void previewPage() {
        status.reset();
        
        try {
            // otherwise the embedded youtube videos don't have a valid referrer
            Desktop.getDesktop().browse(new URI("http://127.0.0.1:8080/" + currentPage.page));
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
    }

    private void saveEditor() {
        String htmlToAdd = htmlContents.getHtml();
        try {
            MarkPlaced.updatePage(currentPage.page, doc -> {
                if (currentPage == WebPage.INDEX) {
                    doc.getElementsByClass("home_content").get(0).html(htmlToAdd);
                } else {
                    doc.getElementsByTag("main").get(0).html(htmlToAdd);
                }
            });
            GithubConnector.commitAndPush(CommitType.OTHER);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadEditor(WebPage page) {
        currentPage = page;
        pageButtons.forEach(button -> button.setEnabled(true));
        String html = loadHtml(page);
       
        htmlContents = new WysiwygPanel(html, new Dimension(CreateListingFrontend.OVERALL_WIDTH, 700));
        wysiwyg.remove(0);
        wysiwyg.add(htmlContents);
        wysiwyg.revalidate();
        wysiwyg.repaint();
    }

    private String loadHtml(WebPage page) {
        try {
            Document htmlFile = GithubConnector.getHtmlFile(page.page);
            htmlFile.outputSettings(htmlFile.outputSettings().maxPaddingWidth(0));
            if (page == WebPage.INDEX) {
                return htmlFile.getElementsByClass("home_content").html();
            } else {
                return htmlFile.getElementsByTag("main").html();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
