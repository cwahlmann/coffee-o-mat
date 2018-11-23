package de.dreierschach.akka.coffeeomat.actor.lager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

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
		@Override
		public String entityId(Object message) {
			return ((LagerMessages.WithEntityId) message).entityId().toString();
		}
	};

	final ActorRef shardRegion = ClusterSharding.get(context().system()).start("lager", LagerEntity.props(),
			ClusterShardingSettings.create(context().system()), messageExtractor);

	private ImmutableZutatenliste zutatenliste = ImmutableZutatenliste.builder().build();

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(LagerMessages.AddZutatData.class, this::onAddZutat)
				.match(LagerMessages.GetZutatData.class, this::onGetZutat)
				.match(LagerMessages.WithEntityId.class, msg -> shardRegion.forward(msg, context()))
				.build();
	}

	private void onAddZutat(LagerMessages.AddZutatData msg) {
		String entityId = msg.name();
		log.info("Neue EntityId {} für Zutat [Name: {}, Anzahl: {}] vergeben.", entityId, msg.name(), msg.anzahl());
		zutatenliste = zutatenliste.withZutatenliste(
				ImmutableSet.<String>builder().addAll(zutatenliste.zutatenliste()).add(entityId).build());
		self().forward(ImmutableAddZutat.of(msg.anzahl(), entityId), context());
	}

	private void onGetZutat(LagerMessages.GetZutatData msg) {
		self().forward(ImmutableGetZutat.of(msg.name()), context());
	}

}
