package de.dreierschach.akka.coffeeomat.actor.barista;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardRegion;
import de.dreierschach.akka.coffeeomat.actor.bedienung.BedienungMessages;
import de.dreierschach.akka.coffeeomat.actor.bedienung.ImmutableCreateBestellung;

public class Barista extends AbstractActor {
	private final static Logger log = LoggerFactory.getLogger(Barista.class);

	public static Props props() {
		return Props.create(Barista.class, Barista::new);
	}

	final ShardRegion.MessageExtractor messageExtractor = new ShardRegion.HashCodeMessageExtractor(1000) {
		@Override
		public String entityId(Object message) {
			return ((BaristaMessages.WithEntityId) message).entityId().toString();
		}
	};

	final ActorRef shardRegion = ClusterSharding.get(context().system()).start("barista", BaristaEntity.props(),
			ClusterShardingSettings.create(context().system()), messageExtractor);

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(BaristaMessages.AddRezeptData.class, this::onAddRezept)
				.match(BaristaMessages.WithEntityId.class, msg -> shardRegion.forward(msg, context())).build();
	}

	private void onAddRezept(BaristaMessages.AddRezeptData msg) {
		self().forward(ImmutableCreateRezept.builder().entityId(msg.name()).name(msg.name()).preis(msg.preis())
				.putAllZutaten(msg.zutaten()).build(), context());
	}

}
