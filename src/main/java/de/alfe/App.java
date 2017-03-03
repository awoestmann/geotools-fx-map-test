package de.alfe;

import de.alfe.gui.Start;
import javafx.application.Application;

/**
 * Created by Jochen Saalfeld <jochen.saalfeld@intevation.de> on 2/16/17.
 */
public class App {

    public static void main(String[] args) {
        new Thread(() -> Application.launch(Start.class)).start();
        Start.waitForStart();
    }

}
