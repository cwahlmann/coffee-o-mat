package de.dreierschach.akka.coffeeomat.actor.bedienung;

import java.io.Serializable;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.dreierschach.akka.coffeeomat.actor.barista.BaristaMessages;



public class BedienungMessages {
    
	// Command (Impuls von außen + Seiteneffekte)
	
    public interface WithEntityId extends Serializable {
        @Value.Parameter UUID entityId();
    }
    
    @JsonSerialize
    @Value.Immutable
    public interface BestellungData {
        @Value.Parameter String kunde();
        @Value.Parameter String produkt();
    }
    
    // 
    
    @JsonSerialize
    @Value.Immutable
    public interface CreateBestellung extends WithEntityId {
        @Value.Parameter String kunde();
        @Value.Parameter String produkt();    	
    }

    @JsonSerialize
    @Value.Immutable
    public interface SetBestellungValidiert extends WithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    public interface SetBestellungBezahlt extends WithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    public interface SetBestellungZubereitet extends WithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    public interface SetBestellungGeliefert extends WithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    public interface SetBestellungAbgebrochen extends WithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    public interface GetBestellung extends WithEntityId {
    }

    // Events (Beschreibt den Fakt)
    
    @JsonSerialize
    @Value.Immutable
    interface BestellungCreated extends WithEntityId {
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
    interface BestellungValidiert extends WithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    interface BestellungBezahlt extends WithEntityId {
    }
    
    @JsonSerialize
    @Value.Immutable
    interface BestellungZubereitet extends WithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    interface BestellungGeliefert extends WithEntityId {
    }

    @JsonSerialize
    @Value.Immutable
    interface BestellungAbgebrochen extends WithEntityId {
    }
}
