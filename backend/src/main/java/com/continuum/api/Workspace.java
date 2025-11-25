// Workspace table structure

package com.continuum.api;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;

@Entity
@Table(name = "workspaces")
public class Workspace {
    @Id
    @Column(nullable = false, updatable = false)
    public String id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String ownerId;

    @Column(nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(nullable = false, length = 4000)
    public String description;

    // Automatically set createdAt when entity is first saved
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
