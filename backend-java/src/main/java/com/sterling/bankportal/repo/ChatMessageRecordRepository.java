package com.sterling.bankportal.repo;

import com.sterling.bankportal.model.ChatMessageRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRecordRepository extends JpaRepository<ChatMessageRecord, String> {
    List<ChatMessageRecord> findTop30ByUserIdOrderByCreatedAtAsc(String userId);
    void deleteByUserId(String userId);
}
