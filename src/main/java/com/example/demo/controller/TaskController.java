package com.example.demo.controller;

import com.example.demo.model.Task;
import com.example.demo.model.TaskPriority;
import com.example.demo.model.TaskStatus;
import com.example.demo.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        return ResponseEntity.ok(taskService.createTask(task));
    }

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task taskDetails) {
        return ResponseEntity.ok(taskService.updateTask(id, taskDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Task>> getOverdueTasks() {
        return ResponseEntity.ok(taskService.getOverdueTasks());
    }

    @GetMapping("/priority/{priority}/status/{status}")
    public ResponseEntity<List<Task>> getTasksByPriorityAndStatus(
            @PathVariable TaskPriority priority,
            @PathVariable TaskStatus status) {
        return ResponseEntity.ok(taskService.getTasksByPriorityAndStatus(priority, status));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<TaskStatus, Long>> getTaskCountByStatus() {
        return ResponseEntity.ok(taskService.getTaskCountByStatus());
    }

    @GetMapping("/due-in/{days}")
    public ResponseEntity<List<Task>> getTasksDueInNextDays(@PathVariable int days) {
        return ResponseEntity.ok(taskService.getTasksDueInNextDays(days));
    }

    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<Task>> getTasksByTag(@PathVariable String tag) {
        return ResponseEntity.ok(taskService.getTasksByTag(tag));
    }

    @GetMapping("/high-priority-overdue")
    public ResponseEntity<List<Task>> getHighPriorityOverdueTasks() {
        return ResponseEntity.ok(taskService.getHighPriorityOverdueTasks());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.notFound().build();
    }
}