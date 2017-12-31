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

import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

public class DefaultUrlHandler implements UrlHandler {

    @Override
    public PageInfo handle(URL url) {
        try {
            Response response = Executor.newInstance().execute(Request.Get(url.toString()));
            StatusLine status = response.returnResponse().getStatusLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new PageInfo(url);
    }
}
