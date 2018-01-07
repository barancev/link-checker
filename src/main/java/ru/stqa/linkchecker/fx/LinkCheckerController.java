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
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.controlsfx.control.table.TableFilter;
import ru.stqa.linkchecker.ScanSession;
import ru.stqa.linkchecker.ScanSettings;
import ru.stqa.linkchecker.ScanStatus;

import java.net.MalformedURLException;
import java.util.Optional;

public class LinkCheckerController {

  private Main mainApp;

  @FXML
  private TextField startUrl;
  @FXML
  private Button scanButton;

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

  private ScanSession session;

  private class PropertyModel {
    StringProperty key;
    StringProperty value;

    PropertyModel(String key, String value) {
      this.key = new SimpleStringProperty(key);
      this.value = new SimpleStringProperty(value);
    }
  }

  private ObservableList<PropertyModel> pageProperties = FXCollections.observableArrayList();

  public void setMainApp(Main mainApp) {
    this.mainApp = mainApp;
    pageTable.setItems(mainApp.getPages());
    pageInfoTable.setItems(pageProperties);
    TableFilter.forTableView(pageTable).apply();
  }

  @FXML
  private void initialize() {
    pageUrlColumn.setCellValueFactory(cellData -> cellData.getValue().urlProperty());
    pageStatusColumn.setCellValueFactory(cellData -> cellData.getValue().httpStatusProperty());

    pageTable.getSelectionModel().selectedItemProperty().addListener(
      (observable, oldValue, newValue) -> showPageInfoProperty(newValue));

    pageInfoKeyColumn.setCellValueFactory(cellData -> cellData.getValue().key);
    pageInfoValueColumn.setCellValueFactory(cellData -> cellData.getValue().value);
  }

  @FXML
  private void goScan() {
    if (startUrl.getText() == null || startUrl.getText().equals("")) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.initOwner(mainApp.getPrimaryStage());
      alert.setTitle("No Start URL");
      alert.setHeaderText(null);
      alert.setContentText("Please specify a start URL.");
      alert.showAndWait();
      return;
    }

    if (mainApp.getPages().size() > 0) {
      Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
      alert.initOwner(mainApp.getPrimaryStage());
      alert.setTitle("Confirmation");
      alert.setHeaderText("List of scanned pages is not empty");
      alert.setContentText("Do you want to delete results of the previous scan session and start a new one?");
      Optional<ButtonType> result = alert.showAndWait();
      if (result.get() == ButtonType.OK){
        mainApp.getPages().clear();
      } else {
        return;
      }
    }

    startUrl.setDisable(true);
    scanButton.setDisable(true);

    try {
      session = new ScanSession(new ScanSettings(startUrl.getText(), 1));
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    session.addListener(pageInfo -> {
      if (pageInfo.getStatus() != ScanStatus.IN_PROGRESS) {
        Platform.runLater(() -> mainApp.getPages().add(new PageInfoModel(pageInfo)));
      }
    });
    Thread t = new Thread(session);
    t.start();

    Thread waiter = new Thread(() -> {
      try {
        t.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      Platform.runLater(this::scanCompleted);
    });
    waiter.start();

    scanButton.setText("Stop");
    scanButton.setDisable(false);
    scanButton.setOnAction(v -> stopScan());
  }

  private void stopScan() {
    session.interrupt();
    while (!session.isStopped()) {
      Thread.yield();
    }
    restoreScanButton();
  }

  private void scanCompleted() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.initOwner(mainApp.getPrimaryStage());
    alert.setTitle("Finish");
    alert.setHeaderText(null);
    alert.setContentText("Scanning completed.");
    alert.showAndWait();
    restoreScanButton();
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

}
