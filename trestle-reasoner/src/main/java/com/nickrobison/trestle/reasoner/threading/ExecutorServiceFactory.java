package com.nickrobison.trestle.reasoner.threading;

/**
 * Created by nickrobison on 3/24/18.
 */
public interface ExecutorServiceFactory {

    TrestleExecutorService create(String executorName, int executorSize);
}
