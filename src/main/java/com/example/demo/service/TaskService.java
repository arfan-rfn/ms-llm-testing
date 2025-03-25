package com.example.demo.service;

import com.example.demo.model.Task;
import com.example.demo.model.TaskPriority;
import com.example.demo.model.TaskStatus;
import com.example.demo.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Transactional
    public Task createTask(Task task) {
        validateTask(task);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }

    @Transactional
    public Task updateTask(Long id, Task taskDetails) {
        Task task = getTaskById(id);
        updateTaskFields(task, taskDetails);
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public List<Task> getOverdueTasks() {
        return taskRepository.findByDueDateBefore(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksByPriorityAndStatus(TaskPriority priority, TaskStatus status) {
        return taskRepository.findByStatusAndPriority(status, priority);
    }

    @Transactional(readOnly = true)
    public Map<TaskStatus, Long> getTaskCountByStatus() {
        return taskRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Task::getStatus,
                        Collectors.counting()
                ));
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksDueInNextDays(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(days);
        return taskRepository.findByDueDateBetween(now, future);
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksByTag(String tag) {
        return taskRepository.findByTagsContaining(tag);
    }

    @Transactional(readOnly = true)
    public List<Task> getHighPriorityOverdueTasks() {
        return taskRepository.findAll().stream()
                .filter(task -> task.getPriority() == TaskPriority.HIGH || task.getPriority() == TaskPriority.URGENT)
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(LocalDateTime.now()))
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
                .collect(Collectors.toList());
    }

    private void validateTask(Task task) {
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty");
        }
        if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Due date cannot be in the past");
        }
    }

    private void updateTaskFields(Task task, Task taskDetails) {
        if (taskDetails.getTitle() != null) {
            task.setTitle(taskDetails.getTitle());
        }
        if (taskDetails.getDescription() != null) {
            task.setDescription(taskDetails.getDescription());
        }
        if (taskDetails.getStatus() != null) {
            task.setStatus(taskDetails.getStatus());
        }
        if (taskDetails.getPriority() != null) {
            task.setPriority(taskDetails.getPriority());
        }
        if (taskDetails.getDueDate() != null) {
            task.setDueDate(taskDetails.getDueDate());
        }
        if (taskDetails.getTags() != null) {
            task.setTags(taskDetails.getTags());
        }
    }
}