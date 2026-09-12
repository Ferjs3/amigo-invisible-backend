package com.amigoinvisible.entity;

public enum RoomStatus {
    OPEN,       // se pueden sumar/bajar participantes, todavia no hubo sorteo
    SEALED,     // ya se sorteo, nadie mas puede unirse
    DISCARDED   // alguien se bajo despues del sorteo, la sala quedo invalida
}
