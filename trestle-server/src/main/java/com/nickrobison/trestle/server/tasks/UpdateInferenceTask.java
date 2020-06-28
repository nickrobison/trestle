package com.nickrobison.trestle.server.tasks;

import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.server.modules.ManagedReasoner;
import io.dropwizard.servlets.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Created by nrobison on 11/29/16.
 */
@Singleton
public class UpdateInferenceTask extends Task {

    public static final String INFERENCE_TASK_NAME = "InferenceTaskName";
    private static final Logger logger = LoggerFactory.getLogger(UpdateInferenceTask.class);
    private final TrestleReasoner reasoner;

    @Inject
    public UpdateInferenceTask(@Named(INFERENCE_TASK_NAME) String name, ManagedReasoner managedReasoner) {
        super(name);
        reasoner = managedReasoner.getReasoner();
        logger.info("Creating task {}", name);
    }

  @Override
  public void execute(Map<String, List<String>> map, PrintWriter printWriter) {
    logger.info("Updating inference");
    this.reasoner.getUnderlyingOntology().runInference();
  }
}
