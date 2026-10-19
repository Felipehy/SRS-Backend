package com.siurbinfo.srs.repository.email;

import com.siurbinfo.srs.entity.email.EmailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutBoxEmailRepository extends JpaRepository<EmailEntity, Long> {

    @Query(value = """
        SELECT * FROM outbox_email
        WHERE status = 'PENDING' AND next_attempt_at <= now()
        ORDER BY id LIMIT 20 FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<EmailEntity> getOutBoxEmail();

}
