// Workspace data access logic

package com.continuum.workspace;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
    List<Workspace> findByOwnerId(String ownerId);
} 


