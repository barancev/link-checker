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

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import ru.stqa.linkchecker.TestServer;

import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ScannerModelTest {

  private TestServer testServer;

  ScannerModelTest(TestServer testServer) {
    this.testServer = testServer;
  }

  private static class SessionStatus {
    private boolean finished = false;
  }

  @Test
  public void canScan() throws MalformedURLException {
    ScannerModel model = new ScannerModel();
    model.startUrlProperty().setValue(testServer.page("multi_link_page.html"));
    SessionStatus status = new SessionStatus();
    model.setFinishHandler(() -> status.finished = true);
    model.startScan();
    while (!status.finished) {
      Thread.yield();
    }
    assertEquals(3, model.getGraph().getNodeCount());
  }

  @Test
  public void canSaveAndLoad() throws IOException {
    String startUrl = testServer.page("multi_link_page.html");
    ScannerModel model = new ScannerModel();
    model.startUrlProperty().setValue(startUrl);
    assertFalse(model.isSaved());
    assertNull(model.getSavedTo());

    Path tempFile = Files.createTempFile("project", ".json");
    model.saveTo(tempFile);

    assertTrue(model.isSaved());
    assertEquals(tempFile, model.getSavedTo());

    JsonElement savedProject = new JsonParser().parse(new FileReader(tempFile.toFile()));
    JsonObject settings = savedProject.getAsJsonObject().getAsJsonObject("settings");
    assertEquals(startUrl, settings.get("startUrl").getAsString());
    assertEquals(10, settings.get("threadCount").getAsInt());

    ScannerModel model2 = new ScannerModel();
    model2.loadFrom(tempFile);

    assertTrue(model2.isSaved());
    assertEquals(tempFile, model.getSavedTo());
    assertEquals(startUrl, model2.startUrlProperty().get());
  }

}
