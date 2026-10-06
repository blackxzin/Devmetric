package com.devmetrics.user;

import com.devmetrics.common.exception.BusinessException;
import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.common.exception.NotFoundException;
import com.devmetrics.github.repository.GitHubAccountRepository;
import com.devmetrics.user.domain.User;
import com.devmetrics.user.dto.ChangePasswordRequest;
import com.devmetrics.user.dto.UpdateUserRequest;
import com.devmetrics.user.dto.UserProfileResponse;
import com.devmetrics.user.dto.UserResponse;
import com.devmetrics.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final GitHubAccountRepository gitHubAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       GitHubAccountRepository gitHubAccountRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.gitHubAccountRepository = gitHubAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile(Long userId) {
        User user = requireUser(userId);
        return gitHubAccountRepository.findByUserId(userId)
                .map(account -> new UserProfileResponse(UserResponse.from(user), true, account.getGithubLogin()))
                .orElseGet(() -> new UserProfileResponse(UserResponse.from(user), false, null));
    }

    @Transactional
    public UserResponse update(Long userId, UpdateUserRequest request) {
        User user = requireUser(userId);
        user.updateProfile(request.displayName(), request.timezone(), request.weeklyGoalPoints());
        if (request.username() != null) {
            if (userRepository.existsByUsernameIgnoreCaseAndIdNot(request.username(), userId)) {
                throw new BusinessException(ErrorCode.USERNAME_ALREADY_USED);
            }
            user.changeUsername(request.username());
        }
        if (request.publicProfile() != null) {
            if (request.publicProfile() && user.getUsername() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Defina um nome de usuario antes de publicar o perfil");
            }
            user.setPublicProfile(request.publicProfile());
        }
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Senha atual incorreta");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }
}
