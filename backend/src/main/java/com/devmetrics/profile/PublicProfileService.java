package com.devmetrics.profile;

import com.devmetrics.achievement.AchievementService;
import com.devmetrics.activity.repository.ActivityRepository;
import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.common.exception.NotFoundException;
import com.devmetrics.dashboard.DashboardService;
import com.devmetrics.profile.dto.PublicProfileResponse;
import com.devmetrics.project.repository.ProjectRepository;
import com.devmetrics.scoring.ScoreService;
import com.devmetrics.user.domain.User;
import com.devmetrics.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class PublicProfileService {

    private final UserRepository userRepository;
    private final DashboardService dashboardService;
    private final AchievementService achievementService;
    private final ActivityRepository activityRepository;
    private final ProjectRepository projectRepository;
    private final ScoreService scoreService;
    private final BadgeRenderer badgeRenderer;

    public PublicProfileService(UserRepository userRepository,
                                DashboardService dashboardService,
                                AchievementService achievementService,
                                ActivityRepository activityRepository,
                                ProjectRepository projectRepository,
                                ScoreService scoreService,
                                BadgeRenderer badgeRenderer) {
        this.userRepository = userRepository;
        this.dashboardService = dashboardService;
        this.achievementService = achievementService;
        this.activityRepository = activityRepository;
        this.projectRepository = projectRepository;
        this.scoreService = scoreService;
        this.badgeRenderer = badgeRenderer;
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse profile(String username) {
        User user = requirePublic(username);
        return new PublicProfileResponse(
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                scoreService.current(user),
                dashboardService.streak(user),
                activityRepository.countByUserId(user.getId()),
                projectRepository.countByUserId(user.getId()),
                dashboardService.topTechnologies(user, LocalDate.now(user.zoneId())),
                achievementService.unlocked(user.getId()),
                dashboardService.lastYear(user));
    }

    @Transactional(readOnly = true)
    public String badge(String username) {
        User user = requirePublic(username);
        return badgeRenderer.render(user.getUsername(), scoreService.current(user),
                dashboardService.streak(user), dashboardService.lastYear(user));
    }

    /** Perfil privado e perfil inexistente respondem igual: nao revela quem tem conta. */
    private User requirePublic(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .filter(User::isPublicProfile)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PROFILE_NOT_FOUND));
    }
}
