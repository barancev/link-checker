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

class ScanSessionTest {

  private TestServer testServer;

  ScanSessionTest(TestServer testServer) {
    this.testServer = testServer;
  }

  private ScanResults scan(String startPage) {
    ScanSettings settings = new ScanSettings(startPage, 10);
    ScanSession session = new ScanSession(settings);

    Thread t = new Thread(session);
    t.start();
    try {
      t.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    return session.getResults();
  }

  @Test
  void canScanPageWithNoLinks() {
    String startPage = testServer.page("simple_page.html");
    ScanResults results = scan(startPage);

    Assertions.assertEquals(1, results.getScannedUrls().size());

    PageInfo pageInfo = results.getPageInfo(startPage);
    Assertions.assertEquals(startPage, pageInfo.getUrl());
  }

  @Test
  void canScanPageWithALink() {
    ScanResults results = scan(testServer.page("single_link_page.html"));
    Assertions.assertEquals(2, results.getScannedUrls().size());
  }

  @Test
  void canScanPageWithMultipleLinks() {
    ScanResults results = scan(testServer.page("multi_link_page.html"));
    Assertions.assertEquals(3, results.getScannedUrls().size());
  }

  @Test
  void canScanPageWithDuplicatedLinks() {
    ScanResults results = scan(testServer.page("duplicated_link_page.html"));
    Assertions.assertEquals(2, results.getScannedUrls().size());
  }

  @Test
  void canScanPageWithSelfReferencingLinks() {
    ScanResults results = scan(testServer.page("self_link_page.html"));
    Assertions.assertEquals(1, results.getScannedUrls().size());
  }

  @Test
  void canScanPageWithDuplicatedSelfReferencingLinks() {
    ScanResults results = scan(testServer.page("duplicated_self_link_page.html"));
    Assertions.assertEquals(1, results.getScannedUrls().size());
  }

  @Test
  void canScanMutuallyReferencingPages() {
    ScanResults results = scan(testServer.page("mutual_link_page1.html"));
    Assertions.assertEquals(2, results.getScannedUrls().size());
  }

  @Test
  void canScanPagesWithLoopReferences() {
    ScanResults results = scan(testServer.page("loop_link_page1.html"));
    Assertions.assertEquals(3, results.getScannedUrls().size());
  }

}
