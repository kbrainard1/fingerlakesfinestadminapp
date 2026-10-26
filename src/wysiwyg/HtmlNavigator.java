package wysiwyg;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

// Handle escaped characters as single chars for navigation etc
public class HtmlNavigator{
    private String html;
    private int offsetIntoHtml;
    private List<String> currentTags = new ArrayList<>();
    
    public HtmlNavigator(String html) {
        html = html.replaceAll("<html>", "");
        html = html.replaceAll("</html>", "");
        html = html.replaceAll("<body>", "");
        html = html.replaceAll("</body>", "");
        offsetIntoHtml = 0;
        this.html = html;
    }
    
    public void setHtml(String html, int renderedPos) {
        this.html = html;
        this.currentTags.clear();
        navigateToRenderedPosition(renderedPos);
    }
    
    private char currentChar() {
        return html.charAt(offsetIntoHtml);
    }
    
    private char prevChar() {
        return html.charAt(offsetIntoHtml - 1);
    }
    
    public void navigateToRenderedPosition(int renderPos) {
        int renderedSoFar = 0;
        currentTags.clear();
        for (offsetIntoHtml = 0; offsetIntoHtml < html.length() && renderedSoFar < renderPos; offsetIntoHtml++) {
            if (currentChar() == '<') {
                offsetIntoHtml++;
                if (currentChar() =='/') {
                    offsetIntoHtml++;
                    String tag = readTag();
                    if (!currentTags.get(currentTags.size() - 1).equals(tag)) {
                        throw new IllegalStateException();
                    }
                    currentTags.remove(currentTags.size() - 1);
                } else {
                    String tag = readTag();
                    tag = tag.contains(" ") ? tag.substring(0, tag.indexOf(" ")) : tag;
                    currentTags.add(tag);
                    if (tag.equals("p") || tag.equals("li")) {// rendered newline
                        renderedSoFar++;
                    }
                }
            } else {
                renderedSoFar++;
            }
        }
        // try to avoid editing links
        if (inGivenTag("a")) {
            if (html.indexOf("</a>", offsetIntoHtml) == offsetIntoHtml) {
                currentTags.remove(currentTags.size() - 1);
                offsetIntoHtml += 4;
            }
        }
    }

    private String readTag() {
        String tag = "";
        while (currentChar() != '>') {
            tag += currentChar();
            offsetIntoHtml++;
        }
        return tag;
    }
    
    private String readReverseTag() {
        String tag = "";
        while (currentChar() != '<') {
            tag = currentChar() + tag;
            offsetIntoHtml--;
        }
        return tag;
    }
    
    public boolean inGivenTag(String tag) {
        return currentTags.contains(tag);
    }
    
    public void addCharacter(char ch) {
        addParaIfNeeded();
        String escapeHtml = escapeHtml(""+ ch);
        html = html.substring(0, offsetIntoHtml) + escapeHtml + html.substring(offsetIntoHtml);
        offsetIntoHtml+=escapeHtml.length();
    }
    
    private void addParaIfNeeded() {
        if (!inGivenTag("p") && !inGivenTag("li") && !inGivenTag("ul")) {
            enterNew("p");
        }
    }

    public boolean removeChar() {
        while (offsetIntoHtml > 0) {
            if (prevChar() == '>') {
                offsetIntoHtml-=2;
                String tag = readReverseTag();
                if (tag.startsWith("/")) {
                    // read a close tag - we're moving backward, so we've now entered this element
                    tag = tag.substring(1);
                    currentTags.add(tag);
                } else {
                    // read an open tag - we've now left this element
                    String plainTag = tag.contains(" ") ? tag.substring(0, tag.indexOf(" ")) : tag;
                    
                    // for paragraphs and list items, merge with the previous (remove the newline)
                    if (plainTag.equals("p") || plainTag.equals("li")) {
                        return mergeBreakingElements(tag);
                    } else {
                        tag = tag.contains(" ") ? tag.substring(0, tag.indexOf(" ")) : tag;
                        if (!currentTags.get(currentTags.size() - 1).equals(tag)) {
                            throw new IllegalStateException();
                        }
                        currentTags.remove(currentTags.size() - 1);
                    }
                }
            } else {
                html = html.substring(0, offsetIntoHtml - 1) + html.substring(offsetIntoHtml);
                offsetIntoHtml--;
                return true;
            }
        } 
        return false;
    }

    private boolean mergeBreakingElements(String tag) {
        // open tags can have attributes, so just use the tag prefix
        
        //</p><p, </li></ul><p, </p><ul><li, </li><li
        String substr = html.substring(Math.max(0, offsetIntoHtml - 12), offsetIntoHtml + tag.length() + 1);
        
        // OIH is currently the '<' in the last open tag
        if (substr.endsWith("</p><p") || substr.endsWith("</li><li")) {
            // no change in context, just merge
            
            int closeLen = tag.length() + 3; // "</tag>"
            int openLen = tag.length() + 2; // "<tag>"
            html = html.substring(0, offsetIntoHtml - closeLen) + html.substring(offsetIntoHtml + openLen);
            offsetIntoHtml -= closeLen;
            return true; // removed the newline
        } else if (substr.endsWith("</li></ul><p")) {
            // find the matching close tag
            int closeTag = html.indexOf("</p>", offsetIntoHtml);
            
            html = html.substring(0, offsetIntoHtml - "</li></ul>".length()) 
                    + html.substring(offsetIntoHtml + tag.length() + 2, closeTag)
                    + "</li></ul>"
                    + html.substring(closeTag + "</p>".length());
            offsetIntoHtml -= "</li></ul>".length();
            
            // update context
            currentTags.remove(currentTags.size() - 1);
            currentTags.add("ul");
            currentTags.add("li");
            return true; // removed the newline
        } else if (substr.endsWith("</p><ul><li")) { // NB attributes on the ul would cause issues
            int closeTag = html.indexOf("</li>", offsetIntoHtml);
            html = html.substring(0, offsetIntoHtml - "</p><ul>".length()) 
                    + html.substring(offsetIntoHtml + tag.length() + 2, closeTag)
                    + "</p><ul>"
                    + html.substring(closeTag + "</li>".length());
            offsetIntoHtml -= "</p><ul>".length();
            
            // extra uls mess up the rendering state
            int emptyListIndex = html.indexOf("<ul></ul>", offsetIntoHtml);
            if (emptyListIndex > 0) {
                html = html.substring(0, emptyListIndex) + html.substring(emptyListIndex + "<ul></ul>".length());
            }
            
            // update context
            currentTags.remove(currentTags.size() - 1); // li
            currentTags.remove(currentTags.size() - 1); // ul
            currentTags.add("p");
            
            return true; // removed the newline
        } else { // most likely, we're at the beginning of the document
            currentTags.remove(currentTags.size() - 1);
            return false;
        }
    }
    
    private String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }

    public boolean enterNew(String tag) {
        String plainTag = tag.contains(" ") ? tag.substring(0, tag.indexOf(" ")) : tag;
        if (inGivenTag(plainTag)) {
            return false;
        }
        html = html.substring(0, offsetIntoHtml) + "<" + tag + "></" + plainTag + ">" + html.substring(offsetIntoHtml);
        offsetIntoHtml += (tag.length() + 2);
        currentTags.add(plainTag);
        return true;
    }

    public void exitTag(String tag) {
        String plainTag = tag.contains(" ") ? tag.substring(0, tag.indexOf(" ")) : tag;
        if (!inGivenTag(plainTag)) {
            return;
        }
        
        // case 1: the exit tag is right there (common for entering text)
        if (html.substring(offsetIntoHtml, offsetIntoHtml + plainTag.length() + 3).equals("</" + plainTag + ">")) {
            offsetIntoHtml += plainTag.length() + 3;
            currentTags.remove(currentTags.size() - 1);
        } else {
            // case 2: we need to split the text
            String close = closeAllContextTagsUpTo(plainTag);
            String open = openAllContextTagsUpTo(plainTag); // NB this does clear nested attributes
            
            html = html.substring(0, offsetIntoHtml) + close + open + html.substring(offsetIntoHtml);
            offsetIntoHtml += close.length();
            while (inGivenTag(plainTag)) {
                currentTags.remove(currentTags.size() - 1);
            }
        }
    }

    private String openAllContextTagsUpTo(String plainTag) {
        String result = "";
        for (int i = currentTags.size() - 1; i >=0; i--) {
            result = "<" + currentTags.get(i) + ">" + result;
            if (currentTags.get(i).equals(plainTag)) {
                break;
            }
        }
        return result;
    }

    private String closeAllContextTagsUpTo(String plainTag) {
        String result = "";
        for (int i = currentTags.size() - 1; i >=0; i--) {
            result += "</" + currentTags.get(i) + ">";
            if (currentTags.get(i).equals(plainTag)) {
                break;
            }
        }
        return result;
    }

    public void split() {
        if (inGivenTag("p")) {
            // split paragraph, context remains the same
            String close = closeAllContextTagsUpTo("p");
            String open = openAllContextTagsUpTo("p");
            html = html.substring(0, offsetIntoHtml) + close + open + html.substring(offsetIntoHtml);
            offsetIntoHtml += close.length();
            offsetIntoHtml += open.length();
        } else if (inGivenTag("li")) {
            String exitList = "<li></li></ul>"; // enter at end of list should exit list
            if (html.substring(Math.max(0, offsetIntoHtml - 4), 
                    Math.min(html.length(), offsetIntoHtml + 10)).equals(exitList)) {
                html = html.substring(0, offsetIntoHtml - 4) + "</ul><p></p>" + html.substring(offsetIntoHtml + 10);
                offsetIntoHtml += 4;
                currentTags.remove(currentTags.size() - 1); // li
                currentTags.remove(currentTags.size() - 1); // ul
                currentTags.add("p");
            } else {
                String close = closeAllContextTagsUpTo("li");
                String open = openAllContextTagsUpTo("li");
                html = html.substring(0, offsetIntoHtml) + close + open + html.substring(offsetIntoHtml);
                offsetIntoHtml += close.length();
                offsetIntoHtml += open.length();
            }
            
        } else { // weird case
            addCharacter('\n');
        }
        
    }

    public String getHtml() {
        return html;
    }
    
    public String getCleanHtml() {
        String result = html;
        result = result.replaceAll("<a [^<>]*></a>", "");
        for (String tag : new String[] {"b", "u", "i", "p"}) {
            result = result.replaceAll("<" + tag + "></" + tag + ">", "");
        }
        return result;
    }

    public String cutText(int selectionStart, int selectionEnd) {
        HtmlNavigator start = new HtmlNavigator(html);
        start.navigateToRenderedPosition(selectionStart);
        String prefix = start.openAllContextTagsUpTo("");
        
        HtmlNavigator end = new HtmlNavigator(html);
        end.navigateToRenderedPosition(selectionEnd);
        String suffix = end.closeAllContextTagsUpTo("");
        
        String middle = html.substring(start.offsetIntoHtml, end.offsetIntoHtml);
        offsetIntoHtml = end.offsetIntoHtml;
        
       for (int i = 0; i < (selectionEnd - selectionStart); i++) {
            removeChar();
        }
        
        return prefix + middle + suffix;
    }

    public String copyText(int selectionStart, int selectionEnd) {
        HtmlNavigator start = new HtmlNavigator(html);
        start.navigateToRenderedPosition(selectionStart);
        String prefix = start.openAllContextTagsUpTo("");
        
        HtmlNavigator end = new HtmlNavigator(html);
        end.navigateToRenderedPosition(selectionEnd);
        String suffix = end.openAllContextTagsUpTo("");
        
        String middle = html.substring(start.offsetIntoHtml, end.offsetIntoHtml);
        offsetIntoHtml = end.offsetIntoHtml;
        
        return prefix + middle + suffix;
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
        
        List<String> openTagsMerged = new ArrayList<>();
        textToAddBack = mergeOpenTags(textToAddBack, openTagsMerged);
        
        // check for matching close tags, and either remove them or add an open tag
        textToAddBack = mergeCloseTags(textToAddBack, openTagsMerged);
        
        html = html.substring(0, offsetIntoHtml) + textToAddBack + html.substring(offsetIntoHtml);
        offsetIntoHtml += textToAddBack.length();
    }

    private String mergeOpenTags(String textToAddBack, List<String> openTagsMerged) {
        for (int i = 0; i < textToAddBack.length(); i++) {
            if (textToAddBack.charAt(i) != '<') {
                break;
            }
            String tag = "";
            int startPos = i;
            i++;
            while (textToAddBack.charAt(i) != '>') {
                tag += textToAddBack.charAt(i);
                i++;
            }
            if (tag.startsWith("/")) {
                break; // this was a close tag, we actually read all the prefix tags (weird)
            }
            if (inGivenTag(tag)) {
                openTagsMerged.add(tag);
                textToAddBack = textToAddBack.substring(0, startPos) + textToAddBack.substring(i + 1);
                i = startPos - 1;
            }
        }
        return textToAddBack;
    }

    private String mergeCloseTags(String textToAddBack, List<String> mergedOpenTags) {
        for (int i = textToAddBack.length() - 1; i >= 0; i--) {
            if (textToAddBack.charAt(i) != '>') {
                break;
            }
            String tag = "";
            int endPos = i;
            i--;
            while (textToAddBack.charAt(i) != '<') {
                tag = textToAddBack.charAt(i) + tag;
                i--;
            }
            if (!tag.startsWith("/")) {
                break; // this was an open tag, we actually read all the suffix tags (weird)
            }
            tag = tag.substring(1);
            
            if (mergedOpenTags.contains(tag)) {
                mergedOpenTags.remove(tag);
                textToAddBack = textToAddBack.substring(0, i) + textToAddBack.substring(endPos + 1);
            }
        }
        // if an open tag doesn't have a matching close, that means that
        // (a) in the copied text, the tag did not still apply at the end of the snippet (ie, it was closed during the snippet)
        // (b) the tag should apply after the end of this snippet, because we are currently in that tag
        // Solution: re-open the tag
        for (String unmatchedOpen : mergedOpenTags) {
            textToAddBack += "<" + unmatchedOpen + ">";
        }
        return textToAddBack;
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
        int start = html.lastIndexOf("<a ", offsetIntoHtml);
        int end = html.indexOf("</a>", offsetIntoHtml);
        Document document = Jsoup.parse(html.substring(start, end + 4));
        Element linkElem = document.getElementsByTag("a").get(0);
        return new LinkData(linkElem.text(), linkElem.attr("href"));
    }

    public void addLink(String linkText, String url) {
        addParaIfNeeded();
        if (inGivenTag("a")) {
            int start = html.lastIndexOf("<a ", offsetIntoHtml);
            int end = html.indexOf("</a>", offsetIntoHtml);
            Document document = Jsoup.parse(html.substring(start, end + 4));
            Element linkElem = document.getElementsByTag("a").get(0);
            linkElem.attr("href", url);
            linkElem.text(linkText);
            html = html.substring(0, start) + linkElem.outerHtml() + html.substring(end + 4);
            offsetIntoHtml = html.indexOf("</a>" + 4, start);
        } else {
            String linkToAdd = "<a href=\"" + url + "\">" + linkText + "</a>";
            html = html.substring(0, offsetIntoHtml) + linkToAdd + html.substring(offsetIntoHtml);
            offsetIntoHtml += linkToAdd.length(); 
        }
    }

    public int findRenderedPos() {
        int renderedSoFar = 0;
        for (int i = 0; i < offsetIntoHtml; i++) {
            if (html.charAt(i) == '<') {
                i++;
                String tag = "";
                while (html.charAt(i) != '>') {
                    tag += html.charAt(i);
                    i++;
                }
                tag = tag.contains(" ") ? tag.substring(0, tag.indexOf(" ")) : tag;
                if (tag.equals("p") || tag.equals("li")) {// rendered newline
                    renderedSoFar++;
                }
            } else {
                renderedSoFar++;
            }
        }
        return renderedSoFar;
    }
}