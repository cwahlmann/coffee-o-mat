package de.dreierschach.akka.coffeeomat.actor.bedienung;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardRegion;
import de.dreierschach.akka.coffeeomat.actor.barista.BaristaMessages;
import de.dreierschach.akka.coffeeomat.actor.lager.LagerMessages;

public class Bedienung extends AbstractActor {
	private final static Logger log = LoggerFactory.getLogger(Bedienung.class);

	public static Props props(ActorRef lager, ActorRef barista) {
        return Props.create(Bedienung.class, () -> new Bedienung(lager, barista));
    }

    final ShardRegion.MessageExtractor messageExtractor = new ShardRegion.HashCodeMessageExtractor(1000) {
        @Override public String entityId (Object message) {
            return ((BedienungMessages.WithEntityId) message).entityId().toString();
        }
    };
    
    final ActorRef shardRegion;

    private Bedienung(ActorRef lager, ActorRef produktion) {
    	shardRegion = ClusterSharding.get(context().system()).start(
                "verwaltung",
                BedienungEntity.props(lager, produktion),
                ClusterShardingSettings.create(context().system()),
                messageExtractor
                );
	}

    @Override
    public Receive createReceive () {
        return receiveBuilder()
                .match(BedienungMessages.BestellungData.class, this::onCreateBestellung)
                .match(LagerMessages.ZutatenGeprueft.class, this::onZutatenGeprueft)
                .match(BaristaMessages.RezeptZubereitet.class, this::onRezeptZubereitet)
                .match(BedienungMessages.WithEntityId.class, msg -> shardRegion.forward(msg, context()))
                .build();
    }

    private void onCreateBestellung(BedienungMessages.BestellungData msg) {
        UUID entityId = UUID.randomUUID();
        log.info("==> Neue EntityId {} für Bestellung [Kunde: {}, Produkt: {}] vergeben.", entityId, msg.kunde(), msg.produkt());
        self().forward(ImmutableCreateBestellung.of(msg.kunde(), msg.produkt(), entityId), context());
    }
    
    private void onZutatenGeprueft(LagerMessages.ZutatenGeprueft msg) {
        UUID entityId = msg.bestellungId();
        if (msg.erfolgreich()) {
        	self().forward(ImmutableSetBestellungValidiert.of(entityId), context());
        	log.info("==> Die Zutaten für die Bestellung {} sind vorhanden", msg.bestellungId());
        } else {
        	self().forward(ImmutableSetBestellungAbgebrochen.of(entityId), context());        	
        	log.error(">>> Es fehlen Zutaten für die Bestellung {}", msg.bestellungId());
        }
    }

    private void onRezeptZubereitet(BaristaMessages.RezeptZubereitet msg) {
        UUID entityId = msg.bestellungId();
        if (msg.erfolgreich()) {
        	self().forward(ImmutableSetBestellungZubereitet.of(entityId), context());
        	log.info("==> Die Zutaten für die Bestellung {} wurden aus dem Lager entnommen", msg.bestellungId());
        } else {
        	self().forward(ImmutableSetBestellungAbgebrochen.of(entityId), context());        	
        	log.error(">>> Die Zutaten für die Bestellung {} konnten nicht entnommen werden", msg.bestellungId());
        }
    }
}
