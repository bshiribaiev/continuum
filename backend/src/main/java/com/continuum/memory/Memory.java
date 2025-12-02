// Memory table structure

package com.continuum.memory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "memories")
public class Memory {

    @Id
    @Column(nullable = false, updatable = false)
    public String id;

    @Column(nullable = false)
    public String userId;

    @Column(nullable = false)
    public String source;

    // High-level semantic type for this memory (preference, goal, task, etc.)
    @Column(nullable = false)
    public String type; // stored as string for now (e.g. "PREFERENCE", "GOAL")

    // Optional topic key to group related memories (e.g. "tone", "language")
    @Column(nullable = true)
    public String topic;

    // Comma-separated tags for now (e.g. "coding,python")
    @Column(nullable = true, length = 512)
    public String tags;

    // 1–5 importance score (higher = more important)
    @Column(nullable = true)
    public Integer importance;

    // Whether this memory is currently active (not superseded)
    @Column(nullable = false)
    public boolean active;

    // If superseded, points to the id of the newer memory
    @Column(nullable = true)
    public String supersededById;

    @Column(nullable = false, length = 4000)
    public String content;

    @Column(nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(nullable = false)
    public LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        // default semantic fields if not provided
        if (type == null || type.isBlank()) {
            type = "OTHER";
        }
        active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
