package de.dreierschach.akka.coffeeomat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import de.dreierschach.akka.coffeeomat.actor.theke.Theke;
import de.dreierschach.akka.coffeeomat.theke.HttpServer;

public class CoffeeOMat {
    private static final Logger log = LoggerFactory.getLogger(CoffeeOMat.class);

    public static void main (String[] args) throws InterruptedException {
        final ActorSystem system = ActorSystem.create("greeter");
        final String httpHost = system.settings().config().getString("coffeeomat.http-server.host");
        final int httpPort = system.settings().config().getInt("coffeeomat.http-server.port");

        final ActorRef persons = system.actorOf(Theke.props());
        system.actorOf(HttpServer.props (httpHost, httpPort, persons), "http-server");
    }
}
