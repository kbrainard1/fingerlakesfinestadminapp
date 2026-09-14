import javax.swing.ButtonGroup;

public class EditHorseListingComponent extends HorseListingBase {

    public EditHorseListingComponent(String detailPage, String title, String thumbnailFile) {
        super(detailPage, title, thumbnailFile);
    }
    
    public void addToButtonGroup(ButtonGroup group) {
        group.add(checked);
    }

}
