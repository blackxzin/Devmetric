package com.devmetrics.technology;

import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.common.exception.NotFoundException;
import com.devmetrics.common.util.Slugifier;
import com.devmetrics.technology.domain.Technology;
import com.devmetrics.technology.domain.TechnologyCategory;
import com.devmetrics.technology.domain.UserTechnology;
import com.devmetrics.technology.dto.TechnologyResponse;
import com.devmetrics.technology.dto.UserTechnologyResponse;
import com.devmetrics.technology.repository.TechnologyRepository;
import com.devmetrics.technology.repository.UserTechnologyRepository;
import com.devmetrics.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TechnologyService {

    private final TechnologyRepository technologyRepository;
    private final UserTechnologyRepository userTechnologyRepository;

    public TechnologyService(TechnologyRepository technologyRepository,
                             UserTechnologyRepository userTechnologyRepository) {
        this.technologyRepository = technologyRepository;
        this.userTechnologyRepository = userTechnologyRepository;
    }

    @Transactional(readOnly = true)
    public List<TechnologyResponse> search(String query, TechnologyCategory category) {
        List<Technology> technologies;
        if (query != null && !query.isBlank()) {
            technologies = technologyRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query.trim());
        } else if (category != null) {
            technologies = technologyRepository.findByCategoryOrderByNameAsc(category);
        } else {
            technologies = technologyRepository.findAll(
                    org.springframework.data.domain.Sort.by("name"));
        }
        return technologies.stream().map(TechnologyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Technology requireTechnology(Long id) {
        return technologyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TECHNOLOGY_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<UserTechnologyResponse> listUserTechnologies(Long userId) {
        return userTechnologyRepository.findAllByUser(userId).stream()
                .map(UserTechnologyResponse::from)
                .toList();
    }

    /**
     * Busca a tecnologia pelo nome; cria no catalogo se ainda nao existir.
     * Usado pelo sync do GitHub, que descobre linguagens e ferramentas dinamicamente.
     */
    @Transactional
    public Technology findOrCreate(String name, TechnologyCategory category) {
        String slug = Slugifier.slugify(name);
        return technologyRepository.findBySlug(slug)
                .orElseGet(() -> technologyRepository.save(Technology.create(name, category)));
    }

    /**
     * Registra o uso de uma tecnologia por um usuario.
     *
     * @return true quando e a primeira vez que este usuario usa esta tecnologia.
     */
    @Transactional
    public boolean registerUsage(User user, Technology technology, Instant occurredAt) {
        if (technology == null) {
            return false;
        }
        Instant moment = occurredAt == null ? Instant.now() : occurredAt;
        return userTechnologyRepository.findByUserIdAndTechnologyId(user.getId(), technology.getId())
                .map(existing -> {
                    existing.registerUsage(moment);
                    return false;
                })
                .orElseGet(() -> {
                    userTechnologyRepository.save(UserTechnology.first(user, technology, moment));
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public boolean hasUsed(Long userId, String technologySlug) {
        return technologyRepository.findBySlug(technologySlug)
                .flatMap(technology -> userTechnologyRepository
                        .findByUserIdAndTechnologyId(userId, technology.getId()))
                .isPresent();
    }
}
