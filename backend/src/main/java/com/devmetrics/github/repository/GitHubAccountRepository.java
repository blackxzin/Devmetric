package com.devmetrics.github.repository;

import com.devmetrics.github.domain.GitHubAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GitHubAccountRepository extends JpaRepository<GitHubAccount, Long> {

    Optional<GitHubAccount> findByUserId(Long userId);

    Optional<GitHubAccount> findByGithubUserId(Long githubUserId);

    @Query("select a from GitHubAccount a join fetch a.user")
    List<GitHubAccount> findAllWithUser();
}
