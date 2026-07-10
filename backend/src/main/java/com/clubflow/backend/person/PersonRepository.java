package com.clubflow.backend.person;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID> {

    Optional<Person> findByClubIdAndEmail(UUID clubId, String email);

    List<Person> findAllByClubIdAndEmailIn(UUID clubId, Set<String> emails);
}
