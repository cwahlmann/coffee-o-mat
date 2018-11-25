package de.dreierschach.akka.coffeeomat.actor.lager;

import java.util.Map;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;



public class LagerMessages {
    
	// Command (Impuls von außen + Seiteneffekte)

	@JsonSerialize
    @Value.Immutable
    public interface Bestand {
    	@Value.Parameter Map<String, Integer> zutaten();
    }
	
    @JsonSerialize
    @Value.Immutable
    public interface AddZutat {
    	@Value.Parameter String name();
    	@Value.Parameter int anzahl();
    }
	
    @JsonSerialize
    @Value.Immutable
    public interface GetZutat {
    	@Value.Parameter String name();
    }

    @JsonSerialize
    @Value.Immutable
    public interface GetBestand {
    	@Value.Parameter String name();
    }

    @JsonSerialize
    @Value.Immutable
    public interface Zutat {
    	@Value.Parameter String name();
    	@Value.Parameter int anzahl();
    }

    @JsonSerialize
    @Value.Immutable
    public interface PruefeZutaten {
    	@Value.Parameter UUID bestellungId();
    	@Value.Parameter Map<String, Integer> zutaten();
    }

    @JsonSerialize
    @Value.Immutable
    public interface EntnehmeZutaten {
    	@Value.Parameter UUID bestellungId();
    	@Value.Parameter Map<String, Integer> zutaten();
    }

    // Events (Beschreibt den Fakt)
    
    @JsonSerialize
    @Value.Immutable
    public interface ZutatenGeprueft {
    	@Value.Parameter UUID bestellungId();
    	@Value.Parameter boolean erfolgreich();
    }
    
    @JsonSerialize
    @Value.Immutable
    public interface ZutatenEntnommen {
    	@Value.Parameter UUID bestellungId();
    	@Value.Parameter boolean erfolgreich();
    }
}
