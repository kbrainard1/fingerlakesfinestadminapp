package wysiwyg;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.common.collect.Sets;

// Handles escaped chars
public class HtmlNavigator {
    private static record Tag(String plainTag, String details) {
        public Tag(String simpleTag) {
            this(simpleTag, "<" + simpleTag + ">");
        }
    }
    
    private StringLike html;
    private int offsetIntoHtml;
    private List<Tag> currentTags = new ArrayList<>();
    
    private final Set<String> renderedNewLine = Sets.newHashSet("p", "li", "h1", "h2", "h3", "h4", "h5", "h6");
    
    public HtmlNavigator(String html) {
        html = html.replaceAll("<html>", "");
        html = html.replaceAll("</html>", "");
        html = html.replaceAll("<body>", "");
        html = html.replaceAll("</body>", "");
        offsetIntoHtml = 0;
        this.html = new StringLike(html);
    }
    
    public void setHtml(String html, int renderedPos) {
        this.html = new StringLike(html);
        this.currentTags.clear();
        navigateToRenderedPosition(renderedPos);
    }
    
    private CharLike currentChar() {
        return html.charAt(offsetIntoHtml);
    }
    
    private CharLike prevChar() {
        return html.charAt(offsetIntoHtml - 1);
    }
    
    public void navigateToRenderedPosition(int renderPos) {
        int renderedSoFar = 0;
        currentTags.clear();
        for (offsetIntoHtml = 0; offsetIntoHtml < html.length() && renderedSoFar < renderPos; offsetIntoHtml++) {
            if (currentChar().isTag()) {
                String tag = currentChar().plainTag();
                if (currentChar().details().charAt(1) =='/') {
                    currentTags.remove(currentTags.size() - 1);
                } else {
                    currentTags.add(new Tag(tag, currentChar().details()));
                    if (renderedNewLine.contains(tag)) {// rendered newline
                        renderedSoFar++;
                    }
                }
            } else {
                renderedSoFar++;
            }
        }
        // try to avoid editing links
        if (inGivenTag("a")) {
            if (html.charAt(offsetIntoHtml).details().equals("</a>")) {
                currentTags.remove(currentTags.size() - 1);
                offsetIntoHtml ++;
            }
        }
    }
    
    public boolean inGivenTag(String tag) {
        for (Tag t : currentTags) {
            if (t.plainTag().equals(tag)) {
                return true;
            }
        }
        return false;
    }
    
    public void addCharacter(char ch) {
        addParaIfNeeded();
        CharLike charToAdd = new CharLike(escapeHtml(""+ ch));
        html.add(charToAdd, offsetIntoHtml);
        offsetIntoHtml++;
    }
    
    private void addParaIfNeeded() {
        if (!inGivenTag("p") && !inGivenTag("li") && !inGivenTag("ul")) {
            enterNew("p", false);
        }
    }

    public boolean removeChar() {
        while (offsetIntoHtml > 0) {
            if (prevChar().isTag()) {
                String tag = prevChar().plainTag();
                if (prevChar().details().startsWith("</")) {
                    // read a close tag - we're moving backward, so we've now entered this element
                    currentTags.add(new Tag(tag, prevChar().details()));
                } else {
                    // read an open tag - we've now left this element
                    
                    // for paragraphs and list items, merge with the previous (remove the newline)
                    if (tag.equals("p") || tag.equals("li")) {
                        return mergeBreakingElements(prevChar().details());
                    } else {
                        currentTags.remove(currentTags.size() - 1);
                    }
                }
                offsetIntoHtml--;
            } else {
                html.remove(offsetIntoHtml - 1);
                offsetIntoHtml--;
                return true;
            }
        } 
        return false;
    }

    private boolean mergeBreakingElements(String tag) {
        // open tags can have attributes, so just use the tag prefix
        
        //</p><p>, </li></ul><p>, </p><ul><li>, </li><li>
        String substr = html.substring(Math.max(0, offsetIntoHtml - 3), offsetIntoHtml).getString();
        
        if (substr.endsWith("</p><p>") || substr.endsWith("</li><li>")) {
            // no change in context, just merge
            // ie, remove the close and open tags
            html.remove(offsetIntoHtml - 1);
            html.remove(offsetIntoHtml - 2);
            offsetIntoHtml -= 2; 
            
            return true; // removed the newline
        } else if (substr.endsWith("</li></ul><p>")) {
            shiftCloseTags("p", "</li></ul>");
            
            // update context
            currentTags.remove(currentTags.size() - 1);
            currentTags.add(new Tag("ul"));
            currentTags.add(new Tag("li"));
            
            return true; // removed the newline
        } else if (substr.endsWith("</p><ul><li>")) { // NB attributes on the ul would cause issues
            shiftCloseTags("li", "</p><ul>");
                        
            // extra uls mess up the rendering state
            int emptyListIndex = findCloseTag("ul");
            if (html.charAt(emptyListIndex - 1).details().equals("<ul>")) {
                html.remove(emptyListIndex);
                html.remove(emptyListIndex - 1);
            }
            
            // update context
            currentTags.remove(currentTags.size() - 1); // li
            currentTags.remove(currentTags.size() - 1); // ul
            currentTags.add(new Tag("p"));
            
            return true; // removed the newline
        } else { // most likely, we're at the beginning of the document
            currentTags.remove(currentTags.size() - 1);
            return false;
        }
    }

    private void shiftCloseTags(String tagToClose, String tagsToShift) {
        // find the matching close tag
        int closeTag = findCloseTag(tagToClose);
        
        html.remove(closeTag);
        html.remove(offsetIntoHtml - 1);
        html.remove(offsetIntoHtml - 2);
        html.remove(offsetIntoHtml - 3);
       
        
        html.addAll(new StringLike(tagsToShift), closeTag - 3);
        
        offsetIntoHtml -= 3;
    }
    
    private String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }

    public boolean enterNew(String tag, boolean allowNesting) {
        String plainTag = tag.contains(" ") ? tag.substring(0, tag.indexOf(" ")) : tag;
        if (inGivenTag(plainTag) && !allowNesting) {
            return false;
        }
        html.addAll(new StringLike("<" + tag + "></" + plainTag + ">"), offsetIntoHtml);
        offsetIntoHtml++;
        currentTags.add(new Tag(plainTag, "<" + tag + ">"));
        return true;
    }

    public void exitTag(String tag) {
        String plainTag = tag.contains(" ") ? tag.substring(0, tag.indexOf(" ")) : tag;
        if (!inGivenTag(plainTag)) {
            return;
        }
        
        // case 1: the exit tag is right there (common for entering text)
        if (currentChar().details().equals("</" + plainTag + ">")) {
            offsetIntoHtml ++;
            currentTags.remove(currentTags.size() - 1);
        } else {
            // case 2: we need to split the text
            StringLike close = closeAllContextTagsUpTo(plainTag);
            StringLike open = openAllContextTagsUpTo(plainTag); // NB this does clear nested attributes
            
            html.addAll(open, offsetIntoHtml);
            html.addAll(close, offsetIntoHtml);
            offsetIntoHtml += close.length();
            while (inGivenTag(plainTag)) {
                currentTags.remove(currentTags.size() - 1);
            }
        }
    }

    private StringLike openAllContextTagsUpTo(String plainTag) {
        StringLike result = new StringLike("");
        for (int i = currentTags.size() - 1; i >=0; i--) {
            result.add(new CharLike(currentTags.get(i).details(), true, currentTags.get(i).plainTag()), 0);
            if (currentTags.get(i).plainTag().equals(plainTag)) {
                break;
            }
        }
        return result;
    }

    private StringLike closeAllContextTagsUpTo(String plainTag) {
        StringLike result = new StringLike("");
        for (int i = currentTags.size() - 1; i >=0; i--) {
            result.add(new CharLike("</" + currentTags.get(i).plainTag() + ">", true, currentTags.get(i).plainTag()), result.length());
            if (currentTags.get(i).plainTag().equals(plainTag)) {
                break;
            }
        }
        return result;
    }

    public void split() {
        if (inGivenTag("p")) {
            handleSplit("p");
        } else if (inGivenTag("li")) {
            int endIndex = findCloseTag("li");
            int startIndex = findOpenTag("li");
            boolean anyCharsInList = false;
            for (int i = startIndex + 1; i < endIndex; i++) {
                if (!html.charAt(i).isTag()) {
                    anyCharsInList = true;
                    break;
                }
            }
            // enter at end of list should exit list (<li></li></ul>)
            if (!anyCharsInList &&
                    html.charAt(endIndex + 1).details().equals("</ul>")) {
                for (int i = 0; i < (endIndex - startIndex + 1); i++) {
                    html.remove(startIndex);
                }
                offsetIntoHtml = startIndex + 1; // skip the /ul
                html.addAll(new StringLike("<p></p>"), offsetIntoHtml);
                offsetIntoHtml++; // move into paragraph
                
                while (inGivenTag("ul")) {
                    currentTags.remove(currentTags.size() - 1);
                }
                currentTags.add(new Tag("p"));
            } else {
                handleSplit("li");
            }
            
        } else { // weird case
            addCharacter('\n');
        }
        
    }

    private void handleSplit(String tag) {
        // split paragraph, context remains the same
        StringLike close = closeAllContextTagsUpTo(tag);
        StringLike open = openAllContextTagsUpTo(tag);
        
        html.addAll(open, offsetIntoHtml);
        html.addAll(close, offsetIntoHtml);
        
        offsetIntoHtml += close.length();
        offsetIntoHtml += open.length();
    }

    public String getHtml() {
        return html.getString();
    }
    
    public String getCleanHtml() {
        String result = html.getString();
        result = result.replaceAll("<a [^<>]*></a>", "");
        for (String tag : new String[] {"b", "u", "i", "p"}) {
            result = result.replaceAll("<" + tag + "></" + tag + ">", "");
        }
        return result;
    }

    public String cutText(int selectionStart, int selectionEnd) {
        HtmlNavigator start = new HtmlNavigator(html.getString());
        start.navigateToRenderedPosition(selectionStart);
        StringLike prefix = start.openAllContextTagsUpTo("");
        
        HtmlNavigator end = new HtmlNavigator(html.getString());
        end.navigateToRenderedPosition(selectionEnd);
        StringLike suffix = end.closeAllContextTagsUpTo("");
        
        StringLike middle = html.substring(start.offsetIntoHtml, end.offsetIntoHtml);
        offsetIntoHtml = end.offsetIntoHtml;
        
       for (int i = 0; i < (selectionEnd - selectionStart); i++) {
            removeChar();
        }
        
       prefix.addAll(middle, prefix.length());
       prefix.addAll(suffix, prefix.length());
       
       return prefix.getString();
    }

    public String copyText(int selectionStart, int selectionEnd) {
        HtmlNavigator start = new HtmlNavigator(html.getString());
        start.navigateToRenderedPosition(selectionStart);
        StringLike prefix = start.openAllContextTagsUpTo("");
        
        HtmlNavigator end = new HtmlNavigator(html.getString());
        end.navigateToRenderedPosition(selectionEnd);
        StringLike suffix = end.openAllContextTagsUpTo("");
        
        StringLike middle = html.substring(start.offsetIntoHtml, end.offsetIntoHtml);
        offsetIntoHtml = end.offsetIntoHtml;
        
        prefix.addAll(middle, prefix.length());
        prefix.addAll(suffix, prefix.length());
        
        return prefix.getString();
    }

    public void paste(String textToAddBack) {
        // both of these should remove ul tags
        if (inGivenTag("li") || inGivenTag("ul")) {
            textToAddBack = changeTags(textToAddBack, "p", "li");
        } else {
            textToAddBack = changeTags(textToAddBack, "li", "p");
        }
        
        // for each open tag, if we're already in that context, remove it and the matching close tag
        
        // what if the close tags don't match? Then need to move the open tag to the end
        
        // ex: <p><b>data</b> value</p> into
        // <p><b>val|ue</b></p> 
        //
        // <p><b>valdata</b> value<b>ue</b></p>  
        
        StringLike parsedAddBack = new StringLike(textToAddBack);
        List<String> openTagsMerged = new ArrayList<>();
        mergeOpenTags(parsedAddBack, openTagsMerged);
        
        // check for matching close tags, and either remove them or add an open tag
        mergeCloseTags(parsedAddBack, openTagsMerged);
        
        
        html.addAll(parsedAddBack, offsetIntoHtml);
        offsetIntoHtml += parsedAddBack.length();
    }

    private void mergeOpenTags(StringLike textToAddBack, List<String> openTagsMerged) {
        for (int i = 0; i < textToAddBack.length(); i++) {
            if (!textToAddBack.charAt(i).isTag()) {
                break;
            }
            if (textToAddBack.charAt(i).details().contains("/")) {
                break; // this was a close tag, we actually read all the prefix tags (weird)
            }
            String tag = textToAddBack.charAt(i).plainTag();
            
            if (inGivenTag(tag)) {
                openTagsMerged.add(tag);
                textToAddBack.remove(i);
                i--;
            }
        }
    }

    private void mergeCloseTags(StringLike textToAddBack, List<String> mergedOpenTags) {
        for (int i = textToAddBack.length() - 1; i >= 0; i--) {
            if (!textToAddBack.charAt(i).isTag()) {
                break;
            }
            if (!textToAddBack.charAt(i).details().contains("/")) {
                break; // this was an open tag, we actually read all the suffix tags (weird)
            }
            String tag = textToAddBack.charAt(i).plainTag();
                        
            if (mergedOpenTags.contains(tag)) {
                mergedOpenTags.remove(tag);
                textToAddBack.remove(i);
            }
        }
        // if an open tag doesn't have a matching close, that means that
        // (a) in the copied text, the tag did not still apply at the end of the snippet (ie, it was closed during the snippet)
        // (b) the tag should apply after the end of this snippet, because we are currently in that tag
        // Solution: re-open the tag
        String mergeBack = "";
        for (String unmatchedOpen : mergedOpenTags) {
            mergeBack += "<" + unmatchedOpen + ">";
        }
        textToAddBack.addAll(new StringLike(mergeBack), textToAddBack.length());
    }

    private String changeTags(String html, String toReplace, String replaceWith) {
        html = html.replaceAll("<" + toReplace + ">", "<" + replaceWith + ">");
        html = html.replaceAll("</" + toReplace + ">", "</" + replaceWith + ">");
        html = html.replaceAll("<" + toReplace + " ", "<" + replaceWith + " "); // attributes
        
        // remove ul's
        html = html.replaceAll("<ul>", ""); 
        html = html.replaceAll("</ul>", ""); 
        return html;
    }
    
    public static record LinkData(String text, String url) {}
    
    public LinkData getLinkData() {
        if (!inGivenTag("a")) {
            return null;
        }
        int start = findOpenTag("a");
        int end = findCloseTag("a");
        Document document = Jsoup.parse(html.substring(start, end + 1).getString());
        Element linkElem = document.getElementsByTag("a").get(0);
        return new LinkData(linkElem.text(), linkElem.attr("href"));
    }

    private int findCloseTag(String plainTag) {
        String closeTagString = "</" + plainTag + ">";
        for (int i = offsetIntoHtml; i < html.length(); i++) {
            if (html.charAt(i).isTag() && html.charAt(i).details().startsWith(closeTagString)) {
               return i;
            }
        }
        return -1;
    }

    private int findOpenTag(String tag) {
        for (int i = offsetIntoHtml; i >=0; i--) {
            if (html.charAt(i).isTag() && 
                    html.charAt(i).plainTag().equals(tag) &&
                    html.charAt(i).details().startsWith("<" + tag)) {
                return i;
            }
        }
        return -1;
    }

    public void addLink(String linkText, String url) {
        addParaIfNeeded();
        if (inGivenTag("a")) {
            int start = findOpenTag("a");
            int end = findCloseTag("a");
            Document document = Jsoup.parse(html.substring(start, end + 1).getString());
            Element linkElem = document.getElementsByTag("a").get(0);
            linkElem.attr("href", url);
            linkElem.text(linkText);
            for (int i = 0; i <= (end - start); i++) {
                html.remove(start); // shifts over
            }
            html.addAll(new StringLike(linkElem.outerHtml()), start);
            offsetIntoHtml = start;
            offsetIntoHtml = findCloseTag("a") + 1;
        } else {
            StringLike linkToAdd = new StringLike("<a href=\"" + url + "\">" + linkText + "</a>");
            html.addAll(linkToAdd, offsetIntoHtml);
            offsetIntoHtml += linkToAdd.length(); 
        }
    }

    public int findRenderedPos() {
        int renderedSoFar = 0;
        for (int i = 0; i < offsetIntoHtml; i++) {
            if (html.charAt(i).isTag()) {
                String tag = html.charAt(i).plainTag();
                if (!html.charAt(i).details().contains("/") &&
                        (tag.equals("p") || tag.equals("li"))) {// rendered newline
                    renderedSoFar++;
                }
            } else {
                renderedSoFar++;
            }
        }
        return renderedSoFar;
    }

    public String getSpanClass() {
        if (!inGivenTag("span")) {
            return "";
        }
        for (int i = offsetIntoHtml; i >= 0; i--) {
            if (html.charAt(i).isTag() && 
                    html.charAt(i).plainTag().equals("span")
                    && !html.charAt(i).details().contains("/")) {
                String fullTag =  html.charAt(i).details();
                int start = fullTag.indexOf("=") + 2;
                int end = fullTag.indexOf("\"", start);
                return fullTag.substring(start, end);
            }
        }
        return "";
    }
}