package org.matthelliwell.minecraftosloader.writer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Date;

import net.morbz.minecraft.blocks.Material;
import net.morbz.minecraft.level.FlatGenerator;
import net.morbz.minecraft.level.GameType;
import net.morbz.minecraft.level.Level;
import net.morbz.minecraft.world.DefaultLayers;
import net.morbz.minecraft.world.World;
import org.opengis.referencing.FactoryException;

/**
 * Generates the minecraft world
 */
public class WorldWriter {

    public static void main(String[] argv) throws IOException, FactoryException {
        if ( argv.length != 5 ) {
            System.out.println("Usage: org.matthelliwell.minecraftosloader.writer.WorldGenerator <world name> <nation grid reference square> <grid square number> <path to OS Terrain 50 data dir> <path to OS Open Map Local>\"");
            System.out.println("eg org.matthelliwell.minecraftosloader.writer.WorldGenerator test sj 24 E:\\Users\\matt\\data\\terr50_cesh_gb\\data\\sj E:\\Users\\matt\\data\\opmplc_essh_sj\\OSOpenMapLocal (ESRI Shape File) SJ\\data");
            System.out.println("Use grid square number = 'all' to load all squares");
            return;
        }

        new WorldWriter().generate(argv[0], argv[1], argv[2], argv[3], argv[4]);
    }

    private void generate(final String worldName,
                          final String nationGridReferenceSquare,
                          final String gridSquareNumber,
                          final String terrainDataDir,
                          final String localDataDir) throws IOException {

        // We'll surround the map with water and bed rock.
        final DefaultLayers layers = new DefaultLayers();
        layers.setLayer(0, Material.BEDROCK);
        layers.setLayer(1, Material.WATER);

        final Level level = new Level(worldName + " - Matt Helliwell", new FlatGenerator(layers));
        level.setGameType(GameType.CREATIVE);
        level.setAllowCommands(true);
        level.setMapFeatures(false);

        // Change the last parameter to true is you want to add to an existing world. This is also not overwrite the
        // level file so if you need a new level file delete the once in the world before running this program
        final World world = new World(level, layers, false);

        // Checks the files exist now to avoid errors after we've spent ages loading one of the files
        final Path terrainDataPath = Paths.get(terrainDataDir);
        if ( Files.notExists(terrainDataPath) || !Files.isDirectory(terrainDataPath)) {
            throw new IOException("Unable to find directory " + terrainDataPath);
        }

        final Path localDataPath = Paths.get(localDataDir);
        if ( Files.notExists(localDataPath) || !Files.isDirectory(localDataPath)) {
            throw new IOException("Unable to find directory " + localDataPath);
        }

        final Date startTime = new Date();
        final long[] blockCount = {0L};
        if (gridSquareNumber.toLowerCase().equals("all")) {
            for (int count = 0; count < 100; ++count) {
                new GridSquareWriter().write(world, localDataPath, terrainDataPath, nationGridReferenceSquare, String.format("%02d", count), blockCount);
            }
        } else {
            new GridSquareWriter().write(world, localDataPath, terrainDataPath, nationGridReferenceSquare, gridSquareNumber, blockCount);
        }

        setSpawnPoint(level);

        System.out.println("Saving the world");
        world.save(false);

        final Date endTime = new Date();
        final long elapsed = (endTime.getTime() - startTime.getTime()) / 1000;

        System.out.println("z = " + CoordConverter.convert(0) + " - y");
        System.out.println("Generated " + NumberFormat.getInstance().format(blockCount[0]) + " blocks in " + elapsed + " secs");
    }

    /**
     * Spawn point is on Out Stack, kustnorth of the Shetland Islands
     * @param level
     */
    private void setSpawnPoint(final Level level) {
        level.setSpawnPoint(460_876, 20, 11_236);
    }
}
