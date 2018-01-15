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

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import ru.stqa.linkchecker.ScanSession;
import ru.stqa.linkchecker.ScanSettings;
import ru.stqa.linkchecker.ScanStatus;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ScannerModel {

  private boolean saved = false;
  private Path savedTo;

  private StringProperty startUrl = new SimpleStringProperty("http://localhost/");
  private IntegerProperty threadCount = new SimpleIntegerProperty(10);
  private ObservableList<PageInfoModel> pages = FXCollections.observableArrayList();

  private ScanSession session;

  private Graph graph;

  private Runnable finishHandler;

  public ScannerModel() {
    graph = new SingleGraph("embedded");
    graph.addAttribute("ui.stylesheet", loadStyleSheet());
  }

  private String loadStyleSheet() {
    InputStream style = LinkCheckerController.class.getResourceAsStream("/graph.css");
    return new Scanner(style, "utf-8").useDelimiter("\\Z").next();
  }

  public Graph getGraph() {
    return graph;
  }

  public StringProperty startUrlProperty() {
    return startUrl;
  }

  public ObservableList<PageInfoModel> getPages() {
    return pages;
  }

  public void save() {

  }

  public void saveTo(Path file) throws IOException {
    try (Writer out = new FileWriter(file.toFile())) {
      out.write(new Gson().toJson(toExternal()));
    }
    saved = true;
    savedTo = file;
  }

  private Object toExternal() throws MalformedURLException {
    return new ExternalModel(new ScanSettings(startUrl.get(), threadCount.get()));
  }

  public void loadFrom(Path file) throws IOException {
    String json = Files.lines(file).collect(Collectors.joining(""));
    ExternalModel external = new Gson().fromJson(json, ExternalModel.class);
    startUrl.setValue(external.settings.getStartUrl());
    threadCount.setValue(external.settings.getThreadCount());
    saved = true;
    savedTo = file;
  }

  public void reset() {
    saved = false;
    savedTo = null;
    pages.clear();
    graph.clear();
    graph.addAttribute("ui.stylesheet", loadStyleSheet());
  }

  public void startScan() throws MalformedURLException {
    saved = false;
    Node startNode = graph.addNode(startUrl.get());
    startNode.addAttribute("ui.label", startUrl.get());
    startNode.addAttribute("ui.class", "start");

    session = new ScanSession(new ScanSettings(startUrl.get(), threadCount.get()));

    session.addListener(pageInfo -> {
      if (pageInfo.getStatus() != ScanStatus.IN_PROGRESS) {
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
        Platform.runLater(() -> pages.add(new PageInfoModel(pageInfo)));
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
      finishHandler.run();
    });
    waiter.start();
  }

  private String shorten(String link) {
    return link.startsWith(startUrl.get()) ? "+" + link.substring(startUrl.get().length()) : link;
  }

  public void setFinishHandler(Runnable finishHandler) {
    this.finishHandler = finishHandler;
  }

  public void interruptScan() {
    session.interrupt();
    while (!session.isStopped()) {
      Thread.yield();
    }
  }

  public boolean isSaved() {
    return saved;
  }

  public Path getSavedTo() {
    return savedTo;
  }

  private class ExternalModel {
    private ScanSettings settings;

    public ExternalModel(ScanSettings settings) {
      this.settings = settings;
    }
  }
}
