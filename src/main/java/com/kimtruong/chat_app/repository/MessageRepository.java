package com.kimtruong.chat_app.repository;

import com.kimtruong.chat_app.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
/**
 * Repository để truy vấn bảng messages.
 *
 * Spring Data JPA tự sinh SQL từ tên method — không cần viết query thủ công.
 * findTop50ByOrderByTimestampAsc → SELECT * FROM messages ORDER BY timestamp ASC LIMIT 50
 */

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findTop50ByOrderByTimestampAsc();
    
    long countByIsReadFalseAndSenderNot(String username);
    List<Message> findByIsReadFalse();
}
