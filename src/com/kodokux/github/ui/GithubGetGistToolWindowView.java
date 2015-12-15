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
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.kodokux.github.EditorManager;
import com.kodokux.github.GitHubGistFileTreeNode;
import org.jetbrains.annotations.NotNull;

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
import java.io.UnsupportedEncodingException;
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

    private JSplitPane mySplitPane = new JSplitPane();
    private JPanel myLeftComponent = new JPanel(new BorderLayout());
    private JPanel myRightComponent = new JPanel(new BorderLayout());

    {
        mySplitPane.setOpaque(false);
        mySplitPane.setBorder(IdeBorderFactory.createEmptyBorder(1, 0, 2, 0));
        mySplitPane.setContinuousLayout(true);
        myLeftComponent.setOpaque(false);
        myRightComponent.setOpaque(false);
    }

    protected Editor editor;
    protected DefaultTreeModel model;
    protected Document document;
    // used for diff view
    private String previousCode;
    private VirtualFile previousFile;
    private Tree jTree;

    private void writeToEditor() {
        final Editor mainEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (mainEditor != null) {
            CommandProcessor.getInstance().executeCommand(mainEditor.getProject(), new Runnable() {
                @Override
                public void run() {
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        @Override
                        public void run() {
                            String sourceCode = "";
                            if (editor.getSelectionModel().hasSelection()) {
                                sourceCode = editor.getSelectionModel().getSelectedText();
                            } else {
                                sourceCode = editor.getDocument().getText();
                            }

                            Document document = mainEditor.getDocument();
                            int offsetStart = mainEditor.getCaretModel().getOffset();
                            int sourceCodeLength = sourceCode.length();
                            SelectionModel selectionModel = mainEditor.getSelectionModel();

                            try {
                                if (selectionModel != null) {
                                    if (selectionModel.hasSelection()) {
                                        offsetStart = selectionModel.getSelectionStart();
                                        int offsetEnd = selectionModel.getSelectionEnd();
                                        document.replaceString(offsetStart, offsetEnd, sourceCode);
                                        selectionModel.setSelection(offsetStart, offsetStart + sourceCodeLength);
                                        mainEditor.getCaretModel().moveToOffset(offsetStart + sourceCodeLength);
                                    } else {
                                        document.insertString(offsetStart, sourceCode);
                                        selectionModel.setSelection(offsetStart, offsetStart + sourceCodeLength);
                                        mainEditor.getCaretModel().moveToOffset(offsetStart + sourceCodeLength);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                            mainEditor.getScrollingModel();
                        }
                    });
                }
            }, "Get Gist", UndoConfirmationPolicy.DEFAULT);
        } else {
            System.out.println("main editor");
        }
    }

    public GithubGetGistToolWindowView(final Project project, KeymapManager keymapManager, final ToolWindowManager toolWindowManager) {
        this(toolWindowManager, keymapManager, project, "php");
    }

    public GithubGetGistToolWindowView(ToolWindowManager toolWindowManager, KeymapManager keymapManager, Project project, String fileExtension) {
        super(true, true);
        this.toolWindowManager = toolWindowManager;
        this.keymapManager = keymapManager;
        this.project = project;
        this.extension = fileExtension;

        ToolWindow window = toolWindowManager.getToolWindow(toolWindowManager.getActiveToolWindowId());
        if (null != window) {
            window.activate(new Runnable() {
                @Override
                public void run() {
                    mySplitPane.setDividerLocation(0.25);
                }
            });
        }

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

                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        @Override
                        public void run() {
//                            final PsiFile cache = GithubGetGistCache.getCache(project, fileNode.getFilename());
//                            int size = 0;
//                            try {
//                                size = Integer.parseInt(fileNode.getSize());
//                            } catch (NumberFormatException e1) {
//                            }
//                            if (cache == null || cache.getText().length() != size) {
//                                getGistsRawContent(fileNode);
//                            } else {
//                                getEditor().getDocument().setText(cache.getText());
//                            }
                            getGistsRawContent(fileNode);
                        }
                    });
                }
            }
        });

        jTree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                Tree _tree = (Tree) e.getSource();
                if (_tree.getLastSelectedPathComponent() instanceof GitHubGistFileTreeNode) {
                    if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                        writeToEditor();
                    }
                }
            }
        });

        JBScrollPane jbScrollPane = new JBScrollPane(jTree);
        myLeftComponent.add(jbScrollPane);
        JComponent editorComponent = editor.getComponent();
        myRightComponent.add(editorComponent);

        mySplitPane.setLeftComponent(myLeftComponent);
        mySplitPane.setRightComponent(myRightComponent);
        add(mySplitPane);

        DefaultActionGroup group = new DefaultActionGroup();
        final ActionManager actionManager = ActionManager.getInstance();

        group.add(new ShowSettingsAction());


        final ActionToolbar actionToolBar = actionManager.createActionToolbar("Get Gist", group, true);
        final JPanel buttonsPanel = new JPanel(new BorderLayout());
        buttonsPanel.add(actionToolBar.getComponent(), BorderLayout.CENTER);
        setToolbar(buttonsPanel);
    }

    private void getGistsRawContent(final GitHubGistFileTreeNode fileNode) {
        if (fileNode != null) {
            new Task.Backgroundable(project, "Get Gist") {
                public String fileSource;

                @Override
                public void onSuccess() {
                    if (fileSource != null) {
                        ApplicationManager.getApplication().runWriteAction(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    byte[] b = fileSource.getBytes("Windows-1251");
                                    fileSource = new String(b, "UTF-8");
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                getEditor().getDocument().setText(fileSource);
                                //GithubGetGistCache.setCache(project, fileNode.getFilename(), fileSource);
                            }
                        });
                    }
                }

                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    try {
                        URL url = new URL(fileNode.getUrl());
                        HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();
                        urlconn.setReadTimeout(10000);
                        urlconn.setConnectTimeout(10000);
                        urlconn.setRequestMethod("GET");
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
                        urlconn.disconnect();

                        fileSource = buffer.toString();
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }.queue();
        }
    }


    private AnAction createShowDiffAction() {
        return new ShowDiffAction();
    }

    public static GithubGetGistToolWindowView getInstance(Project project) {
        return ServiceManager.getService(project, GithubGetGistToolWindowView.class);
    }

    @Override
    public void dispose() {
        System.out.println("test");
    }

    public void focusInRoot() {
//        jTree.setSelectionPath();
        jTree.setSelectionPath(new TreePath(((DefaultMutableTreeNode) model.getRoot()).getPath()));
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
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "GitHub");
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
