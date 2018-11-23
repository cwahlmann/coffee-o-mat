package de.dreierschach.akka.coffeeomat.actor.barista;

import java.io.Serializable;
import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class BaristaMessages {
    
    public interface WithEntityId extends Serializable {
        @Value.Parameter String entityId();
    }
    
	@JsonSerialize
    @Value.Immutable
    public interface CreateRezept extends WithEntityId {
    	@Value.Parameter String name();
    	@Value.Parameter Map<String, Integer> zutaten();
    	@Value.Parameter double preis();
    }
    
	@JsonSerialize
    @Value.Immutable
    public interface Rezeptliste {
    	@Value.Parameter Map<String, CreateRezept> rezepte();
    }

	// Command (Impuls von außen + Seiteneffekte)
	
	@JsonSerialize
    @Value.Immutable
    public interface AddRezeptData {
    	@Value.Parameter String name();
    	@Value.Parameter Map<String, Integer> zutaten();
    	@Value.Parameter double preis();
    }
	
	@JsonSerialize
    @Value.Immutable
    public interface GetRezeptData {
    	@Value.Parameter String name();
    }
	
	@JsonSerialize
    @Value.Immutable
    public interface GetRezept extends WithEntityId {
    }
	
	@JsonSerialize
    @Value.Immutable
    public interface BereiteRezeptZu extends WithEntityId {
    }

	// Events (Beschreibt den Fakt)

	@JsonSerialize
    @Value.Immutable
    public interface RezeptZubereitet extends WithEntityId  {
    }
	
	@JsonSerialize
    @Value.Immutable
    public interface ZubereitungFehlgeschlagen extends WithEntityId {
    }
}
