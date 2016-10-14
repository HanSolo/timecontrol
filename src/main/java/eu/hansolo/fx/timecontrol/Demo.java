/*
 * Copyright (c) 2016 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.fx.timecontrol;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;
import javafx.scene.Scene;

import java.time.LocalTime;


/**
 * User: hansolo
 * Date: 13.10.16
 * Time: 16:09
 */
public class Demo extends Application {
    private TimeControl timeControl;

    @Override public void init() {
        timeControl = new TimeControl();
        timeControl.setBarColor(Color.web("#28e80c"));
        timeControl.setStartTime(LocalTime.of(0, 0));
        timeControl.setStopTime(LocalTime.of(15, 0));

        //timeControl.durationProperty().addListener(o -> System.out.println(timeControl.getDuration()));
    }

    @Override public void start(Stage stage) {
        StackPane pane = new StackPane(timeControl);
        pane.setPadding(new Insets(10));
        pane.setBackground(new Background(new BackgroundFill(Color.web("#0d0d0d"), CornerRadii.EMPTY, Insets.EMPTY)));

        Scene scene = new Scene(pane);

        stage.setTitle("TimeControl");
        stage.setScene(scene);
        stage.show();
    }

    @Override public void stop() {
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
