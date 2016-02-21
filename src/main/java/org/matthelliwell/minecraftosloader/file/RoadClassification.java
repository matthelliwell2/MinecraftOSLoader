package org.matthelliwell.minecraftosloader.file;

public enum RoadClassification {


    MOTORWAY("Motorway", 33.0f),
    A_ROAD("A Road", 6.0f),
    B_ROAD("B Road", 6.0f),
    MINOR_ROAD("Minor Road", 6.0f),
    PEDESTRIANISED_STREET("Pedestrianised Street", 6.0f),
    LOCAL_STREET("Local Street", 6.0f),
    PRIMARY_ROAD("Primary Road", 7.5f),
    PRIVATE_ROAD("Private Road Publicly Accessible", 4.5f),

    MOTORWAY_CDC("Motorway, Collapsed Dual Carriageway", 33.0f),
    PRIMARY_ROAD_CDC("Primary Road, Collapsed Dual Carriageway", 7.5f),
    A_ROAD_CDC("A Road, Collapsed Dual Carriageway", 6.0f),
    B_ROAD_CDC("B Road, Collapsed Dual Carriageway", 6.0f),
    MINOR_ROAD_CDC("Minor Road, Collapsed Dual Carriageway", 6.0f);

    RoadClassification(final String name, final float width) {
        this.name = name;
        this.width = width;
    }

    public static RoadClassification fromValue(final String value) {
        for ( final RoadClassification classification: RoadClassification.values()) {
            if ( classification.name.equals(value)) {
                return classification;
            }
        }

        System.out.println("Unknown classification " + value);
        return B_ROAD;
    }

    public float getWidth() {
        return width;
    }

    private String name;
    private float width;
}
