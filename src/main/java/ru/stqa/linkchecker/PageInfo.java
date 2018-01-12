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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static ru.stqa.linkchecker.ScanStatus.*;

public class PageInfo {

  private String url;
  private ScanStatus status;
  private String message = "";

  private int httpStatus;
  private String contentType;
  private Set<String> links = new HashSet<>();

  private PageInfo(String url, ScanStatus status) {
    this.url = url;
    this.status = status;
  }

  public static Builder inProgress(String url) {
    return new PageInfo(url, IN_PROGRESS).newBuilder();
  }

  public static Builder broken(String url) {
    return new PageInfo(url, BROKEN).newBuilder();
  }

  public static Builder done(String url) {
    return new PageInfo(url, DONE).newBuilder();
  }

  private Builder newBuilder() {
    return new Builder();
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public String getContentType() {
    return contentType;
  }

  public class Builder {

    public Builder message(String message) {
      PageInfo.this.message = message;
      return this;
    }

    public Builder httpStatus(int httpStatus) {
      PageInfo.this.httpStatus = httpStatus;
      return this;
    }

    public Builder contentType(String contentType) {
      PageInfo.this.contentType = contentType;
      return this;
    }

    public Builder links(Set<String> links) {
      PageInfo.this.links = Collections.unmodifiableSet(new HashSet<>(links));
      return this;
    }

    public PageInfo build() {
      return PageInfo.this;
    }
  }

  public String getUrl() {
    return url;
  }

  public ScanStatus getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public Set<String> getLinks() {
    return links;
  }
}
