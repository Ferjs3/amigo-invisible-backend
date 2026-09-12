# Amigo Invisible — Backend

API REST en Spring Boot 3 (Java 21) para organizar sorteos de amigo invisible en salas privadas.

## Requisitos

- Java 21
- Maven (o usa el wrapper si lo agregas con `mvn -N io.takari:maven:wrapper`)
- MySQL 8 corriendo localmente (o en Docker)

## Puesta en marcha

1. Crear la base de datos:
   ```sql
   CREATE DATABASE amigo_invisible CHARACTER SET utf8mb4;
   ```

2. Ajustar `src/main/resources/application.properties` con tu usuario/password de MySQL.

3. Levantar la app:
   ```bash
   mvn spring-boot:run
   ```
   La primera vez, Hibernate crea las tablas solo (`ddl-auto=update`) a partir de las entidades.

4. La API queda en `http://localhost:8080/api`.

## Autenticación

Esquema simple, sin JWT: `POST /api/auth/login` devuelve un `token` opaco (UUID) que
se guarda en la tabla `auth_tokens`. El cliente lo manda en cada request como:

```
Authorization: Bearer <token>
```

## Estructura

```
entity/      -> Entidades JPA (User, Room, RoomParticipant, RoomExclusion, Assignment, WishlistItem, Question, AuthToken)
repository/  -> Spring Data JPA repositories
dto/         -> Records de entrada/salida, separados de las entidades
service/     -> Lógica de negocio (RoomService, DrawService, WishlistService, QuestionService, AuthService)
controller/  -> Endpoints REST
security/    -> Filtro de token + helper para obtener el usuario actual
exception/   -> Excepciones de negocio + manejador global (respuestas HTTP consistentes)
```

## Notas de diseño importantes

- **`DrawService`** resuelve el sorteo con backtracking (no shuffle-and-retry), así que
  siempre termina: o encuentra una asignación válida, o informa que es imposible con
  las exclusiones actuales.
- **Anonimato de preguntas**: `Question` guarda `asker_id` en la base (para poder
  moderar abuso a futuro), pero `QuestionDtos.QuestionResponse` nunca lo incluye. La
  garantía de anonimato vive en el mapeo a DTO dentro de `QuestionService`, no en la
  tabla.
- **Sala sellada**: una vez que el admin dispara `/draw`, nadie más puede unirse
  (`409 Conflict` en `/join`). Si alguien se baja después, la sala pasa a `DISCARDED`.

## Próximo paso

Este backend está pensado para consumirse desde un frontend Angular con
`HttpClient` + un interceptor que agregue el header `Authorization` a cada request.
