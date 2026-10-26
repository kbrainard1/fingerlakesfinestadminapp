package wysiwyg;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;

public class CustomUndoListener implements UndoableEditListener {
    
    private final UndoManager undoManager;
    private CompoundEdit compoundEdit = null;
    private boolean isCompoundEdit = false;


    public CustomUndoListener(UndoManager undoManager) {
        this.undoManager = undoManager;
    }


    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        if (isCompoundEdit) {
            compoundEdit.addEdit(e.getEdit());
        } else {
            undoManager.addEdit(e.getEdit());
        }
    }
    
    public void startCompoundEdit() {
        compoundEdit = new CompoundEdit();
        isCompoundEdit = true;
    }
    
    public void endCompoundEdit() {
        compoundEdit.end();
        undoManager.addEdit(compoundEdit);
        compoundEdit = null;
        isCompoundEdit = false;
    }
}
