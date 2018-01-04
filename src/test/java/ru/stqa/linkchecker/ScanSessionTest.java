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

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScanSessionTest {

  private TestServer testServer;

  ScanSessionTest(TestServer testServer) {
    this.testServer = testServer;
  }

  private ScanResults scan(String startPage) {
    ScanSettings settings = null;
    try {
      settings = new ScanSettings(startPage, 10);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
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

    assertEquals(1, results.getScannedPages().size());

    PageInfo pageInfo = results.getPageInfo(startPage);
    assertEquals(startPage, pageInfo.getUrl());
  }

  @Test
  void canScanPageWithALink() {
    ScanResults results = scan(testServer.page("single_link_page.html"));
    assertEquals(2, results.getScannedPages().size());
  }

  @Test
  void canScanPageWithMultipleLinks() {
    ScanResults results = scan(testServer.page("multi_link_page.html"));
    assertEquals(3, results.getScannedPages().size());
  }

  @Test
  void canScanPageWithDuplicatedLinks() {
    ScanResults results = scan(testServer.page("duplicated_link_page.html"));
    assertEquals(2, results.getScannedPages().size());
  }

  @Test
  void canScanPageWithSelfReferencingLinks() {
    ScanResults results = scan(testServer.page("self_link_page.html"));
    assertEquals(1, results.getScannedPages().size());
  }

  @Test
  void canScanPageWithDuplicatedSelfReferencingLinks() {
    ScanResults results = scan(testServer.page("duplicated_self_link_page.html"));
    assertEquals(1, results.getScannedPages().size());
  }

  @Test
  void canScanMutuallyReferencingPages() {
    ScanResults results = scan(testServer.page("mutual_link_page1.html"));
    assertEquals(2, results.getScannedPages().size());
  }

  @Test
  void canScanPagesWithLoopReferences() {
    ScanResults results = scan(testServer.page("loop_link_page1.html"));
    assertEquals(3, results.getScannedPages().size());
  }

  @Test
  void canDetectBrokenLinks() {
    ScanResults results = scan(testServer.page("broken_link_page.html"));
    assertEquals(3, results.getScannedPages().size()); // jetty adds an extra link
    assertEquals(2, results.getScannedPages().stream().filter(p -> p.getHttpStatus() == 200).count());
  }

  @Test
  void canDetectImageLinks() {
    ScanResults results = scan(testServer.page("page_with_image.html"));
    assertEquals(2, results.getScannedPages().size());
  }

  @Test
  void canDetectBrokenImageLinks() {
    ScanResults results = scan(testServer.page("page_with_broken_image.html"));
    assertEquals(3, results.getScannedPages().size()); // jetty adds an extra link
    assertEquals(2, results.getScannedPages().stream().filter(p -> p.getHttpStatus() == 200).count());
  }

  @Test
  void canDetectAreaLinks() {
    ScanResults results = scan(testServer.page("page_with_map.html"));
    assertEquals(3, results.getScannedPages().size());
  }

  @Test
  void canDetectBrokenAreaLinks() {
    ScanResults results = scan(testServer.page("page_with_broken_map.html"));
    assertEquals(4, results.getScannedPages().size()); // jetty adds an extra link
    assertEquals(3, results.getScannedPages().stream().filter(p -> p.getHttpStatus() == 200).count());
  }

  @Test
  void canDetectObjectLinks() {
    ScanResults results = scan(testServer.page("page_with_object.html"));
    assertEquals(2, results.getScannedPages().size());
  }

  @Test
  void canDetectBrokenObjectLinks() {
    ScanResults results = scan(testServer.page("page_with_broken_object.html"));
    assertEquals(3, results.getScannedPages().size()); // jetty adds an extra link
    assertEquals(2, results.getScannedPages().stream().filter(p -> p.getHttpStatus() == 200).count());
  }

  @Test
  void canDetectPictureLinks() {
    ScanResults results = scan(testServer.page("page_with_picture.html"));
    assertEquals(3, results.getScannedPages().size());
  }

  @Test
  void canDetectBrokenPictureLinks() {
    ScanResults results = scan(testServer.page("page_with_broken_picture.html"));
    assertEquals(4, results.getScannedPages().size()); // jetty adds an extra link
    assertEquals(3, results.getScannedPages().stream().filter(p -> p.getHttpStatus() == 200).count());
  }

  @Test
  void canDetectIframeLinks() {
    ScanResults results = scan(testServer.page("page_with_iframe.html"));
    assertEquals(2, results.getScannedPages().size());
  }

  @Test
  void canDetectBrokenIframeLinks() {
    ScanResults results = scan(testServer.page("page_with_broken_iframe.html"));
    assertEquals(3, results.getScannedPages().size()); // jetty adds an extra link
    assertEquals(2, results.getScannedPages().stream().filter(p -> p.getHttpStatus() == 200).count());
  }

  @Test
  void canDetectAudioLinks() {
    ScanResults results = scan(testServer.page("page_with_audio.html"));
    assertEquals(2, results.getScannedPages().size());
  }

  @Test
  void canDetectBrokenAudioLinks() {
    ScanResults results = scan(testServer.page("page_with_broken_audio.html"));
    assertEquals(3, results.getScannedPages().size()); // jetty adds an extra link
    assertEquals(2, results.getScannedPages().stream().filter(p -> p.getHttpStatus() == 200).count());
  }

  @Test
  void canDetectEmbedLinks() {
    ScanResults results = scan(testServer.page("page_with_embed.html"));
    assertEquals(2, results.getScannedPages().size());
  }

  @Test
  void canDetectBrokenEmbedLinks() {
    ScanResults results = scan(testServer.page("page_with_broken_embed.html"));
    assertEquals(3, results.getScannedPages().size()); // jetty adds an extra link
    assertEquals(2, results.getScannedPages().stream().filter(p -> p.getHttpStatus() == 200).count());
  }

  @Test
  void canDetectVideoLinks() {
    ScanResults results = scan(testServer.page("page_with_video.html"));
    assertEquals(2, results.getScannedPages().size());
  }

  @Test
  void canDetectBrokenVideoLinks() {
    ScanResults results = scan(testServer.page("page_with_broken_video.html"));
    assertEquals(3, results.getScannedPages().size()); // jetty adds an extra link
    assertEquals(2, results.getScannedPages().stream().filter(p -> p.getHttpStatus() == 200).count());
  }

}
