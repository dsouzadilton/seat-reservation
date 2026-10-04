package com.dilton.paytm.repository;

import com.dilton.paytm.entity.UserShowLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserShowLimitRepository extends JpaRepository<UserShowLimit, UserShowLimit.UserShowLimitId> {

    public Optional<UserShowLimit> findByShowIdAndUserId(
            Long showId,
            String userId
    );
	
	@Query(value = "SELECT pg_advisory_xact_lock(hashtextextended(:lockKey, 0))", nativeQuery = true)
	public void acquireUserShowLock(@Param("lockKey") String lockKey);
}