package com.spring.demo.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Greeting(long id, String content) {

  @Override
  public String toString() {
    return "Greeting{" +
        "id=" + id +
        ", content='" + content + '\'' +
        '}';
  }
}
