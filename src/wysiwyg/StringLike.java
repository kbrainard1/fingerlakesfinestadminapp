package wysiwyg;

import java.util.ArrayList;
import java.util.List;

public class StringLike {

    public StringLike(String html) {
        for (int i = 0; i < html.length(); i++) {
            if (html.charAt(i) == '\n' || html.charAt(i) == '\r') {
                continue; // where are these even coming from
            }
            if (html.charAt(i) == '&') {
                String details = "";
                char escaped = html.charAt(i + 1);
                switch (escaped) {
                case 'n': // nbsp
                case 'q': // quot
                    details = html.substring(i, i + 4);
                    break;
                case 'l': // lt
                case 'g': // gt
                    details = html.substring(i, i + 2);
                    break;
                case 'a': // amp or apos
                    if (html.charAt(i + 2) == 'm') {
                        details = html.substring(i, i + 3);
                    } else {
                        details = html.substring(i, i + 4);
                    }
                    break;
                }
                i += details.length() - 1;
                chars.add(new CharLike(details));
            } else if (html.charAt(i) == '<') {
                int start = i;
                i++;
                if (html.charAt(i) =='/') {
                    i++;
                }
                String tag = "";
                while (html.charAt(i) != '>') {
                    tag += html.charAt(i);
                    i++;
                }
                chars.add(new CharLike(html.substring(start, i+1), 
                        tag.contains(" ") ? tag.substring(0, tag.indexOf(" ")) : tag));
            } else {
                chars.add(new CharLike(html.charAt(i)));
            }
        }
    }

    private StringLike(List<CharLike> chars) {
        this.chars.addAll(chars);
    }

    private List<CharLike> chars = new ArrayList<>();

    public int length() {
        return chars.size();
    }

    public CharLike charAt(int i) {
        return chars.get(i);
    }

    public void add(CharLike charToAdd, int index) {
        chars.add(index, charToAdd);
    }

    public String getString() {
        StringBuilder sb = new StringBuilder();
        chars.forEach(ch -> sb.append(ch.details()));
        return sb.toString();
    }

    public void remove(int index) {
        chars.remove(index);
    }

    public void addAll(StringLike toAdd, int index) {
        chars.addAll(index, toAdd.chars);
    }

    public StringLike substring(int start, int end) {
        return new StringLike(chars.subList(start, end));
    } 
}
