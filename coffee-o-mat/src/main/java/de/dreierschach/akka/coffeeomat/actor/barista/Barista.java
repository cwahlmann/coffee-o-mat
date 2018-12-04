package de.dreierschach.akka.coffeeomat.actor.barista;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.PatternsCS;
import akka.persistence.AbstractPersistentActor;
import de.dreierschach.akka.coffeeomat.actor.barista.BaristaMessages.AddRezept;
import de.dreierschach.akka.coffeeomat.actor.lager.ImmutableEntnehmeZutaten;
import de.dreierschach.akka.coffeeomat.actor.lager.ImmutablePruefeZutaten;
import de.dreierschach.akka.coffeeomat.actor.lager.LagerMessages;

public class Barista extends AbstractPersistentActor {
	private final static Logger log = LoggerFactory.getLogger(Barista.class);

	public static Props props(ActorRef lager) {
		return Props.create(Barista.class, () -> new Barista(lager));
	}

	private ActorRef lager;

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
		return receiveBuilder().match(BaristaMessages.AddRezept.class, this::onAddRezept)
				.match(BaristaMessages.GetRezept.class, this::onGetRezept)
				.match(BaristaMessages.GetRezeptliste.class, this::onGetRezeptliste)
				.match(BaristaMessages.PruefeRezept.class, this::onPruefeRezept)
				.match(BaristaMessages.BereiteRezeptZu.class, this::onBereiteRezeptZu)
				.build();
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
		ActorRef sender = sender();
		if (!speisekarte.rezepte().containsKey(msg.name())) {
			sender.tell(ImmutableRezeptGeprueft.builder().bestellungId(msg.bestellungId()).erfolgreich(false).build(), self());
			log.info("==> Bestellung {}, Rezept {} unbekannt", msg.bestellungId(), msg.name());
			return;		
		}
		AddRezept rezept = speisekarte.rezepte().get(msg.name());
		log.info("==> Bestellung {}, Rezept {} gefunden, pruefe Zutaten {}", msg.bestellungId(),msg.name(),  rezept.zutaten());
		PatternsCS.ask(lager, ImmutablePruefeZutaten.builder().bestellungId(msg.bestellungId()).putAllZutaten(rezept.zutaten()).build(), 1000)
		.whenComplete((evt, throawble) ->  {
			LagerMessages.ZutatenGeprueft response = (LagerMessages.ZutatenGeprueft) evt;
			sender.tell(ImmutableRezeptGeprueft.builder().bestellungId(msg.bestellungId()).erfolgreich(response.erfolgreich()).build(),
					self());
			log.info("==> Bestellung {}, Rezept {}, Zutaten geprüft: {}", msg.bestellungId(), msg.name(), response.erfolgreich());
		});
	}

	private void onBereiteRezeptZu(BaristaMessages.BereiteRezeptZu msg) {
		ActorRef sender = sender();
		if (!speisekarte.rezepte().containsKey(msg.name())) {
			sender.tell(ImmutableRezeptZubereitet.builder().bestellungId(msg.bestellungId()).erfolgreich(false).build(), self());
			log.info("==> Bestellung {}, Rezept {} unbekannt", msg.bestellungId(), msg.name());
			return;		
		}
		AddRezept rezept = speisekarte.rezepte().get(msg.name());
		log.info("==> Bestellung {}, Rezept {} gefunden, entnehme Zutaten [}", msg.bestellungId(),msg.name(),  rezept.zutaten());
		PatternsCS.ask(lager, ImmutableEntnehmeZutaten.builder().bestellungId(msg.bestellungId()).putAllZutaten(rezept.zutaten()).build(), 1000)
		.whenComplete((evt, throawble) ->  {
			LagerMessages.ZutatenEntnommen response = (LagerMessages.ZutatenEntnommen) evt;
			sender.tell(ImmutableRezeptZubereitet.builder().bestellungId(msg.bestellungId()).erfolgreich(response.erfolgreich()).build(),
					self());
			log.info("==> Bestellung {}, Rezept {}, Zutaten entnommen: {}", msg.bestellungId(), msg.name(), response.erfolgreich());
		});
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
