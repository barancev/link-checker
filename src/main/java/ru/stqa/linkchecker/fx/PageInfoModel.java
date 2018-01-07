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

package ru.stqa.linkchecker.fx;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import ru.stqa.linkchecker.PageInfo;

public class PageInfoModel {

  private final StringProperty url;
  private final StringProperty httpStatus;
  private final StringProperty contentType;
  private final StringProperty message;

  public PageInfoModel(PageInfo pageInfo) {
    url = new SimpleStringProperty(pageInfo.getUrl());
    httpStatus = new SimpleStringProperty(Integer.toString(pageInfo.getHttpStatus()));
    message = new SimpleStringProperty(pageInfo.getMessage());
    contentType = new SimpleStringProperty(pageInfo.getContentType());
  }

  public String getUrl() {
    return url.get();
  }

  public StringProperty urlProperty() {
    return url;
  }

  public String getHttpStatus() {
    return httpStatus.get();
  }

  public StringProperty httpStatusProperty() {
    return httpStatus;
  }

  public String getMessage() {
    return message.get();
  }

  public StringProperty messageProperty() {
    return message;
  }

  public String getContentType() {
    return contentType.get();
  }

  public StringProperty contentTypeProperty() {
    return contentType;
  }
}
