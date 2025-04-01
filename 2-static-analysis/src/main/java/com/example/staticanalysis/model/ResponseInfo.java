package com.example.staticanalysis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseInfo {
    private String type;
    private String description;
    private Map<String, Object> example;
}