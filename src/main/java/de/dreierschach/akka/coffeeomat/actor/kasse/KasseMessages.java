package de.dreierschach.akka.coffeeomat.actor.kasse;

import java.io.Serializable;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;



public class KasseMessages {
    
	// Command (Impuls von außen + Seiteneffekte)
	
    public interface WithEntityId extends Serializable {
        @Value.Parameter UUID entityId();
    }

    @JsonSerialize
    @Value.Immutable
    public interface PersonData {
        @Value.Parameter String name();
        @Value.Parameter String address();
    }
    
    @JsonSerialize
    @Value.Immutable
    public interface CreatePerson extends WithEntityId {
        @Value.Parameter String name();
        @Value.Parameter String address();
    }

    @JsonSerialize
    @Value.Immutable
    public interface UpdatePerson extends WithEntityId {
        @Value.Parameter String name();
        @Value.Parameter String address();
    }

    @JsonSerialize
    @Value.Immutable
    public interface SetzeAlter extends WithEntityId {
    	@Value.Parameter int alter();
    }
    
    @JsonSerialize
    @Value.Immutable
    public interface GetPerson extends WithEntityId {}

    @JsonSerialize
    @Value.Immutable
    public interface GetPersonResponse extends WithEntityId {
        @Value.Parameter String name();
        @Value.Parameter String address();
        @Value.Parameter int alter();
    }

    // Events (Beschreibt den Fakt)
    
    @JsonSerialize
    @Value.Immutable
    interface PersonUpdated extends WithEntityId {
        @Value.Parameter String name();
        @Value.Parameter String address();
    }

    @JsonSerialize
    @Value.Immutable
    interface AlterGesetzt extends WithEntityId {
    	@Value.Parameter int alter();
    }
    
}
