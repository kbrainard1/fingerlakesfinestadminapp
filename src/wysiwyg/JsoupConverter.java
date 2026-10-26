package wysiwyg;

import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;


public class JsoupConverter {

    private static RenderedElement getElemRecursive(Element root, AtomicInteger renderPos, AtomicInteger seenSoFar) {
        // paragraphs, lis get preceding \ns
        if (root.tagName().equals("p") || root.tagName().equals("li")) {
            seenSoFar.addAndGet(1);
            if (renderPos.get() < seenSoFar.get()) {
                return new RenderedElement(root, 0);
            }
        }
        String htmlSoFar = "";
        for (Node child : root.childNodes()) {
            if (child instanceof Element) {
                RenderedElement possible = getElemRecursive((Element) child, renderPos, seenSoFar);
                if (possible != null) {
                    return possible;
                }
                htmlSoFar += child.outerHtml();
            } else if (child instanceof TextNode) {
                String text = ((TextNode) child).text();
                int length = text.length();
                // total = "abcde", this node is "bc", cursor is after b (pos=2) => seen=1, renderPos=2, offset = 1
                if (renderPos.get() <= seenSoFar.get() + length) {
                    int charsNeeded = renderPos.get() - seenSoFar.get();
                    return new RenderedElement(root, htmlSoFar.length() + charsNeeded);
                }
                seenSoFar.addAndGet(length);
                htmlSoFar += text;
            }
        }
        return null;
    }

    public static RenderedElement getElementAtCaret(Document document, int caretPos) {
       return getElemRecursive(document, new AtomicInteger(caretPos), new AtomicInteger(0));
    }
}
