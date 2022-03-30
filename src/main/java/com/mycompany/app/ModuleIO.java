package com.mycompany.app;

import org.graalvm.polyglot.HostAccess;

public class ModuleIO {

    private final String rootDirectory;

    public ModuleIO(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @HostAccess.Export
    public String getCwd() {
        return rootDirectory;
    }
}
