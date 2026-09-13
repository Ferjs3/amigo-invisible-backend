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

    // Para "lo que pregunte": a diferencia de QuestionResponse, aca SI mostramos
    // a quien se le pregunto -- eso ya lo sabe el que pregunto, no es un secreto
    // para el mismo. Lo que sigue sin exponerse en ningun lado es al reves
    // (quien le pregunto que a el).
    public record AskedQuestionResponse(
            Long id,
            Long targetUserId,
            String targetUsername,
            String questionText,
            String answerText,
            boolean answered,
            Instant createdAt
    ) {}
}
