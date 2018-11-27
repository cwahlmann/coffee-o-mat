package de.dreierschach.akka.coffeeomat.actor.theke;

import static akka.http.javadsl.server.PathMatchers.uuidSegment;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletionStage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import akka.Done;
import akka.NotUsed;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.CoordinatedShutdown;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.ExceptionHandler;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import akka.pattern.PatternsCS;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import de.dreierschach.akka.coffeeomat.actor.barista.ImmutableAddRezept;
import de.dreierschach.akka.coffeeomat.actor.barista.ImmutableGetRezeptliste;
import de.dreierschach.akka.coffeeomat.actor.bedienung.ImmutableBestellungAbgebrochen;
import de.dreierschach.akka.coffeeomat.actor.bedienung.ImmutableBestellungBezahlt;
import de.dreierschach.akka.coffeeomat.actor.bedienung.ImmutableBestellungData;
import de.dreierschach.akka.coffeeomat.actor.bedienung.ImmutableBestellungGeliefert;
import de.dreierschach.akka.coffeeomat.actor.bedienung.ImmutableGetBestellung;
import de.dreierschach.akka.coffeeomat.actor.lager.ImmutableAddZutat;
import de.dreierschach.akka.coffeeomat.actor.lager.ImmutableGetBestand;

public class Theke extends AbstractActor {

	public static Props props(String host, int port, ActorRef bedienung, ActorRef lager, ActorRef barista) {
		return Props.create(Theke.class, () -> new Theke(host, port, bedienung, lager, barista));
	}

	private static final ObjectMapper om = new ObjectMapper();
	static {
		om.registerModule(new GuavaModule());
		om.registerModule(new Jdk8Module());
	}

	public Theke(String host, int port, ActorRef bedienung, ActorRef lager, ActorRef barista) {
		final Http http = Http.get(context().system());
		final Materializer mat = ActorMaterializer.create(context().system());

		final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = new Routes().createRoute(bedienung, lager, barista)
				.flow(context().system(), mat);
		final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost(host, port),
				mat);

		CoordinatedShutdown.get(context().system()).addTask(CoordinatedShutdown.PhaseServiceUnbind(), "http-server",
				() -> binding.thenCompose(ServerBinding::unbind).thenApply(x -> Done.getInstance()));
	}

	@Override
	public Receive createReceive() {
		return AbstractActor.emptyBehavior();
	}

	private class Routes extends AllDirectives {
		private Route createRoute(ActorRef bedienung, ActorRef lager, ActorRef barista) {
			return handleExceptions(
					ExceptionHandler.newBuilder()
						.match(NoSuchElementException.class, exc -> complete(StatusCodes.NOT_FOUND)).build(),
								() -> createRoutes(bedienung, lager, barista)
								);
		}

		private Route createRoutes(ActorRef bedienung, ActorRef lager, ActorRef barista) {
			return route(
					pathPrefix("bestellung", () -> route(
							pathEnd(() -> post(() -> entity(Jackson.unmarshaller(om, ImmutableBestellungData.class),
									msg -> completeOKWithFuture(PatternsCS.ask(bedienung, msg, 10000),
									Jackson.marshaller(om))))),
							pathPrefix(uuidSegment(), bestellungId -> route( 
									pathPrefix("bezahlen", () -> pathEnd(() -> completeOK(PatternsCS.ask(bedienung,
											ImmutableBestellungBezahlt.of(bestellungId), 10000), Jackson.marshaller(om)))),
									pathPrefix("annehmen", () -> pathEnd(() -> completeOK(PatternsCS.ask(bedienung,
											ImmutableBestellungGeliefert.of(bestellungId), 10000), Jackson.marshaller(om)))),
									pathPrefix("abbrechen", () -> pathEnd(() -> completeOK(PatternsCS.ask(bedienung,
											ImmutableBestellungAbgebrochen.of(bestellungId), 10000), Jackson.marshaller(om)))),
									pathEnd(() -> get(() -> completeOKWithFuture(PatternsCS.ask(bedienung,
											ImmutableGetBestellung.of(bestellungId), 10000), Jackson.marshaller(om))))
									
							))
					)),
					pathPrefix("lager", () -> route(
							pathEnd(() -> route(
									post(() -> entity(Jackson.unmarshaller(om, ImmutableAddZutat.class),
											msg -> completeOKWithFuture(PatternsCS.ask(lager, msg, 10000),
													Jackson.marshaller(om)))),
									get(() -> pathEnd(() -> completeOKWithFuture(PatternsCS.ask(lager, ImmutableGetBestand.builder().build(), 10000),
											Jackson.marshaller(om))))
									))									
					)),
					pathPrefix("barista", () -> route(
							pathEnd(() -> route(
									post(() -> entity(Jackson.unmarshaller(om, ImmutableAddRezept.class),
											msg -> completeOKWithFuture(PatternsCS.ask(barista, msg, 10000),
													Jackson.marshaller(om)))),
									get(() -> pathEnd(() -> completeOKWithFuture(PatternsCS.ask(barista, ImmutableGetRezeptliste.builder().build(), 10000),
													Jackson.marshaller(om))))
							))
									
					))

					
					);
			
		}
		
	}
	
}
