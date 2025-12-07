// Data access functions

package com.continuum.memory;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryRepository extends JpaRepository<Memory, String> {
    List<Memory> findByUserId(String userId);

    List<Memory> findByUserIdAndWorkspaceId(String userId, String workspaceId);
}
