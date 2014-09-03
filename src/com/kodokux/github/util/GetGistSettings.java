package com.kodokux.github.util;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import org.jetbrains.plugins.github.util.GithubSettings;

/**
 * Created by johna on 14/09/03.
 */
public class GetGistSettings extends GithubSettings {

    public static GithubSettings getInstance() {
        return ServiceManager.getService(GithubSettings.class);
    }
}
