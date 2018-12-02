package de.dreierschach.akka.coffeeomat.actor.bedienung;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;



public class BedienungMessages { 
    
	// Command (Impuls von außen + Seiteneffekte)
	
    public interface WithEntityId extends Serializable {
        @Value.Parameter UUID entityId();
    }
    
    @JsonSerialize
    @Value.Immutable
    public interface BestellungData extends Serializable {
        @Value.Parameter String kunde();
        @Value.Parameter String produkt();
    }
    
    @JsonSerialize
    @Value.Immutable
    public interface Bestellungen extends Serializable {
    	@Value.Parameter Map<UUID, BestellungCreated> bestellungen();
    }
    
    @JsonSerialize
    @Value.Immutable
    public interface BestellungCreated extends WithEntityId {
        @Value.Parameter String kunde();
        @Value.Parameter String produkt();    	
        @Value.Parameter boolean validiert();
        @Value.Parameter boolean bezahlt();
        @Value.Parameter boolean zubereitet();
        @Value.Parameter boolean geliefert();
        @Value.Parameter boolean abgebrochen();
    }

    @JsonSerialize
    @Value.Immutable
    public interface RezeptVonBaristaGeprueft extends WithEntityId {
        @Value.Parameter boolean erfolgreich();    	
    }

    @JsonSerialize
    @Value.Immutable
    public interface ZutatenImLagerGeprueft extends WithEntityId {
        @Value.Parameter boolean erfolgreich();    	
    }

    @JsonSerialize
    @Value.Immutable
    public interface BestellungValidiert extends WithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    public interface BestellungBezahlt extends WithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    public interface ZutatenAusLagerEntnommen extends WithEntityId {
        @Value.Parameter boolean erfolgreich();    	
    }

    @JsonSerialize
    @Value.Immutable
    public interface BestellungZubereitet extends WithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    public interface BestellungGeliefert extends WithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    public interface BestellungAbgebrochen extends WithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    public interface GetBestellung extends WithEntityId {
    }

    // Events (Beschreibt den Fakt)
}
