package com.example.staticanalysis;

import com.example.staticanalysis.service.EndpointAnalyzer;
import com.example.staticanalysis.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class StaticAnalysisApplication {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar static-analysis.jar <path-to-spring-boot-app>");
            System.out.println("Example: java -jar static-analysis.jar /path/to/spring/boot/app/src/main/java");
            System.exit(1);
        }

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            EndpointAnalyzer analyzer = context.getBean(EndpointAnalyzer.class);
            analyzer.analyzeEndpoints(args[0]);
            System.out.println("Analysis complete. Results written to endpoints.json");
        } catch (Exception e) {
            System.err.println("Error during analysis: " + e.getMessage());
            System.exit(1);
        }
    }
}