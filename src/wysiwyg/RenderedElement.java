package wysiwyg;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

public class RenderedElement{
    
    private final Element elem;
    private int offsetIntoHtml;
    
    public RenderedElement(Element elem, int offsetIntoHtml) {
        this.elem = elem;
        this.offsetIntoHtml = offsetIntoHtml;
    }

    public Element getElem() {
        return elem;
    }
    
    private String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }
    
    public void addCharacter(char ch) {
        String escapeHtml = escapeHtml(""+ ch);
        String newHtml = elem.html().substring(0, offsetIntoHtml) + escapeHtml + elem.html().substring(offsetIntoHtml);
        elem.html(newHtml);
        offsetIntoHtml+=escapeHtml.length();
    }

    public boolean removeChar() {
        if (offsetIntoHtml <= 0) { // handle \ns for paragraphs
            return false;
        }
        if (elem.html().charAt(offsetIntoHtml - 1) == '>') {
            // we're messing with an inner tag, skip it:
            for (int i = offsetIntoHtml - 1; i >= 0; i--) {
                if (elem.html().charAt(i) == '<') {
                    offsetIntoHtml = i;
                    return removeChar();
                }
            }
            return false; // something wonky is happening
        } else {
            String newHtml = elem.html().substring(0, offsetIntoHtml - 1) + elem.html().substring(offsetIntoHtml);
            elem.html(newHtml);
            offsetIntoHtml--;
            return true;
        }
    }

    public RenderedElement addInner(String htmlChild) {
        String newHtml = elem.html().substring(0, offsetIntoHtml) + htmlChild + elem.html().substring(offsetIntoHtml);
        elem.html(newHtml);
        String html = "";
        for (Node child : elem.childNodes()) {
            if (child instanceof Element) {
               html += ((Element) child).outerHtml();
            } else if (child instanceof TextNode) {
                String text = ((TextNode) child).text();
                html += text;
            }
            if (offsetIntoHtml < html.length()) {
                if (!(child instanceof Element)) {
                    throw new IllegalStateException();
                }
                return new RenderedElement((Element) child, 0);
            }
        }
        throw new IllegalStateException();
    }

    public RenderedElement split() {
        if (elem.tagName().equals("a")) {
            addCharacter('\n');
            return this;
        }
        if (elem.tagName().equals("p") || elem.tagName().equals("li")) {
            String first = fixWhitespace(elem.html().substring(0, offsetIntoHtml));
            String second = fixWhitespace(elem.html().substring(offsetIntoHtml));
            elem.html(first);
            if (first.isBlank() && second.isBlank() && elem.tagName().equals("li") && elem.nextElementSibling() == null) {
                elem.parent().after("<p></p>");
                return new RenderedElement(elem.parent().nextElementSibling(), 0);
            }
            elem.after("<" + elem.tagName() + ">" + second + "</" + elem.tagName() + ">");
            return new RenderedElement(elem.nextElementSibling(), 0);
        }
        
        //i, u, b, span
        String parentTag = elem.parent().tagName();
        String breakingHtml = "</" + elem.tagName() + ">" + "</" + parentTag + ">";
        String openingHtml = "<" + parentTag + ">" + "<" + elem.tagName() + ">";
        Element parent = elem.parent();
        String html = "";
        for (int i = 0; i < elem.siblingIndex(); i++) {
            html += parent.childNode(i).outerHtml();
        }
        html += elem.outerHtml().substring(0, offsetIntoHtml + 2 + elem.tagName().length());
        html += breakingHtml;
       
        String nextHtml = openingHtml + elem.outerHtml().substring(offsetIntoHtml + 2 + elem.tagName().length());
        for (int i = elem.siblingIndex() + 1; i < parent.childNodeSize(); i++) {
            nextHtml += parent.childNode(i).outerHtml();
        }
        nextHtml += "</" + parentTag + ">";
        parent.html(html);
        parent.after(nextHtml);
        return new RenderedElement(parent.nextElementSibling().child(0), 0);
        
    }

    private String fixWhitespace(String str) {
        return str;
    }

    public RenderedElement exitLeaf(String tag) {
        Element parent = elem.parent();
        String prefixHtml = "";
        for (int i = 0; i < elem.siblingIndex(); i++) {
            prefixHtml += parent.childNode(i).outerHtml();
        }
        if (offsetIntoHtml == elem.html().length()) {
            int offsetIntoParent = prefixHtml.length() + elem.outerHtml().length();
            return new RenderedElement(parent, offsetIntoParent);
        } else {
            // TODO: this strips attributes
            prefixHtml += "<" + tag + ">" + elem.html().substring(0, offsetIntoHtml) + "</" + tag + ">";
            int offsetIntoParent = prefixHtml.length();
            String suffixHtml = "<" + tag + ">" + elem.html().substring(offsetIntoHtml) + "</" + tag + ">";
            for (int i = elem.siblingIndex() + 1; i < parent.childNodeSize(); i++) {
                suffixHtml += parent.childNode(i).outerHtml();
            }
            parent.html(prefixHtml + suffixHtml);
            return new RenderedElement(parent, offsetIntoParent);
        }
    }

    public RenderedElement moveToLeaf() {
        int offsetSoFar = 0;
        for (Node child : elem.childNodes()) {
            offsetSoFar += child.outerHtml().length();
            if (offsetSoFar > offsetIntoHtml) {
                if (child instanceof Element) {
                    int childOffset = offsetIntoHtml - elem.html().lastIndexOf(">", offsetIntoHtml) - 1;
                    return new RenderedElement((Element) child, childOffset);
                } else {
                    break;
                }
            }
        }
        return this; // we're a leaf
    }
}