package dev.mysd.game.simulation

/** Raised when a command log receives an id that is already present. */
class DuplicateCommandIdException(id: Long) : IllegalArgumentException(
    "Duplicate command id: $id",
)
