package wysiwyg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.stream.Collectors;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ParagraphView;
import javax.swing.text.html.parser.ParserDelegator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * Custom HTMLEditorKit that overrides View painting to force JEditorPane 
 * to measure and render trailing spaces inside <p> and <li> elements.
 */
public class CustomEditorKit extends HTMLEditorKit {
    
    @Override
    public ViewFactory getViewFactory() {
        return new HTMLFactory() {
            @Override
            public View create(Element elem) {
                View view = super.create(elem);
                
                // Override ParagraphView to prevent horizontal width expansion
                if (view instanceof ParagraphView) {
                    return new WrappedParaView(elem);
                }
                
                return view;
            }
        };
    }
    
    private static class WrappedParaView extends ParagraphView {

        public WrappedParaView(Element elem) {
            super(elem);
        }
        
        @Override
        public float getMinimumSpan(int axis) {
            return (axis == View.X_AXIS) ? 10 : super.getMinimumSpan(axis);
        }

        @Override
        public float getPreferredSpan(int axis) {
            return (axis == View.X_AXIS) ? 10 : super.getPreferredSpan(axis);
        }
        public float getMaximumSpan(int axis) {
            if (!isVisible()) {
                return Integer.MAX_VALUE - 1;
            }
            return super.getMaximumSpan(axis);
        }
        
    }

    private static final ParserDelegator JSoupParserAdapter = new ParserDelegator() {
        @Override
        public void parse(Reader r, ParserCallback cb, boolean ignoreCharSet) throws IOException {
            // 1. Read raw HTML stream into String
            String rawHtml;
            try (BufferedReader reader = new BufferedReader(r)) {
                rawHtml = reader.lines().collect(Collectors.joining("\n"));
            }

            // 2. Parse using JSoup (preserves exact character data)
            Document jsoupDoc = Jsoup.parse(rawHtml);


            for (Node child : jsoupDoc.childNodes()) {
                traverseAndCallback(child, cb);
            }

            try {
                cb.flush();
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }

        private void traverseAndCallback(Node node, ParserCallback cb) {
            if (node instanceof TextNode tn) {
                // Get untrimmed raw text buffer
                char[] chars = tn.getWholeText().toCharArray();
                if (chars.length > 0) {
                    cb.handleText(chars, 0); // Preserves all trailing spaces
                }
            } else if (node instanceof org.jsoup.nodes.Element elem) {
                HTML.Tag tag = HTML.getTag(elem.tagName());
                if (tag == null) {
                    tag = HTML.Tag.IMPLIED;
                }

                MutableAttributeSet attrs = convertAttributes(elem);

                if (elem.tag().isSelfClosing()) {
                    cb.handleSimpleTag(tag, attrs, 0);
                } else {
                    cb.handleStartTag(tag, attrs, 0);
                    for (Node child : elem.childNodes()) {
                        traverseAndCallback(child, cb);
                    }
                    cb.handleEndTag(tag, 0);
                }
            }
        }

        private MutableAttributeSet convertAttributes(org.jsoup.nodes.Element elem) {
            MutableAttributeSet swingAttrs = new SimpleAttributeSet();
            if (elem.tagName().equals("a")) {
                swingAttrs.addAttribute(HTML.Attribute.HREF, elem.attr("href"));
                swingAttrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.A);
            }
            for (String clazz : elem.classNames()) {
                swingAttrs.addAttribute(HTML.Attribute.CLASS, clazz);
            }
            return swingAttrs;
        }
    };

    @Override
    public Parser getParser() {
        return JSoupParserAdapter;
    }
}
