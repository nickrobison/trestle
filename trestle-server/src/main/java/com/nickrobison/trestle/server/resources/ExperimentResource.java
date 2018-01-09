package com.nickrobison.trestle.server.resources;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.server.modules.ReasonerModule;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by nickrobison on 1/9/18.
 */
@Path("/experiment")
public class ExperimentResource {
    private static final Random randomGenerator = new Random(12345);
    private static final Map<Integer, List<String>> experimentMap = buildExperimentMap();
    private final TrestleReasoner reasoner;


    @Inject
    public ExperimentResource(ReasonerModule reasonerModule) {
        this.reasoner = reasonerModule.getReasoner();
    }

    @GET
    @Path("{id}")
    public Response getExperimentResponse(@PathParam("id") Integer id) {
        final List<String> strings = experimentMap.get(id);
        if (strings != null) {
            final int mapState = randomGenerator.nextInt(8);
            final ExperimentResponse response = new ExperimentResponse(strings.get(0),
                    strings.subList(1, strings.size()),
                    mapState);
            return Response.ok().entity(response).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity("Cannot find that experiment number").build();
    }

    private static Map<Integer, List<String>> buildExperimentMap() {
        final Map<Integer, List<String>> experimentMap = new HashMap<>();
//        For each individual, build a map of all its potential unionOf values
//        1

        List<String> values1 = ImmutableList.of("hello", "value1", "value2", "value3", "value4");
        experimentMap.put(1, values1);
//        2
        List<String> values2 = ImmutableList.of("next", "v2", "v3", "v4");
        experimentMap.put(2, values2);

//        3


//        4


//        5

//        6

        return experimentMap;
    }


    public static class ExperimentResponse {

        private final String union;
        private final List<String> unionOf;
        private final int state;

        ExperimentResponse(String union, List<String> unionOf, int states) {
            this.union = union;
            this.unionOf = unionOf;
            this.state = states;
        }

        public String getUnion() {
            return union;
        }

        public List<String> getUnionOf() {
            return unionOf;
        }

        public int getState() {
            return state;
        }
    }


    public enum ExperimentState {
        OVERLAY(1),
        OPAQUE(2),
        NO_CONTEXT(4);

        private int value;

        ExperimentState(int val) {
            this.value = val;
        }

        public int getValue() {
            return this.value;
        }

        public static Set<ExperimentState> parsePrivileges(int val) {
            return Stream.of(values())
                    .filter(state -> (val & state.getValue()) != 0)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(ExperimentState.class)));
        }

        public static int buildStateMask(Set<ExperimentState> states) {
            int stateMask = 0;
            for (ExperimentState state : states) {
                stateMask = stateMask | state.getValue();
            }
            return stateMask;
        }
    }
}
