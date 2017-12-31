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

package ru.stqa.linkchecker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URL;

public class ScanSessionTest {

  @Test
  void emptyScanSessionStops(TestServer testServer) throws InterruptedException {
    URL startPage = testServer.getStartPage();
    ScanSettings settings = new ScanSettings(startPage, 10);
    ScanSession session = new ScanSession(settings);

    Thread t = new Thread(session);
    t.start();
    t.join();

    PageInfo pageInfo = session.getResults().getPageInfo(startPage);
    Assertions.assertEquals(startPage, pageInfo.getUrl());
  }
}
