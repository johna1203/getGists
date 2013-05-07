package com.kodokux.github;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import com.kodokux.github.ui.GithubGetGistToolWindowView;

/**
 * Created with IntelliJ IDEA.
 * User: johna
 * Date: 13/05/06
 * Time: 23:17
 * To change this template use File | Settings | File Templates.
 */
public class GithubGetGistToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        GithubGetGistToolWindowView view = GithubGetGistToolWindowView.getInstance(project);
        toolWindow.getContentManager().addContent(ContentFactory.SERVICE.getInstance().createContent(view, "Gists", false));
    }
}
