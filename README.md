# coffee-o-mat
A sample application to demonstrate eventsourcing with akka

The application needs cassandra to persist events. Start an instance of cassandra locally with defaults (f.e. cassandra.bat -f) before starting coffee-o-mat.

In Eclipse there is no built-in Annotation Processing. This is needed for the immutable classes. You have to install the plugin m2e-apt to get that work.
