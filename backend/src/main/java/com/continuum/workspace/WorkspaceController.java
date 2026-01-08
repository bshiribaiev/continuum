package com.continuum.workspace;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    // Create workspace
    @PostMapping("api/workspaces")
    public ResponseEntity<WorkspaceDto.WorkspaceResponse> createWorkspace(
            @Valid @RequestBody WorkspaceDto.CreateWorkspaceRequest request) {
        WorkspaceDto.WorkspaceResponse resp = workspaceService.createWorkspace(request);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    // Get all workspaces
    @GetMapping("api/workspaces")
    public List<WorkspaceDto.WorkspaceResponse> getWorkspaces(@RequestParam(required = false) String ownerId) {
        if (ownerId == null) {
            return workspaceService.listWorkspaces();
        }
        return workspaceService.listWorkspacesByOwner(ownerId);
    }

    // Get specific workspace
    @GetMapping("api/workspaces/{id}")
    public ResponseEntity<WorkspaceDto.WorkspaceResponse> getWorkspaceById(@PathVariable @NonNull String id) {
        WorkspaceDto.WorkspaceResponse resp = workspaceService.getWorkspaceById(id);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
    }

    // Update workspace
    @PutMapping("api/workspaces/{id}")
    public ResponseEntity<WorkspaceDto.WorkspaceResponse> updateWorkspace(@PathVariable @NonNull String id,
            @Valid @RequestBody WorkspaceDto.CreateWorkspaceRequest request) {
        WorkspaceDto.WorkspaceResponse resp = workspaceService.updateWorkspace(id, request);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
    }

    // Delete workspace
    @DeleteMapping("api/workspaces/{id}")
    public ResponseEntity<Void> deleteWorkspaceById(@PathVariable @NonNull String id) {
        boolean deleted = workspaceService.deleteWorkspaceById(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}