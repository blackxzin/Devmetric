package com.devmetrics.activity;

import com.devmetrics.activity.domain.*;
import com.devmetrics.activity.dto.UpdateActivityRequest;
import com.devmetrics.activity.repository.ActivityRepository;
import com.devmetrics.common.exception.BusinessException;
import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.config.AppProperties;
import com.devmetrics.project.repository.ProjectRepository;
import com.devmetrics.scoring.*;
import com.devmetrics.technology.TechnologyService;
import com.devmetrics.technology.domain.Technology;
import com.devmetrics.technology.repository.UserTechnologyRepository;
import com.devmetrics.user.domain.User;
import com.devmetrics.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivityServiceTest {
    private final ActivityRepository repository = mock(ActivityRepository.class);
    private final User user = User.create("test@example.com", "hash", "Test", "UTC");
    private final Instant now = Instant.now();
    private final LocalDate date = now.atZone(user.zoneId()).toLocalDate();

    private ActivityService service() {
        AppProperties properties = mock(AppProperties.class);
        when(properties.scoring()).thenReturn(new AppProperties.Scoring(90, 0.5));
        return spy(new ActivityService(repository, mock(ProjectRepository.class),
                mock(UserRepository.class), mock(UserTechnologyRepository.class),
                mock(TechnologyService.class), mock(ScoringRuleService.class),
                mock(PointsCalculator.class), mock(DailyStatsService.class), properties,
                mock(ApplicationEventPublisher.class)));
    }

    @Test
    void importedActivityCannotBeEdited() {
        Activity activity = Activity.create(user, ActivityType.COMMIT, ActivitySource.GITHUB, "Original", now, date);
        when(repository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(activity));
        assertThatThrownBy(() -> service().update(1L, 1L,
                new UpdateActivityRequest(ActivityType.STUDY, "Edited", null, null, null, now)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.READ_ONLY_RESOURCE));
        assertThat(activity.getTitle()).isEqualTo("Original");
    }

    @Test
    void manualActivityCanRemoveTechnology() {
        Activity activity = Activity.create(user, ActivityType.STUDY, ActivitySource.MANUAL, "Study", now, date);
        activity.attachTechnology(mock(Technology.class));
        when(repository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(activity));
        ActivityService service = service();
        doReturn(1).when(service).recalculate(user, date, date);
        service.update(1L, 1L, new UpdateActivityRequest(ActivityType.STUDY, "Study", null, null, null, now));
        assertThat(activity.getTechnology()).isNull();
    }
}
