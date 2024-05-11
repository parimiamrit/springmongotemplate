package com.example.springmongotemplate.controller;

import com.example.springmongotemplate.config.SwaggerConfigParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.reactivestreams.client.MongoClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.CorePublisher;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple7;
import reactor.util.function.Tuples;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class MongoTemplateController {

    @Autowired
    ReactiveMongoTemplate reactiveMongoTemplate;

    @Autowired
    MongoClient reactiveMongoClient;

    @Autowired
    SwaggerConfigParser swaggerConfigParser;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @RequestMapping(value = "/**")
    public CorePublisher<?> genericFunction(HttpServletRequest httpRequest, @RequestBody(required = false) JsonNode requestBody) throws Exception {
        log.info("Request path: {}, Method: {}", httpRequest.getRequestURI(), httpRequest.getMethod());
        if (swaggerConfigParser.getQueryDetails().containsKey(Tuples.of(httpRequest.getRequestURI(), httpRequest.getMethod().toLowerCase()))) {
            Tuple7<String, String, String, String, String, String, String> queryDetailsTuple = swaggerConfigParser.getQueryDetails().get(Tuples.of(httpRequest.getRequestURI(), httpRequest.getMethod().toLowerCase()));
            String queryType = queryDetailsTuple.getT1();
            String rawQuery = queryDetailsTuple.getT2();
            String update = queryDetailsTuple.getT3();
            String projection = queryDetailsTuple.getT4();
            String sort = queryDetailsTuple.getT5();
            String requestBodyPath = queryDetailsTuple.getT6();
            String responseBodyPath = queryDetailsTuple.getT7();

            if (StringUtils.isNotEmpty(requestBodyPath)) {
                try {
                    objectMapper.convertValue(requestBody, Class.forName(requestBodyPath));
                }
                catch (ClassNotFoundException e) {
                    log.error("Did not find requestBody Class with path: " + requestBodyPath);
                    return Mono.just(new ResponseEntity<Object>("{\"message\":\"Request Body Class Not Found\"}", HttpStatus.INTERNAL_SERVER_ERROR));
                }
                catch (Exception e) {
                    log.error("Failed to convert with error: " + e.fillInStackTrace());
                    return Mono.just(new ResponseEntity<Object>("{\"message\":\"Request Body could not be mapped\"}", HttpStatus.BAD_REQUEST));
                }
            }

            Query finalMongoQuery;
            switch (queryType) {
                case "find":
                    finalMongoQuery = prepareMongoQuery(rawQuery, requestBody, projection, sort);
                    return reactiveMongoTemplate.find((finalMongoQuery), Class.forName(responseBodyPath));
                case "findOne":
                    finalMongoQuery = prepareMongoQuery(rawQuery, requestBody, projection, sort);
                    return reactiveMongoTemplate.findOne((finalMongoQuery), Class.forName(responseBodyPath));
                case "findAll":
                    return reactiveMongoTemplate.findAll(Class.forName(responseBodyPath));
                case "updateFirst":
                case "updateOne":
                    finalMongoQuery = prepareMongoQuery(rawQuery, requestBody.get("query"), projection, sort);
                    Update finalMongoUpdate = prepareMongoUpdate(update, requestBody.get("update"));
                    log.info("final Update: " + finalMongoUpdate);
                    return reactiveMongoTemplate.updateFirst(finalMongoQuery, finalMongoUpdate, Class.forName(responseBodyPath));
                case "save":
                    if (StringUtils.isNotEmpty(requestBodyPath)) {
                        return reactiveMongoTemplate.save(objectMapper.convertValue(requestBody, Class.forName(requestBodyPath)));
                    } else {
                        return reactiveMongoTemplate.save(objectMapper.convertValue(requestBody, Class.forName(responseBodyPath)));
                    }
                case "delete":
                    finalMongoQuery = prepareMongoQuery(rawQuery, requestBody, null, null);
                    return reactiveMongoTemplate.remove(finalMongoQuery, Class.forName(requestBodyPath));
                case "aggregate":
                    TypedAggregation<?> finalMongoAggregation = prepareMongoAggregate(rawQuery, requestBody, responseBodyPath);
                    return reactiveMongoTemplate.aggregate(finalMongoAggregation, Class.forName(responseBodyPath));
            }
        }
        return null;
    }

    private Query prepareMongoQuery(String rawQuery, JsonNode requestBodyQuery, String projection, String sort) throws JsonProcessingException {
        Map<String, Object> finalQueryMap = new HashMap<>();
        Map<String, Object> requestBodyMap = objectMapper.convertValue(requestBodyQuery, new TypeReference<>() {});

        String finalQuery = StrSubstitutor.replace(rawQuery, requestBodyMap);
        Map<String, Object> queryMap = objectMapper.readValue(finalQuery, new TypeReference<>() {});
        for (String field: queryMap.keySet()) {
            log.info("field:" + field);
            if (!queryMap.get(field).toString().matches(".*\\$\\{[^}]*}.*")) {
                finalQueryMap.put(field, queryMap.get(field));
            }
        }
        Query basicQuery = new BasicQuery(objectMapper.writeValueAsString(finalQueryMap));
        if (StringUtils.isNotEmpty(projection)) {
            objectMapper.readTree(projection).fields().forEachRemaining((entry) -> {
                if (entry.getValue().asInt() == 1) {
                    basicQuery.fields().include(entry.getKey());
                }
            });
        }
        if (StringUtils.isNotEmpty(sort)) {
            objectMapper.readTree(sort).fields().forEachRemaining((entry) -> {
                if (entry.getValue().asInt() == 1) {
                    basicQuery.with(Sort.by(Sort.Direction.ASC, entry.getKey()));
                }
                else {
                    basicQuery.with(Sort.by(Sort.Direction.DESC, entry.getKey()));
                }
            });
        }
        log.info("Final Basic Query: " + basicQuery);
        return basicQuery;
    }

    private Update prepareMongoUpdate(String update, JsonNode requestBodyUpdate) throws JsonProcessingException {
        Map<String, Map<String, Object>> finalUpdateMap = new HashMap<>();
        Map<String, Object> requestBodyMap = objectMapper.convertValue(requestBodyUpdate, new TypeReference<>() {});
        Map<String, Map<String, Object>> updateMap = objectMapper.readValue(update, new TypeReference<>() {});
        log.info("requestBodyMap: " + requestBodyMap);
        log.info("updateMap: " + updateMap);

        String finalUpdate = StrSubstitutor.replace(update, requestBodyMap);
        Map<String, Map<String, Object>> substitutedUpdateMap = objectMapper.readValue(finalUpdate, new TypeReference<>() {});
        for (String updateOperation: updateMap.keySet()){
            Map<String, Object> updateFieldMap = new HashMap<>();
            for (String field: updateMap.get(updateOperation).keySet()) {
                log.info("field:" + field + ", value:" + updateMap.get(updateOperation).get(field) + updateMap.get(updateOperation).get(field).toString());
                if (updateMap.get(updateOperation).get(field).toString().matches(".*\\$\\{[^}]*}.*")) {
                    log.info("replaced variable in query: " + updateMap.get(updateOperation).get(field));
                    if (requestBodyMap.containsKey(field)) {
                        if (requestBodyMap.get(field) instanceof String) {
                            updateFieldMap.put(field, substitutedUpdateMap.get(updateOperation).get(field).toString());
                        } else if (requestBodyMap.get(field) instanceof Integer) {
                            updateFieldMap.put(field, Integer.parseInt(substitutedUpdateMap.get(updateOperation).get(field).toString()));
                        } else if (requestBodyMap.get(field) instanceof Double) {
                            updateFieldMap.put(field, Double.parseDouble(substitutedUpdateMap.get(updateOperation).get(field).toString()));
                        } else if (requestBodyMap.get(field) instanceof Number) {
                            updateFieldMap.put(field, Long.parseLong(substitutedUpdateMap.get(updateOperation).get(field).toString()));
                        } else if (requestBodyMap.get(field) instanceof Boolean) {
                            updateFieldMap.put(field, Boolean.parseBoolean(substitutedUpdateMap.get(updateOperation).get(field).toString()));
                        } else {
                            updateFieldMap.put(field, substitutedUpdateMap.get(updateOperation).get(field));
                        }
                    }
                } else {
                    updateFieldMap.put(field, updateMap.get(updateOperation).get(field));
                }
            }
            finalUpdateMap.put(updateOperation, updateFieldMap);
        }
        log.info("finalUpdateMap: " + finalUpdateMap);
        return Update.fromDocument(Document.parse(objectMapper.writeValueAsString(finalUpdateMap)));
    }

    private TypedAggregation<?> prepareMongoAggregate(String rawAggregation, JsonNode requestBody, String responseBodyPath) throws ClassNotFoundException, JsonProcessingException {
        Map<String, Object> requestBodyMap = objectMapper.convertValue(requestBody, new TypeReference<>() {});
        String finalAggregation = StrSubstitutor.replace(rawAggregation, requestBodyMap);
        finalAggregation = finalAggregation.replace("[", "").replace("]", "");
        String[] finalAggregationMap = finalAggregation.split(",");

        return new TypedAggregation<>(Class.forName(responseBodyPath),
                Arrays.stream(finalAggregationMap).map(Aggregation::stage).collect(Collectors.toList()));
    }

}
