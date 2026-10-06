package com.devmetrics.gitlab.repository;

import com.devmetrics.gitlab.domain.GitLabAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GitLabAccountRepository extends JpaRepository<GitLabAccount, Long> {

    Optional<GitLabAccount> findByUserId(Long userId);
}
