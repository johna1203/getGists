package com.kodokux.github;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;

/**
 * Created with IntelliJ IDEA.
 * User: johna
 * Date: 13/05/07
 * Time: 0:38
 * To change this template use File | Settings | File Templates.
 */
public class EditorManager {
    private static EditorManager instance = null;
    private final Editor editor;

    public EditorManager(Project project, FileType fileTypeByExtension, boolean b) {
        final EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument("");
        editor = editorFactory.createEditor(document, project, fileTypeByExtension, true);
        final EditorSettings editorSettings = editor.getSettings();
        editorSettings.setLineMarkerAreaShown(true);
        editorSettings.setLineNumbersShown(true);
        editorSettings.setFoldingOutlineShown(true);
        editorSettings.setAnimatedScrolling(true);
        editorSettings.setWheelFontChangeEnabled(true);
        editorSettings.setVariableInplaceRenameEnabled(true);
    }

    public static EditorManager getInstance(Project project, FileType fileTypeByExtension, boolean b) {
        if (instance == null) {
            return instance = new EditorManager(project, fileTypeByExtension, b);
        }
        return instance;
    }

    public Editor getEditor() {
        return editor;
    }
}
