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
import java.util.stream.Stream;

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
    assert200(results, 1);
    assertEquals(startPage, results.getPageInfo(startPage).getUrl());
  }

  @Test
  void canScanPageWithALink() {
    assert200(scan(testServer.page("single_link_page.html")), 2);
  }

  @Test
  void canScanPageWithMultipleLinks() {
    assert200(scan(testServer.page("multi_link_page.html")), 3);
  }

  @Test
  void canScanPageWithDuplicatedLinks() {
    assert200(scan(testServer.page("duplicated_link_page.html")), 2);
  }

  @Test
  void canScanPageWithSelfReferencingLinks() {
    assert200(scan(testServer.page("self_link_page.html")), 1);
  }

  @Test
  void canScanPageWithDuplicatedSelfReferencingLinks() {
    assert200(scan(testServer.page("duplicated_self_link_page.html")), 1);
  }

  @Test
  void canScanMutuallyReferencingPages() {
    assert200(scan(testServer.page("mutual_link_page1.html")), 2);
  }

  @Test
  void canScanPagesWithLoopReferences() {
    assert200(scan(testServer.page("loop_link_page1.html")), 3);
  }

  @Test
  void canDetectBrokenLinks() {
    assert200and404(scan(testServer.page("broken_link_page.html")), 1, 1);
  }

  @Test
  void canDetectImageLinks() {
    assert200(scan(testServer.page("page_with_image.html")), 2);
  }

  @Test
  void canDetectBrokenImageLinks() {
    assert200and404(scan(testServer.page("page_with_broken_image.html")), 1, 1);
  }

  @Test
  void canDetectAreaLinks() {
    assert200(scan(testServer.page("page_with_map.html")), 3);
  }

  @Test
  void canDetectBrokenAreaLinks() {
    assert200and404(scan(testServer.page("page_with_broken_map.html")), 2, 1);
  }

  @Test
  void canDetectObjectLinks() {
    assert200(scan(testServer.page("page_with_object.html")), 2);
  }

  @Test
  void canDetectBrokenObjectLinks() {
    assert200and404(scan(testServer.page("page_with_broken_object.html")), 1, 1);
  }

  @Test
  void canDetectPictureLinks() {
    assert200(scan(testServer.page("page_with_picture.html")), 3);
  }

  @Test
  void canDetectBrokenPictureLinks() {
    assert200and404(scan(testServer.page("page_with_broken_picture.html")), 2, 1);
  }

  @Test
  void canDetectIframeLinks() {
    assert200(scan(testServer.page("page_with_iframe.html")), 2);
  }

  @Test
  void canDetectBrokenIframeLinks() {
    assert200and404(scan(testServer.page("page_with_broken_iframe.html")), 1, 1);
  }

  @Test
  void canDetectAudioLinks() {
    assert200(scan(testServer.page("page_with_audio.html")), 2);
  }

  @Test
  void canDetectBrokenAudioLinks() {
    assert200and404(scan(testServer.page("page_with_broken_audio.html")), 1, 1);
  }

  @Test
  void canDetectAudioSourceLinks() {
    assert200(scan(testServer.page("page_with_audio2.html")), 2);
  }

  @Test
  void canDetectBrokenAudioSourceLinks() {
    assert200and404(scan(testServer.page("page_with_broken_audio2.html")), 1, 1);
  }

  @Test
  void canDetectEmbedLinks() {
    assert200(scan(testServer.page("page_with_embed.html")), 2);
  }

  @Test
  void canDetectBrokenEmbedLinks() {
    assert200and404(scan(testServer.page("page_with_broken_embed.html")), 1, 1);
  }

  @Test
  void canDetectVideoLinks() {
    assert200(scan(testServer.page("page_with_video.html")), 2);
  }

  @Test
  void canDetectBrokenVideoLinks() {
    assert200and404(scan(testServer.page("page_with_broken_video.html")), 1, 1);
  }

  @Test
  void canDetectVideoPosterLinks() {
    assert200(scan(testServer.page("page_with_video_poster.html")), 3);
  }

  @Test
  void canDetectBrokenVideoPosterLinks() {
    assert200and404(scan(testServer.page("page_with_broken_video_poster.html")), 2, 1);
  }

  @Test
  void canDetectVideoSourceLinks() {
    assert200(scan(testServer.page("page_with_video2.html")), 2);
  }

  @Test
  void canDetectBrokenVideoSourceLinks() {
    assert200and404(scan(testServer.page("page_with_broken_video2.html")), 1, 1);
  }

  @Test
  void canDetectVideoTrackLinks() {
    assert200(scan(testServer.page("page_with_video_subtitles.html")), 3);
  }

  @Test
  void canDetectBrokenVideoTrackLinks() {
    assert200and404(scan(testServer.page("page_with_broken_video_subtitles.html")), 2, 1);
  }

  @Test
  void canDetectScriptLinks() {
    assert200(scan(testServer.page("page_with_simple_script.html")), 2);
  }

  @Test
  void canDetectBrokenScriptLinks() {
    assert200and404(scan(testServer.page("page_with_missing_script.html")), 1, 1);
  }

  @Test
  void canDetectFormActionLinks() {
    assert200(scan(testServer.page("page_with_form.html")), 2);
  }

  @Test
  void canDetectBrokenFormActionLinks() {
    assert200and404(scan(testServer.page("page_with_broken_form.html")), 1, 1);
  }

  @Test
  void canDetectFormSubmitLinks() {
    assert200(scan(testServer.page("page_with_form_submit.html")), 3);
  }

  @Test
  void canDetectBrokenFormSubmitLinks() {
    assert200and404(scan(testServer.page("page_with_broken_form_submit.html")), 2, 1);
  }

  @Test
  void canDetectFormSubmitButtonLinks() {
    assert200(scan(testServer.page("page_with_form_submit2.html")), 3);
  }

  @Test
  void canDetectBrokenFormSubmitButtonLinks() {
    assert200and404(scan(testServer.page("page_with_broken_form_submit2.html")), 2, 1);
  }

  @Test
  void canDetectFormInputImageLinks() {
    assert200(scan(testServer.page("page_with_form_image.html")), 3);
  }

  @Test
  void canDetectBrokenFormInputImageLinks() {
    assert200and404(scan(testServer.page("page_with_broken_form_image.html")), 2, 1);
  }

  @Test
  void canDetectCssLinks() {
    assert200(scan(testServer.page("page_with_simple_css.html")), 2);
  }

  @Test
  void canDetectBrokenCssLinks() {
    assert200and404(scan(testServer.page("page_with_missing_css.html")), 1, 1);
  }

  @Test
  void canDetectLinksRelativeToBase() {
    Stream.of("page_with_map.html", "page_with_video_poster.html", "page_with_form_image.html",
      "page_with_video_subtitles.html", "page_with_form_submit.html", "page_with_form_submit2.html",
      "page_with_picture.html")
      .map(s -> "subdir/" + s)
      .forEach(s -> assert200(scan(testServer.page(s)), 3));

    Stream.of("single_link_page.html", "page_with_audio.html", "page_with_audio2.html", "page_with_embed.html",
      "page_with_form.html", "page_with_iframe.html", "page_with_image.html", "page_with_object.html",
      "page_with_simple_css.html", "page_with_simple_script.html", "page_with_video.html",
      "page_with_video2.html")
      .map(s -> "subdir/" + s)
      .forEach(s -> assert200(scan(testServer.page(s)), 2));
  }

  private void assert200(ScanResults results, int count) {
    assertEquals(count, results.getScannedPages().size());
    assertEquals(count, results.getScannedPages().stream().filter(p -> p.getHttpStatus() == 200).count());
  }

  private void assert200and404(ScanResults results, int count200, int count404) {
    if (count404 > 0) {
      // jetty adds an extra link
      count200 += 1;
    }
    assertEquals(count200 + count404, results.getScannedPages().size());
    assertEquals(count200, results.getScannedPages().stream().filter(p -> p.getHttpStatus() == 200).count());
    assertEquals(count404, results.getScannedPages().stream().filter(p -> p.getHttpStatus() == 404).count());
  }

}
