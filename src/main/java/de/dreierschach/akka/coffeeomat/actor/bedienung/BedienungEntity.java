package de.dreierschach.akka.coffeeomat.actor.bedienung;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.Status;
import akka.cluster.sharding.ShardRegion;
import akka.persistence.AbstractPersistentActor;
import scala.concurrent.duration.FiniteDuration;

class BedienungEntity extends AbstractPersistentActor {
	private final static Logger log = LoggerFactory.getLogger(BedienungEntity.class);

	static Props props(ActorRef lager, ActorRef produktion) {
		return Props.create(BedienungEntity.class, new BedienungEntity(lager, produktion));
	}

	private final static ObjectMapper mapper = new ObjectMapper();
	
	private String toJson(Object o) {
		try {
			return mapper.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			log.error("Fehler beim mapping", e);
			return "";
		}		
	}
	
	@Override
	public String persistenceId() {
		return "verwaltung-" + self().path().name();
	}

	private ImmutableBestellungCreated bestellung;
	private ActorRef lager;
	private ActorRef produktion;

	private BedienungEntity(ActorRef lager, ActorRef produktion) {
		context().setReceiveTimeout(FiniteDuration.create(10, "s"));
		bestellung = ImmutableBestellungCreated.builder().build();
		this.lager = lager;
		this.produktion = produktion;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(BedienungMessages.GetBestellung.class, this::onGet)
				.match(BedienungMessages.CreateBestellung.class, this::onCreate)
				.match(BedienungMessages.SetBestellungValidiert.class, this::onSetValidiert)
				.match(BedienungMessages.SetBestellungBezahlt.class, this::onSetBezahlt)
				.match(BedienungMessages.SetBestellungZubereitet.class, this::onSetZubereitet)
				.match(BedienungMessages.SetBestellungGeliefert.class, this::onSetGeliefert)
				.match(BedienungMessages.SetBestellungAbgebrochen.class, this::onSetAbgebrochen)
				.matchEquals(ReceiveTimeout.getInstance(), msg -> passivate()).build();
	}

	private void onCreate(BedienungMessages.CreateBestellung msg) {
		persist(ImmutableBestellungCreated.of(msg.kunde(), msg.produkt(), false, false, false, false, false,
				msg.entityId()), evt -> {
					bestellung = ImmutableBestellungCreated.of(msg.kunde(), msg.produkt(), false, false, false, false,
							false, evt.entityId());
					sender().tell(bestellung, self());
					lager.tell(bestellung, self());
				});
		log.info("Neue Bestellung entgegengenommen {}", toJson(bestellung));
	}

	private void onSetValidiert(BedienungMessages.SetBestellungValidiert msg) {
		if (bestellung == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
		} else {
			persist(ImmutableBestellungValidiert.of(msg.entityId()), evt -> bestellung = bestellung.withValidiert(true));
			log.info("Bestellung {} ist nun validiert", bestellung.entityId());
		}
	}

	private void onSetBezahlt(BedienungMessages.SetBestellungBezahlt msg) {
		if (bestellung == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
		} else {
			persist(ImmutableBestellungBezahlt.of(msg.entityId()), evt -> {
				bestellung = bestellung.withBezahlt(true);
				produktion.tell(ImmutableBestellungBezahlt.of(evt.entityId()), self());
				sender().tell(bestellung, self());
			});
			log.info("Bestellung {} ist nun bezahlt", bestellung.entityId());
		}
	}

	private void onSetZubereitet(BedienungMessages.SetBestellungZubereitet msg) {
		if (bestellung == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
		} else {
			persist(ImmutableBestellungZubereitet.of(msg.entityId()), evt -> {
				bestellung = bestellung.withZubereitet(true);
			});
			log.info("Bestellung {} ist nun zubereitet", bestellung.entityId());
		}
	}

	private void onSetGeliefert(BedienungMessages.SetBestellungGeliefert msg) {
		if (bestellung == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
		} else {
			persist(ImmutableBestellungGeliefert.of(msg.entityId()), evt -> {
				bestellung = bestellung.withGeliefert(true);
				sender().tell(bestellung, self());
			});
			log.info("Bestellung {} ist nun geliefert", bestellung.entityId());
		}
	}

	private void onSetAbgebrochen(BedienungMessages.SetBestellungAbgebrochen msg) {
		if (bestellung == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
		} else {
			persist(ImmutableBestellungAbgebrochen.of(msg.entityId()), evt -> {
				bestellung = bestellung.withAbgebrochen(true);
				sender().tell(bestellung, self());
			});
			log.info("Bestellung {} ist nun abgebrochen", bestellung.entityId());
		}
	}

	private void onGet(BedienungMessages.GetBestellung msg) {
		if (bestellung == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
		} else
			sender().tell(bestellung, self());
	}

	private void passivate() {
		context().parent().tell(new ShardRegion.Passivate(PoisonPill.getInstance()), self());
	}

	@Override
	public Receive createReceiveRecover() {
		return receiveBuilder()
				.match(BedienungMessages.BestellungCreated.class,
						msg -> bestellung = ImmutableBestellungCreated.copyOf(msg))
				.match(BedienungMessages.BestellungValidiert.class, msg -> bestellung = bestellung.withValidiert(true))
				.match(BedienungMessages.BestellungBezahlt.class, msg -> bestellung = bestellung.withBezahlt(true))
				.match(BedienungMessages.BestellungZubereitet.class,
						msg -> bestellung = bestellung.withZubereitet(true))
				.match(BedienungMessages.BestellungGeliefert.class, msg -> bestellung = bestellung.withGeliefert(true))
				.match(BedienungMessages.BestellungAbgebrochen.class,
						msg -> bestellung = bestellung.withAbgebrochen(true))
				.build();
	}
}
