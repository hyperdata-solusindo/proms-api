package com.hyperdata.proms.module.identity.auth.repository;

import com.hyperdata.proms.module.identity.auth.entity.PersonalAccessToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalAccessTokenRepository extends JpaRepository<PersonalAccessToken, Long> {
    Optional<PersonalAccessToken> findByIdAndToken(Long id, String token);
}
