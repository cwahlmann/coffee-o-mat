package de.dreierschach.akka.coffeeomat.actor.lager;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardRegion;
import de.dreierschach.akka.coffeeomat.actor.bedienung.ImmutableCreateBestellung;

public class Lager extends AbstractActor {
	private final static Logger log = LoggerFactory.getLogger(Lager.class);
    public static Props props() {
        return Props.create(Lager.class, Lager::new);
    }

    final ShardRegion.MessageExtractor messageExtractor = new ShardRegion.HashCodeMessageExtractor(1000) {
        @Override public String entityId (Object message) {
            return ((LagerMessages.LagerWithEntityId) message).entityId().toString();
        }
    };
    
    final ActorRef shardRegion = ClusterSharding.get(context().system()).start(
            "lager",
            LagerEntity.props(),
            ClusterShardingSettings.create(context().system()),
            messageExtractor
            );


    @Override
    public Receive createReceive () {
        return receiveBuilder()
        		.match(LagerMessages.AddZutatData.class, this::onAddZutat)
        		.match(LagerMessages.ValidateZutatData.class, this::onValidateZutat)
        		.match(LagerMessages.LagerWithEntityId.class, msg -> shardRegion.forward(msg, context()))
                .build();
    }
    
    private void onAddZutat(LagerMessages.AddZutatData msg) {
    	String entityId = msg.zutat().name();
        log.info("Neue EntityId {} für Zutat [Zutat: {}, Anzahl: {}] vergeben.", entityId, msg.zutat().name(), msg.anzahl());
        self().forward(ImmutableAddZutat.of(msg.anzahl(), entityId), context());
    }

    private void onValidateZutat(LagerMessages.ValidateZutatData msg) {
    	String entityId = msg.zutat().name();
        log.info("Validiere Zutat [Zutat: {}, Anzahl: {}] vergeben.", entityId, msg.zutat().name(), msg.anzahl());
        self().forward(ImmutableValidateZutat.of(msg.anzahl(), entityId), context());
    }
}
