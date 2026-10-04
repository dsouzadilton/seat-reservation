package com.dilton.paytm.repository;

import com.dilton.paytm.entity.UserShowLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserShowLimitRepository extends JpaRepository<UserShowLimit, UserShowLimit.UserShowLimitId> {

    Optional<UserShowLimit> findByShowIdAndUserId(
            Long showId,
            String userId
    );
}