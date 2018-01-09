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

//        1 (Incorrect)
        List<String> values1 = ImmutableList.of("14449-Divenie-1000-2011",
                "190465-Divenie-2011-3001",
                "190466-Nyanga-2011-3001");
        experimentMap.put(1, values1);

//        2 (Correct)
        List<String> values2 = ImmutableList.of("22916-Edu-1000-1999",
                "191411-Edu-1999-3001",
                "191157-Pategi-1999-3001");
        experimentMap.put(2, values2);

//        3 (Correct)
        List<String> values3 = ImmutableList.of("14439-Loandjili_(pointe_Noire)-1000-2011",
                "190476-Tchiamba_Nzassi-2011-3001",
                "190474-Hinda-2011-3001");
        experimentMap.put(3, values3);

//        4 (Incorrect)
        List<String> values4 = ImmutableList.of("22718-Calabar-1000-1999",
                "191378-Calabar_Municipal-1999-3001",
                "191376-Akpabuyo-1999-3001");
        experimentMap.put(4, values4);

//        5 (Correct)
        List<String> values5 = ImmutableList.of("41374-Cidade_de_Maputo-2013-3001",
                "65253-Aeroporto-1000-2013",
                "65254-Distrito_Municipal_1-1000-2013",
                "65257-Distrito_Municipal_4-1000-2013",
                "65256-Distrito_Municipal_3-1000-2013",
                "65255-Distrito_Municipal_2-1000-2013",
                "65258-Distrito_Municipal_5-1000-2013");
        experimentMap.put(5, values5);

//        6 (Incorrect)
        List<String> values6 = ImmutableList.of("22709-Mobbar-1000-1999",
                "190951-Lake_chad-1999-3001",
                "190950-Abadam-1999-3001",
                "90969-Gubio-1999-3001",
                "191407-Mobbar-1999-300");
        experimentMap.put(6, values6);

//        7 (Incorrect)
        List<String> values7 = ImmutableList.of("14438-Kakameoka-1000-2011",
                "190471-Kakameoka-2011-3001",
                "190472-Madingo_Kayes-2011-3001");
        experimentMap.put(7, values7);

//        8 (Correct)
        List<String> values8 = ImmutableList.of("22617-Song-1000-1999",
                "191126-Gombi-1999-3001",
                "191111-Song-1999-3001");
        experimentMap.put(8, values8);


//        9 (Correct)
//        TODO(nickrobison): Find the one with the hole in the middle
        List<String> values9 = ImmutableList.of("22589-Aba-1000-1999",
                "191375-Aba_South-1999-3001",
                "91375-Aba_South-1999-3001");
        experimentMap.put(9, values9);

//        10 (Incorrect)
        List<String> values0 = ImmutableList.of("22657-Oyi-1000-1999",
                "191283-Ayamelum-1999-3001",
                "191304-Anambra_East-1999-3001",
                "191316-Oyi-1999-3001");
        experimentMap.put(10, values0);

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
