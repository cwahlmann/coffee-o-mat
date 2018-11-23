package de.dreierschach.akka.coffeeomat.actor.lager;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.Status;
import akka.cluster.sharding.ShardRegion;
import akka.persistence.AbstractPersistentActor;
import scala.concurrent.duration.FiniteDuration;

class LagerEntity extends AbstractPersistentActor {
	private final static Logger log = LoggerFactory.getLogger(LagerEntity.class);

	private final static ObjectMapper mapper = new ObjectMapper();

	private String toJson(Object o) {
		try {
			return mapper.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			log.error("Fehler beim mapping", e);
			return "";
		}
	}

	static Props props() {
		return Props.create(LagerEntity.class, LagerEntity::new);
	}

	@Override
	public String persistenceId() {
		return "lager-" + self().path().name();
	}

	private ImmutableZutat zutat = ImmutableZutat.builder().name("UNSET").anzahl(0).build();

	private LagerEntity() {
		context().setReceiveTimeout(FiniteDuration.create(10, "s"));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(LagerMessages.AddZutat.class, this::onAddZutat)
				.match(LagerMessages.GetZutat.class, this::onGetZutat)
				.match(LagerMessages.EntnehmeZutat.class, this::onEntnehmeZutat)
				.match(LagerMessages.PruefeZutat.class, this::onPruefeZutat)
				.matchEquals(ReceiveTimeout.getInstance(), msg -> passivate()).build();
	}

	private void passivate() {
		context().parent().tell(new ShardRegion.Passivate(PoisonPill.getInstance()), self());
	}

	@Override
	public Receive createReceiveRecover() {
		return receiveBuilder().build();
	}

	private void onAddZutat(LagerMessages.AddZutat msg) {
		zutat = zutat.withEntityId(msg.entityId()).withName(msg.entityId()).withAnzahl(zutat.anzahl() + msg.anzahl());
		persist(ImmutableAddZutat.of(msg.anzahl(), msg.entityId()), evt -> {
			zutat = zutat.withEntityId(msg.entityId()).withName(msg.entityId())
					.withAnzahl(zutat.anzahl() + msg.anzahl());
			sender().tell(zutat, self());
		});
		log.info("Zutat hinzugefügt, aktuell: {}", toJson(zutat));
	}

	private void onGetZutat(LagerMessages.GetZutat msg) {
		if (zutat == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
		} else {
			sender().tell(zutat, self());
		}
	}

	private void onEntnehmeZutat(LagerMessages.EntnehmeZutat msg) {
		if (zutat == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
			return;
		}
		if (zutat.anzahl() < msg.anzahl()) {
			log.info("Gewünschte Menge {} konnte nicht von {} entnommen werden (aktuell: {})", msg.anzahl(), zutat.name(), zutat.anzahl());
			sender().tell(ImmutableNichtGenugZutatenVorhanden.builder().from(msg).build(), self());
			return;
		}
		zutat = zutat.withAnzahl(zutat.anzahl() - msg.anzahl());
		log.info("{} von Zutat {} entnommen, aktuell: {}", msg.anzahl(), msg.entityId(), toJson(zutat));
		sender().tell(ImmutableZutatEntnommen.builder().entityId(msg.entityId()).anzahl(msg.anzahl()).build(),
				self());
	}

	private void onPruefeZutat(LagerMessages.PruefeZutat msg) {
		if (zutat == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
			return;
		}
		if (zutat.anzahl() < msg.anzahl()) {
			log.info("Nicht genug {} vorhanden (aktuell: {}, angefragt: {})", zutat.name(), zutat.anzahl(), msg.anzahl());
			sender().tell(ImmutableNichtGenugZutatenVorhanden.builder().from(msg).build(), self());
			return;
		} 
		log.info("Ausreichend {} vorhanden (aktuell: {}, angefragt: {})", zutat.name(), zutat.anzahl(), msg.anzahl());
		sender().tell(ImmutableGenugZutatenVorhanden.builder().from(msg).build(), self());
	}
}
