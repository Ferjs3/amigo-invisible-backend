package com.amigoinvisible.repository;

import com.amigoinvisible.entity.BudgetVote;
import com.amigoinvisible.entity.Room;
import com.amigoinvisible.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetVoteRepository extends JpaRepository<BudgetVote, Long> {
    List<BudgetVote> findByRoom(Room room);
    Optional<BudgetVote> findByRoomAndVoter(Room room, User voter);
    void deleteByRoom(Room room);
    void deleteByRoomAndVoter(Room room, User voter);
}
