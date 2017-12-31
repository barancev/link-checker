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

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanSession implements Runnable {

  private ScanSettings settings;
  private ScanResults results = new ScanResults();
  private AtomicInteger workerCounter = new AtomicInteger();
  private ConcurrentLinkedQueue<String> urlQueue = new ConcurrentLinkedQueue<>();

  public ScanSession(ScanSettings settings) {
    this.settings = settings;
  }

  @Override
  public void run() {
    urlQueue.add(settings.getStartUrl());

    ExecutorService service = Executors.newFixedThreadPool(settings.getThreadCount());

    while (workerCounter.intValue() > 0 || urlQueue.size() > 0) {
      Optional.ofNullable(urlQueue.poll()).ifPresent(url -> {
        if (results.getPageInfo(url) == null) {
          results.addPageInfo(PageInfo.inProgress(url).build());
          service.submit(new ScanWorker(this, url));
          workerCounter.incrementAndGet();
        }
      });
      Thread.yield();
    }
  }

  public void done(ScanWorker worker) {
    urlQueue.addAll(worker.getPageInfo().getLinks());
    results.addPageInfo(worker.getPageInfo());
    workerCounter.decrementAndGet();
  }

  public ScanResults getResults() {
    return results;
  }
}
