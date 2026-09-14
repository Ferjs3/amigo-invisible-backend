package com.amigoinvisible.service;

import com.amigoinvisible.dto.BudgetVoteDtos.BudgetVoteStatusResponse;
import com.amigoinvisible.entity.BudgetVote;
import com.amigoinvisible.entity.Room;
import com.amigoinvisible.entity.RoomStatus;
import com.amigoinvisible.entity.User;
import com.amigoinvisible.exception.BadRequestException;
import com.amigoinvisible.exception.ConflictException;
import com.amigoinvisible.exception.ForbiddenException;
import com.amigoinvisible.exception.ResourceNotFoundException;
import com.amigoinvisible.repository.BudgetVoteRepository;
import com.amigoinvisible.repository.RoomParticipantRepository;
import com.amigoinvisible.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BudgetVoteService {

    private static final BigDecimal MIN_OPTION = BigDecimal.valueOf(10_000);
    private static final BigDecimal MAX_OPTION = BigDecimal.valueOf(100_000);
    private static final BigDecimal STEP = BigDecimal.valueOf(10_000);

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final BudgetVoteRepository budgetVoteRepository;

    public List<BigDecimal> votingOptions() {
        List<BigDecimal> options = new ArrayList<>();
        for (BigDecimal amount = MIN_OPTION; amount.compareTo(MAX_OPTION) <= 0; amount = amount.add(STEP)) {
            options.add(amount);
        }
        return options;
    }

    @Transactional
    public BudgetVoteStatusResponse openVote(User admin, Long roomId) {
        Room room = requireAdmin(admin, roomId);
        requireOpenRoom(room);

        if (room.isBudgetVotingOpen()) {
            throw new ConflictException("Ya hay una votacion de presupuesto en curso");
        }

        budgetVoteRepository.deleteByRoom(room);
        room.setBudgetVotingOpen(true);
        roomRepository.save(room);

        return status(admin, room);
    }

    @Transactional
    public BudgetVoteStatusResponse castVote(User voter, Long roomId, BigDecimal amount) {
        Room room = requireMember(voter, roomId);
        requireOpenRoom(room);

        if (!room.isBudgetVotingOpen()) {
            throw new ConflictException("No hay ninguna votacion de presupuesto abierta");
        }
        if (votingOptions().stream().noneMatch(opt -> opt.compareTo(amount) == 0)) {
            throw new BadRequestException("Ese monto no es una opcion valida");
        }

        Evaluation before = evaluate(room);
        if (before.tiePending()) {
            throw new ConflictException("La votacion ya esta esperando que el admin desempate");
        }

        BudgetVote vote = budgetVoteRepository.findByRoomAndVoter(room, voter)
                .orElseGet(() -> BudgetVote.builder().room(room).voter(voter).build());
        vote.setAmount(amount);
        budgetVoteRepository.save(vote);

        Evaluation after = evaluate(room);
        if (after.everyoneVoted() && !after.tiePending()) {
            applyResult(room, after.topAmounts().get(0));
        }

        return status(voter, room);
    }

    @Transactional
    public BudgetVoteStatusResponse resolveTie(User admin, Long roomId, BigDecimal amount) {
        Room room = requireAdmin(admin, roomId);
        requireOpenRoom(room);

        Evaluation evaluation = evaluate(room);
        if (!evaluation.tiePending()) {
            throw new ConflictException("No hay ningun empate esperando resolucion");
        }
        if (evaluation.topAmounts().stream().noneMatch(a -> a.compareTo(amount) == 0)) {
            throw new BadRequestException("Ese monto no es parte del empate actual");
        }

        applyResult(room, amount);
        return status(admin, room);
    }

    @Transactional(readOnly = true)
    public BudgetVoteStatusResponse getStatus(User viewer, Long roomId) {
        Room room = requireMember(viewer, roomId);
        return status(viewer, room);
    }

    // --- internos ---

    private void applyResult(Room room, BigDecimal finalAmount) {
        room.setSuggestedBudget(finalAmount);
        room.setBudgetVotingOpen(false);
        roomRepository.save(room);
        budgetVoteRepository.deleteByRoom(room);
    }

    // Evalua el estado actual: cuenta votos, ve quien va ganando, y SOLO marca
    // "empate pendiente de desempate" cuando ya votaron todos los participantes.
    // Un empate parcial a mitad de votacion todavia no significa nada.
    private Evaluation evaluate(Room room) {
        long total = participantRepository.countByRoom(room);
        List<BudgetVote> votes = budgetVoteRepository.findByRoom(room);

        Map<BigDecimal, Long> counts = new LinkedHashMap<>();
        for (BudgetVote v : votes) {
            counts.merge(v.getAmount(), 1L, Long::sum);
        }
        long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<BigDecimal> topAmounts = counts.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .toList();

        boolean everyoneVoted = !votes.isEmpty() && votes.size() >= total;
        boolean tiePending = everyoneVoted && topAmounts.size() > 1;

        return new Evaluation(votes, total, topAmounts, everyoneVoted, tiePending);
    }

    private BudgetVoteStatusResponse status(User viewer, Room room) {
        Evaluation evaluation = evaluate(room);

        BigDecimal myVote = evaluation.votes().stream()
                .filter(v -> v.getVoter().getId().equals(viewer.getId()))
                .map(BudgetVote::getAmount)
                .findFirst()
                .orElse(null);

        boolean isAdmin = room.getAdmin().getId().equals(viewer.getId());
        boolean tiePendingVisible = room.isBudgetVotingOpen() && evaluation.tiePending();
        List<BigDecimal> tiedAmountsForResponse = (tiePendingVisible && isAdmin) ? evaluation.topAmounts() : List.of();

        return new BudgetVoteStatusResponse(
                room.isBudgetVotingOpen(),
                tiePendingVisible,
                evaluation.votes().size(),
                (int) evaluation.total(),
                votingOptions(),
                myVote,
                tiedAmountsForResponse,
                room.getSuggestedBudget()
        );
    }

    private Room requireMember(User user, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada"));
        if (!participantRepository.existsByRoomAndUser(room, user)) {
            throw new ForbiddenException("No sos parte de esta sala");
        }
        return room;
    }

    private Room requireAdmin(User user, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada"));
        if (!room.getAdmin().getId().equals(user.getId())) {
            throw new ForbiddenException("Solo el admin de la sala puede hacer esto");
        }
        return room;
    }

    private void requireOpenRoom(Room room) {
        if (room.getStatus() != RoomStatus.OPEN) {
            throw new ConflictException("La votacion de presupuesto solo esta disponible antes del sorteo");
        }
    }

    private record Evaluation(
            List<BudgetVote> votes,
            long total,
            List<BigDecimal> topAmounts,
            boolean everyoneVoted,
            boolean tiePending
    ) {}
}
