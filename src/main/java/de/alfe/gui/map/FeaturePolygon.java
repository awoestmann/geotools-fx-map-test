package de.alfe.gui.map;

import com.vividsolutions.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Created by Jochen Saalfeld <jochen.saalfeld@intevation.de> on 2/16/17.
 */
public class FeaturePolygon {
    /**
     * the polygon.
     **/
    public Polygon polygon;
    /**
     * name of the polygon.
     **/
    public String name;
    /**
     * id of the polygon.
     **/
    public String id;

    /**
     * crs of the polygon.
     */
    public CoordinateReferenceSystem crs;

    /**
     * Constructor.
     **/
    public FeaturePolygon(Polygon polygon,
                          String name,
                          String id,
                          CoordinateReferenceSystem crs) {
        this.polygon = polygon;
        this.name = name;
        this.id = id;
        this.crs = crs;
    }
}