package de.dreierschach.akka.coffeeomat.util;

import akka.actor.AbstractActor;
import akka.actor.ReceiveTimeout;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.PFBuilder;
import scala.PartialFunction;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.BoxedUnit;

import java.util.concurrent.TimeUnit;


public abstract class AbstractActorWithTimeout extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(this);

    protected AbstractActorWithTimeout () {
        this (FiniteDuration.create(10, TimeUnit.SECONDS));
    }

    protected AbstractActorWithTimeout (FiniteDuration timeout) {
        context().setReceiveTimeout (timeout);
    }

    @Override public void aroundReceive (PartialFunction<Object, BoxedUnit> receive, Object msg) {
        final PartialFunction<Object, BoxedUnit> decorated = receive.orElse(
                new PFBuilder<Object, BoxedUnit>()
                        .match (ReceiveTimeout.class, this::onReceiveTimeout)
                        .build()
        );

        super.aroundReceive(decorated, msg);
    }

    private BoxedUnit onReceiveTimeout (ReceiveTimeout msg) {
        log.warning("timed out");
        context().stop(self());
        return null;
    }
}
