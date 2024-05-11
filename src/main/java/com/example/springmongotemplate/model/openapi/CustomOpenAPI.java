package com.example.springmongotemplate.model.openapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomOpenAPI {

    private String openapi = "3.0.1";
    private Info info = null;
    private List<Server> servers = null;
    private CustomPaths paths;
}
