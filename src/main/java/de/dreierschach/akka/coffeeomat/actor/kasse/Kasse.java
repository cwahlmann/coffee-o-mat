package de.dreierschach.akka.coffeeomat.actor.kasse;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardRegion;

public class Kasse extends AbstractActor {
	private final static Logger log = LoggerFactory.getLogger(Kasse.class);
    public static Props props() {
        return Props.create(Kasse.class, Kasse::new);
    }

    final ShardRegion.MessageExtractor messageExtractor = new ShardRegion.HashCodeMessageExtractor(1000) {
        @Override public String entityId (Object message) {
            return ((KasseMessages.WithEntityId) message).entityId().toString();
        }
    };
    
    final ActorRef shardRegion = ClusterSharding.get(context().system()).start(
            "persons",
            KasseEntity.props(),
            ClusterShardingSettings.create(context().system()),
            messageExtractor
            );


    @Override
    public Receive createReceive () {
        return receiveBuilder()
                .match(KasseMessages.PersonData.class, this::onCreatePerson)
                .match(KasseMessages.WithEntityId.class, msg -> shardRegion.forward(msg, context()))
                .build();
    }

    private void onCreatePerson(KasseMessages.PersonData msg) {
        final UUID entityId = UUID.randomUUID();
        self().forward(ImmutableCreatePerson.of(msg.name(), msg.address(), entityId), context());
    }
}
