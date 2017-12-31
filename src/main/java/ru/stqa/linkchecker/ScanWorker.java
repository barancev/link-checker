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

import java.io.IOException;
import java.net.URL;

public class ScanWorker implements Runnable {

    private ScanSession session;
    private URL url;
    private PageInfo pageInfo;

    public ScanWorker(ScanSession session, URL url) {
        this.session = session;
        this.url = url;
    }

    @Override
    public void run() {
        try {
            pageInfo = session.getUrlHandlerFactory().getUrlHandler().handle(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        session.done(this);
    }

    public PageInfo getPageInfo() {
        return pageInfo;
    }
}