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
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.table.TableFilter;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import ru.stqa.linkchecker.ScanSession;
import ru.stqa.linkchecker.ScanSettings;
import ru.stqa.linkchecker.ScanStatus;

import javax.swing.*;
import java.io.*;
import java.net.MalformedURLException;
import java.util.Optional;
import java.util.Scanner;

public class LinkCheckerController {

  private Main mainApp;

  @FXML
  private TextField startUrl;
  @FXML
  private Button scanButton;

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

  private ScanSession session;

  private Graph graph;
  private Viewer graphViewer;

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
    System.setProperty("gs.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

    pageUrlColumn.setCellValueFactory(cellData -> cellData.getValue().urlProperty());
    pageStatusColumn.setCellValueFactory(cellData -> cellData.getValue().httpStatusProperty());

    pageTable.getSelectionModel().selectedItemProperty().addListener(
      (observable, oldValue, newValue) -> showPageInfoProperty(newValue));

    pageInfoKeyColumn.setCellValueFactory(cellData -> cellData.getValue().key);
    pageInfoValueColumn.setCellValueFactory(cellData -> cellData.getValue().value);

    graph = new SingleGraph("embedded");
    graph.addAttribute("ui.stylesheet", loadStyleSheet());

    SwingUtilities.invokeLater(() -> {
      graphViewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
      ViewPanel graphView = graphViewer.addDefaultView(false);

      Platform.runLater(() -> {
        SwingNode swingNode = new SwingNode();
        swingNode.setContent(graphView);
        graphPane.getChildren().add(swingNode);
        graphPane.requestLayout();
      });
    });
  }

  private String loadStyleSheet() {
    InputStream style = LinkCheckerController.class.getResourceAsStream("/graph.css");
    return new Scanner(style, "utf-8").useDelimiter("\\Z").next();
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
      if (result.isPresent() && result.get() == ButtonType.OK){
        mainApp.getPages().clear();
        graph.clear();
      } else {
        return;
      }
    }

    startUrl.setDisable(true);
    scanButton.setDisable(true);

    graphViewer.enableAutoLayout();

    Node startNode = graph.addNode(startUrl.getText());
    startNode.addAttribute("ui.label", startUrl.getText());
    startNode.addAttribute("ui.class", "start");

    try {
      session = new ScanSession(new ScanSettings(startUrl.getText(), 1));
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    session.addListener(pageInfo -> {
      if (pageInfo.getStatus() != ScanStatus.IN_PROGRESS) {
        Platform.runLater(() -> {
          mainApp.getPages().add(new PageInfoModel(pageInfo));
          if (graph.getNode(pageInfo.getUrl()) == null) {
            Node node = graph.addNode(pageInfo.getUrl());
            node.addAttribute("ui.label", shorten(pageInfo.getUrl()));
            //System.out.println("+ Node " + pageInfo.getUrl());
          }
          pageInfo.getLinks().forEach(link -> {
            if (graph.getNode(link) == null) {
              Node node = graph.addNode(link);
              node.addAttribute("ui.label", shorten(link));
              //System.out.println("+ Node " + link);
            }
            if (graph.getEdge(String.format("%s -> %s", pageInfo.getUrl(), link)) == null) {
              Edge edge = graph.addEdge(String.format("%s -> %s", pageInfo.getUrl(), link), pageInfo.getUrl(), link, true);
              //System.out.println("+ Edge " + edge.getId());
            }
          });
        });
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

  private String shorten(String link) {
    return link.startsWith(startUrl.getText()) ? "+" + link.substring(startUrl.getText().length()) : link;
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

  void closeGraphViewer() {
    graphViewer.close();
  }
}
