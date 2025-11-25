package com.HiWord9.RPRenames.mod.impl.renames_manager.updatable;

import com.HiWord9.RPRenames.mod.RPRenames;
import com.HiWord9.RPRenames.mod.RPRenamesItemGroup;
import com.HiWord9.RPRenames.mod.impl.renames_manager.RenamesManagerImpl;
import com.HiWord9.RPRenames.api.rename.Rename;
import com.HiWord9.RPRenames.mod.impl.renames_manager.updatable.parser.Parser;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.profiler.DummyProfiler;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.HiWord9.RPRenames.mod.util.Util.*;

public class UpdatableRenamesManager extends RenamesManagerImpl<Rename> implements ResourceReloader {
    public final ArrayList<Parser> parsers = new ArrayList<>();

    public void updateRenames() {
        updateRenames(client().getResourceManager(), Profilers.get());
    }

    public void updateRenames(ResourceManager resourceManager, Profiler profiler) {
        profiler.push("rprenames:reloading_renames");

        RPRenames.LOGGER.info("Started collecting resource pack renames");
        long startTime = System.currentTimeMillis();

        clearRenames();

        for (Parser parser : parsers) {
            parser.parse(resourceManager, profiler);
        }

        RPRenamesItemGroup.update();

        long finishTime = System.currentTimeMillis() - startTime;
        String ms = String.valueOf(finishTime % 1000);
        if (ms.length() == 1) ms = "00" + ms;
        else if (ms.length() == 2) ms = "0" + ms;
        
        RPRenames.LOGGER.info(
                "Finished collecting resource pack renames [{}.{}s] ({} in total)",
                finishTime / 1000, ms, getAllRenames().size()
        );

        profiler.pop();
    }

    public CompletableFuture<Void> reload(
            ResourceReloader.Store store,
            Executor prepareExecutor,
            ResourceReloader.Synchronizer synchronizer,
            Executor applyExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> {

            return null;
        }, prepareExecutor)
        
        .thenCompose(synchronizer::whenPrepared)
        
        .thenAcceptAsync((voidObj) -> {
            if (config().updateConfig) {
                updateRenames(client().getResourceManager(), DummyProfiler.INSTANCE);
            }
        }, applyExecutor);
    }
}
