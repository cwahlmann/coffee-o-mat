package de.dreierschach.akka.coffeeomat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import de.dreierschach.akka.coffeeomat.actor.barista.Barista;
import de.dreierschach.akka.coffeeomat.actor.barista.ImmutableAddRezept;
import de.dreierschach.akka.coffeeomat.actor.bedienung.Bedienung;
import de.dreierschach.akka.coffeeomat.actor.lager.ImmutableAddZutat;
import de.dreierschach.akka.coffeeomat.actor.lager.Lager;
import de.dreierschach.akka.coffeeomat.actor.theke.Theke;

public class CoffeeOMat {
    private static final Logger log = LoggerFactory.getLogger(CoffeeOMat.class);

    public static void main (String[] args) throws InterruptedException {
        final ActorSystem system = ActorSystem.create("coffeeomat");
        final String httpHost = system.settings().config().getString("coffeeomat.http-server.host");
        final int httpPort = system.settings().config().getInt("coffeeomat.http-server.port");
        
        log.info("-------- Erzeuge ActorRefs");
        ActorRef lager = system.actorOf(Lager.props(), "lager");
        ActorRef barista = system.actorOf(Barista.props(lager), "barista");
        ActorRef bedienung = system.actorOf(Bedienung.props(barista), "bedienung");
    	system.actorOf(Theke.props(httpHost, httpPort, bedienung, lager, barista), "theke");
    	
    	log.info("-------- Fülle Lager");
        lager.tell(ImmutableAddZutat.of("Bohnen", 150), system.lookupRoot());
        lager.tell(ImmutableAddZutat.of("Milch", 1000), system.lookupRoot());
        lager.tell(ImmutableAddZutat.of("Zucker", 1000), system.lookupRoot());
        
        log.info("-------- Rezepte für den Barista");
        barista.tell(ImmutableAddRezept.builder().name("Kaffee klein").putZutaten("Bohnen", 50).preis(1.75).build(), system.lookupRoot());
        barista.tell(ImmutableAddRezept.builder().name("Kaffee mittel").putZutaten("Bohnen", 75).preis(2.49).build(), system.lookupRoot());
        barista.tell(ImmutableAddRezept.builder().name("Kaffee gross").putZutaten("Bohnen", 120).preis(3.30).build(), system.lookupRoot());
        barista.tell(ImmutableAddRezept.builder().name("Espresso").putZutaten("Bohnen", 50).putZutaten("Zucker", 20).preis(2.25).build(), system.lookupRoot());
        barista.tell(ImmutableAddRezept.builder().name("Latte Macchiato").putZutaten("Bohnen", 60).putZutaten("Milch", 100).preis(3.79).build(), system.lookupRoot());
    }
}
