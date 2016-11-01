package com.nickrobison.trestle.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.ConfigFactory;

/**
 * Created by nrobison on 10/25/16.
 */
public class TrestleActor {

    public static void main(String[] args) {
        startupRemoteActor();
    }

    public static void startupRemoteActor() {
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
    }
}
