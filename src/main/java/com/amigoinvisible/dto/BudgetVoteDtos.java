package com.amigoinvisible.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class BudgetVoteDtos {

    public record CastVoteRequest(@NotNull BigDecimal amount) {}

    public record ResolveTieRequest(@NotNull BigDecimal amount) {}

    // "tiedAmounts" solo viene relleno para el admin, y solo cuando hay
    // empate esperando resolucion. Para todos los demas siempre es una lista
    // vacia: nadie mas que el admin puede ver por que monto se esta discutiendo.
    public record BudgetVoteStatusResponse(
            boolean open,
            boolean tieVotePending,
            int votedCount,
            int totalParticipants,
            List<BigDecimal> options,
            BigDecimal myVote,
            List<BigDecimal> tiedAmounts,
            BigDecimal currentBudget
    ) {}
}
