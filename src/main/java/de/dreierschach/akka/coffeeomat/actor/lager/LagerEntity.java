package de.dreierschach.akka.coffeeomat.actor.lager;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.Status;
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
		return "person-" + self().path().name();
	}

	private ImmutableGetPersonResponse person;

	private LagerEntity() {
		context().setReceiveTimeout(FiniteDuration.create(10, "s"));
		person = ImmutableGetPersonResponse.builder().build();
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(LagerMessages.GetPerson.class, this::onGet)
				.match(LagerMessages.UpdatePerson.class, this::onUpdate)
				.match(LagerMessages.CreatePerson.class, this::onCreate)
				.match(LagerMessages.SetzeAlter.class, this::onSetzeAlter)
				.matchEquals(ReceiveTimeout.getInstance(), msg -> passivate()).build();
	}

	private void onCreate(LagerMessages.CreatePerson msg) {
		persist(ImmutablePersonUpdated.of(msg.name(), msg.address(), msg.entityId()), evt -> {
			person = ImmutableGetPersonResponse.of(evt.name(), evt.address(), 0, evt.entityId());
			sender().tell(person, self());
		});
		persist(ImmutableAlterGesetzt.of(0, msg.entityId()), evt -> {});
	}

	private void onUpdate(LagerMessages.UpdatePerson msg) {
		if (person == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
		} else {
			persist(ImmutablePersonUpdated.of(msg.name(), msg.address(), msg.entityId()), evt -> {
				person = person.withName(evt.name()).withAddress(evt.address()).withEntityId(evt.entityId());
				sender().tell(person, self());
			});
		}
	}

	private void onSetzeAlter(LagerMessages.SetzeAlter msg) {
		if (person == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
		} else {
			persist(ImmutableAlterGesetzt.of(msg.alter(), msg.entityId()), evt -> {
				person = person.withAlter(evt.alter()).withEntityId(evt.entityId());
				sender().tell(person, self());
			});
		}
	}

	private void onGet(LagerMessages.GetPerson msg) {
		if (person == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
		} else
			sender().tell(person, self());
	}

	private void passivate() {
		context().parent().tell(new ShardRegion.Passivate(PoisonPill.getInstance()), self());
	}

	@Override
	public Receive createReceiveRecover() {
		return receiveBuilder()
				.match(LagerMessages.PersonUpdated.class,
						msg -> person = person.withName(msg.name()).withAddress(msg.address()))
				.match(LagerMessages.AlterGesetzt.class,
						msg -> person = person.withAlter(msg.alter()))
				.build();
	}
}
