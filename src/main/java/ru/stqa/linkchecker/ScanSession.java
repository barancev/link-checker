/*
 * Copyright 2017 Alexei Barantsev
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

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ScanSession implements Runnable {

  private ScanSettings settings;
  private ScanResults results = new ScanResults();
  private AtomicInteger workerCounter = new AtomicInteger();
  private ConcurrentLinkedQueue<String> urlQueue = new ConcurrentLinkedQueue<>();
  private List<Consumer<PageInfo>> listeners = new ArrayList<>();

  private CloseableHttpClient httpclient;
  private boolean interrupted = false;
  private boolean stopped = false;

  public ScanSession(ScanSettings settings) {
    this.settings = settings;

    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    cm.setMaxTotal(settings.getThreadCount());
    cm.setDefaultMaxPerRoute(settings.getThreadCount());

    httpclient = HttpClients.custom().setConnectionManager(cm)
      .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
      .build();
  }

  public void addListener(Consumer<PageInfo> listener) {
    listeners.add(listener);
  }

  @Override
  public void run() {
    urlQueue.add(settings.getStartUrl());

    ExecutorService service = Executors.newFixedThreadPool(settings.getThreadCount());

    while (!interrupted && (workerCounter.intValue() > 0 || urlQueue.size() > 0)) {
      Optional.ofNullable(urlQueue.poll()).ifPresent(url -> {
        if (results.getPageInfo(url) == null) {
          PageInfo pageInfo = PageInfo.inProgress(url).build();
          listeners.forEach(l -> l.accept(pageInfo));
          results.addPageInfo(pageInfo);
          service.submit(new ScanWorker(this, url, url.startsWith(settings.getBaseUrl())));
          workerCounter.incrementAndGet();
        }
      });
      Thread.yield();
    }

    service.shutdownNow();
    stopped = true;
  }

  CloseableHttpClient getHttpClient() {
    return httpclient;
  }

  public void done(ScanWorker worker) {
    urlQueue.addAll(worker.getPageInfo().getLinks());
    results.addPageInfo(worker.getPageInfo());
    workerCounter.decrementAndGet();
    listeners.forEach(l -> l.accept(worker.getPageInfo()));
  }

  public void interrupt() {
    interrupted = true;
  }

  public ScanResults getResults() {
    return results;
  }

  public boolean isStopped() {
    return stopped;
  }
}
