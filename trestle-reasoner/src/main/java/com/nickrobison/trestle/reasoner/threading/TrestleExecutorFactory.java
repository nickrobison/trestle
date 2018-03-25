package com.nickrobison.trestle.reasoner.threading;

/**
 * Created by nickrobison on 3/24/18.
 */
public interface TrestleExecutorFactory {

    TrestleExecutorService create(String executorName);
}
