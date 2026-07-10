package com.clubflow.backend.club;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubStaffRepository extends JpaRepository<ClubStaff, UUID> {

    @Query("""
            select staff
            from ClubStaff staff
            join fetch staff.club
            where staff.user.id = :userId
              and staff.status = :status
            order by staff.createdAt asc
            """)
    List<ClubStaff> findAllAccessibleByUserId(
            @Param("userId") UUID userId,
            @Param("status") ClubStaffStatus status
    );

    @Query("""
            select staff
            from ClubStaff staff
            join fetch staff.club
            where staff.club.id = :clubId
              and staff.user.id = :userId
              and staff.status = :status
            """)
    Optional<ClubStaff> findAccessibleClub(
            @Param("clubId") UUID clubId,
            @Param("userId") UUID userId,
            @Param("status") ClubStaffStatus status
    );

    @Query("""
            select staff from ClubStaff staff
            join fetch staff.user
            where staff.club.id = :clubId
            order by staff.createdAt asc
            """)
    List<ClubStaff> findAllByClubIdWithUser(@Param("clubId") UUID clubId);

    Optional<ClubStaff> findByClubIdAndUserId(UUID clubId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select staff from ClubStaff staff
            where staff.club.id = :clubId and staff.user.id = :userId
            """)
    Optional<ClubStaff> findByClubIdAndUserIdForUpdate(
            @Param("clubId") UUID clubId,
            @Param("userId") UUID userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select staff from ClubStaff staff
            join fetch staff.user
            where staff.id = :staffId and staff.club.id = :clubId
            """)
    Optional<ClubStaff> findByIdAndClubIdForUpdate(
            @Param("staffId") UUID staffId,
            @Param("clubId") UUID clubId
    );
}
