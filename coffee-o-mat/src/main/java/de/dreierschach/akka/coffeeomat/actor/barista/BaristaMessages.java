package de.dreierschach.akka.coffeeomat.actor.barista;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class BaristaMessages { 
        
	@JsonSerialize
    @Value.Immutable
    public interface AddRezept extends Serializable {
    	@Value.Parameter String name();
    	@Value.Parameter Map<String, Integer> zutaten();
    	@Value.Parameter double preis();
    }
    
	@JsonSerialize
    @Value.Immutable
    public interface Speisekarte extends Serializable {
    	@Value.Parameter Map<String, AddRezept> rezepte();
    }

	// Command (Impuls von außen + Seiteneffekte)

	@JsonSerialize
    @Value.Immutable
    public interface GetRezept extends Serializable {
    	@Value.Parameter String name();
    }
	
	@JsonSerialize
    @Value.Immutable
    public interface GetRezeptliste extends Serializable {
    }
	
	@JsonSerialize
    @Value.Immutable
    public interface PruefeRezept extends Serializable {
    	@Value.Parameter UUID bestellungId();
    	@Value.Parameter String name();
    }

	@JsonSerialize
    @Value.Immutable
    public interface BereiteRezeptZu extends Serializable {
    	@Value.Parameter UUID bestellungId();
    	@Value.Parameter String name();
    }

	// Events (Beschreibt den Fakt)
}
