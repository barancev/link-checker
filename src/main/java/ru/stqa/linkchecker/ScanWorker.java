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
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
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
  private boolean scanLinks;
  private PageInfo pageInfo;

  ScanWorker(ScanSession session, String url, boolean scanLinks) {
    this.session = session;
    this.url = url;
    this.scanLinks = scanLinks;
  }

  @Override
  public void run() {
    try {
      pageInfo = handle(url);
    } catch (IOException e) {
      pageInfo = PageInfo.broken(url).message(e.getMessage()).build();
    }
    session.done(this);
  }

  private PageInfo handle(String url) throws IOException {
    HttpClient httpClient = HttpClients.custom()
      .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
      .build();
    HttpResponse response = httpClient.execute(new HttpGet(url));

    if (scanLinks) {
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
          .httpStatus(response.getStatusLine().getStatusCode())
          .contentType(contentType)
          .links(getLinks(EntityUtils.toString(response.getEntity()), url))
          .build();
      }
      return PageInfo.done(url)
        .httpStatus(response.getStatusLine().getStatusCode())
        .contentType(contentType)
        .build();

    } else {
      return PageInfo.done(url).httpStatus(response.getStatusLine().getStatusCode()).build();
    }
  }

  private Set<String> getLinks(String text, String baseUrl) {
    Document doc = Jsoup.parse(text, baseUrl);
    Set<String> result = new HashSet<>();
    for (Element a : doc.select("a, area")) {
      result.add(a.attr("abs:href"));
    }
    for (Element a : doc.select("img, iframe")) {
      result.add(a.attr("abs:src"));
    }
    for (Element a : doc.select("object")) {
      result.add(a.attr("abs:data"));
    }
    for (Element a : doc.select("source")) {
      result.add(a.attr("abs:srcset"));
    }
    return result;
  }

  public PageInfo getPageInfo() {
    return pageInfo;
  }
}
