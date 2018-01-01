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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ScanReport {

  private Map<String, List<String>> linkedFrom = new HashMap<>();
  private List<PageInfo> unavailable = new ArrayList<>();
  private List<PageInfo> error400 = new ArrayList<>();
  private List<PageInfo> error500 = new ArrayList<>();

  public ScanReport(ScanResults results) {
    for (PageInfo pageInfo : results.getScannedPages()) {
      if (pageInfo.getHttpStatus() == 0) {
        unavailable.add(pageInfo);
      } else if (pageInfo.getHttpStatus() >= 400 && pageInfo.getHttpStatus() < 500) {
        error400.add(pageInfo);
      } else if (pageInfo.getHttpStatus() >= 500) {
        error500.add(pageInfo);
      }

      pageInfo.getLinks().forEach(url -> {
        List<String> seen = linkedFrom.computeIfAbsent(url, k -> new ArrayList<>());
        seen.add(pageInfo.getUrl());
      });
    }
  }

  public void saveTo(String path) throws IOException {
    try (BufferedWriter out = new BufferedWriter(new FileWriter(path))) {
      out.write("<html><head><title>Link Checker Report</title>");
      out.write("<style>");
      out.write("table { border-collapse: collapse; width: 100%;border: 1px solid gray; vertical-align: top; }");
      out.write("td, th { border: 1px solid #ddd; vertical-align: top; text-align: left; padding: 8px; }");
      out.write("th { background-color: #4CAF50; color: white; }");
      out.write("tr:nth-child(even){ background-color: #f2f2f2; }");
      out.write("</style>");
      out.write("</head><body>");
      out.write("<h1>Link Checker Report</h1>");
      writeSection(out, "Unavailable pages", unavailable);
      writeSection(out, "4xx error", error400);
      writeSection(out, "5xx error", error500);
      out.write("</body></html>");
    }
  }

  private void writeSection(BufferedWriter out, String header, List<PageInfo> pages) throws IOException {
    if (pages.size() > 0) {
      out.write(String.format("<h2>%s</h2>", header));
      out.write("<table><thread><tr><th>URL</th><th>Status</th><th>Seen on pages</th></tr></thread><tbody>");
      for (PageInfo pageInfo : pages) {
        out.write(String.format("<tr><td>%s</td><td>%s</td><td>%s</td></tr>",
          String.format("<a href=\"%s\">%s</a>", pageInfo.getUrl(), pageInfo.getUrl()),
          pageInfo.getHttpStatus() > 0 ? pageInfo.getHttpStatus() : pageInfo.getMessage(),
          linkedFrom.getOrDefault(pageInfo.getUrl(), new ArrayList<>()).stream()
            .map(link -> String.format("<a href=\"%s\">%s</a>", link, link))
            .collect(Collectors.joining("<br/>"))));
      }
      out.write("</tbody></table>");
    }
  }
}
