package de.dreierschach.akka.coffeeomat.actor.lager;

import java.io.Serializable;
import java.util.UUID;

import org.immutables.value.Value;



public class LagerMessages {
    
	// Command (Impuls von außen + Seiteneffekte)
	
    public interface LagerWithEntityId extends Serializable {
        @Value.Parameter UUID entityId();
    }

    // Events (Beschreibt den Fakt)
    
}
