package de.dreierschach.akka.coffeeomat.actor.barista;

import java.io.Serializable;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;



public class BaristaMessages {
    
	// Command (Impuls von außen + Seiteneffekte)
	
    public interface BaristaWithEntityId extends Serializable {
        @Value.Parameter UUID entityId();
    }

    // Events (Beschreibt den Fakt)
    
}
