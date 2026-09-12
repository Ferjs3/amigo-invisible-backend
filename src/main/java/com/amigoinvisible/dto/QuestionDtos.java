package com.amigoinvisible.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class QuestionDtos {

    public record AskQuestionRequest(
            @NotBlank @Size(max = 300) String questionText
    ) {}

    public record AnswerQuestionRequest(
            @NotBlank @Size(max = 300) String answerText
    ) {}

    // Nunca incluye quien pregunto: el anonimato se garantiza aca, en el DTO de salida.
    public record QuestionResponse(
            Long id,
            String questionText,
            String answerText,
            boolean answered,
            Instant createdAt
    ) {}
}
