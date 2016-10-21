package org.matthelliwell.minecraftosloader.writer;

import com.google.common.collect.ImmutableMap;
import net.morbz.minecraft.world.FileManager;
import net.morbz.minecraft.world.Region;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * I can't fit all the region files on my local disk so need a strategy for achiving them to zips. This runs
 * separetly from the generator as the archiving can't be done for a square until all adjacent squares have been
 * generated.
 */
public class Archiver {
    public static void main(String[] argv) {
        if ( argv.length != 2) {
            System.out.println("Usage org.matthelliwell.minecraftosloader.writer.Archive <world name> <grid square>");
            System.out.println("eg org.matthelliwell.minecraftosloader.writer.Archive UK HP");
            System.out.println("or org.matthelliwell.minecraftosloader.writer.Archive UK all");
            return;
        }

        if (argv[1].equals("all")) {
            Set<String> squares = bounds.keySet();
            final Archiver a = new Archiver();
            for (final String s : squares) {
                a.zipRegionFiles(argv[0] + " - Matt Helliwell", s);
            }
        } else {
            final String[] squares = argv[1].split(",");
            final Archiver a = new Archiver();
            for (final String s : squares) {
                a.zipRegionFiles(argv[0] + " - Matt Helliwell", s);
            }
        }
    }

    // This is the lower left coords of each grid square. We could derive them from OS data but as they aren't about
    // to change I've hard coded them
    private static final Map<String, Point> bounds = ImmutableMap.<String, Point>builder()
            .put("HP", new Point(400_000, 1_200_000))
            .put("HT", new Point(300_000, 1_100_000))
            .put("HU", new Point(400_000, 1_100_000))
            .put("HW", new Point(100_000, 1_000_000))
            .put("HX", new Point(200_000, 1_000_000))
            .put("HY", new Point(300_000, 1_000_000))
            .put("HZ", new Point(400_000, 1_000_000))

            .put("NA", new Point(0      , 900_000))
            .put("NB", new Point(100_000, 900_000))
            .put("NC", new Point(200_000, 900_000))
            .put("ND", new Point(300_000, 900_000))

            .put("NF", new Point(0      , 800_000))
            .put("NG", new Point(100_000, 800_000))
            .put("NH", new Point(200_000, 800_000))
            .put("NJ", new Point(300_000, 800_000))
            .put("NK", new Point(400_000, 800_000))

            .put("NL", new Point(0      , 700_000))
            .put("NM", new Point(100_000, 700_000))
            .put("NN", new Point(200_000, 700_000))
            .put("NO", new Point(300_000, 700_000))

            .put("NR", new Point(100_000, 600_000))
            .put("NS", new Point(200_000, 600_000))
            .put("NT", new Point(300_000, 600_000))
            .put("NU", new Point(400_000, 600_000))

            .put("NW", new Point(100_000, 500_000))
            .put("NX", new Point(200_000, 500_000))
            .put("NY", new Point(300_000, 500_000))
            .put("NZ", new Point(400_000, 500_000))
            .put("OV", new Point(500_000, 500_000))

            .put("SD", new Point(300_000, 400_000))
            .put("SE", new Point(400_000, 400_000))
            .put("TA", new Point(500_000, 400_000))

            .put("SH", new Point(200_000, 300_000))
            .put("SJ", new Point(300_000, 300_000))
            .put("SK", new Point(400_000, 300_000))
            .put("TF", new Point(500_000, 300_000))
            .put("TG", new Point(600_000, 300_000))

            .put("SM", new Point(100_000, 200_000))
            .put("SN", new Point(200_000, 200_000))
            .put("SO", new Point(300_000, 200_000))
            .put("SP", new Point(400_000, 200_000))
            .put("TL", new Point(500_000, 200_000))
            .put("TM", new Point(600_000, 200_000))

            .put("SR", new Point(100_000, 100_000))
            .put("SS", new Point(200_000, 100_000))
            .put("ST", new Point(300_000, 100_000))
            .put("SU", new Point(400_000, 100_000))
            .put("TQ", new Point(500_000, 100_000))
            .put("TR", new Point(600_000, 100_000))

            .put("SV", new Point(      0, 0))
            .put("SW", new Point(100_000, 0))
            .put("SX", new Point(200_000, 0))
            .put("SY", new Point(300_000, 0))
            .put("SZ", new Point(400_000, 0))
            .put("TV", new Point(500_000, 0))

            .build();

    private final int LENGTH = 100_000;

    /**
     * For all minecraft regions contained in specified square, add the region to a zip file.
     * We include the level.dat file in each zip so that they can be played independently
     */
    private void zipRegionFiles(final String levelName, final String gridSquare) {
        try {
            final Point lowerLeft = bounds.get(gridSquare.toUpperCase());
            if (lowerLeft == null) {
                System.out.println("Unable to find bounds for square " + gridSquare);
                return;
            }

            System.out.println("Archiving square " + gridSquare);

            final Set<String> regionFilesInZip = new HashSet<>();
            final FileManager fileManager = new FileManager(levelName, true);
            final FileSystem zipFile = getZipFile(fileManager, levelName, gridSquare);

            // Iterate over the OS grid square and get the region file at each point. Copy the region file
            // into the zip.
            for (int x = lowerLeft.x; x <= lowerLeft.x + LENGTH; x += Region.BLOCKS_PER_REGION_SIDE - 20) {
                for (int y = lowerLeft.y; y <= lowerLeft.y + LENGTH; y += Region.BLOCKS_PER_REGION_SIDE - 20) {
                    final Path regionFile = fileManager.getRegionFileForBlock(x, CoordConverter.convert(y));
                    if (Files.exists(regionFile) && !regionFilesInZip.contains(regionFile.getFileName().toString())) {
                        copyRegionFileToZip(zipFile, levelName, regionFile);
                        regionFilesInZip.add(regionFile.getFileName().toString());
                    }
                }
            }

            // Include the level file in every zip so they are independently playable
            addLevelFileToZip(fileManager, levelName, zipFile);

            zipFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addLevelFileToZip(final FileManager fileManager, final String levelName, final FileSystem zipFile) throws IOException {
        final Path pathInZipfile = zipFile.getPath("/" + levelName + "/level.dat");
        Files.copy(fileManager.getLevelDir().resolve("level.dat") , pathInZipfile, StandardCopyOption.REPLACE_EXISTING );
    }

    private void copyRegionFileToZip(final FileSystem zipFile, final String levelName, final Path regionFile) throws IOException {
        final Path pathInZipfile = zipFile.getPath("/" + levelName + "/region/" + regionFile.getFileName().toString());
        Files.copy(regionFile, pathInZipfile, StandardCopyOption.REPLACE_EXISTING);
    }

    private FileSystem getZipFile(final FileManager fileManager, final String levelName, final String gridSquare) throws IOException {
        final Path zipFilePath = fileManager.getWorldDir().resolve(gridSquare + ".zip");
        Map<String, Object> env = ImmutableMap.of("create", "true", "useTempFile", Boolean.TRUE);
        final URI uri = URI.create("jar:" + zipFilePath.toUri());

        final FileSystem zipFile = FileSystems.newFileSystem(uri, env);

        try {
            Files.createDirectories(zipFile.getPath("/" + levelName + "/region"));
        } catch (FileAlreadyExistsException e) {
            // ignore
        }
        return zipFile;
    }
}
