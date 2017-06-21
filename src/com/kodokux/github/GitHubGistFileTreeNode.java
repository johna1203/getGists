package com.kodokux.github;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Created with IntelliJ IDEA.
 * User: johna
 * Date: 13/05/07
 * Time: 1:28
 * To change this template use File | Settings | File Templates.
 */
public class GitHubGistFileTreeNode extends DefaultMutableTreeNode {
    private final String size;
    private final String filename;
    private final String url;

    public GitHubGistFileTreeNode(String size, String filename, String url) {
        this.size = size;
        this.filename = filename;
        this.url = url;
    }

    @Override
    public String toString() {
        return this.filename;
    }

    public String getFilename() {
        return this.filename;
    }

    public String getSize() {
        return size;
    }

    public String getUrl() {
        return url;
    }
}
