package com.kimtruong.chat_app.service;

import com.kimtruong.chat_app.model.AuditLog;
import com.kimtruong.chat_app.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Log an audit event to the database.
     *
     * @param username the username performing the action
     * @param action the action being performed (e.g., LOGIN_SUCCESS, LOGIN_FAIL)
     * @param ipAddress the client IP address
     * @param detail additional details about the action
     */
    public void log(String username, String action, String ipAddress, String detail) {
        AuditLog auditLog = new AuditLog(username, action, ipAddress, detail);
        auditLogRepository.save(auditLog);
    }

    /**
     * Retrieve the most recent 20 audit log entries.
     *
     * @return list of the 20 most recent audit logs in descending order by timestamp
     */
    public List<AuditLog> getTop20Logs() {
        return auditLogRepository.findTop20ByOrderByTimestampDesc();
    }
}
