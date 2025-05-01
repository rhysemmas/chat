package co.uk.nootnoot.network.blocking;

import java.io.Serializable;

public record Message(Integer clientId, String message) implements Serializable {
}
