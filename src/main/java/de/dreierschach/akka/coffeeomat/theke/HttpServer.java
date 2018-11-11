package de.dreierschach.akka.coffeeomat.theke;

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
import akka.http.javadsl.server.Route;
import akka.pattern.PatternsCS;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import de.dreierschach.akka.coffeeomat.actor.theke.ImmutableGetPerson;
import de.dreierschach.akka.coffeeomat.actor.theke.ImmutablePersonData;
import de.dreierschach.akka.coffeeomat.actor.theke.ImmutableSetzeAlter;
import de.dreierschach.akka.coffeeomat.actor.theke.ImmutableUpdatePerson;

public class HttpServer extends AbstractActor {
    public static Props props(String host, int port, ActorRef persons) {
        return Props.create(HttpServer.class, () -> new HttpServer(host, port, persons));
    }

    private static final ObjectMapper om = new ObjectMapper();
    static {
        om.registerModule(new GuavaModule());
        om.registerModule(new Jdk8Module());
    }

    public HttpServer (String host, int port, ActorRef persons) {
        final Http http = Http.get(context().system());
        final Materializer mat = ActorMaterializer.create(context().system());

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = new Routes().createRoute(persons).flow(context().system(), mat);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost(host, port), mat);

        CoordinatedShutdown.get(context().system())
                .addTask(CoordinatedShutdown.PhaseServiceUnbind(),
                        "http-server",
                        () -> binding
                                .thenCompose(ServerBinding::unbind)
                                .thenApply(x -> Done.getInstance())
                );
    }


    @Override public Receive createReceive () {
        return AbstractActor.emptyBehavior();
    }

    private class Routes extends AllDirectives {
        private Route createRoute(ActorRef persons) {
            return handleExceptions(
                    ExceptionHandler.newBuilder()
                            .match(NoSuchElementException.class, exc -> complete(StatusCodes.NOT_FOUND))
                            .build(),
                    () -> route (
                            pathPrefix("persons", () -> route (
                                    pathEnd(() -> post(() -> entity(Jackson.unmarshaller(om, ImmutablePersonData.class),
                                            msg -> completeOKWithFuture(PatternsCS.ask(persons, msg, 10000), Jackson.marshaller(om))))),
                                    path(uuidSegment(), personId -> route (
                                            get(() -> completeOKWithFuture (PatternsCS.ask(persons, ImmutableGetPerson.of(personId), 10000), Jackson.marshaller(om))),
                                            put(() -> entity(Jackson.unmarshaller(om, ImmutableUpdatePerson.class),
                                                    msg -> completeOKWithFuture(PatternsCS.ask(persons, msg, 10000), Jackson.marshaller(om)))),
                                            path("alter", () -> route(
                                                    put(() -> entity(Jackson.unmarshaller(om, ImmutableSetzeAlter.class),
                                                            msg -> completeOKWithFuture(PatternsCS.ask(persons, msg, 10000), Jackson.marshaller(om))))                                           		
                                            		))
                                    ))
                            ))
                    ));
        }
    }
}
