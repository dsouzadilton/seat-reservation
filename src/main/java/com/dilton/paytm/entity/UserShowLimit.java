package com.dilton.paytm.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "user_show_limits")
@IdClass(UserShowLimit.UserShowLimitId.class)
public class UserShowLimit {

    @Id
    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "seat_count", nullable = false)
    private Integer seatCount = 0;

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }

    public static class UserShowLimitId implements Serializable {

        private Long showId;
        private String userId;

        public UserShowLimitId() {
        }

        public UserShowLimitId(Long showId, String userId) {
            this.showId = showId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UserShowLimitId that)) return false;
            return Objects.equals(showId, that.showId)
                    && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(showId, userId);
        }
    }
}