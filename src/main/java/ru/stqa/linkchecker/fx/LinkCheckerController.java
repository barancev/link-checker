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

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.control.table.TableFilter;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Optional;

public class LinkCheckerController {

  @FXML
  private TextField startUrl;
  @FXML
  private Button scanButton;
  @FXML
  private Spinner<Integer> threadCount;

  @FXML
  private SplitPane splitPane;

  @FXML
  private StackPane graphPane;

  @FXML
  private TableView<PageInfoModel> pageTable;
  @FXML
  private TableColumn<PageInfoModel, String> pageUrlColumn;
  @FXML
  private TableColumn<PageInfoModel, String> pageStatusColumn;

  @FXML
  private TableView<PropertyModel> pageInfoTable;
  @FXML
  private TableColumn<PropertyModel, String> pageInfoKeyColumn;
  @FXML
  private TableColumn<PropertyModel, String> pageInfoValueColumn;

  private ScannerModel model;

  private Stage stage;
  private Viewer graphViewer;
  private ViewerPipe fromViewer;
  private ViewPanel graphView;
  private boolean doPump = true;

  private class PropertyModel {
    StringProperty key;
    StringProperty value;

    PropertyModel(String key, String value) {
      this.key = new SimpleStringProperty(key);
      this.value = new SimpleStringProperty(value);
    }
  }

  private ObservableList<PropertyModel> pageProperties = FXCollections.observableArrayList();

  public void setStage(Stage stage) {
    this.stage = stage;
  }

  public void setModel(ScannerModel model) {
    this.model = model;

    startUrl.textProperty().bindBidirectional(model.startUrlProperty());
    threadCount.getValueFactory().valueProperty().bindBidirectional(model.threadCountProperty().asObject());

    pageTable.setItems(model.getPages());
    pageInfoTable.setItems(pageProperties);
    TableFilter.forTableView(pageTable).apply();

    SwingUtilities.invokeLater(() -> {
      graphViewer = new Viewer(model.getGraph(), Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
      fromViewer = graphViewer.newViewerPipe();
      fromViewer.addViewerListener(new ViewerListener() {
        @Override
        public void viewClosed(String viewName) {
          doPump = false;
        }

        @Override
        public void buttonPushed(String id) {
        }

        @Override
        public void buttonReleased(String id) {
          FilteredList<PageInfoModel> filtered = model.getPages().filtered(pageInfo -> pageInfo.getUrl().equals(id));
          if (filtered.size() > 0) {
            showPageInfoProperty(filtered.get(0));
          }
        }
      });
      fromViewer.addAttributeSink(model.getGraph());
      new Thread(() -> {
        while (doPump) {
          try {
            fromViewer.blockingPump();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      }).start();

      graphView = graphViewer.addDefaultView(false);

      Platform.runLater(() -> {
        SwingNode swingNode = new SwingNode();
        swingNode.setContent(graphView);
        graphPane.getChildren().add(swingNode);
      });
    });
  }

  @FXML
  private void initialize() {
    System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

    pageUrlColumn.setCellValueFactory(cellData -> cellData.getValue().urlProperty());
    pageStatusColumn.setCellValueFactory(cellData -> cellData.getValue().httpStatusProperty());

    pageTable.getSelectionModel().selectedItemProperty().addListener(
      (observable, oldValue, newValue) -> showPageInfoProperty(newValue));

    pageInfoKeyColumn.setCellValueFactory(cellData -> cellData.getValue().key);
    pageInfoValueColumn.setCellValueFactory(cellData -> cellData.getValue().value);

    splitPane.getDividers().forEach(divider -> divider.positionProperty().addListener(n -> repaintGraphView()));
  }

  @FXML
  private void createNewProject() {
    model.reset();
  }

  @FXML
  private void saveProject() {
    if (model.getSavedTo() != null) {
      model.save();

    } else {
      FileChooser fileChooser = new FileChooser();
      FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json");
      fileChooser.getExtensionFilters().add(extFilter);

      File file = fileChooser.showSaveDialog(stage);

      if(file != null){
        try {
          model.saveTo(file.toPath());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @FXML
  private void openProject() {
    FileChooser fileChooser = new FileChooser();
    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json");
    fileChooser.getExtensionFilters().add(extFilter);

    File file = fileChooser.showOpenDialog(stage);

    if(file != null){
      try {
        model.loadFrom(file.toPath());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @FXML
  private void exit() {
    closeGraphViewer();
    Platform.exit();
  }

  @FXML
  private void typingInStartUrl(KeyEvent event) {
    if (event.getCode().equals(KeyCode.ENTER)) {
      goScan();
    }
  }

  @FXML
  private void goScan() {
    if (startUrl.getText() == null || startUrl.getText().equals("")) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.initOwner(stage);
      alert.setTitle("No Start URL");
      alert.setHeaderText(null);
      alert.setContentText("Please specify a start URL.");
      alert.showAndWait();
      return;
    }

    if (model.getPages().size() > 0) {
      Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
      alert.initOwner(stage);
      alert.setTitle("Confirmation");
      alert.setHeaderText("List of scanned pages is not empty");
      alert.setContentText("Do you want to delete results of the previous scan session and start a new one?");
      Optional<ButtonType> result = alert.showAndWait();
      if (!result.isPresent() || result.get() != ButtonType.OK){
        return;
      }
    }

    startUrl.setDisable(true);
    scanButton.setDisable(true);

    graphViewer.enableAutoLayout();

    model.reset();
    model.setFinishHandler(() -> Platform.runLater(this::scanCompleted));
    try {
      model.startScan();
    } catch (MalformedURLException e) {
      e.printStackTrace();

      startUrl.setDisable(false);
      scanButton.setDisable(false);
      graphViewer.disableAutoLayout();
      return;
    }

    scanButton.setText("Stop");
    scanButton.setDisable(false);
    scanButton.setOnAction(v -> stopScan());
  }

  private void stopScan() {
    model.interruptScan();
    restoreScanButton();
  }

  private void scanCompleted() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.initOwner(stage);
    alert.setTitle("Finish");
    alert.setHeaderText(null);
    alert.setContentText("Scanning completed.");
    alert.showAndWait();
    restoreScanButton();
    graphViewer.disableAutoLayout();
  }

  private void restoreScanButton() {
    startUrl.setDisable(false);
    scanButton.setText("Start");
    scanButton.setOnAction(v -> goScan());
  }

  private void showPageInfoProperty(PageInfoModel pageInfo) {
    pageProperties.clear();
    pageProperties.addAll(
      new PropertyModel("URL", pageInfo.getUrl()),
      new PropertyModel("Content Type", pageInfo.getContentType()),
      new PropertyModel("HTTP Status", pageInfo.getHttpStatus()),
      new PropertyModel("Message", pageInfo.getMessage()));
  }

  public void repaintGraphView() {
    if (graphView != null) {
      SwingUtilities.invokeLater(graphView::repaint);
    }
  }

  void closeGraphViewer() {
    try {
      graphViewer.close();
    } catch (NullPointerException ignore) {
    }
  }
}
