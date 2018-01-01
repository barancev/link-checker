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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class Main {

  private static class CliOptions {
    @Parameter(names = "-target", arity = 1, required = true)
    String target;

    @Parameter(names = "-report", arity = 1)
    String report = "report.html";

    @Parameter(names = "-threads", arity = 1)
    int threadCount = 10;
  }

  public static void main(String[] args) throws Exception {
    CliOptions options = new CliOptions();
    JCommander.newBuilder().addObject(options).args(args).build();

    ScanSession session = new ScanSession(new ScanSettings(options.target, options.threadCount));
    session.addListener(pageInfo -> System.out.print("."));
    Thread t = new Thread(session);
    t.start();
    t.join();

    new ScanReport(session.getResults()).saveTo(options.report);
  }
}
