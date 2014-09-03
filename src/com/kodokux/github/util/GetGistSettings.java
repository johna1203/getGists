package com.kodokux.github.util;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import org.jetbrains.plugins.github.util.GithubSettings;

public class GetGistSettings extends GithubSettings {

    public static GetGistSettings getInstance() {
        return ServiceManager.getService(GetGistSettings.class);
    }
}
