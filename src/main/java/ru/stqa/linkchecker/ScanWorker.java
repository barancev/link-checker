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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
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
    } catch (Throwable e) {
      pageInfo = PageInfo.broken(url).message(e.getMessage()).build();
    }
    session.done(this);
  }

  private PageInfo handle(String url) throws IOException {
    try (CloseableHttpResponse response = session.getHttpClient().execute(new HttpGet(url), HttpClientContext.create())) {
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
  }

  private Set<String> getLinks(String text, String baseUrl) {
    Document doc = Jsoup.parse(text, baseUrl);
    Set<String> result = new HashSet<>();
    for (Element e : doc.select("a, area, link")) {
      addLink(result, e, "abs:href");
    }
    for (Element e : doc.select("img, iframe, audio, embed, video, source, track, script, input")) {
      addLink(result, e, "abs:src");
    }
    for (Element e : doc.select("video")) {
      addLink(result, e, "abs:poster");
    }
    for (Element e : doc.select("object")) {
      addLink(result, e, "abs:data");
    }
    for (Element e : doc.select("source")) {
      addLink(result, e, "abs:srcset");
    }
    for (Element e : doc.select("form")) {
      addLink(result, e, "abs:action");
    }
    for (Element e : doc.select("input, button")) {
      addLink(result, e, "abs:formaction");
    }
    return result;
  }

  private Set<String> getLinks2(String text, String baseUrl) {
    Document doc = Jsoup.parse(text, baseUrl);
    Set<String> result = new HashSet<>();
    for (Element e : doc.select("a, area, link, img, iframe, audio, embed, video, source, track, script, input, object, form, button")) {
      addLink(result, e, "abs:href");
      addLink(result, e, "abs:src");
      addLink(result, e, "abs:poster");
      addLink(result, e, "abs:data");
      addLink(result, e, "abs:srcset");
      addLink(result, e, "abs:action");
      addLink(result, e, "abs:formaction");
    }
    return result;
  }

  private void addLink(Set<String> result, Element e, String attrName) {
    Optional.of(e.attr(attrName)).filter(attr -> attr.length() > 0).ifPresent(result::add);
  }

  public PageInfo getPageInfo() {
    return pageInfo;
  }
}
