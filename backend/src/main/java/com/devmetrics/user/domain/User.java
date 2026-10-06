package com.devmetrics.user.domain;

import com.devmetrics.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.ZoneId;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(name = "weekly_goal_points", nullable = false)
    private int weeklyGoalPoints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(length = 40)
    private String username;

    @Column(name = "public_profile", nullable = false)
    private boolean publicProfile;

    protected User() {
    }

    public static User create(String email, String passwordHash, String displayName, String timezone) {
        User user = new User();
        user.email = email.toLowerCase().trim();
        user.passwordHash = passwordHash;
        user.displayName = displayName.trim();
        user.timezone = (timezone == null || timezone.isBlank()) ? "America/Sao_Paulo" : timezone;
        user.weeklyGoalPoints = 150;
        user.role = Role.USER;
        return user;
    }

    public void updateProfile(String displayName, String timezone, Integer weeklyGoalPoints) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName.trim();
        }
        if (timezone != null && !timezone.isBlank()) {
            this.timezone = timezone;
        }
        if (weeklyGoalPoints != null && weeklyGoalPoints > 0) {
            this.weeklyGoalPoints = weeklyGoalPoints;
        }
    }

    public void changeUsername(String username) {
        this.username = username.toLowerCase().trim();
    }

    public void setPublicProfile(boolean publicProfile) {
        this.publicProfile = publicProfile;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void updateAvatar(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public ZoneId zoneId() {
        try {
            return ZoneId.of(timezone);
        } catch (RuntimeException ex) {
            return ZoneId.of("America/Sao_Paulo");
        }
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getTimezone() {
        return timezone;
    }

    public int getWeeklyGoalPoints() {
        return weeklyGoalPoints;
    }

    public Role getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public boolean isPublicProfile() {
        return publicProfile;
    }
}
