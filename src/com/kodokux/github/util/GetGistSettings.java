package com.kodokux.github.util;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import org.jetbrains.plugins.github.util.GithubSettings;

/**
 * Created by johna on 14/09/03.
 */
@SuppressWarnings("MethodMayBeStatic")
@State(
        name = "GithubSettings",
        storages = {@Storage(
                file = StoragePathMacros.APP_CONFIG + "/github_settings.xml")})
public class GetGistSettings extends GithubSettings {

    public static GetGistSettings getInstance() {
        return ServiceManager.getService(GetGistSettings.class);
    }
}
