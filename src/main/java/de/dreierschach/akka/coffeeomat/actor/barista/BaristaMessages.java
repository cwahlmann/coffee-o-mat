package de.dreierschach.akka.coffeeomat.actor.barista;

import java.util.Map;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class BaristaMessages {
        
	@JsonSerialize
    @Value.Immutable
    public interface AddRezept {
    	@Value.Parameter String name();
    	@Value.Parameter Map<String, Integer> zutaten();
    	@Value.Parameter double preis();
    }
    
	@JsonSerialize
    @Value.Immutable
    public interface Speisekarte {
    	@Value.Parameter Map<String, AddRezept> rezepte();
    }

	// Command (Impuls von außen + Seiteneffekte)
		
	@JsonSerialize
    @Value.Immutable
    public interface GetRezept {
    	@Value.Parameter String name();
    }
	
	@JsonSerialize
    @Value.Immutable
    public interface GetRezeptliste {
    }
	
	@JsonSerialize
    @Value.Immutable
    public interface PruefeRezept {
    	@Value.Parameter UUID bestellungId();
    	@Value.Parameter String name();
    }

	@JsonSerialize
    @Value.Immutable
    public interface BereiteRezeptZu {
    	@Value.Parameter UUID bestellungId();
    	@Value.Parameter String name();
    }

	// Events (Beschreibt den Fakt)

	@JsonSerialize
    @Value.Immutable
    public interface RezeptGeprueft {
    	@Value.Parameter UUID bestellungId();
    	@Value.Parameter boolean erfolgreich();
    }

	@JsonSerialize
    @Value.Immutable
    public interface RezeptZubereitet {
    	@Value.Parameter UUID bestellungId();
    	@Value.Parameter boolean erfolgreich();
    }
}
