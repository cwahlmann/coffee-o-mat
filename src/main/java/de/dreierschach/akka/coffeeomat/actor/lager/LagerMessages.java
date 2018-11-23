package de.dreierschach.akka.coffeeomat.actor.lager;

import java.io.Serializable;
import java.util.Set;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;



public class LagerMessages {
    
	// Command (Impuls von außen + Seiteneffekte)

	@JsonSerialize
    @Value.Immutable
    public interface Zutatenliste {
    	@Value.Parameter Set<String> zutatenliste();
    }
	
	public interface WithEntityId extends Serializable {
		@Value.Parameter String entityId();
	}

    @JsonSerialize
    @Value.Immutable
    public interface AddZutatData {
    	@Value.Parameter String name();
    	@Value.Parameter int anzahl();
    }
	
    @JsonSerialize
    @Value.Immutable
    public interface GetZutatData {
    	@Value.Parameter String name();
    }

    @JsonSerialize
    @Value.Immutable
    public interface Zutat extends WithEntityId {
    	@Value.Parameter String name();
    	@Value.Parameter int anzahl();
    }

    @JsonSerialize
    @Value.Immutable
    public interface PruefeZutat extends WithEntityId {
    	@Value.Parameter int anzahl();
    }
    
    @JsonSerialize
    @Value.Immutable
    public interface AddZutat extends WithEntityId {
    	@Value.Parameter int anzahl();
    }    

    @JsonSerialize
    @Value.Immutable
    public interface GetZutat extends WithEntityId {
    }    

    @JsonSerialize
    @Value.Immutable
    public interface EntnehmeZutat extends WithEntityId {
    	@Value.Parameter int anzahl();
    }    

    // Events (Beschreibt den Fakt)
    
    @JsonSerialize
    @Value.Immutable
    public interface GenugZutatenVorhanden extends WithEntityId {
    }
    
    @JsonSerialize
    @Value.Immutable
    public interface ZutatEntnommen extends WithEntityId {
    	@Value.Parameter int anzahl();
    }

    @JsonSerialize
    @Value.Immutable
    public interface NichtGenugZutatenVorhanden extends WithEntityId {
    }
}
