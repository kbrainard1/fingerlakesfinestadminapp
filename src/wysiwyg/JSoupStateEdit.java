package wysiwyg;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class JSoupStateEdit extends AbstractUndoableEdit {
    
    public static class StateSnapshot {
        final String html;
        final int caretOffset;

        StateSnapshot(String html, int caretOffset) {
            this.html = html;
            this.caretOffset = caretOffset;
        }
    }

    private final ExperimentalJSoupEditorPane editor;
    
    private StateSnapshot before;
    private StateSnapshot after;

    public JSoupStateEdit(ExperimentalJSoupEditorPane editor, 
                          StateSnapshot before,
                          StateSnapshot after) {
        this.editor = editor;
        this.before = before;
        this.after = after;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        restoreState(before);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        restoreState(after);
    }

    private void restoreState(StateSnapshot snapshot) {
        // Apply state back to the editor
        editor.applyState(snapshot);
    }
}