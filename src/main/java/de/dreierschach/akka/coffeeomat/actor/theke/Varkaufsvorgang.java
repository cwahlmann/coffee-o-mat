package de.dreierschach.akka.coffeeomat.actor.theke;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardRegion;
import de.dreierschach.akka.coffeeomat.persons.ImmutableCreatePerson;

public class Varkaufsvorgang extends AbstractActor {
	private final static Logger log = LoggerFactory.getLogger(Varkaufsvorgang.class);
    public static Props props() {
    	log.info("---===> Persons-Constructor called");
        return Props.create(Varkaufsvorgang.class, Varkaufsvorgang::new);
    }

    final ShardRegion.MessageExtractor messageExtractor = new ShardRegion.HashCodeMessageExtractor(1000) {
        @Override public String entityId (Object message) {
            return ((VerkaufsvorgangMessages.WithEntityId) message).entityId().toString();
        }
    };
    
    final ActorRef shardRegion = ClusterSharding.get(context().system()).start(
            "persons",
            VerkaufsvorgangEntity.props(),
            ClusterShardingSettings.create(context().system()),
            messageExtractor
            );


    @Override
    public Receive createReceive () {
        return receiveBuilder()
                .match(VerkaufsvorgangMessages.PersonData.class, this::onCreatePerson)
                .match(VerkaufsvorgangMessages.WithEntityId.class, msg -> shardRegion.forward(msg, context()))
                .build();
    }

    private void onCreatePerson(VerkaufsvorgangMessages.PersonData msg) {
        final UUID entityId = UUID.randomUUID();
        self().forward(ImmutableCreatePerson.of(msg.name(), msg.address(), entityId), context());
    }
}
