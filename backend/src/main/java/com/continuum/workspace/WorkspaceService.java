// Logic for Workspace http requests

package com.continuum.workspace;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

@Service
public class WorkspaceService {

    private final WorkspaceRepository repository;

    public WorkspaceService(WorkspaceRepository repository) {
        this.repository = repository;
    }

    // Convert Workspace entity into WorkspaceResponse DTO
    private WorkspaceDto.WorkspaceResponse toResponse(Workspace workspace) {
        WorkspaceDto.WorkspaceResponse resp = new WorkspaceDto.WorkspaceResponse();
        resp.id = workspace.id;
        resp.name = workspace.name;
        resp.ownerId = workspace.ownerId;
        resp.createdAt = workspace.createdAt;
        resp.description = workspace.description;
        return resp;
    }

    // Create a workspace
    public WorkspaceDto.WorkspaceResponse createWorkspace(WorkspaceDto.CreateWorkspaceRequest request) {
        Workspace workspace = new Workspace();
        workspace.id = UUID.randomUUID().toString();
        workspace.name = request.name;
        workspace.ownerId = request.ownerId;
        workspace.description = request.description;

        Workspace saved = repository.save(workspace);
        return toResponse(saved);
    }

    // Update a workspace
    public WorkspaceDto.WorkspaceResponse updateWorkspace(@NonNull String id, WorkspaceDto.CreateWorkspaceRequest request) {
        Optional<Workspace> optional = repository.findById(id);
        if (optional.isEmpty()) {
            return null; // controller will turn this into 404
        }

        Workspace existing = optional.get();
        existing.name = request.name;
        existing.ownerId = request.ownerId;
        existing.description = request.description;
        // createdAt is not updated (updatable = false)

        Workspace updated = repository.save(existing);
        return toResponse(updated);
    }

    // List all workspaces
    public List<WorkspaceDto.WorkspaceResponse> listWorkspaces() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // List workspaces by owner
    public List<WorkspaceDto.WorkspaceResponse> listWorkspacesByOwner(@NonNull String ownerId) {
        return repository.findByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    // Get workspace by id
    public WorkspaceDto.WorkspaceResponse getWorkspaceById(@NonNull String id) {
        Optional<Workspace> optional = repository.findById(id);
        if (optional.isEmpty()) {
            return null;
        }
        return toResponse(optional.get());
    }

    // Delete workspace
    public boolean deleteWorkspaceById(@NonNull String id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}


