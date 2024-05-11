package com.example.springmongotemplate.model.openapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomPathItem {
    private String summary = null;
    private String description = null;
    private CustomOperation get = null;
    private CustomOperation put = null;
    private CustomOperation post = null;
    private CustomOperation delete = null;
    private CustomOperation options = null;
    private CustomOperation head = null;
    private CustomOperation patch = null;
    private CustomOperation trace = null;
    private List<Server> servers = null;
    private List<Parameter> parameters = null;
    private String $ref = null;
    private Map<String, Object> extensions = null;
}
