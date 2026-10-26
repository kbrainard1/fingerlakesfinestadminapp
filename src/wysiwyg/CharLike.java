package wysiwyg;

public record CharLike(String details, boolean isTag, String plainTag) {
    
    public CharLike(String escaped) {
        this(escaped, false, "");
    }

    public CharLike(String fullTag, String plainTag) {
        this(fullTag, true, plainTag);
    }

    public CharLike(char normal) {
        this("" + normal, false, "");
    }
}
