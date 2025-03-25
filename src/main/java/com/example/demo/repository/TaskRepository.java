package com.example.demo.repository;

import com.example.demo.model.Task;
import com.example.demo.model.TaskPriority;
import com.example.demo.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByPriority(TaskPriority priority);
    List<Task> findByDueDateBefore(LocalDateTime date);

    @Query("SELECT t FROM Task t WHERE t.status = :status AND t.priority = :priority")
    List<Task> findByStatusAndPriority(@Param("status") TaskStatus status, @Param("priority") TaskPriority priority);

    @Query("SELECT t FROM Task t WHERE t.dueDate BETWEEN :start AND :end")
    List<Task> findByDueDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Task> findByTagsContaining(String tag);
}