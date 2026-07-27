package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.JobInterestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IJobInterestRepository extends JpaRepository<JobInterestEntity, UUID> {

    List<JobInterestEntity> findByEditorIdOrderByCreatedAtDesc(UUID editorUserId);

    boolean existsByJobRequestIdAndEditorId(UUID jobRequestId, UUID editorUserId);
}
