package de.alfe.util;

import javafx.stage.Stage;

import java.util.Observable;

/**
 * Created by Jochen Saalfeld <jochen.saalfeld@intevation.de> on 2/16/17.
 */
public class DataBean extends Observable {

    private Stage primaryStage = null;

    public DataBean(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public Stage getPrimaryStage() {
        return this.primaryStage;
    }

    public static String singleTurnedWGS84ComplexPolygonWKT() {
        return "POLYGON((" + singleTurnedWGS84ComplexCoordinates() + "))";
    }

    private static final String singleTurnedWGS84ComplexCoordinates() {
        return "4.687499999999998 53.60416666666667," +
               "5.9687499999999964 44.4375," +
               "16.093749999999993 44.8125," +
               "15.593749999999993 54.85416666666667," +
               "4.687499999999998 53.60416666666667";
    }

    public static String singleUnturnedWGS84ComplexPolygonWKT() {
        return "POLYGON ((" + singleTurnedWGS84ComplexCoordinates() + "))";
    }
}
