package co.uk.nootnoot.network_blocking;

import java.io.Serializable;

public record Message(Integer clientId, String message) implements Serializable {
}
