package com.nickrobison.spatialdifference.storm.utils;

import com.esotericsoftware.kryo.Kryo;
import org.apache.storm.serialization.IKryoFactory;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.util.Map;

/**
 * Created by nrobison on 5/5/16.
 */
public class KryoFactory implements IKryoFactory {

    public Kryo CreateKryoSerializer() {

        return createGenericSerializer();
    }

    private Kryo createGenericSerializer() {
        Kryo kryo = new Kryo();
        //        Need this because many of the ESRI classes don't have a no-arg constructor
        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        return kryo;
    }

    @Override
    public Kryo getKryo(Map conf) {

        return createGenericSerializer();
    }

    @Override
    public void preRegister(Kryo k, Map conf) {

    }

    @Override
    public void postRegister(Kryo k, Map conf) {

    }

    @Override
    public void postDecorate(Kryo k, Map conf) {

    }
}
