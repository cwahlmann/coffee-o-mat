package de.dreierschach.akka.coffeeomat.actor.bedienung;

import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardRegion;
import akka.pattern.PatternsCS;
import de.dreierschach.akka.coffeeomat.actor.barista.BaristaMessages;
import de.dreierschach.akka.coffeeomat.actor.lager.ImmutablePruefeZutat;
import de.dreierschach.akka.coffeeomat.actor.lager.LagerMessages;

public class Bedienung extends AbstractActor {
	private final static Logger log = LoggerFactory.getLogger(Bedienung.class);

	public static Props props(ActorRef lager, ActorRef produktion) {
        return Props.create(Bedienung.class, lager, produktion);
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
                .match(BedienungMessages.WithEntityId.class, msg -> shardRegion.forward(msg, context()))
                .build();
    }

    private void onCreateBestellung(BedienungMessages.BestellungData msg) {
        final UUID entityId = UUID.randomUUID();
        log.info("Neue EntityId {} für Bestellung [Kunde: {}, Produkt: {}] vergeben.", entityId, msg.kunde(), msg.produkt());
        self().forward(ImmutableCreateBestellung.of(msg.kunde(), msg.produkt(), entityId), context());
    }
    
}
