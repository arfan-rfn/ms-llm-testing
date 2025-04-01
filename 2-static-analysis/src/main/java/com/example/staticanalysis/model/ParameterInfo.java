package com.example.staticanalysis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParameterInfo {
    private String name;
    private String type;
    private String location; // path, query, body, header
    private boolean required;
    private String description;
    private Object exampleValue;
}