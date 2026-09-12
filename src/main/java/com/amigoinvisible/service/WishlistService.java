package com.amigoinvisible.service;

import com.amigoinvisible.dto.WishlistDtos.WishlistItemRequest;
import com.amigoinvisible.dto.WishlistDtos.WishlistItemResponse;
import com.amigoinvisible.entity.Room;
import com.amigoinvisible.entity.User;
import com.amigoinvisible.entity.WishlistItem;
import com.amigoinvisible.exception.ForbiddenException;
import com.amigoinvisible.exception.ResourceNotFoundException;
import com.amigoinvisible.repository.RoomParticipantRepository;
import com.amigoinvisible.repository.RoomRepository;
import com.amigoinvisible.repository.UserRepository;
import com.amigoinvisible.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(User viewer, Long roomId, Long targetUserId) {
        Room room = requireMember(viewer, roomId);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return wishlistItemRepository.findByRoomAndUserOrderByCreatedAtAsc(room, target).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WishlistItemResponse addItem(User user, Long roomId, WishlistItemRequest request) {
        Room room = requireMember(user, roomId);

        WishlistItem item = WishlistItem.builder()
                .room(room)
                .user(user)
                .title(request.title())
                .note(request.note())
                .url(request.url())
                .build();
        item = wishlistItemRepository.save(item);

        return toResponse(item);
    }

    @Transactional
    public WishlistItemResponse updateItem(User user, Long roomId, Long itemId, WishlistItemRequest request) {
        Room room = requireMember(user, roomId);
        WishlistItem item = wishlistItemRepository.findByIdAndRoomAndUser(itemId, room, user)
                .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado"));

        item.setTitle(request.title());
        item.setNote(request.note());
        item.setUrl(request.url());
        item = wishlistItemRepository.save(item);

        return toResponse(item);
    }

    @Transactional
    public void deleteItem(User user, Long roomId, Long itemId) {
        Room room = requireMember(user, roomId);
        WishlistItem item = wishlistItemRepository.findByIdAndRoomAndUser(itemId, room, user)
                .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado"));
        wishlistItemRepository.delete(item);
    }

    private Room requireMember(User user, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada"));
        if (!participantRepository.existsByRoomAndUser(room, user)) {
            throw new ForbiddenException("No sos parte de esta sala");
        }
        return room;
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        return new WishlistItemResponse(
                item.getId(),
                item.getUser().getId(),
                item.getUser().getUsername(),
                item.getTitle(),
                item.getNote(),
                item.getUrl(),
                item.getUpdatedAt()
        );
    }
}
