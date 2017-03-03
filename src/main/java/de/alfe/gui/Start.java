package de.alfe.gui;

import de.alfe.util.Controller;
import de.alfe.util.DataBean;
import javafx.application.Application;
import javafx.stage.Stage;
import org.geotools.ows.ServiceException;
import org.opengis.referencing.FactoryException;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Jochen Saalfeld <jochen.saalfeld@intevation.de> on 2/16/17.
 */
public class Start extends Application {

        private static final CountDownLatch LATCH = new CountDownLatch(1);
        private static Start start;

        public static Start waitForStart() {
            try {
                LATCH.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return start;
        }

        public Start() {
            start = this;
            LATCH.countDown();
        }

        @Override
        public void start(Stage primaryStage) throws IOException,
                ServiceException, FactoryException {
            DataBean dataBean = new DataBean(primaryStage);
            Controller c = new Controller(dataBean);
            c.show();
        }
    }
