package com.amigoinvisible.repository;

import com.amigoinvisible.entity.Room;
import com.amigoinvisible.entity.User;
import com.amigoinvisible.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByRoomAndUserOrderByCreatedAtAsc(Room room, User user);
    Optional<WishlistItem> findByIdAndRoomAndUser(Long id, Room room, User user);
    List<WishlistItem> findByRoom(Room room);
}
