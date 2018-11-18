package de.dreierschach.akka.coffeeomat.actor.lager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.cluster.sharding.ShardRegion;
import akka.persistence.AbstractPersistentActor;
import scala.concurrent.duration.FiniteDuration;

class LagerEntity extends AbstractPersistentActor {
	private final static Logger log = LoggerFactory.getLogger(LagerEntity.class);

	static Props props() {
		return Props.create(LagerEntity.class, LagerEntity::new);
	}

	@Override
	public String persistenceId() {
		return "lager-" + self().path().name();
	}

	private LagerEntity() {
		context().setReceiveTimeout(FiniteDuration.create(10, "s"));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.matchEquals(ReceiveTimeout.getInstance(), msg -> passivate()).build();
	}

	private void passivate() {
		context().parent().tell(new ShardRegion.Passivate(PoisonPill.getInstance()), self());
	}

	@Override
	public Receive createReceiveRecover() {
		return receiveBuilder()
				.build();
	}
}
