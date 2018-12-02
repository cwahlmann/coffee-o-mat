package de.dreierschach.akka.coffeeomat.actor.barista;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import akka.actor.Props;
import akka.persistence.AbstractPersistentActor;
import de.dreierschach.akka.coffeeomat.actor.bedienung.ImmutableBestellungZubereitet;
import de.dreierschach.akka.coffeeomat.actor.bedienung.ImmutableRezeptVonBaristaGeprueft;

public class Barista extends AbstractPersistentActor {
	private final static Logger log = LoggerFactory.getLogger(Barista.class);

	public static Props props() {
		return Props.create(Barista.class, Barista::new);
	}

	@Override
	public String persistenceId() {
		return "barista-" + self().path().name();
	}

	private ImmutableSpeisekarte speisekarte = ImmutableSpeisekarte.builder().build();

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(BaristaMessages.AddRezept.class, this::onAddRezept)
				.match(BaristaMessages.GetRezept.class, this::onGetRezept)
				.match(BaristaMessages.GetRezeptliste.class, this::onGetRezeptliste)
				.match(BaristaMessages.PruefeRezept.class, this::onPruefeRezept)
				.match(BaristaMessages.BereiteRezeptZu.class, this::onBereiteRezeptZu).build();
	}

	private void onAddRezept(BaristaMessages.AddRezept msg) {
		persist(msg, evt -> {
			speisekarte = addRezept(speisekarte, ImmutableAddRezept.copyOf(msg));
			log.info("==> Neues Rezept hinzugefügt: {}", toJson(msg));
			sender().tell(speisekarte, self());
		});
	}

	private ImmutableSpeisekarte addRezept(ImmutableSpeisekarte speisekarte, ImmutableAddRezept rezept) {
		ImmutableSpeisekarte.Builder builder = ImmutableSpeisekarte.builder();
		speisekarte.rezepte().entrySet().stream().filter(e -> !e.getKey().equals(rezept.name())).forEach(
				builder::putRezepte);
		return builder.putRezepte(rezept.name(), rezept).build();
	}
	
	private void onGetRezept(BaristaMessages.GetRezept msg) {
		BaristaMessages.AddRezept rezept = speisekarte.rezepte().getOrDefault(msg.name(),
				ImmutableAddRezept.builder().name("").preis(0).build());
		sender().tell(rezept, self());
		log.info("--- Rezept abgerufen: {}", toJson(rezept));
	}

	private void onGetRezeptliste(BaristaMessages.GetRezeptliste msg) {
		sender().tell(speisekarte, self());
		log.info("--- Rezeptliste abgerufen: {}", toJson(speisekarte));
	}

	private void onPruefeRezept(BaristaMessages.PruefeRezept msg) {
		boolean erfolgreich = speisekarte.rezepte().containsKey(msg.name());
		log.info("{}", sender());
		
		sender().tell(ImmutableRezeptVonBaristaGeprueft.builder().entityId(msg.bestellungId()).erfolgreich(erfolgreich).build(),
				self());
		log.info("==> Rezept {}, Bestellung {} geprüft: {}", msg.name(), msg.bestellungId(), erfolgreich);
	}

	private void onBereiteRezeptZu(BaristaMessages.BereiteRezeptZu msg) {
		sender().tell(ImmutableBestellungZubereitet.builder().entityId(msg.bestellungId()).build(),
				self());
		log.info("==> Rezept {} wurde erfolgreich zubereitet: {}", msg.name());
	}

	@Override
	public Receive createReceiveRecover() {
		return receiveBuilder().match(BaristaMessages.AddRezept.class, msg -> {
			speisekarte = addRezept(speisekarte, ImmutableAddRezept.copyOf(msg));
			log.info("==> REPLAY: Fuege Rezept {} hinzu", toJson(msg));
		}).build();
	}

	// util

	private final static ObjectMapper mapper = new ObjectMapper();

	private String toJson(Object o) {
		try {
			return mapper.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			log.error("--==>> Fehler beim mapping", e);
			return "";
		}
	}

}
