package de.dreierschach.akka.coffeeomat.actor.bedienung;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.persistence.AbstractPersistentActor;
import de.dreierschach.akka.coffeeomat.actor.barista.BaristaMessages;
import de.dreierschach.akka.coffeeomat.actor.barista.ImmutableBereiteRezeptZu;
import de.dreierschach.akka.coffeeomat.actor.barista.ImmutablePruefeRezept;

public class Bedienung extends AbstractPersistentActor {
	private final static Logger log = LoggerFactory.getLogger(Bedienung.class);

	public static Props props(ActorRef barista) {
		return Props.create(Bedienung.class, () -> new Bedienung(barista));
	}

	private ActorRef barista;

	private Bedienung(ActorRef barista) {
		this.barista = barista;
	}

	@Override
	public String persistenceId() {
		return "bedienung-" + self().path().name();
	}

	private Map<UUID, ImmutableBestellungCreated> bestellungen = new HashMap<>();

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(BedienungMessages.BestellungData.class, this::onCreateBestellung)
				.match(BedienungMessages.GetBestellung.class, this::onGet)
				.match(BaristaMessages.RezeptGeprueft.class, this::onRezeptGeprueft)
				.match(BedienungMessages.BestellungValidiert.class, this::onValidiert)
				.match(BedienungMessages.BestellungBezahlt.class, this::onBezahlt)
				.match(BaristaMessages.RezeptZubereitet.class, this::onRezeptZubereitet)
				.match(BedienungMessages.BestellungZubereitet.class, this::onBestellungZubereitet)
				.match(BedienungMessages.BestellungGeliefert.class, this::onGeliefert)
				.match(BedienungMessages.BestellungAbgebrochen.class, this::onAbgebrochen).build();
	}

	private void onCreateBestellung(BedienungMessages.BestellungData msg) {
		UUID entityId = UUID.randomUUID();
		log.info("==> Neue EntityId {} für Bestellung [Kunde: {}, Produkt: {}] vergeben.", entityId, msg.kunde(),
				msg.produkt());
		ImmutableBestellungCreated bestellung = ImmutableBestellungCreated.of(msg.kunde(), msg.produkt(), false, false,
				false, false, false, entityId);
		persist(bestellung, evt -> {
			bestellungen.put(bestellung.entityId(), bestellung);
			sender().tell(bestellung, self());
			log.info("==> Eine neue Bestellung ist reingekommen {}", toJson(msg));
			barista.tell(ImmutablePruefeRezept.of(bestellung.entityId(), bestellung.produkt()), self());
		});
	}

	private void onRezeptGeprueft(BaristaMessages.RezeptGeprueft msg) {
		log.info("==> Rezept von Bestellung {} geprüft: {}", msg.bestellungId(), msg.erfolgreich());
		if (msg.erfolgreich()) {
			self().tell(ImmutableBestellungValidiert.of(msg.bestellungId()), self());
		} else {
			self().tell(ImmutableBestellungAbgebrochen.of(msg.bestellungId()), self());
		}
	}

	private void onValidiert(BedienungMessages.BestellungValidiert msg) {
		persist(msg, evt -> {
			ImmutableBestellungCreated bestellung = bestellungen.get(msg.entityId()).withValidiert(true);
			bestellungen.put(msg.entityId(), bestellung);
			sender().tell(bestellung, self());
			log.info("==> Die Bestellung ist validiert {}", bestellung.entityId());
		});
	}

	private void onBezahlt(BedienungMessages.BestellungBezahlt msg) {
		persist(msg, evt -> {
			ImmutableBestellungCreated bestellung = bestellungen.get(msg.entityId()).withBezahlt(true);
			sender().tell(bestellung, self());
			log.info("==> Die Bestellung wurde bezahlt {}", bestellung.entityId());
			barista.tell(ImmutableBereiteRezeptZu.builder().bestellungId(bestellung.entityId()).name(bestellung.produkt()), self());
		});
	}
	
	private void onRezeptZubereitet(BaristaMessages.RezeptZubereitet msg) {
		log.info("==> Rezept von Bestellung {} zubereitet: {}", msg.bestellungId(), msg.erfolgreich());
		if (msg.erfolgreich()) {
			self().tell(ImmutableBestellungZubereitet.of(msg.bestellungId()), self());
		} else {
			self().tell(ImmutableBestellungAbgebrochen.of(msg.bestellungId()), self());
		}
	}

	private void onBestellungZubereitet(BedienungMessages.BestellungZubereitet msg) {
		persist(msg, evt -> {
			ImmutableBestellungCreated bestellung = bestellungen.get(msg.entityId());
			bestellung = bestellung.withZubereitet(true);
			bestellungen.put(msg.entityId(), bestellung);
			log.info("==> Die Bestellung {} ist nun zubereitet", bestellung.entityId());
		});
	}

	private void onGeliefert(BedienungMessages.BestellungGeliefert msg) {
		persist(msg, evt -> {
			ImmutableBestellungCreated bestellung = bestellungen.get(msg.entityId()).withGeliefert(true);
			bestellungen.put(msg.entityId(), bestellung);
			log.info("==> Die Bestellung ist serviert {}", bestellung.entityId());
		});
	}

	private void onAbgebrochen(BedienungMessages.BestellungAbgebrochen msg) {
		persist(msg, evt -> {
			ImmutableBestellungCreated bestellung = bestellungen.get(msg.entityId());
			bestellung = bestellung.withAbgebrochen(true);
			bestellungen.put(msg.entityId(), bestellung);
			log.error(">>> Bestellung wurde abgebrochen {}", bestellung.entityId());
		});
	}

	private void onGet(BedienungMessages.GetBestellung msg) {
		ImmutableBestellungCreated bestellung = bestellungen.get(msg.entityId());
		sender().tell(bestellung, self());
		log.info("--- Bestelldaten wurden abgerufen {}", bestellung.entityId());
	}

	@Override
	public Receive createReceiveRecover() {
		return receiveBuilder().match(BedienungMessages.BestellungCreated.class, msg -> {
			ImmutableBestellungCreated bestellung = ImmutableBestellungCreated.copyOf(msg);
			bestellungen.put(bestellung.entityId(), bestellung);
			log.info("==> REPLAY neue Bestellung {}", toJson(bestellung));
		}).match(BedienungMessages.BestellungValidiert.class, msg -> {
			ImmutableBestellungCreated bestellung = bestellungen.get(msg.entityId());
			bestellungen.put(bestellung.entityId(), bestellung.withValidiert(true));
			log.info("==> REPLAY Bestellung validiert {}", toJson(bestellung));
		}).match(BedienungMessages.BestellungBezahlt.class, msg -> {
			ImmutableBestellungCreated bestellung = bestellungen.get(msg.entityId());
			bestellungen.put(bestellung.entityId(), bestellung.withBezahlt(true));
			log.info("==> REPLAY Bestellung bezahlt {}", toJson(bestellung));
		}).match(BedienungMessages.BestellungZubereitet.class, msg -> {
			ImmutableBestellungCreated bestellung = bestellungen.get(msg.entityId());
			bestellungen.put(bestellung.entityId(), bestellung.withZubereitet(true));
			log.info("==> REPLAY Bestellung zubereitet {}", toJson(bestellung));
		}).match(BedienungMessages.BestellungGeliefert.class, msg -> {
			ImmutableBestellungCreated bestellung = bestellungen.get(msg.entityId());
			bestellungen.put(bestellung.entityId(), bestellung.withGeliefert(true));
			log.info("==> REPLAY Bestellung geliefert {}", toJson(bestellung));
		}).match(BedienungMessages.BestellungAbgebrochen.class, msg -> {
			ImmutableBestellungCreated bestellung = bestellungen.get(msg.entityId());
			bestellungen.put(bestellung.entityId(), bestellung.withAbgebrochen(true));
			log.info("==> REPLAY Bestellung abgebrochen {}", toJson(bestellung));
		}).build();
	}

	// Utils
	private final static ObjectMapper mapper = new ObjectMapper();

	private String toJson(Object o) {
		try {
			return mapper.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			log.error("Fehler beim mapping", e);
			return "";
		}
	}

}
