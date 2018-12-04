package de.dreierschach.akka.coffeeomat.actor.lager;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import akka.actor.Props;
import akka.persistence.AbstractPersistentActor;

public class Lager extends AbstractPersistentActor {
	private final static Logger log = LoggerFactory.getLogger(Lager.class);

	public static Props props() {
		return Props.create(Lager.class, Lager::new);
	}

	@Override
	public String persistenceId() {
		return "lager-" + self().path().name();
	}

	private ImmutableBestand bestand = ImmutableBestand.builder().build();

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(LagerMessages.AddZutat.class, this::onAddZutat)
				.match(LagerMessages.GetBestand.class, this::onGetBestand)
				.match(LagerMessages.GetZutat.class, this::onGetZutat)
				.match(LagerMessages.PruefeZutaten.class, this::onPruefeZutaten)
				.match(LagerMessages.EntnehmeZutaten.class, this::onEntnehmeZutaten).build();
	}

	private void onAddZutat(LagerMessages.AddZutat msg) {
		persist(msg, evt -> {
			bestand = addZutat(bestand, msg.name(), msg.anzahl());
			log.info("==> Fuege {} mal {} hinzu. Bestand ist jetzt {}.", msg.anzahl(), msg.name(),
					toJson(bestand.zutaten()));
			sender().tell(bestand, self());
		});
	}

	private ImmutableBestand addZutat(ImmutableBestand bestand, String name, int anzahl) {
		int anzahlNeu = bestand.zutaten().getOrDefault(name, 0) + anzahl;
		ImmutableBestand.Builder builder = ImmutableBestand.builder();
		bestand.zutaten().entrySet().stream().filter(e -> !e.getKey().equals(name)).forEach(builder::putZutaten);
		bestand = builder.putZutaten(name, anzahlNeu).build();
		return bestand;
	}
	
	private void onGetZutat(LagerMessages.GetZutat msg) {
		int anzahl = bestand.zutaten().getOrDefault(msg.name(), 0);
		ImmutableZutat zutat = ImmutableZutat.builder().name(msg.name()).anzahl(anzahl).build();
		sender().tell(zutat, self());
		log.info("--- Zutat abgerufen {}", toJson(zutat));
	}

	private void onGetBestand(LagerMessages.GetBestand msg) {
		sender().tell(bestand, self());
		log.info("--- Bestand abgerufen {}", toJson(bestand));
	}

	private void onPruefeZutaten(LagerMessages.PruefeZutaten msg) {
		boolean erfolgreich = !msg.zutaten().entrySet().stream()
				.filter(e -> bestand.zutaten().getOrDefault(e.getKey(), 0) < e.getValue()).findAny().isPresent();
		log.info("==> Zutaten {} geprüft: {}", toJson(msg.zutaten()), erfolgreich);
		sender().tell(
				ImmutableZutatenGeprueft.builder().entityId(msg.bestellungId()).erfolgreich(erfolgreich).build(),
				self());
	}

	private void onEntnehmeZutaten(LagerMessages.EntnehmeZutaten msg) {
		boolean erfolgreich = !msg.zutaten().entrySet().stream()
				.filter(e -> bestand.zutaten().getOrDefault(e.getKey(), 0) < e.getValue()).findAny().isPresent();
		if (erfolgreich) {
			persist(ImmutableZutatenEntnommen.builder().bestellungId(msg.bestellungId()).erfolgreich(true)
					.putAllZutaten(msg.zutaten()).build(), evt -> {
						bestand = ImmutableBestand.builder()
								.putAllZutaten(bestand.zutaten().entrySet().stream()
										.collect(Collectors.toMap(e -> e.getKey(),
												e -> e.getValue() - msg.zutaten().getOrDefault(e.getKey(), 0))))
								.build();
						log.info("==> Zutaten {} erfolgreich entnommen, Bestand ist jetzt {}", toJson(msg.zutaten()),
								toJson(bestand.zutaten()));
						sender().tell(ImmutableZutatenEntnommen.builder().bestellungId(msg.bestellungId())
								.erfolgreich(true).build(), self());
					});
		} else {
			log.error(">>> Zutaten {} konnten nicht erfolgreich entnommen werden", toJson(msg.zutaten()));
			sender().tell(
					ImmutableZutatenEntnommen.builder().bestellungId(msg.bestellungId()).erfolgreich(false).build(),
					self());
		}
	}

	@Override
	public Receive createReceiveRecover() {
		return receiveBuilder()
				.match(LagerMessages.AddZutat.class, msg -> {
					int anzahl = bestand.zutaten().getOrDefault(msg.name(), 0) + msg.anzahl();
					bestand = addZutat(bestand, msg.name(), anzahl);
					log.info("==> REPLAY: Fuege {} mal {} hinzu. Bestand ist jetzt {}.", msg.anzahl(), msg.name(),
							toJson(bestand.zutaten()));					
				})
				.match(LagerMessages.EntnehmeZutaten.class, msg -> {
					bestand = ImmutableBestand.builder().putAllZutaten(bestand.zutaten().entrySet().stream().collect(
							Collectors.toMap(e -> e.getKey(), e -> e.getValue() - msg.zutaten().getOrDefault(e.getKey(), 0))))
							.build();
					log.info("==> REPLAY: Zutaten {} erfolgreich entnommen, Bestand ist jetzt {}", toJson(msg.zutaten()),
							toJson(bestand.zutaten()));					
				})
				.build();
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
