package me.roboroads.robosort;

import gearth.extensions.ThemedExtensionFormCreator;

import java.net.URL;

public class RobosortLauncher extends ThemedExtensionFormCreator {
    public static void main(String[] args) {
        runExtensionForm(args, RobosortLauncher.class);
    }

    @Override
    protected String getTitle() {
        // %%VERSION%% will be replaced by the Github Actions workflow
        return "Robosort %%VERSION%%";
    }

    @Override
    protected URL getFormResource() {
        return getClass().getResource("/Robosort.fxml");
    }
}
