// WorldEditSchematicLoader.java
package com.auroraschaos.minigames.arena;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.session.ClipboardHolder;  // correct for 7.2.x :contentReference[oaicite:0]{index=0}
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.bukkit.BukkitAdapter;

import org.bukkit.World;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;

/**
 * Loads and pastes WorldEdit schematics into a Bukkit world.
 */
public class WorldEditSchematicLoader implements SchematicLoader {
    private final MinigamesPlugin plugin;

    public WorldEditSchematicLoader(MinigamesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void loadSchematic(String schematicName, World bukkitWorld, Vector originVec)
            throws ArenaCreationException {
        File schemFile = new File(plugin.getDataFolder(), "schematics/" + schematicName + ".schem");
        if (!schemFile.exists()) {
            throw new ArenaCreationException("Schematic not found: " + schemFile.getAbsolutePath());
        }

        try {
            // 1) Determine format
            ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
            if (format == null) {
                throw new ArenaCreationException("Unknown schematic format for file: " + schematicName);
            }

            // 2) Read into a WorldEdit Clipboard
            Clipboard clipboard;
            try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
                clipboard = reader.read();
            }

            // 3) Prepare a paste operation
            BlockVector3 pasteOrigin = BlockVector3.at(
                originVec.getBlockX(),
                originVec.getBlockY(),
                originVec.getBlockZ()
            );
            ClipboardHolder holder = new ClipboardHolder(clipboard);

            // 4) Create and run an EditSession, then paste
            try (EditSession editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder()
                    .world(BukkitAdapter.adapt(bukkitWorld))
                    .maxBlocks(Integer.MAX_VALUE)
                    .build()) {

                Operation op = holder.createPaste(editSession)
                                     .to(pasteOrigin)
                                     .ignoreAirBlocks(true)
                                     .build();
                Operations.complete(op);
            }

        } catch (ArenaCreationException e) {
            throw e;
        } catch (Exception e) {
            throw new ArenaCreationException(
                "Failed to paste schematic '" + schematicName + "'", e
            );
        }
    }
}
