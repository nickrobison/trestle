package com.nickrobison.trestle.actor;

import akka.actor.*;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import org.apache.log4j.spi.LoggerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

import static com.nickrobison.trestle.actor.TrestleActor.startupRemoteActor;

/**
 * Created by nrobison on 10/26/16.
 */
@Tag("integration")
@Tag("oracle")
public class RemoteTest {

    Timeout timeout = Timeout.apply(120, TimeUnit.SECONDS);

    @BeforeAll
    public static void setup() {

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
//        Start the remote actor system and instantiate an actor
                final ActorSystem remoteActorSystem = ActorSystem.create("TrestleActorSystem", ConfigFactory.load("application"));
                final ActorRef remoteActor = remoteActorSystem.actorOf(Props.create(TrestleRemoteActor.class), "trestleActor");
            }
        });
        thread.isDaemon();
        thread.start();

//        startupRemoteActor();
    }

    @Test
    public void testHello() throws Exception {

//        Now the local actor
        final ActorSystem localSystem = ActorSystem.create("Application", ConfigFactory.load("remote"));
        final ActorRef localActor = localSystem.actorOf(Props.create(LocalActor.class), "localActor");

        final Future<Object> ask = Patterns.ask(localActor, "Hello world", 120000);

        String result = (String) Await.result(ask, timeout.duration());
        System.out.println(result);
    }


    private static class LocalActor extends UntypedActor {

        Timeout timeout = Timeout.apply(120, TimeUnit.SECONDS);
        private ActorSelection actorSelection;

        @Override
        public void preStart() {
            actorSelection = getContext().actorSelection("akka.tcp://TrestleActorSystem@127.0.0.1:2552/user/trestleActor");
        }

        @Override
        public void onReceive(Object message) throws Throwable {
            final Future<Object> ask = Patterns.ask(actorSelection, message.toString(), timeout);
            String result = (String) Await.result(ask, timeout.duration());
            sender().tell(result, self());
        }
    }
}
