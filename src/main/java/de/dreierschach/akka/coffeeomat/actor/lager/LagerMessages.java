package de.dreierschach.akka.coffeeomat.actor.lager;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.dreierschach.akka.coffeeomat.common.Zutat;



public class LagerMessages {
    
	// Command (Impuls von außen + Seiteneffekte)
	
	public interface LagerWithEntityId extends Serializable {
		@Value.Parameter String entityId();
	}

    @JsonSerialize
    @Value.Immutable
    public interface AddZutatData {
    	@Value.Parameter Zutat zutat();
    	@Value.Parameter int anzahl();
    }
	
    @JsonSerialize
    @Value.Immutable
    public interface ValidateZutatData {
    	@Value.Parameter Zutat zutat();
    	@Value.Parameter int anzahl();
    }

    @JsonSerialize
    @Value.Immutable
    public interface GetZutatData {
    	@Value.Parameter Zutat zutat();
    	@Value.Parameter int anzahl();
    }

    @JsonSerialize
    @Value.Immutable
    public interface ValidateZutat extends LagerWithEntityId {
    	@Value.Parameter int anzahl();
    }
    
    @JsonSerialize
    @Value.Immutable
    public interface addZutat extends LagerWithEntityId {
    	@Value.Parameter int anzahl();
    }    

    @JsonSerialize
    @Value.Immutable
    public interface getZutat extends LagerWithEntityId {
    }    

    @JsonSerialize
    @Value.Immutable
    public interface removeZutat extends LagerWithEntityId {
    	@Value.Parameter int anzahl();
    }    

    // Events (Beschreibt den Fakt)
    
}
