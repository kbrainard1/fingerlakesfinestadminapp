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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;

import adminapp.CreateListingFrontend;
import wysiwyg.JSoupStateEdit.StateSnapshot;


// Reasoning: JEditorPane + HtmlKit is buggy (trying to add another bullet point to a list? GLHF). We don't actually need
// a totally crazy rich text feature set: bold, italic, underline, font size, links, and bullet points
// should be plenty
// 
// So the question is, is it really that hard to keep a rendered view in sync with the html DOM?
//
// Sadly, the answer appears to be 'yes', but this version is pretty complete. A good chunk of the challenge has been fighting
// with the JEditorPane, but the rendering and cursor management works pretty well and would be a PITA to reimplement.
public class ExperimentalJSoupEditorPane extends JPanel {

    private final JEditorPane editorPane;

    private HtmlNavigator2 backingState;
    
    private AtomicBoolean selfTriggeredCaretChange = new AtomicBoolean(false);
    private AtomicBoolean selfTriggeredComboBox = new AtomicBoolean(false);
    
    private UndoManager undoManager;
    private boolean inUndoAlready = false;
    
    private void executeWithUndo(Runnable mutationCommand) {
        if (!inUndoAlready) {
            StateSnapshot before = new StateSnapshot(backingState.getHtml(), editorPane.getCaretPosition());
            inUndoAlready = true;
            mutationCommand.run();
            inUndoAlready = false;
            StateSnapshot after = new StateSnapshot(backingState.getHtml(), editorPane.getCaretPosition());

            JSoupStateEdit edit = new JSoupStateEdit(this, before, after);
            undoManager.addEdit(edit);
        } else {
            mutationCommand.run();
        }
    }
    
    public void applyState(StateSnapshot snapshot) {        
        backingState.setHtml(snapshot.html, snapshot.caretOffset);
        refreshViewFromJsoup(snapshot.caretOffset);
    }
    
    private void setupUndoRedo() {
        undoManager = new UndoManager();

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

    public ExperimentalJSoupEditorPane(String initialHtml, Dimension preferredSize){
        setLayout(new BorderLayout());
        
        this.backingState = new HtmlNavigator2(initialHtml);

        editorPane = new JEditorPane();

        HTMLEditorKit kit = new CustomEditorKit();
        try {
            kit.getStyleSheet().loadRules(new FileReader("repo/style.css"), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        kit.getStyleSheet().addRule("a { color: blue; }");
        editorPane.setEditorKit(kit);
        editorPane.setContentType("text/html");
        editorPane.setEditable(false); // Disables Swing's internal typing/doc mutation engine
        
        refreshViewFromJsoup(0);
        setupUndoRedo();
        
        editorPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
                boolean isShortcut = (e.getModifiersEx() & shortcutMask) != 0;

                if (isShortcut) {
                    if (e.getKeyCode() == KeyEvent.VK_B) {
                        e.consume();
                        toggleFormat("b");
                        return;
                    } else if (e.getKeyCode() == KeyEvent.VK_I) {
                        e.consume();
                        toggleFormat("i");
                        return;
                    } else if (e.getKeyCode() == KeyEvent.VK_U) {
                        e.consume();
                        toggleFormat("u");
                        return;
                    } else if (e.getKeyCode() == KeyEvent.VK_C) {
                        e.consume();
                        handleCopy();
                        return;
                    } else if (e.getKeyCode() == KeyEvent.VK_V) {
                        e.consume();
                        handlePaste();
                        return;
                    } else if (e.getKeyCode() == KeyEvent.VK_X) {
                        e.consume();
                        handleCut();
                        return;
                    }
                }

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ENTER -> {
                        e.consume();
                        handleEnterKey();
                    }
                    case KeyEvent.VK_BACK_SPACE -> {
                        e.consume();
                        handleBackspace();
                    }
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                char ch = e.getKeyChar();
                int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

                if ((e.getModifiersEx() & shortcutMask) == 0
                        && ch != KeyEvent.VK_BACK_SPACE
                        && ch != KeyEvent.VK_ENTER
                        && ch != KeyEvent.VK_ESCAPE
                        && ch != KeyEvent.CHAR_UNDEFINED) {

                    e.consume();
                    handleCharacterTyped(ch);
                }
            }
        });
        
        
        // Edit Actions
        TextEditorButton boldBtn = new TextEditorButton("B");
        boldBtn.setFont(boldBtn.getFont().deriveFont(Font.BOLD));

        TextEditorButton italicBtn = new TextEditorButton("I");
        italicBtn.setFont(italicBtn.getFont().deriveFont(Font.ITALIC));
        
        TextEditorButton underlineBtn = new TextEditorButton("U");
        Map<TextAttribute, Integer> attributes = new HashMap<>();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        underlineBtn.setFont(underlineBtn.getFont().deriveFont(attributes));

        String[] headingOptions = {"Largest", "Large", "Medium", 
                                   "Small"};
        JComboBox<String> headingCombo = new JComboBox<>(headingOptions);
        headingCombo.setSelectedIndex(2);
        headingCombo.setFont(CreateListingFrontend.DEFAULT_FONT);
        headingCombo.addActionListener(e -> {
            if (!selfTriggeredComboBox.get()) {
                applyHeadingStyle(headingCombo.getSelectedIndex());
            }
        });
        headingCombo.setToolTipText("Highlight text to enable");
        
        Map<String, TextEditorButton> styleButtons = new HashMap<>();
        for (TextEditorButton button : new TextEditorButton[]{boldBtn, italicBtn, underlineBtn}) {
            styleButtons.put(button.getText().toLowerCase(), button);
            button.addActionListener(e -> {
                String tag = button.getText().toLowerCase();
                toggleFormat(tag); 
            });
        }
        editorPane.addCaretListener(e -> {
            if (!selfTriggeredCaretChange.get()) {
                int caretPos = editorPane.getCaretPosition();
                backingState.navigateToRenderedPosition(caretPos);
            }
            styleButtons.forEach((tag, button) -> {
                button.toggle(backingState.inGivenTag(tag));
            });
            int comboIndex = 2;
            if (backingState.inGivenTag("span")) {
                String spanClazz = backingState.getSpanClass();
                comboIndex = Integer.parseInt(spanClazz.substring(spanClazz.length() - 1)) - 1;
            }
            selfTriggeredComboBox.set(true);
            headingCombo.setSelectedIndex(comboIndex);
            selfTriggeredComboBox.set(false);
        });

        JButton insertLinkBtn = new EditorButton("Insert/Edit Link");
        insertLinkBtn.addActionListener(e -> insertLink());

        JButton bulletListBtn = new EditorButton("• Bullet Point");
        bulletListBtn.addActionListener(e -> {
            insertBulletList();
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
    
    private void applyHeadingStyle(int index) {
        executeWithUndo(() -> {
            String style = "edit_font"+(index + 1);
        
            String textToAddBack = "";
            if (editorPane.getSelectedText() != null) {
                backingState.navigateToRenderedPosition(editorPane.getSelectionStart() + 1);
                textToAddBack = backingState.cutText(editorPane.getSelectionStart(), editorPane.getSelectionEnd());
                textToAddBack = removeExtraTags(textToAddBack, "span");
            } 
           
            backingState.enterNew("span class=\"" + style + "\"", true);
            
            if (!textToAddBack.isEmpty()) {
                backingState.paste(textToAddBack);
            }
            refreshViewFromJsoup(editorPane.getCaretPosition());
        });
    }
    
    private void insertLink() {
        executeWithUndo(() -> {
            boolean alreadyInLink = backingState.inGivenTag("a");

            wysiwyg.HtmlNavigator2.LinkData linkData = new wysiwyg.HtmlNavigator2.LinkData(editorPane.getSelectedText(), "https://");
            if (alreadyInLink) {
                linkData = backingState.getLinkData();
            }

            JTextField textInput = new JTextField(linkData.text() != null ? linkData.text() : "");
            JTextField urlInput = new JTextField(linkData.url());

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(new JLabel("Link Text:"));
            panel.add(textInput);
            panel.add(new JLabel("URL:"));
            panel.add(urlInput);

            int result = JOptionPane.showConfirmDialog(this, panel, "Create/Update Link", 
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String linkText = textInput.getText().trim();
                String url = urlInput.getText().trim();

                if (url.isEmpty() && linkText.isEmpty()) {
                    return;
                }

                if (!alreadyInLink && editorPane.getSelectedText() != null) {
                    // Remove selected text (if any)
                    backingState.navigateToRenderedPosition(editorPane.getSelectionEnd());
                    int numDeletes = editorPane.getSelectionEnd() - editorPane.getSelectionStart();
                    for (int i = 0; i < numDeletes; i++) {
                        if (!handleSingleBackspace()) {
                            break;
                        }
                    }
                }

                backingState.addLink(linkText, url);
                refreshViewFromJsoup(backingState.findRenderedPos());
            }
        });
    }

    private void insertBulletList() {
        executeWithUndo(() -> {
            String textToAddBack = "";
            int caretPos = editorPane.getCaretPosition();
            if (editorPane.getSelectedText() != null) {
                backingState.navigateToRenderedPosition(editorPane.getSelectionStart() + 1);
                textToAddBack = backingState.cutText(editorPane.getSelectionStart(), editorPane.getSelectionEnd());
                caretPos = editorPane.getSelectionEnd();
            } 

            if (backingState.inGivenTag("p")) {
                backingState.exitTag("p");
            }
            if (backingState.enterNew("ul", false)) {
                backingState.enterNew("li", false);
                caretPos++;
            }

            if (!textToAddBack.isEmpty()) {
                backingState.paste(textToAddBack);
            }
            refreshViewFromJsoup(caretPos);
        });
    }

    private void refreshViewFromJsoup(int newCaret) {
        selfTriggeredCaretChange.set(true);
        editorPane.setText(backingState.getHtml());
        editorPane.setCaretPosition(Math.min(newCaret, editorPane.getDocument().getLength()));
        selfTriggeredCaretChange.set(false);
    }

    private void handleCharacterTyped(char ch) {
        if (editorPane.getSelectedText() != null) {
            handleBackspace(); // remove selection
        } 
        executeWithUndo(() -> {  
            backingState.addCharacter(ch);
            refreshViewFromJsoup(editorPane.getCaretPosition() + 1);
        });
    }

    private void handleBackspace() {
        StateSnapshot before = new StateSnapshot(backingState.getHtml(), editorPane.getCaretPosition());
        boolean dataChanged = false;
        int numDeleted = 0;
        int caretPos = editorPane.getCaretPosition();
        if (editorPane.getSelectedText() != null) {
            caretPos = editorPane.getSelectionEnd();
            backingState.navigateToRenderedPosition(editorPane.getSelectionEnd());
            int numDeletes = editorPane.getSelectionEnd() - editorPane.getSelectionStart();
            for (int i = 0; i < numDeletes; i++) {
                if (handleSingleBackspace()) {
                    dataChanged = true;
                    numDeleted++;
                } else {
                    break;
                }
            }
        } else {
            dataChanged = handleSingleBackspace();
            if (dataChanged) {
                numDeleted = 1;
            }
        }
        if (dataChanged) {
            refreshViewFromJsoup(caretPos - numDeleted);
            StateSnapshot after = new StateSnapshot(backingState.getHtml(), editorPane.getCaretPosition());

            JSoupStateEdit edit = new JSoupStateEdit(this, before, after);
            undoManager.addEdit(edit);
        }
    }

    private boolean handleSingleBackspace() {
        return backingState.removeChar();
    }

    private void handleEnterKey() {
        if (editorPane.getSelectedText() != null) {
            handleBackspace(); // remove selection
        } 
        executeWithUndo(() -> {
            boolean wasLi = backingState.inGivenTag("li");
            backingState.split();
            boolean stillLi = backingState.inGivenTag("li");
            refreshViewFromJsoup(editorPane.getCaretPosition() + (wasLi && !stillLi ? 0 : 1));
        });
    }
    
    private void toggleFormat(String tag) {
        executeWithUndo(() -> {
            String textToAddBack = "";
            boolean enable;
            if (editorPane.getSelectedText() != null) {
                backingState.navigateToRenderedPosition(editorPane.getSelectionStart() + 1);
                enable = !backingState.inGivenTag(tag);
                textToAddBack = backingState.cutText(editorPane.getSelectionStart(), editorPane.getSelectionEnd());
                textToAddBack = removeExtraTags(textToAddBack, tag);
            } else {
                enable = !backingState.inGivenTag(tag);
            }

            if (enable) {
                backingState.enterNew(tag, false);
            } else {
                // ok, if we're nesting stuff inside <b> etc, we get what we get. Unsupported.
                backingState.exitTag(tag);
            }

            if (!textToAddBack.isEmpty()) {
                backingState.paste(textToAddBack);
            }
            refreshViewFromJsoup(editorPane.getCaretPosition());
        });
    }

    private String removeExtraTags(String html, String tag) {
        html = html.replaceAll("</" + tag + ">", "");
        html = html.replaceAll("<" + tag + "[^<>]*>", "");
        return html;
    }

    private void handleCut() {
        if (editorPane.getSelectedText() == null) {
            return;
        }
        executeWithUndo(() -> {
            String htmlSnippet = backingState.cutText(editorPane.getSelectionStart(), editorPane.getSelectionEnd());
            HtmlSelection selection = new HtmlSelection(htmlSnippet, editorPane.getSelectedText());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);
            refreshViewFromJsoup(editorPane.getSelectionStart());
        });  
    }
    
    private static class HtmlSelection implements Transferable {

        private static DataFlavor plainTextFlavor = createFlavor("plain");
        private static DataFlavor htmlTextFlavor =  createFlavor("html");
        private static DataFlavor createFlavor(String textType) {
            try {
                return new DataFlavor("text/" + textType + "; class=java.lang.String");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        private final String html;
        private final String plainText;

        public HtmlSelection(String html, String text) {
           this.html = html;
           this.plainText = text;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] {plainTextFlavor, htmlTextFlavor};
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return plainTextFlavor.equals(flavor) || htmlTextFlavor.equals(flavor);
        }

        public String getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException
        {
            if (plainTextFlavor.equals(flavor)) {
                return plainText;
            } else if (htmlTextFlavor.equals(flavor)) {
                return html;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }
    }
    
    private void handlePaste() {
        if (editorPane.getSelectedText() != null) {
            handleBackspace();
        }
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents.isDataFlavorSupported(HtmlSelection.htmlTextFlavor)) {
            executeWithUndo(() -> {
                String toPaste;
                try {
                    toPaste = (String) contents.getTransferData(HtmlSelection.htmlTextFlavor);
                    backingState.paste(toPaste);
                    refreshViewFromJsoup(editorPane.getCaretPosition() + ((String)contents.getTransferData(HtmlSelection.plainTextFlavor)).length());
                } catch (UnsupportedFlavorException | IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            // see if we can get plain text going here
            for (DataFlavor flavor : contents.getTransferDataFlavors()) {
                if (flavor.getMimeType().startsWith("text/")) {
                    executeWithUndo(() -> {
                        Reader reader;
                        try {
                            reader = flavor.getReaderForText(contents);
                        } catch (UnsupportedFlavorException | IOException e) {
                            throw new RuntimeException(e);
                        }
                        int caretAdjust = 0;
                        List<String> lines = IOUtils.readLines(reader);
                        for (String line : lines) {
                            for (char ch : line.toCharArray()) {
                                backingState.addCharacter(ch);
                                caretAdjust++;
                            }
                            boolean wasLi = backingState.inGivenTag("li");
                            backingState.split();
                            boolean stillLi = backingState.inGivenTag("li");
                            if (!wasLi || stillLi) {
                                caretAdjust++;
                            }
                        }
                        refreshViewFromJsoup(editorPane.getCaretPosition() + caretAdjust);
                    });
                    break;
                }
            }
        }
    }
    
    private void handleCopy() {
        if (editorPane.getSelectedText() == null) {
           return;
        }
        executeWithUndo(() -> {
            String htmlSnippet = backingState.copyText(editorPane.getSelectionStart(), editorPane.getSelectionEnd());
            HtmlSelection selection = new HtmlSelection(htmlSnippet, editorPane.getSelectedText());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);
            refreshViewFromJsoup(editorPane.getSelectionEnd());
        });
    }

    public String getHtml() {
        return backingState.getCleanHtml();
    }

    public String getText() {
        return Jsoup.parse(getHtml()).text();
    }

//    public String getText() {
//        return backingState.getCleanHtml();
//    }
}