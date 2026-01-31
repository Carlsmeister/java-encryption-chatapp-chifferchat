package se.mau.chifferchat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.mau.chifferchat.model.Group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByGroupId(UUID groupId);

    @Query("SELECT g FROM Group g JOIN g.memberships m WHERE m.user.id = :userId")
    List<Group> findGroupsForUser(@Param("userId") Long userId);
}
