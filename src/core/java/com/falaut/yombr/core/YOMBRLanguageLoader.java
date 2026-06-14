package com.falaut.yombr.core;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.IModLanguageLoader;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YOMBRLanguageLoader implements IModLanguageLoader {
    public static final String ID = "yombr-core";
    public static final Logger LOGGER = LogManager.getLogger("YOMBR-Logger");

    public YOMBRLanguageLoader() {
        YOMBRCore.apply();
    }

    @Override
    public String name() {
        return ID;
    }

    @Override
    public String version() {
        return "1";
    }

    @Override
    public ModContainer loadMod(IModInfo info, ModFileScanData modFileScanResults, ModuleLayer layer) throws ModLoadingException {
        throw new UnsupportedOperationException("This language loader only bootstraps default files before regular mod loading.");
    }
}
