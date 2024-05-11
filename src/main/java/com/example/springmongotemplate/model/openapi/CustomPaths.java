package com.example.springmongotemplate.model.openapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedHashMap;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomPaths extends LinkedHashMap<String, CustomPathItem> {

    public CustomPaths() {
    }
    public CustomPaths addPathItem(String name, CustomPathItem item) {
        this.put(name, item);
        return this;
    }
}
