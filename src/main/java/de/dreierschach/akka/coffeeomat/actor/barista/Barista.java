package de.dreierschach.akka.coffeeomat.actor.barista;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.PatternsCS;
import akka.persistence.AbstractPersistentActor;
import de.dreierschach.akka.coffeeomat.actor.lager.ImmutableEntnehmeZutaten;
import de.dreierschach.akka.coffeeomat.actor.lager.ImmutablePruefeZutaten;
import de.dreierschach.akka.coffeeomat.actor.lager.LagerMessages;

public class Barista extends AbstractPersistentActor {
	private final static Logger log = LoggerFactory.getLogger(Barista.class);

	private ActorRef lager;

	public static Props props(ActorRef lager) {
		return Props.create(Barista.class, () -> new Barista(lager));
	}

	public Barista(ActorRef lager) {
		this.lager = lager;
	}

	@Override
	public String persistenceId() {
		return "barista-" + self().path().name();
	}

	private ImmutableSpeisekarte speisekarte = ImmutableSpeisekarte.builder().build();

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(BaristaMessages.AddRezept.class, this::onAddRezept)
				.match(BaristaMessages.GetRezept.class, this::onGetRezept)
				.match(BaristaMessages.GetRezeptliste.class, this::onGetRezeptliste)
				.match(BaristaMessages.PruefeRezept.class, this::onPruefeRezept)
				.match(BaristaMessages.BereiteRezeptZu.class, this::onBereiteRezeptZu)
				.build();
	}

	@Override
	public Receive createReceiveRecover() {
		return receiveBuilder()
				.match(BaristaMessages.RezeptAdded.class, this::onRezeptAdded)
				.build();
	}

	private void onAddRezept(BaristaMessages.AddRezept msg) {
		persist(ImmutableRezeptAdded.builder().name(msg.name()).preis(msg.preis()).putAllZutaten(msg.zutaten()).build(), evt -> {
			speisekarte = ImmutableSpeisekarte.builder().putAllRezepte(speisekarte.rezepte())
					.putRezepte(msg.name(), ImmutableRezeptAdded.builder().name(msg.name()).preis(msg.preis()).putAllZutaten(msg.zutaten()).build()).build();
			log.info("==> Neues Rezept hinzugefügt: {}", toJson(msg));
			sender().tell(speisekarte, self());
		});
	}

	private void onGetRezept(BaristaMessages.GetRezept msg) {
		BaristaMessages.RezeptAdded rezept = speisekarte.rezepte().getOrDefault(msg.name(), ImmutableRezeptAdded.builder().build()); 
		sender().tell(rezept, self());
		log.info("--- Rezept abgerufen: {}", toJson(rezept));
	}

	private void onGetRezeptliste(BaristaMessages.GetRezeptliste msg) {
		sender().tell(speisekarte, self());
		log.info("--- Rezeptliste abgerufen: {}", toJson(speisekarte));
	}

	private void onPruefeRezept(BaristaMessages.PruefeRezept msg) {
		if (!speisekarte.rezepte().containsKey(msg.name())) {
			sender().tell(
					ImmutableRezeptGeprueft.builder().bestellungId(msg.bestellungId()).erfolgreich(false).build(),
					self());
				log.error(">>> Rezept ist nicht vorhanden {}", toJson(msg.name()));
			return;
		}
		BaristaMessages.RezeptAdded rezept = speisekarte.rezepte().get(msg.name());
		log.info("==> beginne Prüfung Rezept {}", toJson(rezept));
		PatternsCS.ask(lager, ImmutablePruefeZutaten.builder().bestellungId(msg.bestellungId())
				.putAllZutaten(rezept.zutaten()).build(), 1000)
		.whenComplete((evt, throwable) -> {
			LagerMessages.ZutatenGeprueft result = (LagerMessages.ZutatenGeprueft)evt;
			sender().tell(ImmutableRezeptGeprueft.builder().bestellungId(result.bestellungId()).erfolgreich(result.erfolgreich()), self());
			log.info("==> Rezept {} wurde geprüft: {}", msg.name(), result.erfolgreich());
		});
	}

	private void onBereiteRezeptZu(BaristaMessages.BereiteRezeptZu msg) {
		if (!speisekarte.rezepte().containsKey(msg.name())) {
			sender().tell(
					ImmutableRezeptZubereitet.builder().bestellungId(msg.bestellungId()).erfolgreich(false).build(),
					self());
			log.error(">>> Rezept {} kann nicht zubereitet werden, es ist nicht vorhanden", toJson(msg.name()));
			return;
		}
		BaristaMessages.RezeptAdded rezept = speisekarte.rezepte().get(msg.name());
		PatternsCS.ask(lager, ImmutableEntnehmeZutaten.builder().bestellungId(msg.bestellungId())
				.putAllZutaten(rezept.zutaten()).build(), 1000)
		.whenComplete((evt, throwable) -> {
			LagerMessages.ZutatenEntnommen result = (LagerMessages.ZutatenEntnommen)evt;
			sender().tell(ImmutableRezeptZubereitet.builder().bestellungId(result.bestellungId()).erfolgreich(result.erfolgreich()), self());
			log.info("==> Rezept {} wurde zubereitet: {}", msg.name(), result.erfolgreich());
		});
	}

	private void onRezeptAdded(BaristaMessages.RezeptAdded msg) {
		speisekarte = ImmutableSpeisekarte.builder().putAllRezepte(speisekarte.rezepte())
				.putRezepte(msg.name(), msg).build();
		log.info("==> REPLAY: Neues Rezept hinzugefügt: {}", toJson(msg));
		sender().tell(speisekarte, self());
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
