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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import ru.stqa.linkchecker.ScanSession;
import ru.stqa.linkchecker.ScanSettings;
import ru.stqa.linkchecker.ScanStatus;

import java.io.InputStream;
import java.util.Scanner;

public class ScannerModel {

  private ScanSession session;

  private Graph graph;

  private ObservableList<PageInfoModel> pages = FXCollections.observableArrayList();

  private Runnable finishHandler;

  public Graph getGraph() {
    return graph;
  }

  public ScannerModel() {
    graph = new SingleGraph("embedded");
    graph.addAttribute("ui.stylesheet", loadStyleSheet());
  }

  private String loadStyleSheet() {
    InputStream style = LinkCheckerController.class.getResourceAsStream("/graph.css");
    return new Scanner(style, "utf-8").useDelimiter("\\Z").next();
  }

  public ObservableList<PageInfoModel> getPages() {
    return pages;
  }

  public void reset() {
    pages.clear();
    graph.clear();
    graph.addAttribute("ui.stylesheet", loadStyleSheet());
  }

  public void startScan(ScanSettings settings) {
    Node startNode = graph.addNode(settings.getStartUrl());
    startNode.addAttribute("ui.label", settings.getStartUrl());
    startNode.addAttribute("ui.class", "start");

    session = new ScanSession(settings);

    session.addListener(pageInfo -> {
      if (pageInfo.getStatus() != ScanStatus.IN_PROGRESS) {
        Platform.runLater(() -> {
          pages.add(new PageInfoModel(pageInfo));
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
      finishHandler.run();
    });
    waiter.start();
  }

  private String shorten(String link) {
    return link.startsWith(session.getStartUrl()) ? "+" + link.substring(session.getStartUrl().length()) : link;
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
}
