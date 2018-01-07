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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

  private Stage primaryStage;
  private BorderPane rootLayout;

  private ObservableList<PageInfoModel> pages = FXCollections.observableArrayList();

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    this.primaryStage = primaryStage;
    this.primaryStage.setTitle("Link Checker");
    initRootLayout();
  }

  public void initRootLayout() {
    FXMLLoader loader = new FXMLLoader();
    loader.setLocation(Main.class.getResource("/fx/root_layout.fxml"));
    try {
      rootLayout = loader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Scene scene = new Scene(rootLayout);
    primaryStage.setScene(scene);
    primaryStage.show();

    LinkCheckerController controller = loader.getController();
    controller.setMainApp(this);
  }

  public Stage getPrimaryStage() {
    return primaryStage;
  }

  public ObservableList<PageInfoModel> getPages() {
    return pages;
  }
}
