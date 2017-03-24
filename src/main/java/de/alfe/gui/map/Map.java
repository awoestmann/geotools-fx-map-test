/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.alfe.gui.map;

/**
 * @author Jochen Saalfeld (jochen@intevation.de)
 */

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Translate;
import org.geotools.data.DataUtilities;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.xml.Extent;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.request.GetFeatureInfoRequest;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.data.wms.response.GetFeatureInfoResponse;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.GeneralDirectPosition;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.map.MapViewport;
import org.geotools.map.WMSLayer;
import org.geotools.ows.ServiceException;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import org.jfree.fx.FXGraphics2D;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * This class is going to Manage the Display of a Map based on a WFS Service.
 * It should have some widgets to zoom and to draw a Bounding Box.
 */
public class Map extends Parent {

    //http://docs.geotools.org/latest/userguide/tutorial/raster/image.html
    //https://github.com/rafalrusin/geotools-fx-test/blob/master/src/geotools
    // /fx/test/GeotoolsFxTest.java
    private String serviceURL;
    private String serviceName;
    private int dimensionX;
    private int dimensionY;
    private static final String FORMAT = "image/png";
    private static final boolean TRANSPARACY = true;
    private static final String INIT_SPACIAL_REF_SYS = "EPSG:4326";
    private static final int INIT_LAYER_NUMBER = 0;
    private static final String POLYGON_LAYER_TITLE = "polygon-layer";
    WebMapServer wms;
    Layer displayLayer;
    private static final Logger log
            = Logger.getLogger(Map.class.getName());
    private WMSCapabilities capabilities;
    private List layers;
    private VBox vBox;
    private Label sourceLabel;

    private GeneralEnvelope layerBBox;
    private GeneralEnvelope maxBBox;

    private TextField epsgField;
    private Button updateImageButton;

    private int markerCount;

    private double mouseXPosOnClick;
    private double mouseYPosOnClick;
    private double lastMouseXPos;
    private double lastMouseYPos;

    private double previousMouseXPosOnClick;
    private double previousMouseYPosOnClick;

    private static final double DRAGGING_OFFSET = 4;
    private static final double HUNDRED = 100d;

    private static final double INITIAL_EXTEND_X1 = 850028;
    private static final double INITIAL_EXTEND_Y1 = 6560409;
    private static final double INITIAL_EXTEND_X2 = 1681693;
    private static final double INITIAL_EXTEND_Y2 = 5977713;
    private final double RELATIVE_MAP_MARGIN = 0.99;

    DefaultFeatureCollection polygonFeatureCollection;

    private static final double TEN_PERCENT_OF = 0.01d;

    private static String WPSG_WGS84 = "EPSG:4326";
    private static String WGS84 = "4326";
    private static String POLYGON_NAME = "polygon";
    private Group boxGroup;
    private AffineTransform screenToWorld;
    private AffineTransform worldToScreen;
	private CoordinateReferenceSystem crs;

    private int zoomLevel;
    private int lastZoomLevel;
    private Timer zoomTimer;
    private TimerTask zoomTask;
    private final int ZOOM_TIMEOUT = 150;
    private final int MAX_ZOOM_LEVEL = 100;
    private final double ZOOM_FACTOR = 1.5;

	private double aspectXY;
    private Rectangle2D imageViewport;
    private MapContent mapContent;
    private GraphicsContext gc;
    private Canvas mapCanvas;
    private ScrollPane mapPane;
    private StreamingRenderer renderer;
    private FXGraphics2D graphics;

    private String selectedPolygonName;
    private String selectedPolygonID;
    private GeometryDescriptor geomDesc;
    private String geometryAttributeName;
    private String source;
    private FilterFactory2 ff;
    private StyleFactory sf;
    private StyleBuilder sb;

    private static final Color OUTLINE_COLOR = Color.BLACK;
    private static final Color SELECTED_COLOUR = Color.YELLOW;
    private static final Color FILL_COLOR = Color.CYAN;
    private static final Float OUTLINE_WIDTH = 0.3f;
    private static final Float FILL_TRANSPARACY = 0.4f;
    private static final Float STROKY_TRANSPARACY = 0.8f;

    /**
     * gets the children of this node.
     * @return the children of the node
     */
    @Override
    public ObservableList getChildren() {
        return super.getChildren();
    }

    /**
     * adds a node to this map.
     * @param n the node
     */
    public void add(Node n) {
        this.vBox.getChildren().remove(n);
        this.vBox.getChildren().add(n);
    }

    /**
     * Constructor
     * @param wms WMS server
     * @param layer Layer containing map
     * @param dimensionX Map width
     * @param dimensionY map height
     * @param bounds Bounding box
     */
    public Map(WebMapServer wms, Layer layer, int dimensionX, int dimensionY, org.opengis.geometry.Envelope bounds){
            System.setProperty("org.geotools.referencing.forceXY", "true");
            mapCanvas = new Canvas(dimensionX, dimensionY);
            gc = mapCanvas.getGraphicsContext2D();
            zoomLevel = 0;
            lastZoomLevel = 0;
            GeneralEnvelope layerBounds = null;
            try{
				this.crs = CRS.decode(this.INIT_SPACIAL_REF_SYS);
                layerBounds = layer.getEnvelope(crs);
            }catch (NoSuchAuthorityCodeException nsa) {
                System.out.println("Unknown auth code: " + this.INIT_SPACIAL_REF_SYS);
            }
            catch (FactoryException f) {
                System.out.println("Factory Exception");
            }
            System.out.println("Layer bounds: " + layerBounds);
            //this.layerBBox = new GeneralEnvelope(new ReferencedEnvelope(-85, 85, -175, 175, this.crs));
			//this.layerBBox = new GeneralEnvelope(new ReferencedEnvelope(-22, 50, 31, 67, this.crs));
            this.layerBBox = new GeneralEnvelope(bounds);
            this.layerBBox.setCoordinateReferenceSystem(this.crs);
            this.maxBBox = layerBBox;


            this.updateImageButton = new Button("Reset");
            Button resizeButton = new Button("resize");
            this.vBox = new VBox();
            HBox hBox = new HBox();
            this.wms = wms;
            this.displayLayer = layer;
            this.dimensionX = dimensionX;
            this.dimensionY = dimensionY;

            layers = new ArrayList<Layer>(0);
            layers.add(layer);

            WMSLayer wmsLayer = new WMSLayer(wms, displayLayer);
            this.mapContent = new MapContent();
            this.mapContent.addLayer(wmsLayer);
            this.mapContent.getViewport().setCoordinateReferenceSystem(crs);
            this.mapContent.getViewport().setBounds(new ReferencedEnvelope(layerBBox));

            updateImageButton.setOnAction(new EventHandler<ActionEvent>(){
                @Override
                public void handle(ActionEvent e){
                    setExtent(new ReferencedEnvelope(layerBBox));
                }
            });

            resizeButton.setOnAction(new EventHandler<ActionEvent>(){
                @Override
                public void handle(ActionEvent e){
                    resize(1200, 800);
                }
            });

            this.epsgField = new TextField(this.INIT_SPACIAL_REF_SYS);
            hBox.getChildren().add(updateImageButton);
            hBox.getChildren().add(epsgField);
            hBox.getChildren().add(resizeButton);

            this.getChildren().add(vBox);
            mapPane = new ScrollPane();
            mapPane.setMaxSize(dimensionX, dimensionY);
            mapPane.setPrefSize(dimensionX, dimensionY);
            mapPane.setContent(mapCanvas);
            mapPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            mapPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

            this.vBox.getChildren().add(mapPane);

            this.mapCanvas.addEventHandler(MouseEvent.MOUSE_RELEASED, new
                OnMouseReleasedEvent());
            this.mapCanvas.addEventHandler(MouseEvent.MOUSE_CLICKED, new
                OnMousePressedEvent());
            this.mapCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, new
                OnMousePressedEvent());
            this.mapCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, new
                OnMouseDraggedEvent());
            this.mapCanvas.addEventHandler(ScrollEvent.SCROLL, new
                OnMouseScrollEvent());

            zoomTimer = new Timer(true);
            repaint();
            refreshViewport();
            System.out.println(wmsLayer.getLastGetMap().getFinalURL());
    }

    /**
     * Clears all drawn shapes
     */
    public void clearShapes() {
        ArrayList<Object> list = new ArrayList<Object>(1);
        list.add(this.vBox);
        this.getChildren().retainAll(list);
        markerCount = 0;
    }

    /**
     * Resize map
     * @param width New width
     * @param height New height
     */
    public void resize(double width, double height){
        this.mapCanvas.setWidth(width);
        this.mapCanvas.setHeight(height);
        this.dimensionX = (int) width;
        this.dimensionY = (int)height;
        repaint();
    }

    /**
     * Repaint the map using Geotools StreamingRenderer and MapContent
     */
    public void repaint() {
        renderer = new StreamingRenderer();
        renderer.setMapContent(mapContent);
        graphics = new FXGraphics2D(this.gc);
        graphics.setBackground(java.awt.Color.BLACK);
        gc.clearRect(0, 0, dimensionX, dimensionY);
        Rectangle rectangle = new Rectangle(dimensionX, dimensionY);
        renderer.paint(graphics, rectangle, mapContent.getViewport().getBounds());
        Platform.runLater(() -> {
            clearShapes();
        });
    }

    /**
     * Recalculate screen-to-world and world-to-screen transformations
     */
    private void refreshViewport(){
        this.mapContent.getViewport().setScreenArea(new Rectangle(dimensionX, dimensionY));
        screenToWorld = mapContent.getViewport().getScreenToWorld();
        worldToScreen = mapContent.getViewport().getWorldToScreen();
    }

    /**
     * Reset extent to initial value
     */
    public void resetExtent(){
        setExtent(new ReferencedEnvelope(this.layerBBox));
    }

    /**
     * Set Extent
     * @param newExtent the new Extent
     */
    public void setExtent(ReferencedEnvelope newExtent){
        this.mapContent.getViewport().setBounds(newExtent);
        //refreshViewport();
        repaint();

    }

    /**
     * Adds a layer to the map and repaints
     * @param layer The new layer
     */
    public void addLayer(org.geotools.map.Layer layer){
        if(layer != null){
            this.mapContent.addLayer(layer);
            repaint();
        }
    }

    /**
     * Set the used coordinate reference system
     * @param crs The new crs
     */
    public void setMapCRS(CoordinateReferenceSystem crs){
        this.mapContent.getViewport().setCoordinateReferenceSystem(crs);
        try{
            this.maxBBox = new GeneralEnvelope(new ReferencedEnvelope(maxBBox).transform(crs, true));
            this.layerBBox = new GeneralEnvelope(new ReferencedEnvelope(layerBBox).transform(crs, true));
        } catch (Exception tEx) {
            System.out.println(tEx);
        }
        //this.layerBBox = this.displayLayer.getEnvelope(crs);
        //this.maxBBox = this.layerBBox;
        this.crs = crs;
        //refreshViewport();
        repaint();
    }

    /**
     * Transform a point from screen to world coordinates.
     * @param screenPoint Point in screen coordinates
     * @return Transformed point
     */
	public Point2D.Double transformScreenToWorld(Point2D.Double screenPoint) {
		Point2D.Double worldPoint = new Point2D.Double();
		//AffineTransform.getRotateInstance(java.lang.Math.PI, dimensionX/2, dimensionY/2)
			//.transform(screenPoint, worldPoint);
		this.mapContent.getViewport().getScreenToWorld().transform(screenPoint, worldPoint);
		return worldPoint;
	}

    /**
     * Transform a point from world to screen coordinates.
     * @param worldPoint Point in world coordinates
     * @return Transformed point
     */
	public Point2D.Double transformWorldToScreen(Point2D.Double worldPoint) {
		Point2D.Double screenPoint = new Point2D.Double();
		this.mapContent.getViewport().getWorldToScreen().transform(worldPoint, screenPoint);
		//AffineTransform.getRotateInstance(java.lang.Math.PI, dimensionX/2, dimensionY/2)
			//.transform(screenPoint, screenPoint);
		return screenPoint;
	}

    /**
     * Zooms in/out.
     * TODO: center zoom on mouse position
     * @param zoomDelta 
     * @param x Mouse position x
     * @param y Mouse position y
     */
    private void zoom(int zoomDelta, double x, double y) {
        if(zoomDelta == 0) {
            return;
        }
        Point2D.Double lower;
        Point2D.Double upper;
        double newZoom = ZOOM_FACTOR * zoomDelta;
        ReferencedEnvelope newBounds;


        if(zoomDelta > 0) {
            lower = new Point2D.Double((dimensionX / 2) - (0.5 * dimensionX / newZoom),
                    (dimensionY / 2) - (0.5 * dimensionY / newZoom));
            upper = new Point2D.Double((dimensionX / 2) + (0.5 * dimensionX / newZoom),
                    (dimensionY / 2) + (0.5  * dimensionY/newZoom));
        }
        else {
            newZoom *= -1;
            lower = new Point2D.Double((dimensionX / 2) - (0.5 * dimensionX * newZoom),
                    (dimensionY / 2) - (0.5 * dimensionY * newZoom));
            upper = new Point2D.Double((dimensionX / 2) + (0.5 * dimensionX * newZoom),
                    (dimensionY / 2) + (0.5  * dimensionY * newZoom));
        }
        lower = transformScreenToWorld(lower);
        upper = transformScreenToWorld(upper);
        newBounds = new ReferencedEnvelope(lower.getX(), upper.getX(), lower.getY(), upper.getY(), this.crs);

        setExtent(newBounds);
    }

    /**
     * Drags the map.
     * @param fromXScreen Original x coordinate in screen coordinates
     * @param fromYScreen Original y coordinate in screen coordinates
     * @param toXScreen Target x coordinate in screen coordinates
     * @param toYScreen Target y coordinate in screen coordinates
     */
    private void drag(double fromXScreen, double fromYScreen, double toXScreen, double toYScreen) {
        System.out.println("Do drag");
        mapCanvas.setTranslateX(0);
        mapCanvas.setTranslateY(0);
        Point2D.Double from = new Point2D.Double(fromYScreen, fromXScreen);
        Point2D.Double to = new Point2D.Double(toYScreen, toXScreen);
		
		from = transformScreenToWorld(from);
		to = transformScreenToWorld(to);
		
        double fromX = from.getX();
        double fromY = from.getY();
        double toX = to.getX();
        double toY = to.getY();

        double xOffset = (toX - fromX);// * ZOOM_FACTOR;
        double yOffset = (toY - fromY);// * ZOOM_FACTOR;
        ReferencedEnvelope bBox = this.mapContent.getViewport().getBounds();

        //ReferencedEnvelope newBounds = new ReferencedEnvelope(
        //    bBox.getMinX() + yOffset,
        //    bBox.getMaxX() + yOffset,
        //    bBox.getMinY() + xOffset,
        //    bBox.getMaxY() + xOffset,
        //    this.crs);

        Point2D.Double minXY = transformScreenToWorld(new Point2D.Double(0 - toXScreen, 0 - toYScreen));
        Point2D.Double maxXY = transformScreenToWorld(new Point2D.Double(dimensionX - toXScreen, dimensionY - toYScreen));

        ReferencedEnvelope newBounds = new ReferencedEnvelope(
            minXY.getX(), maxXY.getX(),
            minXY.getY(), maxXY.getY(), this.crs);
        //TODO: Prevent exceeding max coordinate bounds
        if(!maxBBox.contains(newBounds, true)){
            System.out.println("Dragging out of bounds");
            System.out.println(newBounds + " > " + maxBBox);
            /*
            double lowerX = newBounds.getLowerCorner().getOrdinate(0);
            double lowerY = newBounds.getLowerCorner().getOrdinate(1);
            double upperX = newBounds.getUpperCorner().getOrdinate(0);
            double upperY = newBounds.getUpperCorner().getOrdinate(1);

            if(lowerX < maxBBox.getLowerCorner().getOrdinate(0)) {
                System.out.println("Bottom");
                lowerX = maxBBox.getLowerCorner().getOrdinate(0) ;
            }
            if(lowerY < maxBBox.getLowerCorner().getOrdinate(1)) {
                System.out.println("Left");
                lowerY = maxBBox.getLowerCorner().getOrdinate(1) ;
            }
            if(upperX > maxBBox.getUpperCorner().getOrdinate(0)) {
                System.out.println("Top");
                upperX = maxBBox.getUpperCorner().getOrdinate(0) ;
            }
            if(upperY > maxBBox.getUpperCorner().getOrdinate(1)) {
                System.out.println("Right");
                upperY = maxBBox.getUpperCorner().getOrdinate(1) ;
            }
            newBounds = new ReferencedEnvelope(lowerX, lowerY, upperX, upperY, this.crs);*/
            newBounds = bBox;
        }

        setExtent(newBounds);
    }

    /**
     * Draws a marker on a speficic position.
     * @param xPosition Marker x coordinate in screen coordinates
     * @param yPosition Marker y coordinate in screen coordinates
     */
    public void drawMarker(double xPosition, double yPosition) {
        double markerSpan = this.mapCanvas.getWidth() / HUNDRED;
        double upperLeftX = xPosition - markerSpan;
        double upperLeftY = yPosition + markerSpan;
        double upperRightX = xPosition + markerSpan;
        double upperRightY = yPosition + markerSpan;
        double lowerLeftX = xPosition - markerSpan;
        double lowerLeftY = yPosition - markerSpan;
        double lowerRightX = xPosition + markerSpan;
        double lowerRightY = yPosition - markerSpan;
        Line upperLeftToLowerRight = new Line(upperLeftX, upperLeftY,
                lowerRightX, lowerRightY);
        Line upperRightToLowerLeft = new Line(upperRightX, upperRightY,
                lowerLeftX, lowerLeftY);
        upperLeftToLowerRight.setFill(null);
        upperLeftToLowerRight.setStroke(Color.RED);
        upperLeftToLowerRight.setStrokeWidth(2);
        upperLeftToLowerRight.setVisible(true);
        upperRightToLowerLeft.setFill(null);
        upperRightToLowerLeft.setStroke(Color.RED);
        upperRightToLowerLeft.setStrokeWidth(2);
        upperRightToLowerLeft.setVisible(true);
        this.getChildren().add(upperLeftToLowerRight);
        this.getChildren().add(upperRightToLowerLeft);
    }

    /**
     * Draws a box, defined by two corners.
     * @param beginX First corner x coordinate in screen coordinates
     * @param beginY First corner y coordinate in screen coordinates
     * @param endX Second corner x coordinate in screen coordinates
     * @param endY Second corner y coordinate in screen coordinates
     */
    private void drawBox(double beginX, double beginY, double endX, double
            endY) {
        Line upperLine = new Line(beginX, beginY, endX, beginY);
        upperLine.setFill(null);
        upperLine.setStroke(Color.RED);
        upperLine.setStrokeWidth(2);
        this.getChildren().add(upperLine);

        Line leftLine = new Line(beginX, beginY, beginX, endY);
        leftLine.setFill(null);
        leftLine.setStroke(Color.RED);
        leftLine.setStrokeWidth(2);
        this.getChildren().add(leftLine);

        Line buttomLine = new Line(beginX, endY, endX, endY);
        buttomLine.setFill(null);
        buttomLine.setStroke(Color.RED);
        buttomLine.setStrokeWidth(2);
        this.getChildren().add(buttomLine);

        Line rightLine = new Line(endX, beginY , endX, endY);
        rightLine.setFill(null);
        rightLine.setStroke(Color.RED);
        rightLine.setStrokeWidth(2);
        this.getChildren().add(rightLine);
    }

    /** Eventhandler for mouse events on map. */
    private class OnMousePressedEvent
            implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent e) {
            //WHEN ON SAME X,Y SET MARKER, WEHN MARKER SET, MAKE BBBOX, WHEN
            //ON DIFFERENT, MOVE MAP. WHEN DOUBLE LEFT-CLICKED, ZOOM IN, WHEN
            //DOUBLE RIGHT, ZOOM OUT
            if (e.getButton().equals(MouseButton.PRIMARY)) {
                if (e.getClickCount() > 1) {
                    zoom(10, 0,0);
                }
                if (e.getClickCount() == 1) {
                    mouseXPosOnClick = e.getSceneX();
                    mouseYPosOnClick = e.getSceneY();
                    lastMouseXPos = mouseXPosOnClick;
                    lastMouseYPos = mouseYPosOnClick;
                }
            }
            if (e.getButton().equals(MouseButton.SECONDARY)) {
                if (e.getClickCount() > 1) {
                    zoom(-10,0,0);

                }
                if (e.getClickCount() == 1) {
                    boxGroup.getChildren().clear();
                }
            }
            Point2D clickWorld = transformScreenToWorld(new Point2D.Double(e.getSceneX(), e.getSceneY()));
            System.out.println("Clicked: " + e.getSceneX() + " - " + e.getSceneY() + " ; " + clickWorld);

        }
    }

    /** Eventhandler for mouse events on map. */
    private class OnMouseReleasedEvent
            implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent e) {
            //SAVE STATES WHEN MOUSE IS RELEASED TO DETERMINE IF DRAGGED OR
            //IF MARKER WAS SET
            if (e.getX() < (mouseXPosOnClick + DRAGGING_OFFSET)
                    && e.getX() > (mouseXPosOnClick - DRAGGING_OFFSET)
                    && e.getY() < (mouseYPosOnClick + DRAGGING_OFFSET)
                    && e.getY() > (mouseYPosOnClick - DRAGGING_OFFSET)) {
                drawMarker(mouseXPosOnClick, mouseYPosOnClick);
                markerCount++;
                                if (markerCount == 2) {
                    if (mouseXPosOnClick > previousMouseXPosOnClick) {
                        drawBox(mouseXPosOnClick, mouseYPosOnClick,
                                previousMouseXPosOnClick,
                                previousMouseYPosOnClick);
                    } else {
                        drawBox(previousMouseXPosOnClick,
                                previousMouseYPosOnClick, mouseXPosOnClick,
                                mouseYPosOnClick);
                    }
                } else if (markerCount > 2) {
                    markerCount = 0;
                }
                previousMouseXPosOnClick = mouseXPosOnClick;
                previousMouseYPosOnClick = mouseYPosOnClick;
            } else {
                //drag(mouseXPosOnClick, mouseYPosOnClick, e.getX(), e.getY());
                drag(0, 0, mapCanvas.getTranslateX(), mapCanvas.getTranslateY());
                markerCount = 0;
            }
        }
    }

    /** Eventhandler for mouse events on map. */
    private class OnMouseScrollEvent
            implements EventHandler<ScrollEvent> {
        @Override
        public void handle(ScrollEvent e) {
            //WHEN SCROLLED IN, ZOOOM IN, WHEN SCROLLED OUT, ZOOM OUT
            if (e.getDeltaY() > 0) {
                zoomLevel++;
            }
            if (e.getDeltaY() < 0) {
                zoomLevel--;
            }
            try{
                zoomTimer.cancel();
            } catch(IllegalStateException ex) {System.out.println(ex);}
            zoomTimer = new Timer(true);
            zoomTask = new TimerTask() {
                public void run() {
                    int zoomDelta = zoomLevel - lastZoomLevel;
                    //if(zoomDelta >= 0) {
                      //    zoomIn(zoomDelta, 0,0);
                    //} else {
                      //  zoomOut(zoomDelta, 0, 0);
                    //}
                    zoom(zoomDelta, e.getX(), e.getY());
                    lastZoomLevel = zoomLevel;
                }
            };
            zoomTimer.schedule(zoomTask, ZOOM_TIMEOUT);

        }
    }

    /** Event handler for dragging events, does not reload the actual map */
    private class OnMouseDraggedEvent
            implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent e){
            double xOffset = lastMouseXPos - e.getSceneX();
            double yOffset = lastMouseYPos - e.getSceneY();
            lastMouseXPos = e.getSceneX();
            lastMouseYPos = e.getSceneY();
            mapCanvas.setTranslateX(mapCanvas.getTranslateX() + (-1 * xOffset));
            mapCanvas.setTranslateY(mapCanvas.getTranslateY() + (-1 * yOffset));
        }
    }

    //Does not work, because the map itself aint an Input-Field
    /*
    private class OnPressedPlusOrMinusEvent
            implements EventHandler<KeyEvent> {
        @Override
        public void handle(KeyEvent e) {
            //WHEN PRESSED PLUS, ZOOOM IN, WHEN PRESSED MINUS, ZOOM OUT
            System.out.println(e.getCharacter());
            System.out.println(e.getCode());
            if (e.getCode() == KeyCode.MINUS) {
                zoomOut();
            }
            if (e.getCode() == KeyCode.PLUS) {
                zoomIn();
            }
        }
    }
    */

    /**
     * Draws WKT formated polygons.
     * @param wktPolygonList WKT formated Polygons
     */
    public void drawStringPolygons(List<String> wktPolygonList) throws
              ParseException, FactoryException, SchemaException {
          WKTReader reader = new WKTReader(new GeometryFactory());
          CoordinateReferenceSystem crs = CRS.decode(WPSG_WGS84);
          List<FeaturePolygon> featurePolygons = new ArrayList<>();
          int i = 1;
          for (String wktPolygon: wktPolygonList) {
              FeaturePolygon fp =
                      new FeaturePolygon( (Polygon) reader.read(wktPolygon),
                              POLYGON_NAME + i,
                              String.valueOf(i),
                              crs);
              featurePolygons.add(fp);
              i++;
          }
          SimpleFeatureType polygonFeatureType = DataUtilities.createType(
                  "Dataset",
                  "geometry:Geometry:srid="
                          + WGS84
                          + ","
                          + "name:String,"
                          + "id:String"
          );
          DefaultFeatureCollection polygonFeatureCollection =
                  new DefaultFeatureCollection("internal",
                          polygonFeatureType);
          GeometryDescriptor geomDesc = polygonFeatureCollection.getSchema()
                  .getGeometryDescriptor();
          String geometryAttributeName = geomDesc.getLocalName();
          for (FeaturePolygon fp : featurePolygons) {
              SimpleFeatureBuilder featureBuilder =
                      new SimpleFeatureBuilder(polygonFeatureType);
              try {
                  MathTransform transform = CRS.findMathTransform(
                          fp.crs, this.mapContent.getViewport().getCoordinateReferenceSystem());
                  featureBuilder.add((Polygon) JTS.transform(fp.polygon,
                          transform));
                  featureBuilder.add(fp.name);
                  featureBuilder.add(fp.id);
                  SimpleFeature feature = featureBuilder.buildFeature(null);
                  polygonFeatureCollection.add(feature);
              } catch (FactoryException | TransformException e) {
                  System.err.println(e);
              }
          }
          MapStyles ms = new MapStyles(geometryAttributeName);
          org.geotools.map.Layer polygonLayer = new FeatureLayer(
                  polygonFeatureCollection, ms.createPolygonStyle());
          polygonLayer.setTitle(POLYGON_LAYER_TITLE);
          List<org.geotools.map.Layer> layers = mapContent.layers();
          for (org.geotools.map.Layer layer : layers) {
              if (layer.getTitle() != null) {
                  if (layer.getTitle().equals(POLYGON_LAYER_TITLE)) {
                      mapContent.removeLayer(layer);
                  }
              }
          }
          addLayer(polygonLayer);
      }

    /**
     * Draws Polygons on the map.
     * @param featurePolygons List of drawable Polygons
     */
    public void drawPolygons(List<FeaturePolygon> featurePolygons) {
        try {

            SimpleFeatureType polygonFeatureType;

            String epsgCode = this
                    .crs
                    .getIdentifiers()
                    .toArray()[0]
                    .toString();
            epsgCode = epsgCode.substring(epsgCode.lastIndexOf(":") + 1,
                    epsgCode.length());
            polygonFeatureType = DataUtilities.createType(
                    "Dataset",
                    "geometry:Geometry:srid="
                            + epsgCode
                            + ","
                            + "name:String,"
                            + "id:String"
            );
            polygonFeatureCollection =
                    new DefaultFeatureCollection("internal",
                            polygonFeatureType);
            geomDesc = polygonFeatureCollection.getSchema()
                    .getGeometryDescriptor();
            geometryAttributeName = geomDesc.getLocalName();

           for (FeaturePolygon fp : featurePolygons) {
                SimpleFeatureBuilder featureBuilder =
                        new SimpleFeatureBuilder(polygonFeatureType);
                try {
                    MathTransform transform = CRS.findMathTransform(
                            fp.crs, this.crs);
                    featureBuilder.add((Polygon) JTS.transform(fp.polygon,
                            transform));
                    featureBuilder.add(fp.name);
                    featureBuilder.add(fp.id);
                    SimpleFeature feature = featureBuilder.buildFeature(null);
                    polygonFeatureCollection.add(feature);
                } catch (FactoryException | TransformException e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
            }
            org.geotools.map.Layer polygonLayer = new FeatureLayer(
                    polygonFeatureCollection, createPolygonStyle());
            polygonLayer.setTitle(POLYGON_LAYER_TITLE);
            List<org.geotools.map.Layer> layers = mapContent.layers();
            for (org.geotools.map.Layer layer : layers) {
                if (layer.getTitle() != null) {
                    if (layer.getTitle().equals(POLYGON_LAYER_TITLE)) {
                        mapContent.removeLayer(layer);
                    }
                }
            }
            mapContent.addLayer(polygonLayer);
        } catch (SchemaException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Creates the polygon style
     */
    private Style createPolygonStyle() {
        Fill fill = sf.createFill(ff.literal(FILL_COLOR),
                ff.literal(FILL_TRANSPARACY));
        Stroke stroke = sf.createStroke(ff.literal(OUTLINE_COLOR),
                ff.literal(OUTLINE_WIDTH),
                ff.literal(STROKY_TRANSPARACY));
        PolygonSymbolizer polygonSymbolizer =
                sf.createPolygonSymbolizer(stroke, fill, null);
        return this.sb.createStyle(polygonSymbolizer);
    }

    public void highlightSelectedPolygon(String s) {

    }

    public static String getEPSGWGS84String(){
        return Map.WPSG_WGS84;
    }
}

