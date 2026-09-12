package com.amigoinvisible.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class WishlistDtos {

    public record WishlistItemRequest(
            @NotBlank @Size(max = 150) String title,
            @Size(max = 300) String note,
            @Size(max = 300) String url
    ) {}

    public record WishlistItemResponse(
            Long id,
            Long userId,
            String username,
            String title,
            String note,
            String url,
            Instant updatedAt
    ) {}
}
