package de.dreierschach.akka.coffeeomat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import de.dreierschach.akka.coffeeomat.actor.barista.Barista;
import de.dreierschach.akka.coffeeomat.actor.bedienung.Bedienung;
import de.dreierschach.akka.coffeeomat.actor.lager.Lager;
import de.dreierschach.akka.coffeeomat.actor.theke.Theke;

public class CoffeeOMat {
    private static final Logger log = LoggerFactory.getLogger(CoffeeOMat.class);

    public static void main (String[] args) throws InterruptedException {
        final ActorSystem system = ActorSystem.create("coffee-o-mat");
        final String httpHost = system.settings().config().getString("coffeeomat.http-server.host");
        final int httpPort = system.settings().config().getInt("coffeeomat.http-server.port");
        
        ActorRef lager = system.actorOf(Lager.props(), "lager");
        ActorRef produktion = system.actorOf(Barista.props(), "produktion");
        ActorRef verwaltung = system.actorOf(Bedienung.props(lager, produktion), "verwaltung");
        system.actorOf(Theke.props(httpHost, httpPort, verwaltung), "http-server");
    }
}
