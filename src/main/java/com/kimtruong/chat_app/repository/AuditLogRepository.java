package com.kimtruong.chat_app.repository;

import com.kimtruong.chat_app.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findTop20ByOrderByTimestampDesc();
}
