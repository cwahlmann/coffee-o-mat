package de.dreierschach.akka.coffeeomat.actor.bedienung;

import java.util.NoSuchElementException;
import java.util.UUID;

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
import de.dreierschach.akka.coffeeomat.actor.barista.BaristaMessages;
import de.dreierschach.akka.coffeeomat.actor.barista.ImmutableAddRezept;
import de.dreierschach.akka.coffeeomat.actor.barista.ImmutableBereiteRezeptZu;
import de.dreierschach.akka.coffeeomat.actor.barista.ImmutablePruefeRezept;
import scala.concurrent.duration.FiniteDuration;

class BedienungEntity extends AbstractPersistentActor {
	private final static Logger log = LoggerFactory.getLogger(BedienungEntity.class);

	static Props props(ActorRef lager, ActorRef barista) {
		return Props.create(BedienungEntity.class, () -> new BedienungEntity(lager, barista));
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

	private ImmutableCreateBestellung bestellung;
	private ActorRef barista;

	private BedienungEntity(ActorRef lager, ActorRef barista) {
		context().setReceiveTimeout(FiniteDuration.create(10, "s"));
		this.barista = barista;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(BedienungMessages.GetBestellung.class, this::onGet)
				.match(BedienungMessages.CreateBestellung.class, this::onCreate)
				.match(BaristaMessages.RezeptGeprueft.class, this::onRezeptGeprueft)
				.match(BedienungMessages.SetBestellungBezahlt.class, this::onSetBezahlt)
				.match(BaristaMessages.RezeptZubereitet.class, this::onRezeptZubereitet)
				.match(BedienungMessages.SetBestellungZubereitet.class, this::onSetZubereitet)
				.match(BedienungMessages.SetBestellungGeliefert.class, this::onSetGeliefert)
				.match(BedienungMessages.SetBestellungAbgebrochen.class, this::onSetAbgebrochen)
				.matchEquals(ReceiveTimeout.getInstance(), msg -> passivate()).build();
	}

	private void onCreate(BedienungMessages.CreateBestellung msg) {
		persist(msg, evt -> {
					bestellung = ImmutableCreateBestellung.copyOf(msg);
					sender().tell(bestellung, self());	
					barista.tell(ImmutablePruefeRezept.of(msg.entityId(), msg.produkt()), self());
					log.info("==> Eine neue Bestellung ist reingekommen {}", toJson(bestellung));
				});
	}

	private void onRezeptGeprueft(BaristaMessages.RezeptGeprueft msg) {
		if (msg.erfolgreich()) {
			persist(ImmutableSetBestellungValidiert.of(msg.bestellungId()),
					evt -> {
						bestellung = bestellung.withValidiert(true);	
						log.info("==> Das Rezept wurde erfolgreich geprüft {}", toJson(bestellung));
					});
		} else {
			sender().tell(ImmutableSetBestellungAbgebrochen.builder().entityId(msg.bestellungId()).build(), self());
			log.error(">>> Das Rezept ist zur Zeit nicht verfügbar {}", toJson(bestellung));
		}		
	}

	private void onSetBezahlt(BedienungMessages.SetBestellungBezahlt msg) {
		if (bestellung == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
		} else {
			persist(msg, evt -> {
				bestellung = bestellung.withBezahlt(true);
				sender().tell(bestellung, self());
				barista.tell(ImmutableBereiteRezeptZu.builder().bestellungId(bestellung.entityId()).name(bestellung.produkt()).build(), self());
				log.info("==> Die Bestellung wurde bezahlt {}", bestellung.entityId());
			});
		}
	}

	private void onRezeptZubereitet(BaristaMessages.RezeptZubereitet msg) {
		if (!(msg instanceof BaristaMessages.RezeptZubereitet)) {
			self().tell(ImmutableSetBestellungZubereitet.builder().entityId(msg.bestellungId()), self());
			log.info("==> Das Rezept ist zubereitet {}", bestellung.entityId());
		} else {
			self().tell(ImmutableSetBestellungAbgebrochen.builder().entityId(msg.bestellungId()), self());
			log.error(">>> Das Rezept konnte nicht zubereitet werden {}", bestellung.entityId());
		}					
	}

	private void onSetZubereitet(BedienungMessages.SetBestellungZubereitet msg) {
		if (bestellung == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
		} else {
			persist(msg, evt -> {
				bestellung = bestellung.withZubereitet(true);
				log.info("==> Die Bestellung kann nun serviert werden {}", bestellung.entityId());
			});
		}
	}

	private void onSetGeliefert(BedienungMessages.SetBestellungGeliefert msg) {
		if (bestellung == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
		} else {
			persist(msg, evt -> {
				bestellung = bestellung.withGeliefert(true);
				sender().tell(bestellung, self());
				log.info("==> Die Bestellung ist serviert {}", bestellung.entityId());
			});
		}
	}

	private void onSetAbgebrochen(BedienungMessages.SetBestellungAbgebrochen msg) {
		if (bestellung == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
		} else {
			persist(msg, evt -> {
				bestellung = bestellung.withAbgebrochen(true);
				sender().tell(bestellung, self());
				log.error(">>> Bestellung wurde abgebrochen {}", bestellung.entityId());
			});
		}
	}

	private void onGet(BedienungMessages.GetBestellung msg) {
		if (bestellung == null) {
			sender().tell(new Status.Failure(new NoSuchElementException()), self());
			passivate();
		} else {
			sender().tell(bestellung, self());
			log.info("--- Bestelldaten wurden abgerufen {}", bestellung.entityId());
		}
	}

	private void passivate() {
		context().parent().tell(new ShardRegion.Passivate(PoisonPill.getInstance()), self());
	}

	@Override
	public Receive createReceiveRecover() {
		return receiveBuilder()
				.match(BedienungMessages.CreateBestellung.class,
						msg -> {
							bestellung = ImmutableCreateBestellung.copyOf(msg);
							log.info("==> REPLAY neue Bestellung {}", toJson(bestellung));
						})
				.match(BedienungMessages.SetBestellungValidiert.class, msg -> {
					bestellung = bestellung.withValidiert(true);
					log.info("==> REPLAY Bestellung validiert {}", toJson(bestellung));
				})
				.match(BedienungMessages.SetBestellungBezahlt.class, msg -> {
					bestellung = bestellung.withBezahlt(true);	
					log.info("==> REPLAY Bestellung bezahlt {}", toJson(bestellung));
				})
				.match(BedienungMessages.SetBestellungZubereitet.class,
						msg -> bestellung = bestellung.withZubereitet(true))
				.match(BedienungMessages.SetBestellungGeliefert.class, msg -> bestellung = bestellung.withGeliefert(true))
				.match(BedienungMessages.SetBestellungAbgebrochen.class,
						msg -> bestellung = bestellung.withAbgebrochen(true))
				.build();
	}
}
