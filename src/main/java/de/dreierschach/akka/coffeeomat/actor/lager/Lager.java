package de.dreierschach.akka.coffeeomat.actor.lager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardRegion;

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
                .build();
    }
}
