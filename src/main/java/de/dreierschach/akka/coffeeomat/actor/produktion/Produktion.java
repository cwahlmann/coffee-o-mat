package de.dreierschach.akka.coffeeomat.actor.produktion;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardRegion;

public class Produktion extends AbstractActor {
	private final static Logger log = LoggerFactory.getLogger(Produktion.class);
    public static Props props() {
        return Props.create(Produktion.class, Produktion::new);
    }

    final ShardRegion.MessageExtractor messageExtractor = new ShardRegion.HashCodeMessageExtractor(1000) {
        @Override public String entityId (Object message) {
            return ((ProduktionMessages.WithEntityId) message).entityId().toString();
        }
    };
    
    final ActorRef shardRegion = ClusterSharding.get(context().system()).start(
            "persons",
            ProduktionEntity.props(),
            ClusterShardingSettings.create(context().system()),
            messageExtractor
            );


    @Override
    public Receive createReceive () {
        return receiveBuilder()
                .match(ProduktionMessages.PersonData.class, this::onCreatePerson)
                .match(ProduktionMessages.WithEntityId.class, msg -> shardRegion.forward(msg, context()))
                .build();
    }

    private void onCreatePerson(ProduktionMessages.PersonData msg) {
        final UUID entityId = UUID.randomUUID();
        self().forward(ImmutableCreatePerson.of(msg.name(), msg.address(), entityId), context());
    }
}
