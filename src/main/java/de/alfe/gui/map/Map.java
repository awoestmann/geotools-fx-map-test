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

import com.vividsolutions.jts.geom.Envelope;
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
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.request.GetFeatureInfoRequest;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.data.wms.response.GetFeatureInfoResponse;
import org.geotools.geometry.GeneralDirectPosition;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.ServiceException;
import org.geotools.referencing.CRS;
import org.geotools.data.wms.xml.Dimension;
import org.geotools.data.wms.xml.Extent;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.geotools.map.MapContent;
import org.geotools.map.MapViewport;
import org.geotools.map.WMSLayer;

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
    private static final double ZOOM_FACTOR = 100d;
    private static final double HUNDRED = 100d;

    private static final double INITIAL_EXTEND_X1 = 850028;
    private static final double INITIAL_EXTEND_Y1 = 6560409;
    private static final double INITIAL_EXTEND_X2 = 1681693;
    private static final double INITIAL_EXTEND_Y2 = 5977713;


    private static final double TEN_PERCENT_OF = 0.1d;

    private static String WPSG_WGS84 = "EPSG:4326";

    private Group boxGroup;
    private AffineTransform screenToWorld;
    private AffineTransform worldToScreen;
	private CoordinateReferenceSystem crs;
	
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
        //TODO
        //try {
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
			
			//TODO
			//CRSEnvelope env = new CRSEnvelope();
			//env.setMinX(49.0772);
			//env.setMinY(9.15977);
			//env.setMaxX(51);
			//env.setMaxY(11);
			//this.layerBBox.setEnvelope(GeneralEnvelope.toGeneralEnvelope(env));
			
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
            
            this.ig = new Group();
            boxGroup = new Group();

            sourceLabel = new Label(this.serviceName);
            sourceLabel.setLabelFor(this.ig);
            this.ig.getChildren().add(boxGroup);
            this.add(ig);
            this.add(sourceLabel);
            this.add(epsgField);
            this.add(boundingBoxField);
            this.add(updateImageButton);
            this.getChildren().add(vBox);

            this.setMapImage(this.outerBBOX,
                    this.INIT_SPACIAL_REF_SYS,
                    layers.size() - 1);

            this.addEventHandler(MouseEvent.MOUSE_RELEASED, new
                OnMouseReleasedEvent());
            this.addEventHandler(MouseEvent.MOUSE_CLICKED, new
                OnMousePressedEvent());
            this.addEventHandler(MouseEvent.MOUSE_PRESSED, new
                OnMousePressedEvent());
            this.addEventHandler(ScrollEvent.SCROLL, new
                OnMouseScrollEvent());
            this.updateImageButton.setOnAction(
                new UpdateImageButtonEventHandler()
        );
       //} catch (IOException | ServiceException e) {
        //    log.log(Level.SEVERE, e.getMessage(), e);
        //}

    }

    /**
     * Constructor.
     * @param serviceURL URL of the Service
     * @param serviceName Name of the Service
     * @param outerBBOX Outer Bounds of the Picture
     * @param dimensionX X Dimension of the picuter
     * @param dimensionY Y Dimenstion of the Picture
     * @param spacialRefSystem Spacial Ref System ID
     */
    public Map(String serviceURL,
                  String serviceName,
                  String outerBBOX,
                  int dimensionX,
                  int dimensionY,
                  String spacialRefSystem) {
        this.serviceURL = serviceURL;
        this.serviceName = serviceName;
        this.outerBBOX = outerBBOX;
        // TODO:
        // Make subclass from OuterBBOX with fouir fields, for x1, x2, y1, y2
        this.dimensionX = dimensionX;
        this.dimensionY = dimensionY;
        this.spacialRefSystem = spacialRefSystem;
        this.iw = new ImageView();
        this.epsgField = new TextField(this.spacialRefSystem);
        this.boundingBoxField = new TextField(this.outerBBOX);
        this.updateImageButton = new Button("Update Image");
        vBox = new VBox();
        this.addEventHandler(MouseEvent.MOUSE_RELEASED, new
                OnMouseReleasedEvent());
        this.addEventHandler(MouseEvent.MOUSE_CLICKED, new
                OnMousePressedEvent());
        this.addEventHandler(MouseEvent.MOUSE_PRESSED, new
                OnMousePressedEvent());
        this.addEventHandler(ScrollEvent.SCROLL, new
                OnMouseScrollEvent());
        this.updateImageButton.setOnAction(
                new UpdateImageButtonEventHandler()
        );
        try {
            URL serviceURLObj = new URL(this.serviceURL);
            wms = new WebMapServer(serviceURLObj);
            capabilities = wms.getCapabilities();
            layers = capabilities.getLayerList();
            this.ig = new Group();
            boxGroup = new Group();
            this.setMapImage(this.outerBBOX,
                    this.spacialRefSystem,
                    this.INIT_LAYER_NUMBER);

            sourceLabel = new Label(this.serviceName);
            sourceLabel.setLabelFor(this.ig);
            this.ig.getChildren().add(boxGroup);
            this.add(ig);
            this.add(sourceLabel);
            this.add(epsgField);
            this.add(boundingBoxField);
            this.add(updateImageButton);
            this.getChildren().add(vBox);

        } catch (IOException | ServiceException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Constructor.
     * @param serviceURL URL of the Service
     * @param serviceName Name of the Service
     * @param outerBBOX Outer Bounds of the Picture
     * @param dimensionX X Dimension of the picuter
     * @param dimensionY Y Dimenstion of the Picture
     */
    public Map(String serviceURL,
                  String serviceName,
                  String outerBBOX,
                  int dimensionX,
                  int dimensionY) {
        this(serviceURL,
                serviceName,
                outerBBOX,
                dimensionX,
                dimensionY,
                INIT_SPACIAL_REF_SYS);
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
        try {
            boxGroup.getChildren().clear();
            GetMapRequest request = wms.createGetMapRequest();
            request.setFormat(this.FORMAT);
            request.setDimensions(this.dimensionX, this.dimensionY);
            request.setTransparent(false);//this.TRANSPARACY);
            request.setSRS(spacialRefSys);
//            request.setBBox(INITIAL_EXTEND_X1 + "," + INITIAL_EXTEND_Y1 + "," + INITIAL_EXTEND_X2 + "," + INITIAL_EXTEND_Y2);
            this.outerBBOX = bBox;
			request.setBBox(this.outerBBOX);
            
            this.spacialRefSystem = spacialRefSys;
            request.addLayer((Layer) layers.get(layerNumber));
            System.out.println("WMS Call for Map Image: "
                    + request.getFinalURL().toString());
			Layer mapLayer = (Layer) layers.get(layerNumber);		
            GetMapResponse response
                    = (GetMapResponse) wms.issueRequest(request);
            Image im = new Image(response.getInputStream());
            this.ig.getChildren().clear();
            this.iw = new ImageView();
            this.iw.setImage(im);
            this.ig.getChildren().add(this.iw);			
			
            double mapWidth = layerBBox.getUpperCorner().getOrdinate(0)
                - layerBBox.getLowerCorner().getOrdinate(0);
            double mapHeight = layerBBox.getUpperCorner().getOrdinate(1)
                - layerBBox.getLowerCorner().getOrdinate(1);

            double imageCenterX = 0.5 * this.dimensionX;
            double imageCenterY = 0.5 * this.dimensionY;
            double mapCenterX = layerBBox.getLowerCorner().getOrdinate(0)
                    + 0.5 * mapWidth;
            double mapCenterY = layerBBox.getLowerCorner().getOrdinate(1)
                    + 0.5 * mapHeight;
            
			System.out.println("Center XY Image, Map: " + imageCenterX + " - " + imageCenterY + " , " + mapCenterX + " - " + mapCenterY);
			System.out.println("Width XY Image, Map " + dimensionX + " - " + dimensionY + " , " + mapWidth + " - " + mapHeight);
			System.out.println("BBox: " + this.outerBBOX);
			
            
			MapContent content = new MapContent();
            content.addLayer(new WMSLayer(wms, displayLayer));
            MapViewport viewport = content.getViewport();			
			viewport.setCoordinateReferenceSystem(crs);
            viewport.setScreenArea(new Rectangle(dimensionX, dimensionY));			
			viewport.setBounds(getBoundsForViewport());			
			System.out.println(viewport.getBounds());
			content.setViewport(viewport);
            screenToWorld = content.getViewport().getScreenToWorld();
            worldToScreen = content.getViewport().getWorldToScreen();
			
            Point2D.Double center = new Point2D.Double(mapCenterX, mapCenterY);
			System.out.println("Point world, screen");
			
			Point2D.Double d = transformWorldToScreen(new Point2D.Double(48.86577105570864, 9.122956112634665));
			System.out.println("48.86577105570864, 9.122956112634665");
			System.out.println(d);
			drawMarker(d.getX(), d.getY());
			content.dispose();			
			
        } catch (IOException | ServiceException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
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


    private void zoomIn() {
       /* System.out.println("Zoom In");
        Envelope bBox = getBoundsAsEnvelope();
        double median = bBox.getMaxX() + bBox.getMaxY() + bBox.getMinX() + bBox.getMinY();
        median = median / 4;
        String bBoxStr
                = (bBox.getMaxX() - (ZOOM_FACTOR)) + "," + (bBox.getMaxY() - (ZOOM_FACTOR))+ ","
                + (bBox.getMinX() - (ZOOM_FACTOR)) + "," + (bBox.getMinY() - (ZOOM_FACTOR));
        setMapImage(bBoxStr, INIT_SPACIAL_REF_SYS, INIT_LAYER_NUMBER);*/
    }

    private void zoomOut() {
      /*  System.out.println("Zomm Out");
        Envelope bBox = getBoundsAsEnvelope();
        double median = bBox.getMaxX() + bBox.getMaxY() + bBox.getMinX() + bBox.getMinY();
        median = median / 4;
        String bBoxStr
                = (bBox.getMaxX() + ZOOM_FACTOR) + "," + (bBox.getMaxY() + ZOOM_FACTOR)+ ","
                + (bBox.getMinX() + ZOOM_FACTOR) + "," + (bBox.getMinY() + ZOOM_FACTOR);
        setMapImage(bBoxStr, INIT_SPACIAL_REF_SYS, INIT_LAYER_NUMBER);*/
    }


    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int TWO = 2;
    private static final int THREE = 3;

    private void drag(double fromXScreen, double fromYScreen, double toXScreen, double toYScreen) {
        System.out.println("Dragging Image...");
        Point2D.Double from = new Point2D.Double(fromYScreen, fromXScreen);
        Point2D.Double to = new Point2D.Double(toYScreen, toXScreen);
		System.out.println("Screen");
		System.out.println(from);
		System.out.println(to);
		from = transformScreenToWorld(from);
		to = transformScreenToWorld(to);
		System.out.println("World");
		System.out.println(from);
		System.out.println(to);
        double fromX = from.getX();
        double fromY = from.getY();
        double toX = to.getX();
        double toY = to.getY();

        double xOffset = (toX - fromX);// * ZOOM_FACTOR;
        double yOffset = (toY - fromY);// * ZOOM_FACTOR;
        GeneralEnvelope bBox = this.getBoundsAsEnvelope();
		System.out.println("Old BBox " + bBox);

        String bBoxStr
            = (bBox.getLowerCorner().getOrdinate(1) - xOffset) + "," 
			+ (bBox.getLowerCorner().getOrdinate(0) - yOffset)+ ","
            + (bBox.getUpperCorner().getOrdinate(1) - xOffset) + "," 
			+ (bBox.getUpperCorner().getOrdinate(0) - yOffset);
		System.out.println("New BBox " + bBoxStr);
        setMapImage(bBoxStr, INIT_SPACIAL_REF_SYS, INIT_LAYER_NUMBER);
    }

    private void drawMarker(double xPosition, double yPosition) {
        double markerSpan = this.iw.getImage().getWidth() / HUNDRED;
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
        ig.getChildren().add(upperLeftToLowerRight);
        ig.getChildren().add(upperRightToLowerLeft);
    }

    private void drawBox(double beginX, double beginY, double endX, double
            endY) {
        Line upperLine = new Line(beginX, beginY, endX, beginY);
        upperLine.setFill(null);
        upperLine.setStroke(Color.RED);
        upperLine.setStrokeWidth(2);
        boxGroup.getChildren().add(upperLine);

        Line leftLine = new Line(beginX, beginY, beginX, endY);
        leftLine.setFill(null);
        leftLine.setStroke(Color.RED);
        leftLine.setStrokeWidth(2);
        boxGroup.getChildren().add(leftLine);

        Line buttomLine = new Line(beginX, endY, endX, endY);
        buttomLine.setFill(null);
        buttomLine.setStroke(Color.RED);
        buttomLine.setStrokeWidth(2);
        boxGroup.getChildren().add(buttomLine);

        Line rightLine = new Line(endX, beginY , endX, endY);
        rightLine.setFill(null);
        rightLine.setStroke(Color.RED);
        rightLine.setStrokeWidth(2);
        boxGroup.getChildren().add(rightLine);
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
                    zoomIn();
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
                    zoomOut();

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
                zoomOut();
            }
            if (e.getDeltaY() < 0) {
                zoomIn();
            }
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

    public void drawPolygons(List<String> polyList) {
    }

    public void repaint() {

    }

    public void highlightSelectedPolygon(String s) {

    }

    public static String getEPSGWGS84String(){
        return Map.WPSG_WGS84;
    }

    public void setMapCRS(CoordinateReferenceSystem crs){

    }
}

