package com.kodokux.github;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.Consumer;
import com.kodokux.github.api.GithubApiUtil;
import com.kodokux.github.ui.GithubGetGistToolWindowView;
import com.kodokux.github.util.GetGistSettings;
import icons.GithubIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.github.util.GithubSettings;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: johna
 * Date: 13/05/05
 * Time: 23:24
 * To change this template use File | Settings | File Templates.
 */
public class GithubGetGistAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(GithubGetGistAction.class);
    private static final String TOOLWINDOW_ID = "getGist";
    private ToolWindow myToolWindow;

    public GithubGetGistAction() {
        super("Get Gist...", "Get github gist", GithubIcons.Github_icon);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {

        final Project project = e.getData(PlatformDataKeys.PROJECT);

        getGistWithProgress(project, new Consumer<JsonElement>() {
            @Override
            public void consume(JsonElement jsonElement) {
                GithubGetGistToolWindowView view = GithubGetGistToolWindowView.getInstance(project);
                view.focusInRoot();
                DefaultTreeModel model = view.getModel();

                ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Get Gist");
                if (toolWindow != null) {
                    toolWindow.setIcon(GithubIcons.Github_icon);
                    if (!toolWindow.isActive()) {
                        toolWindow.activate(null);
                    }
                }


                final DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
                root.removeAllChildren();
                if (jsonElement != null) {
                    for (JsonElement jsonElement1 : jsonElement.getAsJsonArray()) {
                        String name = jsonElement1.getAsJsonObject().get("description").getAsString();
                        try {
                            byte b[] = name.getBytes("Windows-1251");
                            name = new String(b, "UTF-8");
                        } catch (UnsupportedEncodingException e1) {
                            e1.printStackTrace();
                        }
                        DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
                        root.add(node);
                        if (jsonElement1.getAsJsonObject().has("files")) {
                            JsonObject files = jsonElement1.getAsJsonObject().get("files").getAsJsonObject();
                            Set<Map.Entry<String, JsonElement>> file = files.entrySet();
                            for (Map.Entry<String, JsonElement> entry : files.entrySet()) {
                                JsonObject filesObject = entry.getValue().getAsJsonObject();

                                if (filesObject.has("size") && filesObject.has("filename") && filesObject.has("raw_url")) {
                                    String size = filesObject.get("size").getAsString();
                                    String filename = filesObject.get("filename").getAsString();
                                    String raw_url = filesObject.get("raw_url").getAsString();
                                    node.add(new GitHubGistFileTreeNode(size, filename, raw_url));
                                }
                            }
                        }

                    }
                }
                model.reload();
            }
        });
    }

    private void getGistWithProgress(final Project project, final Consumer<JsonElement> consumer) {
        new Task.Backgroundable(project, "Get Gist") {

            public JsonElement jsonElement = null;

            @Override
            public void onSuccess() {
                consumer.consume(jsonElement);
            }

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    GithubSettings settings = GetGistSettings.getInstance();
                    jsonElement = GithubApiUtil.getRequest(settings.getAuthData(), "/gists");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.queue();
    }


    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
    }

    private void writeSourceCode(final Editor editor, final String sourceCode) {
        if (editor != null) {

            CommandProcessor.getInstance().executeCommand(editor.getProject(), new Runnable() {
                @Override
                public void run() {
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        @Override
                        public void run() {
                            Document document = editor.getDocument();
                            int offsetStart = editor.getCaretModel().getOffset();
                            int sourceCodeLength = sourceCode.length();
                            SelectionModel selectionModel = editor.getSelectionModel();


                            if (selectionModel.hasSelection()) {
                                offsetStart = selectionModel.getSelectionStart();
                                int offsetEnd = selectionModel.getSelectionEnd();
                                document.replaceString(offsetStart, offsetEnd, sourceCode);
                                selectionModel.setSelection(offsetStart, offsetStart + sourceCodeLength);
                                editor.getCaretModel().moveToOffset(offsetStart + sourceCodeLength);
                            } else {
                                document.insertString(offsetStart, sourceCode);
                                selectionModel.setSelection(offsetStart, offsetStart + sourceCodeLength);
                                editor.getCaretModel().moveToOffset(offsetStart + sourceCodeLength);
                            }
                        }
                    });
                }
            }, "Get Gist", UndoConfirmationPolicy.DEFAULT);


        }
    }
}
