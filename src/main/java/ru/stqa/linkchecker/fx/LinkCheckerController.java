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
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.controlsfx.control.table.TableFilter;
import ru.stqa.linkchecker.ScanSession;
import ru.stqa.linkchecker.ScanSettings;
import ru.stqa.linkchecker.ScanStatus;

import java.net.MalformedURLException;

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
  private TableColumn<PageInfoModel, String> pageMessageColumn;

  private ScanSession session;

  public void setMainApp(Main mainApp) {
    this.mainApp = mainApp;
    pageTable.setItems(mainApp.getPages());
    TableFilter.forTableView(pageTable).apply();
  }

  @FXML
  private void initialize() {
    pageUrlColumn.setCellValueFactory(cellData -> cellData.getValue().urlProperty());
    pageStatusColumn.setCellValueFactory(cellData -> cellData.getValue().httpStatusProperty());
    pageMessageColumn.setCellValueFactory(cellData -> cellData.getValue().messageProperty());
  }

  @FXML
  private void goScan() {
    startUrl.setDisable(true);
    scanButton.setText("Stop");
    scanButton.setOnAction(v -> stopScan());

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
  }

  private void stopScan() {
    startUrl.setDisable(false);
    scanButton.setText("Start");
    scanButton.setOnAction(v -> goScan());
  }

}
