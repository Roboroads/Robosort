package me.roboroads.robosort;

import gearth.extensions.ThemedExtensionFormCreator;

import java.net.URL;

public class RobosortLauncher extends ThemedExtensionFormCreator {
    public static void main(String[] args) {
        runExtensionForm(args, RobosortLauncher.class);
    }

    @Override
    protected String getTitle() {
        return "Robosort 1.0.0";
    }

    @Override
    protected URL getFormResource() {
        return getClass().getResource("/Robosort.fxml");
    }
}
