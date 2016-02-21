package org.matthelliwell.minecraftosloader.writer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.morbz.minecraft.blocks.FlowerBlock;
import net.morbz.minecraft.blocks.IBlock;
import net.morbz.minecraft.blocks.Material;
import net.morbz.minecraft.blocks.SandBlock;
import net.morbz.minecraft.blocks.SandstoneBlock;
import net.morbz.minecraft.blocks.SaplingBlock;
import net.morbz.minecraft.blocks.SimpleBlock;
import net.morbz.minecraft.blocks.StainedBlock;
import net.morbz.minecraft.level.FlatGenerator;
import net.morbz.minecraft.level.GameType;
import net.morbz.minecraft.level.Level;
import net.morbz.minecraft.world.DefaultLayers;
import net.morbz.minecraft.world.World;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.matthelliwell.minecraftosloader.feature.BuildingGenerator;
import org.matthelliwell.minecraftosloader.feature.FeatureGrid;
import org.matthelliwell.minecraftosloader.feature.ForeshoreGenerator;
import org.matthelliwell.minecraftosloader.feature.FunctionalSiteGenerator;
import org.matthelliwell.minecraftosloader.feature.GlassHouseGenerator;
import org.matthelliwell.minecraftosloader.feature.HeightGenerator;
import org.matthelliwell.minecraftosloader.feature.HeightGrid;
import org.matthelliwell.minecraftosloader.feature.ImportantBuildingGenerator;
import org.matthelliwell.minecraftosloader.feature.LakeGenerator;
import org.matthelliwell.minecraftosloader.feature.RailwayGenerator;
import org.matthelliwell.minecraftosloader.feature.RoadGenerator;
import org.matthelliwell.minecraftosloader.feature.RoadTunnelGenerator;
import org.matthelliwell.minecraftosloader.feature.StreamGenerator;
import org.matthelliwell.minecraftosloader.feature.TidalWaterGenerator;
import org.matthelliwell.minecraftosloader.feature.WoodlandGenerator;
import org.opengis.referencing.FactoryException;

/**
 * Generates the minecraft world
 */
public class WorldWriter {
    private static final int MAX_GROUND_DEPTH = 20;
    private long blockCount = 0;

    private World world;
    private RailwayWriter railwayWriter = new RailwayWriter();

    public static void main(String[] argv) throws IOException, FactoryException {
        if ( argv.length != 4 ) {
            System.out.println("Usage: org.matthelliwell.minecraftosloader.writer.WorldGenerator <grid square> <region number> <path to OS Terrain 50 data dir> <path to OS Open Map Local>\"");
            System.out.println("eg org.matthelliwell.minecraftosloader.writer.WorldGenerator sj 24 E:\\Users\\matt\\data\\terr50_cesh_gb\\data\\sj E:\\Users\\matt\\data\\opmplc_essh_sj\\OSOpenMapLocal (ESRI Shape File) SJ\\data");
            return;
        }

        new WorldWriter().generate(argv[0], argv[1], argv[2], argv[3]);
    }

    private void generate(final String gridSquare,
                          final String regionNumber,
                          final String terrainDataDir,
                          final String localDataDir) throws IOException, FactoryException {

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

        // Process the heigh related features as we need the height grid before we can do much else
        final HeightGrid heightGrid = new HeightGenerator().generate(terrainDataPath, gridSquare, regionNumber);

        // Create the feature grid into which we'll do the features on the terrain
        final FeatureGrid featureGrid = new FeatureGrid(heightGrid.getBounds());

        // Load functional sites before buildings as building may be inside these so don't want to overwrite the building
        System.out.println("Adding functional sites");
        new FunctionalSiteGenerator(featureGrid).generate(localDataPath, gridSquare);

        System.out.println("Adding roads");
        new RoadGenerator(featureGrid).generate(localDataPath, gridSquare);

        System.out.println("Adding important buildings");
        new ImportantBuildingGenerator(featureGrid).generate(localDataPath, gridSquare);

        System.out.println("Adding buildings");
        new BuildingGenerator(featureGrid).generate(localDataPath, gridSquare);

        System.out.println("Adding road tunnels");
        new RoadTunnelGenerator(featureGrid).generate(localDataPath, gridSquare);

        System.out.println("Adding woodland");
        new WoodlandGenerator(featureGrid).generate(localDataPath, gridSquare);

        System.out.println("Adding lakes");
        new LakeGenerator(featureGrid).generate(localDataPath, gridSquare);

        System.out.println("Adding streams");
        new StreamGenerator(featureGrid).generate(localDataPath, gridSquare);

        System.out.println("Adding glass houses");
        new GlassHouseGenerator(featureGrid).generate(localDataPath, gridSquare);

        System.out.println("Adding tidal area");
        new TidalWaterGenerator(featureGrid).generate(localDataPath, gridSquare);

        System.out.println("Adding foreshore");
        new ForeshoreGenerator(featureGrid).generate(localDataPath, gridSquare);

        System.out.println("Adding railways");
        new RailwayGenerator(featureGrid).generate(localDataPath, gridSquare);

        // We'll surround the map with water and bed rock.
        final DefaultLayers layers = new DefaultLayers();
        layers.setLayer(0, Material.BEDROCK);
        layers.setLayer(1, Material.WATER);

        final Level level = new Level(gridSquare + "-" + regionNumber, new FlatGenerator(layers));
        level.setGameType(GameType.CREATIVE);
        level.setAllowCommands(true);

        world = new World(level, layers);

        System.out.println("Adding blocks to regions");
        final AtomicInteger count = new AtomicInteger(0);
        final int maxCount = heightGrid.getNumCells();
        final HeightScaler scaler = new HeightScaler(heightGrid.getMinHeight(), heightGrid.getMaxHeight());

        final ReferencedEnvelope realBounds = heightGrid.getRealBounds();

        heightGrid.forEach((x, y, h) -> {
            final int scaledHeight = Math.round(scaler.scale(h));
            if ( scaledHeight > 0 ) {
                final List<IBlock> blocks = setBlocksForColumn(x, y, scaledHeight, featureGrid, heightGrid);

                // North is negative Z so we need to change sign of the Z coord being used otherwise left and right
                // will be switched. However J2Blocks, may be due to my change, has a problem with negative coords,
                // creating a wierd artifact like the Berlin wall, so we need to shift the coords to be positive
                world.setBlocks(x, (int) (-y + realBounds.getMinY() + realBounds.getMaxY()), blocks.toArray(new IBlock[]{}));
            }

            count.getAndIncrement();
            if ( count.get() % 10000 == 0 ) {
                System.out.println("Done " + 100.0 * count.get()/maxCount + " %");
            }
        });

        setSpawnPoint(level, heightGrid);

        System.out.println("Saving the world");
        world.save();

        final Date endTime = new Date();
        final long elapsed = (endTime.getTime() - startTime.getTime()) / 1000 / 60;

        System.out.println("Scaling height by " + scaler.getScale() * 100 + "%");
        System.out.println("z = -y + " + (int)(realBounds.getMinY() + realBounds.getMaxY()));
        System.out.println("Written " + blockCount + " blocks in " + elapsed + " minutes");
    }

    private List<IBlock> setBlocksForColumn(final int x, final int y, final int height, final FeatureGrid featureGrid, final HeightGrid heightGrid) {
        final List<IBlock> blocks = new ArrayList<>(height + 2);
        blocks.add(SimpleBlock.BEDROCK);

        for ( int h = 1; h < height - MAX_GROUND_DEPTH; ++h ) {
            blocks.add(SimpleBlock.AIR);
        }

        for ( int h = height - MAX_GROUND_DEPTH; h < height; ++h ) {
            if (h > 0) {
                blocks.add(SimpleBlock.COBBLESTONE);
            }
        }

        switch ( featureGrid.getFeature(x, y) ) {
            case FeatureGrid.GRASS:
                blocks.add(SimpleBlock.GRASS);
                break;
            case FeatureGrid.OAK:
                blocks.add(SimpleBlock.GRASS);
                blocks.add(SaplingBlock.OAK_SAPLING);
                break;
            case FeatureGrid.BIRCH:
                blocks.add(SimpleBlock.GRASS);
                blocks.add(SaplingBlock.BIRCH_SAPLING);
                break;
            case FeatureGrid.DANELION:
                blocks.add(SimpleBlock.GRASS);
                blocks.add(FlowerBlock.DANDELION);
                break;
            case FeatureGrid.POPPY:
                blocks.add(SimpleBlock.GRASS);
                blocks.add(FlowerBlock.POPPY);
                break;
            case FeatureGrid.OXEYE_DAISY:
                blocks.add(SimpleBlock.GRASS);
                blocks.add(FlowerBlock.OXEYE_DAISY);
                break;
            case FeatureGrid.LAKE:
                // TODO add some depth to the lakes
                blocks.add(SimpleBlock.WATER);
                break;
            case FeatureGrid.BUILDING:
                // We don't know how high building are so we'll just do them a few blocks high in a uniform colour
                // on a cobblestone foundation
                blocks.add(SimpleBlock.COBBLESTONE);
                blocks.add(SimpleBlock.BRICK_BLOCK);
                blocks.add(SimpleBlock.BRICK_BLOCK);
                blocks.add(SimpleBlock.BRICK_BLOCK);
                blocks.add(SimpleBlock.BRICK_BLOCK);
                blocks.add(SimpleBlock.HARDENED_CLAY);
                break;
            case FeatureGrid.FUNCTIONAL_SITE:
                // Pave the site in cobblestones
                blocks.add(SimpleBlock.COBBLESTONE);
                break;
            case FeatureGrid.IMPORTANT_BUILDING:
                // Build these in sandstone and higher than normal buildings
                blocks.add(SimpleBlock.COBBLESTONE);
                blocks.add(SandstoneBlock.CHISELED);
                blocks.add(SandstoneBlock.CHISELED);
                blocks.add(SandstoneBlock.CHISELED);
                blocks.add(SandstoneBlock.CHISELED);
                blocks.add(SandstoneBlock.CHISELED);
                blocks.add(SandstoneBlock.CHISELED);
                blocks.add(SandstoneBlock.CHISELED);
                blocks.add(SandstoneBlock.SMOOTH);
                break;
            case FeatureGrid.ROAD:
                // Pave the site in clay
                blocks.add(new StainedBlock(StainedBlock.StainedMaterial.CLAY, StainedBlock.StainedColor.GRAY));
                break;
            case FeatureGrid.GLASSHOUSE:
                blocks.add(SimpleBlock.COBBLESTONE);
                blocks.add(SimpleBlock.GLASS);
                blocks.add(SimpleBlock.GLASS);
                blocks.add(SimpleBlock.GLASS);
                break;
            case FeatureGrid.TIDAL_WATER:
                blocks.add(SimpleBlock.WATER);
                break;
            case FeatureGrid.FORESHORE:
                blocks.add(SandBlock.SAND);
                break;
            case FeatureGrid.RAILWAY:
                blocks.add(SimpleBlock.GRAVEL);
                blocks.add(railwayWriter.write(x, y, featureGrid, heightGrid));
                break;
            default:
                System.out.println("Unknown feature " + featureGrid.getFeature(x, y));
                break;

        }

        blockCount += blocks.size();
        return blocks;
    }

    private void setSpawnPoint(final Level level, final HeightGrid heightGrid) {
        final int midX = heightGrid.getMinX() + (heightGrid.getMaxX() - heightGrid.getMinX()) / 2;
        final int midY = heightGrid.getMinY() + (heightGrid.getMaxY() - heightGrid.getMinY()) / 2;
        level.setSpawnPoint(midX, 100, -midY + heightGrid.getMaxY() + heightGrid.getMinY());
    }
}
