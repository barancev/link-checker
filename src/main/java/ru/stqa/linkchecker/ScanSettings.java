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

import java.net.MalformedURLException;
import java.net.URL;

public class ScanSettings {

  private String startUrl;
  private String startUrlHost;
  private int threadCount;

  public ScanSettings(String startUrl, int threadCount) throws MalformedURLException {
    this.startUrl = startUrl;
    this.startUrlHost = new URL(startUrl).getHost();
    this.threadCount = threadCount;
  }

  public String getStartUrl() {
    return startUrl;
  }

  public String getStartUrlHost() {
    return startUrlHost;
  }

  public int getThreadCount() {
    return threadCount;
  }
}
