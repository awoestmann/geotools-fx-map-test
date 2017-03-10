/*
 * DownloadClient Geodateninfrastruktur Bayern
 *
 * (c) 2016 GSt. GDI-BY (gdi.bayern.de)
 *
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
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
import org.geotools.data.wms.xml.Dimension;
import org.geotools.data.wms.xml.Extent;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.request.GetFeatureInfoRequest;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.data.wms.response.GetFeatureInfoResponse;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
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
    private String outerBBOX;
    private String serviceURL;
    private String serviceName;
    private int dimensionX;
    private int dimensionY;
    private static final String FORMAT = "image/png";
    private static final boolean TRANSPARACY = true;
    private static final String INIT_SPACIAL_REF_SYS = "EPSG:4326";
    private static final int INIT_LAYER_NUMBER = 0;
    private static final String POLYGON_LAYER_TITLE = "polygon-layer";
    private String spacialRefSystem;
    WebMapServer wms;
    Layer displayLayer;
    private static final Logger log
            = Logger.getLogger(Map.class.getName());
    private WMSCapabilities capabilities;
    private List layers;
    private VBox vBox;
    private Label sourceLabel;
    private ImageView iw;
    private Group ig;

    private GeneralEnvelope layerBBox;

    private TextField epsgField;
    private TextField boundingBoxField;
    private Button updateImageButton;

    private int markerCount;

    private double mouseXPosOnClick;
    private double mouseYPosOnClick;

    private double previousMouseXPosOnClick;
    private double previousMouseYPosOnClick;

    private static final double DRAGGING_OFFSET = 4;
    private static final double ZOOM_FACTOR = 1.5d;
    private static final double HUNDRED = 100d;

    private static final double INITIAL_EXTEND_X1 = 850028;
    private static final double INITIAL_EXTEND_Y1 = 6560409;
    private static final double INITIAL_EXTEND_X2 = 1681693;
    private static final double INITIAL_EXTEND_Y2 = 5977713;

    DefaultFeatureCollection polygonFeatureCollection;

    private static final double TEN_PERCENT_OF = 0.01d;

    private static String WPSG_WGS84 = "EPSG:4326";
    private static String WGS84 = "4326";
    private static String POLYGON_NAME = "polygon";
    private Group boxGroup;
    private AffineTransform screenToWorld;
    private AffineTransform worldToScreen;
	private CoordinateReferenceSystem crs;

	private double aspectXY;
    private Rectangle2D imageViewport;
    private MapContent mapContent;
    private GraphicsContext gc;
    private Canvas mapCanvas;

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

    public Map(WebMapServer wms, Layer layer, int dimensionX, int dimensionY){
            mapCanvas = new Canvas(dimensionX, dimensionY);
            gc = mapCanvas.getGraphicsContext2D();
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
            this.layerBBox = layerBounds;
			
            this.outerBBOX = layerBounds.getLowerCorner().getOrdinate(0) + ","
                + layerBounds.getLowerCorner().getOrdinate(1) + ","
                + layerBounds.getUpperCorner().getOrdinate(0) + ","
                + layerBounds.getUpperCorner().getOrdinate(1);
			System.out.println(outerBBOX);
            this.spacialRefSystem = INIT_SPACIAL_REF_SYS;
            this.iw = new ImageView();
            this.epsgField = new TextField(this.spacialRefSystem);
            this.boundingBoxField = new TextField(this.outerBBOX);
            this.updateImageButton = new Button("Update Image");
            this.vBox = new VBox();
            this.wms = wms;
            this.displayLayer = layer;
            this.dimensionX = dimensionX;
            this.dimensionY = dimensionY;

            layers = new ArrayList<Layer>(0);
            layers.add(layer);
    	    this.mapContent = new MapContent();
            this.mapContent.addLayer(new WMSLayer(wms, displayLayer));
            this.ig = new Group();
            boxGroup = new Group();

            System.out.println(wms.getCapabilities().getLayerList());

            sourceLabel = new Label(this.serviceName);
            sourceLabel.setLabelFor(this.ig);

            this.getChildren().add(mapCanvas);
            this.setMapImage(this.outerBBOX,
                    this.INIT_SPACIAL_REF_SYS,
                    layers.size() - 1);

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
    }

    /**
     * Constructor.
     */
    public Map() {
    }

    /**
     * sets the Map Image.
     * @param bBox the Bounding Box
     * @param spacialRefSys The EPSG of the Bounding Box
     * @param layerNumber The number of the Layer
     */
    private void setMapImage(String bBox,
                             String spacialRefSys,
                             int layerNumber) {
        System.out.println("SetImage");
        this.outerBBOX = bBox;

        double lonWidth = layerBBox.getUpperCorner().getOrdinate(0)
                 - layerBBox.getLowerCorner().getOrdinate(0);
        double latHeight = layerBBox.getUpperCorner().getOrdinate(1)
                 - layerBBox.getLowerCorner().getOrdinate(1);
        this.aspectXY = lonWidth/latHeight;

        boxGroup.getChildren().clear();

        refreshViewport();
        
        repaint();

        WMSLayer wmsLayer = (WMSLayer) mapContent.layers().get(0);
        System.out.println(wmsLayer.getLastGetMap().getFinalURL());

        System.out.println("Point world, screen");
        Point2D.Double d = transformWorldToScreen(new Point2D.Double(48.86577105570864, 9.122956112634665));
        System.out.println("48.86577105570864, 9.122956112634665");
        System.out.println(d);
        drawMarker(d.getX(), d.getY());
    }

    //public void 

    public void repaint() {
        StreamingRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(mapContent);
        FXGraphics2D graphics = new FXGraphics2D(this.gc);
        graphics.setBackground(java.awt.Color.BLACK);
        gc.clearRect(0, 0, dimensionX, dimensionY);
        Rectangle rectangle = new Rectangle(dimensionX, dimensionY);
        renderer.paint(graphics, rectangle, mapContent.getViewport().getBounds());
    }

    private void refreshViewport(){
        MapViewport viewport = mapContent.getViewport();
        viewport.setCoordinateReferenceSystem(crs);
        viewport.setScreenArea(new Rectangle(dimensionX, dimensionY));
        viewport.setBounds(getBoundsForViewport());
        System.out.println("Bounds, crs");
        System.out.println(viewport.getBounds());
        System.out.println(this.mapContent.getCoordinateReferenceSystem());
        this.mapContent.setViewport(viewport);
        screenToWorld = mapContent.getViewport().getScreenToWorld();
        worldToScreen = mapContent.getViewport().getWorldToScreen();
    }

    public void addLayer(org.geotools.map.Layer layer){
        if(layer != null){
            this.mapContent.addLayer(layer);
            repaint();
        }
    }

    /**
     * gets the referenced Evelope from the Map.
     * @return the reference Evelope
     */
    public String getBoundsAsString() {
        return this.outerBBOX;
    }

    /**
     * gets the referenced Envelope as BoundingBox
     * @return the Bounding Box
     */
    public GeneralEnvelope getBoundsAsEnvelope() {
        List<String> bBoxStrList = Arrays.asList(this.outerBBOX.split(","));
		System.out.println(bBoxStrList);
        double upperRightX = Double.parseDouble(bBoxStrList.get(THREE));
        double upperRightY = Double.parseDouble(bBoxStrList.get(TWO));
        double lowerLeftX = Double.parseDouble(bBoxStrList.get(ONE));
        double lowerLeftY = Double.parseDouble(bBoxStrList.get(ZERO));
        GeneralEnvelope bBox = new GeneralEnvelope(
			new GeneralDirectPosition(lowerLeftX, lowerLeftY),
			new GeneralDirectPosition(upperRightX, upperRightY)
		);
		bBox.setCoordinateReferenceSystem(crs);
        return bBox;
    }
	
	/**
     * Returns bounding box as ReferencedEnvelope with coordinate order: xMin, xMax, yMin, yMax
     * @return the Bounding Box
     */
	public ReferencedEnvelope getBoundsForViewport(){
		List<String> bBoxStrList = Arrays.asList(this.outerBBOX.split(","));
		System.out.println(bBoxStrList);
        double upperRightX = Double.parseDouble(bBoxStrList.get(THREE));
        double upperRightY = Double.parseDouble(bBoxStrList.get(TWO));
        double lowerLeftX = Double.parseDouble(bBoxStrList.get(ONE));
        double lowerLeftY = Double.parseDouble(bBoxStrList.get(ZERO));
        ReferencedEnvelope bBox = new ReferencedEnvelope(
			lowerLeftY, upperRightY,
			lowerLeftX, upperRightX,
			crs);
        return bBox;
	}

    /**
     * gets the spacial reference system.
     * @return spacial ref system
     */
    public String getSpacialRefSystem() {
        return this.spacialRefSystem;
    }

	private Point2D.Double transformScreenToWorld(Point2D.Double screenPoint) {
		Point2D.Double worldPoint = new Point2D.Double();
		AffineTransform.getRotateInstance(java.lang.Math.PI, dimensionX/2, dimensionY/2)
			.transform(screenPoint, worldPoint);
		screenToWorld.transform(worldPoint, worldPoint);
		return worldPoint;
	}
	
	private Point2D.Double transformWorldToScreen(Point2D.Double worldPoint) {
		Point2D.Double screenPoint = new Point2D.Double();
		worldToScreen.transform(worldPoint, screenPoint);
		//AffineTransform.getRotateInstance(java.lang.Math.PI, dimensionX/2, dimensionY/2)
			//.transform(screenPoint, screenPoint);
		return screenPoint;
	}

    //TODO: https://github.com/geotools/geotools/blob/master/modules/unsupported/swing/src/main/java/org/geotools/swing/tool/ScrollWheelTool.java
    private void zoomIn(double delta) {
		System.out.println("Zoom In " + delta);
		delta *= 0.1;
        GeneralEnvelope bBox = getBoundsAsEnvelope();
        Point2D.Double lower = transformScreenToWorld(new Point2D.Double(0 + (delta * ZOOM_FACTOR), 0 + (delta * ZOOM_FACTOR)));
        Point2D.Double upper = transformScreenToWorld(new Point2D.Double(dimensionX - (delta * ZOOM_FACTOR), dimensionY - (delta * ZOOM_FACTOR)));
        String bBoxStr = lower.getX() + "," + lower.getY() + "," + upper.getX() + "," + upper.getY();
		/*String bBoxStr
            = (bBox.getLowerCorner().getOrdinate(1) * 1.1) + "," //((aspectXY) * delta * ZOOM_FACTOR)) + ","
			+ (bBox.getLowerCorner().getOrdinate(0) * 1.1) + "," //((aspectXY) * delta * ZOOM_FACTOR))+ ","
            + (bBox.getUpperCorner().getOrdinate(1) / 1.1) + "," //((aspectXY) * delta * ZOOM_FACTOR)) + ","
			+ (bBox.getUpperCorner().getOrdinate(0) / 1.1) ;     //((aspectXY) * delta * ZOOM_FACTOR));*/
        setMapImage(bBoxStr, INIT_SPACIAL_REF_SYS, INIT_LAYER_NUMBER);
    }

    private void zoomOut(double delta) {
        System.out.println("Zomm Out " + delta);
        delta *= 0.1;
        GeneralEnvelope bBox = getBoundsAsEnvelope();
        
		String bBoxStr
            = (bBox.getLowerCorner().getOrdinate(1) + ((1 - aspectXY) * delta * ZOOM_FACTOR)) + ","
			+ (bBox.getLowerCorner().getOrdinate(0) + (aspectXY * delta * ZOOM_FACTOR))+ ","
            + (bBox.getUpperCorner().getOrdinate(1) - ((1 - aspectXY) * delta * ZOOM_FACTOR)) + ","
			+ (bBox.getUpperCorner().getOrdinate(0) - (aspectXY * delta * ZOOM_FACTOR));
        setMapImage(bBoxStr, INIT_SPACIAL_REF_SYS, INIT_LAYER_NUMBER);
    }

    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int TWO = 2;
    private static final int THREE = 3;

    private void drag(double fromXScreen, double fromYScreen, double toXScreen, double toYScreen) {
        System.out.println("Dragging Image...");
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
        GeneralEnvelope bBox = this.getBoundsAsEnvelope();

        String bBoxStr
            = (bBox.getLowerCorner().getOrdinate(1) - xOffset) + ","
			+ (bBox.getLowerCorner().getOrdinate(0) - yOffset)+ ","
            + (bBox.getUpperCorner().getOrdinate(1) - xOffset) + ","
			+ (bBox.getUpperCorner().getOrdinate(0) - yOffset);
        //setMapImage(bBoxStr, INIT_SPACIAL_REF_SYS, INIT_LAYER_NUMBER);
        this.outerBBOX = bBoxStr;
        //refreshViewport();
        this.mapContent.getViewport().setBounds(getBoundsForViewport());
        repaint();
    }

    private void drawMarker(double xPosition, double yPosition) {
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
    /**
     * Event Handler for the choose Service Button.
     */
    private class UpdateImageButtonEventHandler implements
            EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent e) {
            setMapImage(boundingBoxField.getText(),
                    epsgField.getText(),
                    INIT_LAYER_NUMBER);
        }
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
                    zoomIn(10);
                }
                if (e.getClickCount() == 1) {
                    mouseXPosOnClick = e.getX();
                    mouseYPosOnClick = e.getY();
					Point2D clickWorld = transformScreenToWorld(new Point2D.Double(e.getY(), e.getX()));
					System.out.println("Clicked: S - W " + e.getX() + "," + e.getY() + " - " + clickWorld);
                }
            }
            if (e.getButton().equals(MouseButton.SECONDARY)) {
                if (e.getClickCount() > 1) {
                    zoomOut(1);

                }
                if (e.getClickCount() == 1) {
                    boxGroup.getChildren().clear();
                }
            }
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
                System.out.println("Maker Set");
                drawMarker(mouseXPosOnClick, mouseYPosOnClick);
                markerCount++;
                if (markerCount == 2) {
                    //TODO: Bounding Box
                    if (mouseXPosOnClick > previousMouseXPosOnClick) {
                        drawBox(mouseXPosOnClick, mouseYPosOnClick,
                                previousMouseXPosOnClick,
                                previousMouseYPosOnClick);
                    } else {
                        drawBox(previousMouseXPosOnClick,
                                previousMouseYPosOnClick, mouseXPosOnClick,
                                mouseYPosOnClick);
                    }
                    System.out.println("Draw Bounding-Box");
                } else if (markerCount > 2) {
                    boxGroup.getChildren().clear();
                    markerCount = 0;
                }
                previousMouseXPosOnClick = mouseXPosOnClick;
                previousMouseYPosOnClick = mouseYPosOnClick;
            } else {
                drag(mouseXPosOnClick, mouseYPosOnClick, e.getX(), e.getY());
                boxGroup.getChildren().clear();
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
                zoomIn(e.getDeltaY());
            }
            if (e.getDeltaY() < 0) {
                zoomOut(e.getDeltaY());
            }
        }
    }

    /** Event handler for dragging events, doesnt reload the actual map */
    private class OnMouseDraggedEvent
            implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent e){
            double xOffset = e.getSceneX() - mouseXPosOnClick;
            double yOffset = e.getSceneY() - mouseYPosOnClick;
            //TODO:
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

    public void reset() {

    }
    public void setExtend(ReferencedEnvelope env) {

    }

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
     * Draws Polygons on the maps.
     *
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

    public void setMapCRS(CoordinateReferenceSystem crs){

    }
}

