package com.example.springmongotemplate.model.openapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomOperation {
    private List<String> tags = null;
    private String summary = null;
    private String description = null;
    private String query = null;
    private String update = null;
    private String operation = null;
    private String projection = null;
    private String sort = null;
    private ExternalDocumentation externalDocs = null;
    private String operationId = null;
    private List<Parameter> parameters = null;
    private RequestBody requestBody = null;
    private ApiResponses responses = null;
    private Map<String, Callback> callbacks = null;
    private Boolean deprecated = null;
    private List<SecurityRequirement> security = null;
    private List<Server> servers = null;
    private Map<String, Object> extensions = null;
}
