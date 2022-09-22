package uk.gov.di.ipv.stub.cred;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import spark.embeddedserver.EmbeddedServerFactory;
import spark.embeddedserver.EmbeddedServers;
import spark.embeddedserver.jetty.EmbeddedJettyFactory;
import spark.embeddedserver.jetty.JettyServerFactory;

import java.util.Collection;
import java.util.stream.Stream;


public class App {

    public static void main(String[] args) {
        EmbeddedServers.add(EmbeddedServers.Identifiers.JETTY, createEmbeddedServerFactory());

        new CredentialIssuer();
    }

    private static EmbeddedServerFactory createEmbeddedServerFactory() {
        return new EmbeddedJettyFactory(new JettyServerFactory() {
            @Override
            public Server create(int maxThreads, int minThreads, int threadTimeoutMillis) {
                return create(maxThreads <= 0 ? null
                        : new QueuedThreadPool(maxThreads, minThreads, threadTimeoutMillis));
            }

            @Override
            public Server create(ThreadPool threadPool) {
                Server server = new Server(threadPool);

                Stream.of(server.getConnectors()).map(Connector::getConnectionFactories).flatMap(Collection::stream)
                        .filter(t -> t.getClass().isAssignableFrom(HttpConnectionFactory.class))
                        .map(t -> ((HttpConnectionFactory) t).getHttpConfiguration()).forEach(t -> {
                            t.setRequestHeaderSize(10 * 1024);
                            t.setSendServerVersion(false);
                            t.setSendDateHeader(false);
                        });
                return server;
            }
        });
    }
}
