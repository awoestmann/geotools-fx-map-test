package de.alfe.gui;

import com.vividsolutions.jts.io.ParseException;
import de.alfe.gui.map.Map;
import de.alfe.util.DataBean;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.geotools.data.ows.Layer;
import org.geotools.data.wms.WebMapServer;
import org.geotools.feature.SchemaException;
import org.geotools.ows.ServiceException;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.FactoryException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jochen Saalfeld <jochen.saalfeld@intevation.de> on 2/16/17.
 */
public class View {

    private Scene scene;
    private Map map;
    private BorderPane borderPane;
    private MenuBar menuBar;
    private Menu optionsMenu;
    private MenuItem displaySingleUnturnedComplexPolygonMenuItem;
    private MenuItem displaySingleTurnedComplexPolygonMenuItem;
    private VBox statusBar;
    private static final double sceneHeight = 768;
    private static final double sceneWidth = 1024;
    //private static final String wmsURL = "http://sg.geodatenzentrum.de/wms_webatlasde.light";
    private static final String wmsURL = "http://ows.terrestris.de/osm/service";

    //private static final String wmsLayer = "webatlasde.light";
    private static final String wmsLayer = "OpenStreetMap WMS - by terrestris";

    public View() throws IOException, ServiceException, FactoryException {
        this(new URL(wmsURL), wmsLayer);
    }

    public View(URL wmsURL, String wmsLayer) throws IOException,
            ServiceException, FactoryException {
        this.borderPane = new BorderPane();

        WebMapServer wms = new WebMapServer(wmsURL);
        Layer displayLayer = wms.getCapabilities().getLayer();
        for (Layer layer: wms.getCapabilities().getLayerList()) {
            System.out.println("Layer: " + layer);
            if (layer.getTitle().toLowerCase().equals(wmsLayer)
                | layer.getTitle().equals(wmsLayer)) {
                displayLayer = layer;
                break;
            }
        }
        
        this.map = new Map(wms, displayLayer, 1024, 768, new GeneralEnvelope(new double[]{-22, 31}, new double[]{50, 67}));
        this.map.setMapCRS(CRS.decode(Map.getEPSGWGS84String()));

        this.menuBar = new MenuBar();
        this.optionsMenu = new Menu("Options");
        this.menuBar.getMenus().add(optionsMenu);
        this.displaySingleUnturnedComplexPolygonMenuItem =
                new MenuItem("Display " +
                "Single Unturned Complex Polygon");
        this.displaySingleUnturnedComplexPolygonMenuItem.addEventHandler
                (Event.ANY,
                        new displaySingleUnturnedComplexPolygonMenuItemEventHandler());
        this.optionsMenu.getItems().add(this
                .displaySingleUnturnedComplexPolygonMenuItem);
        this.displaySingleTurnedComplexPolygonMenuItem =
                new MenuItem("Display " +
                        "Single Turned Complex Polygon");
        this.displaySingleTurnedComplexPolygonMenuItem.addEventHandler
                (Event.ANY,
                        new displaySingleTurnedComplexPolygonMenuItem());
        this.optionsMenu.getItems().add(this
                .displaySingleTurnedComplexPolygonMenuItem);

        this.statusBar = new VBox();
        this.statusBar.setStyle("-fx-background-color: gainsboro");
        final Text statusText = new Text("Ready");
        this.statusBar.getChildren().add(statusText);

        VBox vbox = new VBox();
        HBox hbox = new HBox();

        Button reset = new Button("reset");
        reset.setOnAction(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent e){
                map.resetExtent();
            }
        });

        TextField epsgField = new TextField(Map.getEPSGWGS84String());
        Button changeCrs = new Button("Change CRS");
        changeCrs.setOnAction(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent e){
                try{
                    map.setMapCRS(CRS.decode(epsgField.getText()));
                } catch (Exception ex) {
                    System.out.println("Error on decoding: " + epsgField.getText());
                }
            }
        });

        Button resize = new Button("resize");
        resize.setOnAction(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent e){
                map.resize(400, 400);
            }
        });

        vbox.getChildren().add(hbox);
        vbox.getChildren().add(map);
        hbox.getChildren().add(reset);
        hbox.getChildren().add(epsgField);
        hbox.getChildren().add(changeCrs);
        hbox.getChildren().add(resize);
        this.borderPane.setTop(this.menuBar);
        this.borderPane.setCenter(vbox);
        this.borderPane.setBottom(this.statusBar);

        this.scene = new Scene(this.borderPane, sceneHeight, sceneWidth);
    }

    public static double getWindowsHeight() {
        return sceneHeight;
    }

    public static double getWindowWidth() {
        return sceneWidth;
    }

    public void show(Stage stage) {
        stage.setTitle("GeoTools WMS Map Test");
        stage.setScene(this.scene);
        Rectangle2D primaryScreenBounds =
                Screen.getPrimary().getVisualBounds();
        stage.setWidth(primaryScreenBounds.getWidth());
        stage.setHeight(primaryScreenBounds.getHeight());
        stage.show();
    }

    private class displaySingleUnturnedComplexPolygonMenuItemEventHandler
            implements EventHandler<Event> {
        @Override
        public void handle(Event event) {
            List<String> polygonList = new ArrayList<String>();
            polygonList.add(DataBean.singleUnturnedWGS84ComplexPolygonWKT());
            try {
                map.drawStringPolygons(polygonList);
            } catch (ParseException
                    | FactoryException
                    | SchemaException
                    e){
                System.err.println(e);
            }
        }
    }

    private class displaySingleTurnedComplexPolygonMenuItem
            implements EventHandler<Event> {
        @Override
        public void handle(Event event) {
            List<String> polygonList = new ArrayList<String>();
            polygonList.add(DataBean.singleTurnedWGS84ComplexPolygonWKT());
            try {
                map.drawStringPolygons(polygonList);
            } catch (ParseException
                    | FactoryException
                    | SchemaException
                    e){
                System.err.println(e);
            }
        }
    }
}
