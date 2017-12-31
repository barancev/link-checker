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

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import java.net.MalformedURLException;
import java.net.URL;

public class TestServer {

    private static TestServer singleton;

    private Server jetty;

    private TestServer() {
        jetty = new Server(0);
        ResourceHandler handler = new ResourceHandler();
        handler.setDirectoriesListed(true);
        handler.setResourceBase("src/test/resources/web");
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { handler, new DefaultHandler() });
        jetty.setHandler(handlers);
        try {
            jetty.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TestServer getInstance() {
        if (singleton == null) {
            singleton = new TestServer();
        }
        return singleton;
    }

    public URL getStartPage() {
        int port = ((ServerConnector) jetty.getConnectors()[0]).getLocalPort();
        try {
            return new URL(String.format("http://127.0.0.1:%s/", port));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
