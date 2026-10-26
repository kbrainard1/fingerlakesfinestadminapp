package wysiwyg;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.TextAction;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ParagraphView;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.jsoup.Jsoup;

import com.google.common.collect.Lists;

import adminapp.CreateListingFrontend;

// TODO: Improve the B/I/U buttons
public class WysiwegPanel extends JComponent {

    private final JEditorPane editorPane;
    
    private UndoManager undoManager;
    private CustomUndoListener undoListener;
    
    private void setupUndoRedo(JEditorPane editorPane) {
        undoManager = new UndoManager();
        undoListener = new CustomUndoListener(undoManager);
        Document doc = editorPane.getDocument();

        doc.addUndoableEditListener(undoListener);

        Action undoAction = new AbstractAction("Undo") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canUndo()) {
                        undoManager.undo();
                    }
                } catch (CannotUndoException ex) {
                    // ignore
                }
            }
        };

        Action redoAction = new AbstractAction("Redo") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canRedo()) {
                        undoManager.redo();
                    }
                } catch (CannotRedoException ex) {
                    // ignore
                }
            }
        };

        // 4. Register Keyboard Shortcuts (Ctrl+Z / Cmd+Z, Ctrl+Y / Cmd+Shift+Z)
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        InputMap inputMap = editorPane.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = editorPane.getActionMap();

        // Undo: Ctrl+Z (or Cmd+Z on macOS)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask), "Undo");
        actionMap.put("Undo", undoAction);

        // Redo: Ctrl+Y (or Cmd+Y on macOS)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask), "Redo");
        
        // Standard alternative Redo: Ctrl+Shift+Z (or Cmd+Shift+Z on macOS)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask | KeyEvent.SHIFT_DOWN_MASK), "Redo");
        actionMap.put("Redo", redoAction);
    }

    public WysiwegPanel(String initialHtml, Dimension preferredSize) {
        editorPane = new JEditorPane();

        HTMLEditorKit htmlKit = getWordWrapKit();
        try {
            htmlKit.getStyleSheet().loadRules(new FileReader("repo/style.css"), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        setupKeyBindings();

        editorPane.setEditorKit(htmlKit);
        editorPane.setContentType("text/html");
        editorPane.setText(initialHtml);
        
        setupUndoRedo(editorPane);

        // Edit Actions
        JButton boldBtn = new EditorButton(new HTMLEditorKit.BoldAction(), "B");
        boldBtn.setFont(boldBtn.getFont().deriveFont(Font.BOLD));

        JButton italicBtn = new EditorButton(new HTMLEditorKit.ItalicAction(), "I");
        italicBtn.setFont(italicBtn.getFont().deriveFont(Font.ITALIC));

        JButton underlineBtn = new EditorButton(new HTMLEditorKit.UnderlineAction(), "U");
        Map<TextAttribute, Integer> attributes = new HashMap<>();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        underlineBtn.setFont(underlineBtn.getFont().deriveFont(attributes));

        String[] headingOptions = {"Largest", "Large", "Medium", 
                                   "Small", "Smallest"};
        JComboBox<String> headingCombo = new JComboBox<>(headingOptions);
        headingCombo.setSelectedIndex(2);
        headingCombo.setFont(CreateListingFrontend.DEFAULT_FONT);
        headingCombo.addActionListener(e -> applyHeadingStyle(headingCombo.getSelectedIndex()));
        headingCombo.setToolTipText("Highlight text to enable");
        
        List<JComponent> requireSelection = Lists.newArrayList(boldBtn, italicBtn, underlineBtn, headingCombo);
        requireSelection.forEach(comp -> {
            comp.setEnabled(false);
            comp.setToolTipText("Highlight text to enable");
        });
        editorPane.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                boolean hasSelection = e.getDot() != e.getMark();
                requireSelection.forEach(comp -> comp.setEnabled(hasSelection));            }
        });

        JButton insertLinkBtn = new EditorButton(null, "Insert/Edit Link");
        insertLinkBtn.addActionListener(e -> insertLink());

        JButton bulletListBtn = new EditorButton(null, "• Bullet Point");
        bulletListBtn.addActionListener(e -> {
            try {
                insertBulletList();
            } catch (BadLocationException e1) {
                throw new RuntimeException(e1);
            }
        });

        // Menu
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(boldBtn);
        toolBar.add(italicBtn);
        toolBar.add(underlineBtn);
        toolBar.addSeparator();
        JLabel selectionLabel = new JLabel(" Font Size: ");
        selectionLabel.setFont(CreateListingFrontend.DEFAULT_FONT);
        toolBar.add(selectionLabel);
        toolBar.add(headingCombo);
        toolBar.addSeparator();
        toolBar.add(insertLinkBtn);
        toolBar.add(bulletListBtn);

        setLayout(new BorderLayout());        
        JScrollPane scrollPane = new JScrollPane(editorPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(preferredSize);
        scrollPane.setColumnHeaderView(toolBar);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void insertBulletList() throws BadLocationException {
        HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
        int caretPos = editorPane.getCaretPosition();
        Element paragraph = doc.getParagraphElement(caretPos);

        // Get current line text
        int start = paragraph.getStartOffset();
        int len = paragraph.getEndOffset() - start;
        String lineText = doc.getText(start, len).trim();

        // 1. Check if caret is already in a <ul>
        Element existingUl = getEnclosingUl(doc, caretPos);

        // 2. Check if paragraph immediately below or above is a <ul>
        if (existingUl == null) {
            int nextOffset = Math.min(doc.getLength(), paragraph.getEndOffset() + 1);
            existingUl = getEnclosingUl(doc, nextOffset);
        }
        if (existingUl == null) {
            int prevOffset = Math.max(0, paragraph.getStartOffset() - 1);
            existingUl = getEnclosingUl(doc, prevOffset);
        }

        undoListener.startCompoundEdit();
        if (existingUl != null) {
            // MERGE: Insert <li> into the adjacent/existing <ul>
            doc.remove(start, len);
            editorPane.setCaretPosition(start);
            String liHtml = "<li>" + lineText + "</li>";
            HTMLEditorKit.InsertHTMLTextAction insertLi = 
                    new HTMLEditorKit.InsertHTMLTextAction("MergeLI", liHtml, HTML.Tag.UL, HTML.Tag.LI);
            insertLi.actionPerformed(new ActionEvent(editorPane, ActionEvent.ACTION_PERFORMED, null));
        } else {
            // CREATE NEW: Insert a new <ul><li>...</li></ul>
            doc.remove(start, len);
            editorPane.setCaretPosition(start);
            String ulHtml = "<ul><li>" + lineText + "</li></ul>";
            HTMLEditorKit.InsertHTMLTextAction insertUl = 
                    new HTMLEditorKit.InsertHTMLTextAction("NewUL", ulHtml, HTML.Tag.BODY, HTML.Tag.UL);
            insertUl.actionPerformed(new ActionEvent(editorPane, ActionEvent.ACTION_PERFORMED, null));
        }
        undoListener.endCompoundEdit();
    }
    
    private Element getEnclosingElement(Element elem, HTML.Tag targetTag) {
        while (elem != null) {
            Object tag = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
            if (tag == targetTag) {
                return elem;
            }
            elem = elem.getParentElement();
        }
        return null;
    }

    private void setupKeyBindings() {
        int shortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        InputMap inputMap = editorPane.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = editorPane.getActionMap();
        
        // Add bullet points
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "smart-enter");
        actionMap.put("smart-enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
                    int caretPos = editorPane.getCaretPosition();
                    Element ul = getEnclosingUl(doc, caretPos);

                    
                    if (ul != null) {
                        undoListener.startCompoundEdit();
                        // Caret is inside a UL
                        Element para = doc.getParagraphElement(caretPos);
                        String firstLiText = doc.getText(para.getStartOffset(), caretPos - para.getStartOffset()).trim();
                        String secondLiText = doc.getText(caretPos, para.getEndOffset() - caretPos).trim();

                        if (firstLiText.isEmpty() && secondLiText.isEmpty()) {
                            // Empty bullet + Enter = Exit list (insert regular paragraph outside UL)
                            try {
                                doc.removeElement(ul.getElement(ul.getElementCount() - 1));
                                doc.insertAfterEnd(ul, "<p></p>");
                                editorPane.setCaretPosition(caretPos + 1);
                            } catch (BadLocationException | IOException e1) {
                                throw new RuntimeException(e1);
                            }
                        } else {
                            Element elem = doc.getCharacterElement(caretPos);
                            while (elem != null) {
                                Object tag = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
                                if (tag == HTML.Tag.LI) {
                                    doc.replace(caretPos, elem.getEndOffset() - caretPos, "", null);
                                    doc.insertAfterEnd(elem, "<li>" + secondLiText + "</li>");
                                    break;
                                }
                                elem = elem.getParentElement();
                                editorPane.setCaretPosition(para.getStartOffset() + firstLiText.length() - 1);
                            }
                        }
                        undoListener.endCompoundEdit();
                    } else {
                        // Regular Enter behavior outside lists
                        editorPane.replaceSelection("\n");
                    }
                } catch (Exception e1) {
                    throw new RuntimeException(e1);
                }
            }
        });
        
        Object originalKey = inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
        Action defaultBackspaceAction = actionMap.get(originalKey != null ? originalKey : "delete-previous");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "smart-backspace");
        actionMap.put("smart-backspace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
                int caretPos = editorPane.getCaretPosition();

                // 1. Find current element and check if inside an <li> tag
                Element paragraph = doc.getParagraphElement(caretPos);
                Element liElement = getEnclosingElement(paragraph, HTML.Tag.LI);

                if (liElement != null) {
                    int startOffset = liElement.getStartOffset();
                    int endOffset = liElement.getEndOffset();
                    int length = Math.max(0, endOffset - startOffset);

                    String text = "";
                    try {
                        text = doc.getText(startOffset, length).trim();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    // 2. If caret is at the start of the list item AND the <li> is empty
                    if (text.isEmpty() && (caretPos == startOffset || caretPos == startOffset + 1)) {
                        try {
                            // Remove the empty <li> element
                            doc.remove(startOffset, length);
                            editorPane.setCaretPosition(startOffset);

                            // Insert a standard paragraph tag (<p>) outside the <ul> list
                            HTMLEditorKit.InsertHTMLTextAction insertParagraph = 
                                    new HTMLEditorKit.InsertHTMLTextAction("ExitList", "<p></p>", HTML.Tag.BODY, HTML.Tag.P);
                            insertParagraph.actionPerformed(e);
                            return; // Successfully handled
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }

                // Fallback to default Swing backspace behavior for normal text
                if (defaultBackspaceAction != null) {
                    defaultBackspaceAction.actionPerformed(e);
                }
            }
        });

        // 1. Ctrl+B -> Bold Action
        KeyStroke ctrlB = KeyStroke.getKeyStroke(KeyEvent.VK_B, shortcutKeyMask);
        inputMap.put(ctrlB, "html-bold");
        actionMap.put("html-bold", new HTMLEditorKit.BoldAction());

        // 2. Ctrl+I -> Italic Action
        KeyStroke ctrlI = KeyStroke.getKeyStroke(KeyEvent.VK_I, shortcutKeyMask);
        inputMap.put(ctrlI, "html-italic");
        actionMap.put("html-italic", new HTMLEditorKit.ItalicAction());

        // 3. Ctrl+U -> Underline Action
        KeyStroke ctrlU = KeyStroke.getKeyStroke(KeyEvent.VK_U, shortcutKeyMask);
        inputMap.put(ctrlU, "html-underline");
        actionMap.put("html-underline", new HTMLEditorKit.UnderlineAction());
        
        KeyStroke ctrlV = KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutKeyMask);
        inputMap.put(ctrlV, "html-paste");
        actionMap.put("html-paste", new TextAction("paste-from-clipboard") {

            @Override
            public void actionPerformed(ActionEvent e) {
                Clipboard systemClipboard = editorPane.getToolkit().getSystemClipboard();
                
                Transferable contents = systemClipboard.getContents(null);
                Transferable newContents = new Transferable() {

                    @Override
                    public DataFlavor[] getTransferDataFlavors() {
                       List<DataFlavor> flavors = Arrays.asList(contents.getTransferDataFlavors()).stream()
                               .filter(flavor -> !flavor.getMimeType().contains("html"))
                               .toList();
                       return flavors.toArray(new DataFlavor[] {});
                    }

                    @Override
                    public boolean isDataFlavorSupported(DataFlavor flavor) {
                        return contents.isDataFlavorSupported(flavor);
                    }

                    @Override
                    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                        return contents.getTransferData(flavor);
                    }
                };
                systemClipboard.setContents(newContents, null);
                editorPane.paste();
            }
            
        });
    }

    public static HTMLEditorKit getWordWrapKit() {
        return new HTMLEditorKit() {
            @Override
            public ViewFactory getViewFactory() {
                return new HTMLFactory() {
                    @Override
                    public View create(Element elem) {
                        View view = super.create(elem);
                        
                        // Override ParagraphView to prevent horizontal width expansion
                        if (view instanceof ParagraphView) {
                            return new ParagraphView(elem) {
                                @Override
                                public float getMinimumSpan(int axis) {
                                    return (axis == View.X_AXIS) ? 10 : super.getMinimumSpan(axis);
                                }

                                @Override
                                public float getPreferredSpan(int axis) {
                                    return (axis == View.X_AXIS) ? 10 : super.getPreferredSpan(axis);
                                }
                                
                                
                            };
                        }
                        
                        return view;
                    }
                };
            }
        };
    }

    private void applyHeadingStyle(int index) {
        String style = "edit_font"+(index + 1);

        String selectedText = editorPane.getSelectedText();
        undoListener.startCompoundEdit();
        if (selectedText == null || selectedText.isBlank()) {
            selectedText = " ";
        } else {
            editorPane.replaceSelection("");
        }
        String htmlSnippet = "<span class=\"" + style + "\">" + selectedText + "</span>";

        HTMLEditorKit.InsertHTMLTextAction action = 
                new HTMLEditorKit.InsertHTMLTextAction("Heading", htmlSnippet, HTML.Tag.P, HTML.Tag.SPAN);
        action.actionPerformed(new ActionEvent(editorPane, ActionEvent.ACTION_PERFORMED, null));
        undoListener.endCompoundEdit();
    }

    private void insertLink() {
        HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
        int caretPos = editorPane.getCaretPosition();

        Element maybeLink = doc.getCharacterElement(caretPos);
        if (maybeLink == null) return;
        
        boolean alreadyInLink = false;
        if (maybeLink.getAttributes().getAttribute(HTML.Tag.A) != null) {
            alreadyInLink = true;
        }

        String initialText =  editorPane.getSelectedText();
        String initialLink = "https://";
        if (alreadyInLink) {
            int startOffset = maybeLink.getStartOffset();
            int endOffset = maybeLink.getEndOffset();
            int length = Math.max(0, endOffset - startOffset);
            try {
                initialText = doc.getText(startOffset, length).trim();
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
            initialLink = (String) ((SimpleAttributeSet) maybeLink.getAttributes().getAttribute(HTML.Tag.A)).getAttribute(HTML.Attribute.HREF);
        }
        
        JTextField textInput = new JTextField(initialText != null ? initialText : "");
        JTextField urlInput = new JTextField(initialLink);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Link Text:"));
        panel.add(textInput);
        panel.add(new JLabel("URL:"));
        panel.add(urlInput);

        int result = JOptionPane.showConfirmDialog(this, panel, "Create Link", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String linkText = textInput.getText().trim();
            String url = urlInput.getText().trim();

            if (url.isEmpty() && linkText.isEmpty()) {
                return;
            }

            undoListener.startCompoundEdit();

            if (!alreadyInLink) {
                // Remove selected text (if any)
                editorPane.replaceSelection("");
            } else {
                // remove previous link
                doc.removeElement(maybeLink);
            }

            String linkHtml = "<a href=\"" + url + "\">" + linkText + "</a>"; 
            HTMLEditorKit.InsertHTMLTextAction insertUl = 
                    new HTMLEditorKit.InsertHTMLTextAction("InsertUL", linkHtml, HTML.Tag.P, HTML.Tag.A);
            insertUl.actionPerformed(new ActionEvent(editorPane, ActionEvent.ACTION_PERFORMED, null));

            undoListener.endCompoundEdit();
        }
    }
    
    public static boolean isCaretInOrNextToUl(JEditorPane editorPane) {
        HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
        int caretPos = editorPane.getCaretPosition();

        // Check if caret is inside a UL
        if (getEnclosingUl(doc, caretPos) != null) {
            return true;
        }

        // Check if paragraph immediately BEFORE caret is a UL
        Element currentParagraph = doc.getParagraphElement(caretPos);
        int prevOffset = Math.max(0, currentParagraph.getStartOffset() - 1);
        if (prevOffset < currentParagraph.getStartOffset() && getEnclosingUl(doc, prevOffset) != null) {
            return true;
        }

        // Check if paragraph immediately AFTER caret is a UL
        int nextOffset = Math.min(doc.getLength(), currentParagraph.getEndOffset() + 1);
        if (nextOffset > currentParagraph.getEndOffset() && getEnclosingUl(doc, nextOffset) != null) {
            return true;
        }

        return false;
    }
    
    public static Element getEnclosingUl(HTMLDocument doc, int pos) {
        Element elem = doc.getParagraphElement(pos);
        while (elem != null) {
            Object tag = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
            if (tag == HTML.Tag.UL) {
                return elem;
            }
            elem = elem.getParentElement();
        }
        return null;
    }

    public String getHtml() {
        org.jsoup.nodes.Document document = Jsoup.parse(editorPane.getText());
        return document.getElementsByTag("body").get(0).html();
    }

    public String getText() {
        Document doc = editorPane.getDocument();
        try {
            return doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }
}