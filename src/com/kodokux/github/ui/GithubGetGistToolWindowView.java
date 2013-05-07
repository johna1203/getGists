package com.kodokux.github.ui;

import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diff.DiffContent;
import com.intellij.openapi.diff.DiffManager;
import com.intellij.openapi.diff.DiffRequest;
import com.intellij.openapi.diff.SimpleContent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.kodokux.github.EditorManager;
import com.kodokux.github.GitHubGistFileTreeNode;
import com.kodokux.github.GithubGetGistAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.github.ui.GitHubSettingsConfigurable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: johna
 * Date: 13/05/06
 * Time: 23:21
 * To change this template use File | Settings | File Templates.
 */
public class GithubGetGistToolWindowView extends SimpleToolWindowPanel implements Disposable {

    private static final String DIFF_WINDOW_TITLE = "Show differences from previous class contents";
    private static final String[] DIFF_TITLES = {"Previous version", "Current version"};

    private final ToolWindowManager toolWindowManager;
    private final KeymapManager keymapManager;
    private final Project project;
    private final String extension;

    protected Editor editor;
    protected DefaultTreeModel model;
    protected Document document;
    // used for diff view
    private String previousCode;
    private VirtualFile previousFile;
    private Tree jTree;

    public GithubGetGistToolWindowView(final Project project, KeymapManager keymapManager, final ToolWindowManager toolWindowManager) {
        this(toolWindowManager, keymapManager, project, "php");
    }

    public GithubGetGistToolWindowView(ToolWindowManager toolWindowManager, KeymapManager keymapManager, Project project, String fileExtension) {
        super(true, true);
        this.toolWindowManager = toolWindowManager;
        this.keymapManager = keymapManager;
        this.project = project;
        this.extension = fileExtension;
        setupUI();
    }

    public Editor getEditor() {
        return editor;
    }

    public DefaultTreeModel getModel() {
        return model;
    }


    private void setupUI() {
        EditorManager editorManager = EditorManager.getInstance(project, FileTypeManager.getInstance().getFileTypeByExtension(extension), true);
        editor = editorManager.getEditor();
        document = editor.getDocument();

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Gists");
        model = new DefaultTreeModel(rootNode);

        jTree = new Tree(model);

        jTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                Tree _tree = (Tree) e.getSource();
                if (_tree.getLastSelectedPathComponent() instanceof GitHubGistFileTreeNode) {
                    final GitHubGistFileTreeNode fileNode = (GitHubGistFileTreeNode) _tree.getLastSelectedPathComponent();
                    getGistsRawContent(fileNode);
                }
            }
        });

        jTree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                Tree _tree = (Tree) e.getSource();
                if (_tree.getLastSelectedPathComponent() instanceof GitHubGistFileTreeNode) {
                    if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                        final Editor editor1 = FileEditorManager.getInstance(project).getSelectedTextEditor();
                        final String sourceCode = editor.getDocument().getText();
                        if (editor1 != null) {
                            CommandProcessor.getInstance().executeCommand(editor1.getProject(), new Runnable() {
                                @Override
                                public void run() {
                                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            Document document = editor1.getDocument();
                                            int offsetStart = editor1.getCaretModel().getOffset();
                                            int sourceCodeLength = sourceCode.length();
                                            SelectionModel selectionModel = editor1.getSelectionModel();

                                            if (selectionModel.hasSelection()) {
                                                offsetStart = selectionModel.getSelectionStart();
                                                int offsetEnd = selectionModel.getSelectionEnd();
                                                document.replaceString(offsetStart, offsetEnd, sourceCode);
                                                selectionModel.setSelection(offsetStart, offsetStart + sourceCodeLength);
                                                editor1.getCaretModel().moveToOffset(offsetStart + sourceCodeLength);
                                            } else {
                                                document.insertString(offsetStart, sourceCode);
                                                selectionModel.setSelection(offsetStart, offsetStart + sourceCodeLength);
                                                editor1.getCaretModel().moveToOffset(offsetStart + sourceCodeLength);
                                            }
                                            editor1.getScrollingModel();
                                        }
                                    });
                                }
                            }, "Get Gist", UndoConfirmationPolicy.DEFAULT);
                        }
                    }
                }
            }
        });

        JBScrollPane jbScrollPane = new JBScrollPane(jTree);

        final JComponent editorComponent = editor.getComponent();
        add(editorComponent);

        JPanel jPanel = new JPanel();
        jPanel.setLayout(new GridLayout(1, 2));
        jPanel.add(jbScrollPane);
        jPanel.add(editorComponent);
        add(jPanel);

        final AnAction diffAction = createShowDiffAction();
        DefaultActionGroup group = new DefaultActionGroup();
        final ActionGroup actionGroup = (ActionGroup) ActionManager.getInstance().getAction("Kodokux.Gist.ActionGrup");
        final ActionManager actionManager = ActionManager.getInstance();

        group.add(actionGroup);
//        group.add(diffAction);
        group.add(new ShowSettingsAction());


        final ActionToolbar actionToolBar = actionManager.createActionToolbar("GetGist", group, true);
        final JPanel buttonsPanel = new JPanel(new BorderLayout());
        buttonsPanel.add(actionToolBar.getComponent(), BorderLayout.CENTER);
//        PopupHandler.installPopupHandler(editor.getContentComponent(), group, "ASM", actionManager);
        setToolbar(buttonsPanel);
    }

    private void getGistsRawContent(final GitHubGistFileTreeNode fileNode) {

        new Task.Backgroundable(project, "Creating Gist") {

            public String fileSource;

            @Override
            public void onSuccess() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        getEditor().getDocument().setText(fileSource);
                    }
                });
            }

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    URL url = new URL(fileNode.getUrl());
                    HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();
                    urlconn.setRequestMethod("GET");
//                    urlconn.setInstanceFollowRedirects(false);
                    urlconn.setRequestProperty("UserAgent", "Kodokux github intellij plugin");
                    urlconn.setRequestProperty("Accept", "text/html, text/plain");
                    urlconn.connect();

                    BufferedReader in = new BufferedReader(new InputStreamReader(urlconn.getInputStream()));
                    StringBuilder buffer = new StringBuilder();
                    char[] b = new char[1024];
                    int line;
                    while (0 <= (line = in.read(b))) {
                        buffer.append(b, 0, line);
                    }
                    in.close();


                    fileSource = buffer.toString();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }.queue();
    }


    private AnAction createShowDiffAction() {
        return new ShowDiffAction();
    }

    public static GithubGetGistToolWindowView getInstance(Project project) {
        return ServiceManager.getService(project, GithubGetGistToolWindowView.class);
    }

    @Override
    public void dispose() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void focusInRoot() {
//        jTree.setSelectionPath();
        jTree.setSelectionPath(new TreePath(((DefaultMutableTreeNode)model.getRoot()).getPath()));
    }

    private class ShowSettingsAction extends AnAction {

        private ShowSettingsAction() {
            super("Settings", "Show settings for ASM plugin", IconLoader.getIcon("/general/projectSettings.png"));
        }

        @Override
        public boolean displayTextInToolbar() {
            return true;
        }

        @Override
        public void actionPerformed(final AnActionEvent e) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, project.getComponent(GitHubSettingsConfigurable.class));
        }
    }


    private class ShowDiffAction extends AnAction {

        public ShowDiffAction() {
            super("Show differences",
                    "Shows differences from the previous version of bytecode for this file",
                    IconLoader.getIcon("/actions/diffWithCurrent.png"));
        }

        @Override
        public void update(final AnActionEvent e) {
            e.getPresentation().setEnabled(!"".equals(previousCode) && (previousFile != null));
        }

        @Override
        public boolean displayTextInToolbar() {
            return true;
        }

        @Override
        public void actionPerformed(final AnActionEvent e) {
            DiffManager.getInstance().getDiffTool().show(new DiffRequest(project) {
                @Override
                public DiffContent[] getContents() {
                    // there must be a simpler way to obtain the file type
//                    PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText("asm." + extension, "");
                    PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText("void johna();", Language.findLanguageByID("java"), "test");
                    final DiffContent currentContent = previousFile == null ? new SimpleContent("") : new SimpleContent(document.getText(), psiFile.getFileType());
                    final DiffContent oldContent = new SimpleContent(previousCode == null ? "" : previousCode, psiFile.getFileType());
                    return new DiffContent[]{
                            oldContent,
                            currentContent
                    };
                }

                @Override
                public String[] getContentTitles() {
                    return DIFF_TITLES;
                }

                @Override
                public String getWindowTitle() {
                    return DIFF_WINDOW_TITLE;
                }
            });
        }
    }


}
