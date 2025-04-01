package com.example.staticanalysis.service;

import com.example.staticanalysis.model.EndpointInfo;
import com.example.staticanalysis.model.ParameterInfo;
import com.example.staticanalysis.model.ResponseInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EndpointAnalyzer {
    private final ObjectMapper objectMapper;

    public EndpointAnalyzer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void analyzeEndpoints(String sourcePath) throws IOException {
        // Setup JavaParser with symbol solver
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(new File(sourcePath)));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

        // Find all Java files
        List<Path> javaFiles = Files.walk(Paths.get(sourcePath))
                .filter(path -> path.toString().endsWith(".java"))
                .collect(Collectors.toList());

        List<EndpointInfo> endpoints = new ArrayList<>();

        for (Path javaFile : javaFiles) {
            CompilationUnit cu = StaticJavaParser.parse(javaFile);

            // Look for controller classes
            cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                    .filter(this::isController)
                    .forEach(controller -> {
                        String basePath = getBasePath(controller);
                        controller.findAll(MethodDeclaration.class).stream()
                                .filter(this::isEndpoint)
                                .forEach(method -> {
                                    try {
                                        EndpointInfo endpoint = analyzeEndpoint(method, basePath);
                                        endpoints.add(endpoint);
                                    } catch (Exception e) {
                                        System.err.println("Error analyzing endpoint in " + javaFile + ": " + e.getMessage());
                                    }
                                });
                    });
        }

        // Write endpoints to JSON file
        objectMapper.writeValue(new File("endpoints.json"), endpoints);
        System.out.println("Endpoint analysis complete. Results written to endpoints.json");
    }

    private boolean isController(ClassOrInterfaceDeclaration classDecl) {
        return classDecl.getAnnotations().stream()
                .anyMatch(ann -> ann.getNameAsString().equals("RestController") ||
                               ann.getNameAsString().equals("Controller"));
    }

    private String getBasePath(ClassOrInterfaceDeclaration controller) {
        return controller.getAnnotationByName("RequestMapping")
                .map(this::getPathFromAnnotation)
                .orElse("");
    }

    private boolean isEndpoint(MethodDeclaration method) {
        return method.getAnnotations().stream()
                .anyMatch(ann -> ann.getNameAsString().matches("GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping"));
    }

    private EndpointInfo analyzeEndpoint(MethodDeclaration method, String basePath) {
        EndpointInfo endpoint = new EndpointInfo();

        // Get HTTP method and path
        method.getAnnotationByName("GetMapping").ifPresent(ann -> {
            endpoint.setMethod("GET");
            endpoint.setPath(basePath + getPathFromAnnotation(ann));
        });
        method.getAnnotationByName("PostMapping").ifPresent(ann -> {
            endpoint.setMethod("POST");
            endpoint.setPath(basePath + getPathFromAnnotation(ann));
        });
        method.getAnnotationByName("PutMapping").ifPresent(ann -> {
            endpoint.setMethod("PUT");
            endpoint.setPath(basePath + getPathFromAnnotation(ann));
        });
        method.getAnnotationByName("DeleteMapping").ifPresent(ann -> {
            endpoint.setMethod("DELETE");
            endpoint.setPath(basePath + getPathFromAnnotation(ann));
        });
        method.getAnnotationByName("PatchMapping").ifPresent(ann -> {
            endpoint.setMethod("PATCH");
            endpoint.setPath(basePath + getPathFromAnnotation(ann));
        });
        method.getAnnotationByName("RequestMapping").ifPresent(ann -> {
            endpoint.setMethod(getMethodFromRequestMapping(ann));
            endpoint.setPath(basePath + getPathFromAnnotation(ann));
        });

        // Analyze parameters
        endpoint.setParameters(analyzeParameters(method));

        // Analyze response
        endpoint.setResponse(analyzeResponse(method));

        return endpoint;
    }

    private String getMethodFromRequestMapping(AnnotationExpr annotation) {
        if (annotation instanceof NormalAnnotationExpr) {
            NormalAnnotationExpr normalAnn = (NormalAnnotationExpr) annotation;
            return normalAnn.getPairs().stream()
                    .filter(pair -> pair.getNameAsString().equals("method"))
                    .findFirst()
                    .map(pair -> pair.getValue().toString().replace("RequestMethod.", ""))
                    .orElse("GET");
        }
        return "GET";
    }

    private String getPathFromAnnotation(AnnotationExpr annotation) {
        if (annotation instanceof SingleMemberAnnotationExpr) {
            return ((SingleMemberAnnotationExpr) annotation).getMemberValue().asStringLiteralExpr().getValue();
        }
        if (annotation instanceof NormalAnnotationExpr) {
            NormalAnnotationExpr normalAnn = (NormalAnnotationExpr) annotation;
            return normalAnn.getPairs().stream()
                    .filter(pair -> pair.getNameAsString().equals("value"))
                    .findFirst()
                    .map(pair -> pair.getValue().asStringLiteralExpr().getValue())
                    .orElse("");
        }
        return "";
    }

    private List<ParameterInfo> analyzeParameters(MethodDeclaration method) {
        List<ParameterInfo> parameters = new ArrayList<>();

        method.getParameters().forEach(param -> {
            ParameterInfo paramInfo = new ParameterInfo();
            paramInfo.setName(param.getNameAsString());
            paramInfo.setType(param.getTypeAsString());

            // Determine parameter location
            if (param.isAnnotationPresent("PathVariable")) {
                paramInfo.setLocation("path");
                paramInfo.setRequired(true);
            } else if (param.isAnnotationPresent("RequestParam")) {
                paramInfo.setLocation("query");
                paramInfo.setRequired(true);
            } else if (param.isAnnotationPresent("RequestBody")) {
                paramInfo.setLocation("body");
                paramInfo.setRequired(true);
            } else if (param.isAnnotationPresent("RequestHeader")) {
                paramInfo.setLocation("header");
                paramInfo.setRequired(true);
            }

            parameters.add(paramInfo);
        });

        return parameters;
    }

    private ResponseInfo analyzeResponse(MethodDeclaration method) {
        ResponseInfo response = new ResponseInfo();

        try {
            ResolvedMethodDeclaration resolvedMethod = method.resolve();
            response.setType(resolvedMethod.getReturnType().describe());
        } catch (Exception e) {
            response.setType(method.getTypeAsString());
        }

        return response;
    }
}