package com.example.staticanalysis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EndpointInfo {
    private String path;
    private String method;
    private String description;
    private List<ParameterInfo> parameters;
    private ResponseInfo response;
    private Map<String, Object> exampleRequest;
    private Map<String, Object> exampleResponse;
}