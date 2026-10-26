package wysiwyg;

import java.awt.event.ActionEvent;

import javax.swing.JEditorPane;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.StyledEditorKit.StyledTextAction;

public class TextActions {
    
    public abstract static class StyleAction extends StyledTextAction {
        
        public StyleAction(String text) {
            super(text);
        }
        
        public abstract void setAttr(MutableAttributeSet set);

        public void actionPerformed(ActionEvent e) {
            JEditorPane editor = getEditor(e);
            if (editor != null) {
                StyledEditorKit kit = getStyledEditorKit(editor);
                MutableAttributeSet attr = kit.getInputAttributes();
                setAttr(attr);
                setCharacterAttributes(editor, attr, false);
            }
        }
    }

    public static class BoldAction extends StyleAction {
        
        public BoldAction() {
            super("font-bold");
        }

        public void setAttr(MutableAttributeSet attr) {
            StyleConstants.setBold(attr, !StyleConstants.isBold(attr));
        }
    }
    
    public static class ItalicAction extends StyleAction {

        public ItalicAction() {
            super("font-italic");
        }

        public void setAttr(MutableAttributeSet attr) {
            StyleConstants.setItalic(attr, !StyleConstants.isItalic(attr));
        }
    }
    
    public static class UnderlineAction extends StyleAction {

        public UnderlineAction() {
            super("font-underline");
        }

        public void setAttr(MutableAttributeSet attr) {
            StyleConstants.setUnderline(attr, !StyleConstants.isUnderline(attr));
        }
    }
}
