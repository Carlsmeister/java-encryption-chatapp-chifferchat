package se.mau.chifferchat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.mau.chifferchat.model.GroupMembership;

import java.util.List;

@Repository
public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    @Query("SELECT gm FROM GroupMembership gm WHERE gm.group.id = :groupId")
    List<GroupMembership> findByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT gm FROM GroupMembership gm WHERE gm.user.id = :userId")
    List<GroupMembership> findByUserId(@Param("userId") Long userId);

    @Query(
            "SELECT CASE WHEN COUNT(gm) > 0 THEN true ELSE false END "
                    + "FROM GroupMembership gm "
                    + "WHERE gm.user.id = :userId AND gm.group.id = :groupId"
    )
    boolean existsByUserIdAndGroupId(
            @Param("userId") Long userId,
            @Param("groupId") Long groupId
    );
}
