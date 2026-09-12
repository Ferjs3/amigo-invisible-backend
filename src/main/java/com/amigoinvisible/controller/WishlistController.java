package com.amigoinvisible.controller;

import com.amigoinvisible.dto.WishlistDtos.WishlistItemRequest;
import com.amigoinvisible.dto.WishlistDtos.WishlistItemResponse;
import com.amigoinvisible.security.CurrentUser;
import com.amigoinvisible.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final CurrentUser currentUser;

    @GetMapping("/{userId}")
    public ResponseEntity<List<WishlistItemResponse>> getWishlist(@PathVariable Long roomId, @PathVariable Long userId) {
        return ResponseEntity.ok(wishlistService.getWishlist(currentUser.get(), roomId, userId));
    }

    @PostMapping
    public ResponseEntity<WishlistItemResponse> addItem(@PathVariable Long roomId, @Valid @RequestBody WishlistItemRequest request) {
        var response = wishlistService.addItem(currentUser.get(), roomId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<WishlistItemResponse> updateItem(@PathVariable Long roomId, @PathVariable Long itemId,
                                                             @Valid @RequestBody WishlistItemRequest request) {
        return ResponseEntity.ok(wishlistService.updateItem(currentUser.get(), roomId, itemId, request));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long roomId, @PathVariable Long itemId) {
        wishlistService.deleteItem(currentUser.get(), roomId, itemId);
        return ResponseEntity.noContent().build();
    }
}
