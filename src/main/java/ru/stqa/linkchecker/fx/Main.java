/*
 * Copyright 2018 Alexei Barantsev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.stqa.linkchecker.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

  private Stage primaryStage;
  private ScannerModel model;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    this.primaryStage = primaryStage;
    this.primaryStage.setTitle("Link Checker");

    model = new ScannerModel();
    initRootLayout();
  }

  private void initRootLayout() {
    FXMLLoader loader = new FXMLLoader();
    loader.setLocation(Main.class.getResource("/fx/root_layout.fxml"));
    BorderPane rootLayout;
    try {
      rootLayout = loader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Scene scene = new Scene(rootLayout);
    primaryStage.setScene(scene);
    primaryStage.setMinWidth(rootLayout.getMinWidth());
    primaryStage.setMinHeight(rootLayout.getMinHeight());
    primaryStage.show();

    LinkCheckerController controller = loader.getController();
    controller.setStage(primaryStage);
    controller.setModel(model);

    primaryStage.setOnCloseRequest((event) -> controller.closeGraphViewer());

    primaryStage.widthProperty().addListener(n -> controller.repaintGraphView());
    primaryStage.heightProperty().addListener(n -> controller.repaintGraphView());
    primaryStage.maximizedProperty().addListener(x -> controller.repaintGraphView());
  }
}
