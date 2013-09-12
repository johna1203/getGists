package com.kodokux.github;

import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: johna
 * Date: 13/05/08
 * Time: 18:52
 * To change this template use File | Settings | File Templates.
 */
public class GithubGetGistCache {
    private static final Logger LOG = Logger.getLogger("Cache");
    private static GithubGetGistCache instance;
    private static String CONFIG_DIR = StoragePathMacros.PROJECT_CONFIG_DIR + "/getgist";

//    final Project project;

    public GithubGetGistCache(Project project) {
//        this.project = project;
//        VirtualFile directory = LocalFileSystem.getInstance().findFileByIoFile(new File(CONFIG_DIR));
//        if(directory == null){
//            try {
//                directory = VfsUtil.createDirectories(CONFIG_DIR);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    static public PsiDirectory getCacheDir(Project project) {
        File dirName = new File(project.getBasePath(), ".idea/gits");
        VirtualFile directory = LocalFileSystem.getInstance().findFileByIoFile(new File(project.getBasePath(), ".idea/gits"));
        if (directory == null) {
            try {
                LOG.info("dir is null" + dirName.getPath());
                directory = VfsUtil.createDirectories(dirName.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(directory);
        return psiDirectory;
    }

    static public PsiFile setCache(Project project, String fileName, Object data) {

        final PsiDirectory psiDirectory = getCacheDir(project);
        FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(fileName);
        if (fileType == FileTypes.UNKNOWN) {
            fileType = FileTypeManager.getInstance().getFileTypeByFileName("*.txt");
        }

        final PsiFile file = psiDirectory.findFile(fileName);
        if (file != null) {
//            ApplicationManager.getApplication().runWriteAction(new Runnable() {
//                @Override
//                public void run() {
            file.delete();
//                }
//            });
        }

        final PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, fileType, data.toString());
//        ApplicationManager.getApplication().runWriteAction(new Runnable() {
//            @Override
//            public void run() {
        psiDirectory.add(psiFile);
//            }
//        });

        return psiFile;
    }

    static public PsiFile getCache(Project project, String key) {
        final PsiDirectory psiDirectory = getCacheDir(project);
        return ((psiDirectory.findFile(key) == null) ? null : psiDirectory.findFile(key));

    }


}
