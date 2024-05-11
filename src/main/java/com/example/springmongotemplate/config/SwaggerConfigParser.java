package com.example.springmongotemplate.config;

import com.example.springmongotemplate.model.openapi.CustomOpenAPI;
import com.example.springmongotemplate.model.openapi.CustomOperation;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple7;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@Data
@Slf4j
public class SwaggerConfigParser {

//  Fields in the map:
//    <path, method>: <queryType, query, update, projection, sort, Request Class, Response Class>
    private Map<Tuple2<String, String>, Tuple7<String, String, String, String, String, String, String>> queryDetails = new HashMap<>();

    @PostConstruct
    public void init() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("openapi.json");
        ObjectMapper mapper = new ObjectMapper();
        CustomOpenAPI openAPI = mapper.readValue(is, CustomOpenAPI.class);
        log.info("Custom Swagger: " + mapper.writeValueAsString(openAPI));
//        JsonNode jsonMap = mapper.readTree(is);
//        System.out.println(jsonMap);
//        SwaggerParseResult openAPI = new OpenAPIV3Parser().parseJsonNode(null, jsonMap);
//        SwaggerParseResult openAPI = new OpenAPIV3Parser().readLocation("openapi.json", null, null);
//        log.info("SwaggerParseResult:: " + new ObjectMapper().writeValueAsString(openAPI));

        openAPI.getPaths().forEach((path, pathItem) -> {
            Arrays.stream(pathItem.getClass().getDeclaredFields()).forEach((field) -> {
                String method = field.getName();
                CustomOperation customOperation = switch (method) {
                    case "get" -> pathItem.getGet();
                    case "put" -> pathItem.getPut();
                    case "post" -> pathItem.getPost();
                    case "delete" -> pathItem.getDelete();
                    case "patch" -> pathItem.getPatch();
                    default -> null;
                };
                if (customOperation == null) {
                    return;
                }

                String operation = Objects.toString(customOperation.getOperation(), "");
                String query = Objects.toString(customOperation.getQuery(), "");
                String update = Objects.toString(customOperation.getUpdate(), "");
                String projection = Objects.toString(customOperation.getProjection(), "");
                String sort = Objects.toString(customOperation.getSort(), "");

                String baseModelPath = "com.example.springmongotemplate.model";
                String requestBodyPath = "";
                if (customOperation.getRequestBody() != null) {
                    String requestBodyRef = customOperation.getRequestBody().get$ref();
                    requestBodyPath = baseModelPath + ".request." + requestBodyRef.replace("#/components/schemas/request/", "");
                }
                String responseBodyPath = "";
                if (customOperation.getResponses().get("default").getContent().get("application/json").getSchema().get$ref() != null) {
                    String responseBodyRef = customOperation.getResponses().get("default").getContent().get("application/json").getSchema().get$ref();
                    responseBodyPath = baseModelPath + ".response." + responseBodyRef.replace("#/components/schemas/response/", "");
                }

                log.info("query:: " + Tuples.of(path, method) + Tuples.of(query, requestBodyPath, responseBodyPath));
                queryDetails.put(Tuples.of(path, method), Tuples.of(operation, query, update, projection, sort, requestBodyPath, responseBodyPath));

            });
        });
        log.info("Final Query Details:: " + queryDetails);
    }

}
