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

import org.apache.http.Header;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

class ScanWorker implements Runnable {

  private ScanSession session;
  private String url;
  private PageInfo pageInfo;

  ScanWorker(ScanSession session, String url) {
    this.session = session;
    this.url = url;
  }

  @Override
  public void run() {
    try {
      pageInfo = handle(url);
    } catch (IOException e) {
      pageInfo = PageInfo.broken(url).build();
    }
    session.done(this);
  }

  private PageInfo handle(String url) throws IOException {
    return Executor.newInstance().execute(Request.Get(url)).handleResponse(response -> {
      Header[] headers = response.getHeaders("Content-Type");
      if (headers.length == 0) {
        return PageInfo.broken(url).message("No Content-Type header").build();
      }
      if (headers.length > 1) {
        return PageInfo.broken(url).message("Multiple Content-Type headers").build();
      }
      String contentType = headers[0].getValue();
      if (contentType.startsWith("text/")) {
        return PageInfo.done(url)
          .links(getLinks(EntityUtils.toString(response.getEntity()), url)).build();
      }
      return PageInfo.broken(url)
        .message(String.format("Unrecognized Content-Type: %s", contentType)).build();
    });
  }

  private Set<String> getLinks(String text, String baseUrl) {
    Document doc = Jsoup.parse(text, baseUrl);
    Set<String> result = new HashSet<>();
    for (Element a : doc.select("a")) {
      result.add(a.attr("abs:href"));
    }
    return result;
  }

  public PageInfo getPageInfo() {
    return pageInfo;
  }
}
