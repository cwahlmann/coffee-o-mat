package de.dreierschach.akka.coffeeomat.actor.barista;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.cluster.sharding.ShardRegion;
import akka.persistence.AbstractPersistentActor;
import scala.concurrent.duration.FiniteDuration;

class BaristaEntity extends AbstractPersistentActor {
	private final static Logger log = LoggerFactory.getLogger(BaristaEntity.class);

	static Props props() {
		return Props.create(BaristaEntity.class, BaristaEntity::new);
	}

	@Override
	public String persistenceId() {
		return "barista-" + self().path().name();
	}

	private ImmutableCreateRezept rezept = null;
	
	private BaristaEntity() {
		context().setReceiveTimeout(FiniteDuration.create(10, "s"));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(BaristaMessages.CreateRezept.class, this::createRezept)
				.match(BaristaMessages.GetRezept.class, this::getRezept)
				.match(BaristaMessages.BereiteRezeptZu.class, this::bereiteRezeptZu)
				.matchEquals(ReceiveTimeout.getInstance(), msg -> passivate()).build();
	}

	private void passivate() {
		context().parent().tell(new ShardRegion.Passivate(PoisonPill.getInstance()), self());
	}

	@Override
	public Receive createReceiveRecover() {
		return receiveBuilder()
				.match(BaristaMessages.CreateRezept.class, this::createRezept)
				.build();
	}
	
	private void createRezept(BaristaMessages.CreateRezept msg) {
		// persist
	}

	private void getRezept(BaristaMessages.GetRezept msg) {
		sender().tell(rezept, self());
	}
	
	private void bereiteRezeptZu(BaristaMessages.BereiteRezeptZu msg) {
		
	}

}
