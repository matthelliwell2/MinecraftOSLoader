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
        if ( argv.length != 4 ) {
            System.out.println("Usage: org.matthelliwell.minecraftosloader.writer.WorldGenerator <world name> <nation grid reference squares> <grid square number> <path to data dir>\"");
            System.out.println("eg org.matthelliwell.minecraftosloader.writer.WorldGenerator test na,nb,nc 24 E:\\Users\\matt\\data");
            System.out.println("Use grid square number = 'all' to load all squares");
            return;
        }

        new WorldWriter().generate(argv[0], argv[1], argv[2], argv[3]);
    }

    private void generate(final String worldName,
                          final String nationGridReferenceSquares,
                          final String gridSquareNumber,
                          final String dataDir) throws IOException {

        // Checks the files exist now to avoid errors after we've spent ages loading one of the files
        for ( final String square: nationGridReferenceSquares.split(",")) {
            final String nationGridReferenceSquare = square.trim().toLowerCase();

            final Path terrainDataPath = getTerrainDataPath(dataDir, nationGridReferenceSquare);

            final Path localDataPath = getLocalDataPath(dataDir, nationGridReferenceSquare);

            if (Files.notExists(terrainDataPath) || !Files.isDirectory(terrainDataPath)) {
                throw new IOException("Unable to find directory " + terrainDataPath);
            }

            if (Files.notExists(localDataPath) || !Files.isDirectory(localDataPath)) {
                throw new IOException("Unable to find directory " + localDataPath);
            }
        }

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
        final World world = new World(level, layers, true);

        final Date startTime = new Date();
        final long[] blockCount = {0L};
        for ( final String square: nationGridReferenceSquares.split(",")) {
            final String nationGridReferenceSquare = square.trim().toLowerCase();
            final Path terrainDataPath = getTerrainDataPath(dataDir, nationGridReferenceSquare);
            final Path localDataPath = getLocalDataPath(dataDir, nationGridReferenceSquare);

            if (gridSquareNumber.toLowerCase().equals("all")) {
                for (int count = 0; count < 100; ++count) {
                    new GridSquareWriter().write(world, localDataPath, terrainDataPath, nationGridReferenceSquare, String.format("%02d", count), blockCount);
                }
            } else {
                new GridSquareWriter().write(world, localDataPath, terrainDataPath, nationGridReferenceSquare, gridSquareNumber, blockCount);
            }
        }

        setSpawnPoint(level);

        System.out.println("Saving the world");
        world.save();

        System.out.println("Archiving data");

        final Date endTime = new Date();
        final long elapsed = (endTime.getTime() - startTime.getTime()) / 1000;

        System.out.println("z = " + CoordConverter.convert(0) + " - y");
        System.out.println("Generated " + NumberFormat.getInstance().format(blockCount[0]) + " blocks in " + elapsed + " secs");
    }

    private Path getLocalDataPath(final String dataDir, final String nationGridReferenceSquare) {
        return Paths.get(dataDir)
                        .resolve("opmplc_essh_" + nationGridReferenceSquare)
                        .resolve("OSOpenMapLocal (ESRI Shape File) " + nationGridReferenceSquare.toUpperCase())
                        .resolve("data");
    }

    private Path getTerrainDataPath(final String dataDir, final String nationGridReferenceSquare) {
        return Paths.get(dataDir)
                        .resolve("terr50_cesh_gb")
                        .resolve("data").resolve(nationGridReferenceSquare);
    }

    /**
     * Spawn point is on Out Stack, kustnorth of the Shetland Islands
     */
    private void setSpawnPoint(final Level level) {
        level.setSpawnPoint(460_876, 20, 11_236);
    }
}
