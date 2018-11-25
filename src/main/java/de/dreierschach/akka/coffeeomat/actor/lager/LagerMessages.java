package de.dreierschach.akka.coffeeomat.actor.lager;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;



public class LagerMessages {
    
	// Command (Impuls von außen + Seiteneffekte)

	@JsonSerialize
    @Value.Immutable
    public interface Bestand extends Serializable {
    	@Value.Parameter Map<String, Integer> zutaten();
    }
	
    @JsonSerialize
    @Value.Immutable
    public interface AddZutat extends Serializable {
    	@Value.Parameter String name();
    	@Value.Parameter int anzahl();
    }
	
    @JsonSerialize
    @Value.Immutable
    public interface GetZutat extends Serializable {
    	@Value.Parameter String name();
    }

    @JsonSerialize
    @Value.Immutable
    public interface GetBestand extends Serializable {
    	@Value.Parameter String name();
    }

    @JsonSerialize
    @Value.Immutable
    public interface Zutat extends Serializable {
    	@Value.Parameter String name();
    	@Value.Parameter int anzahl();
    }

    @JsonSerialize
    @Value.Immutable
    public interface PruefeZutaten extends Serializable {
    	@Value.Parameter UUID bestellungId();
    	@Value.Parameter Map<String, Integer> zutaten();
    }

    @JsonSerialize
    @Value.Immutable
    public interface EntnehmeZutaten extends Serializable {
    	@Value.Parameter UUID bestellungId();
    	@Value.Parameter Map<String, Integer> zutaten();
    }

    // Events (Beschreibt den Fakt)
    
    @JsonSerialize
    @Value.Immutable
    public interface ZutatAdded extends Serializable {
    	@Value.Parameter String name();
    	@Value.Parameter int anzahl();
    }

    @JsonSerialize
    @Value.Immutable
    public interface ZutatenGeprueft extends Serializable {
    	@Value.Parameter UUID bestellungId();
    	@Value.Parameter boolean erfolgreich();
    }
    
    @JsonSerialize
    @Value.Immutable
    public interface ZutatenEntnommen extends Serializable {
    	@Value.Parameter UUID bestellungId();
    	@Value.Parameter Map<String, Integer> zutaten();
    	@Value.Parameter boolean erfolgreich();
    }
}
