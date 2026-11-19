package com.hyperdata.proms.module.identity.user.repository;

import com.hyperdata.proms.module.identity.user.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    @Query("""
            SELECT u FROM User u
            WHERE u.email IN :emails
        """)
    List<User> findByEmailIn(@Param("emails")
    Collection<String> emails);
}
