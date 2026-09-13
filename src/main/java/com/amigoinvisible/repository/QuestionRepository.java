package com.amigoinvisible.repository;

import com.amigoinvisible.entity.Question;
import com.amigoinvisible.entity.Room;
import com.amigoinvisible.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByRoomAndTargetUserOrderByCreatedAtDesc(Room room, User targetUser);
    List<Question> findByRoomAndAskerOrderByCreatedAtDesc(Room room, User asker);
    Optional<Question> findByIdAndTargetUser(Long id, User targetUser);
    List<Question> findByRoom(Room room);
}
