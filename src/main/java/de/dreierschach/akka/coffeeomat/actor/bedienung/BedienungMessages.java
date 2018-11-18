package de.dreierschach.akka.coffeeomat.actor.bedienung;

import java.io.Serializable;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.dreierschach.akka.coffeeomat.common.Kaffeesorte;



public class BedienungMessages {
    
	// Command (Impuls von außen + Seiteneffekte)
	
    public interface BestellungWithEntityId extends Serializable {
        @Value.Parameter UUID entityId();
    }
    
    @JsonSerialize
    @Value.Immutable
    public interface BestellungData {
        @Value.Parameter String kunde();
        @Value.Parameter Kaffeesorte produkt();
    }
    
    @JsonSerialize
    @Value.Immutable
    public interface CreateBestellung extends BestellungWithEntityId {
        @Value.Parameter String kunde();
        @Value.Parameter Kaffeesorte produkt();    	
    }

    @JsonSerialize
    @Value.Immutable
    public interface SetBestellungValidiert extends BestellungWithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    public interface SetBestellungBezahlt extends BestellungWithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    public interface SetBestellungZubereitet extends BestellungWithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    public interface SetBestellungGeliefert extends BestellungWithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    public interface SetBestellungAbgebrochen extends BestellungWithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    public interface GetBestellung extends BestellungWithEntityId {
    }

    // Events (Beschreibt den Fakt)
    
    @JsonSerialize
    @Value.Immutable
    interface BestellungCreated extends BestellungWithEntityId {
        @Value.Parameter String kunde();
        @Value.Parameter Kaffeesorte produkt();
        @Value.Parameter boolean validiert();
        @Value.Parameter boolean bezahlt();
        @Value.Parameter boolean zubereitet();
        @Value.Parameter boolean geliefert();
        @Value.Parameter boolean abgebrochen();
    }
    
    @JsonSerialize
    @Value.Immutable
    interface BestellungValidiert extends BestellungWithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    interface BestellungBezahlt extends BestellungWithEntityId {
    }
    
    @JsonSerialize
    @Value.Immutable
    interface BestellungZubereitet extends BestellungWithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    interface BestellungGeliefert extends BestellungWithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    interface BestellungAbgebrochen extends BestellungWithEntityId {
    }
}
