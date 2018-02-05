package com.nickrobison.trestle.server.resources;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.server.models.UserExperimentResultDAO;
import com.nickrobison.trestle.server.modules.ReasonerModule;
import com.nickrobison.trestle.server.models.UserExperimentResult;
import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by nickrobison on 1/9/18.
 */
@Path("/experiment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExperimentResource {
    private static final Logger logger = LoggerFactory.getLogger(ExperimentResource.class);
    private static final Random randomGenerator = new Random(12345);
    private static final Map<Integer, List<String>> experimentMap = buildExperimentMap();
    private final TrestleReasoner reasoner;
    private final UserExperimentResultDAO userExperimentResultDAO;


    @Inject
    public ExperimentResource(ReasonerModule reasonerModule,
                              UserExperimentResultDAO userExperimentResultDAO) {
        this.reasoner = reasonerModule.getReasoner();
        this.userExperimentResultDAO = userExperimentResultDAO;
    }

    @POST
    @Path("/submit")
    @UnitOfWork
    public Response submitResults(UserExperimentResult results) {
        logger.debug("Results:", results);
        final Long aLong = this.userExperimentResultDAO.create(results);
        return Response.ok().entity(aLong).build();
    }

    @GET
    @Path("{id}")
    public Response getExperimentResponse(@PathParam("id") Integer id) {
        final List<String> strings = experimentMap.get(id);
        if (strings != null) {
            final int mapState = randomGenerator.nextInt(8);
            final List<String> unionOf = strings.subList(1, strings.size());
            final ExperimentResponse response = new ExperimentResponse(strings.get(0),
                    unionOf,
                    mapState);
            return Response.ok().entity(response).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity("Cannot find that experiment number").build();
    }

    private static Map<Integer, List<String>> buildExperimentMap() {
        final Map<Integer, List<String>> experimentMap = new HashMap<>();
//        For each individual, build a map of all its potential unionOf values

//        1 (Incorrect)
//        Set<String> values1 = ImmutableList.of("14449-Divenie-1000-2011",
//                "190465-Divenie-2011-3001",
//                "190466-Nyanga-2011-3001");
        List<String> values1 = ImmutableList.of("14449-Divenie-1000-2011", "190465-Divenie-2011-3001", "14450-Kibangou-1000-2011", "14454-Mossendjo-1000-2011", "190466-Nyanga-2011-3001", "190461-Mayoko-2011-3001", "190469-Kibangou-2011-3001", "14453-Mayoko-1000-2011", "190464-Moungoundou_Sud-2011-3001", "190506-Moutamba-2011-3001");
        experimentMap.put(1, values1);

//        2 (Correct)
//        Set<String> values2 = ImmutableList.of("22916-Edu-1000-1999",
//                "191411-Edu-1999-3001",
//                "191157-Pategi-1999-3001");
        List<String> values2 = ImmutableList.of("22916-Edu-1000-1999", "22905-Kabba_/Bunu-1000-3001", "22907-Lokoja-1000-3001", "191411-Edu-1999-3001", "22948-Mokwa-1000-1999", "191169-Yagba_West-1999-3001", "22917-Ifelodun-1000-3001", "22913-Yagba_West-1000-1999", "191422-Lavun-1999-3001", "191157-Pategi-1999-3001", "22939-Katcha-1000-3001", "22946-Lavun-1000-1999", "191127-Mokwa-1999-3001", "22922-Moro-1000-3001", "191173-Yagba_East-1999-3001", "22912-Yagba_Est-1000-1999");
        experimentMap.put(2, values2);

//        3 (Correct)
//        Set<String> values3 = ImmutableList.of("14439-Loandjili_(pointe_Noire)-1000-2011",
//                "190476-Tchiamba_Nzassi-2011-3001",
//                "190474-Hinda-2011-3001");
        List<String> values3 = ImmutableList.of("14439-Loandjili_(pointe_Noire)-1000-2011", "14441-Mvouti-1000-3001", "190471-Kakameoka-2011-3001", "14440-Madingo_Kayes-1000-2011", "190472-Madingo_Kayes-2011-3001", "190476-Tchiamba_Nzassi-2011-3001", "190475-Pointe-Noire-1000-3001", "190474-Hinda-2011-3001", "14438-Kakameoka-1000-2011");
        experimentMap.put(3, values3);

//        4 (Incorrect)
//        Set<String> values4 = ImmutableList.of("22718-Calabar-1000-1999",
//                "191378-Calabar_Municipal-1999-3001",
//                "191376-Akpabuyo-1999-3001");
        List<String> values4 = ImmutableList.of("22718-Calabar-1000-1999", "191357-Odukpani-1999-3001", "191378-Calabar_Municipal-1999-3001", "191405-Calabar_South-1999-3001", "22714-Akamkpa-1000-3001", "22715-Akpabuyo-1000-1999", "191376-Akpabuyo-1999-3001", "22723-Odukpani-1000-1999");
        experimentMap.put(4, values4);

//        5 (Correct)
//        Set<String> values5 = ImmutableList.of("41374-Cidade_de_Maputo-2013-3001",
//                "65253-Aeroporto-1000-2013",
//                "65254-Distrito_Municipal_1-1000-2013",
//                "65257-Distrito_Municipal_4-1000-2013",
//                "65256-Distrito_Municipal_3-1000-2013",
//                "65255-Distrito_Municipal_2-1000-2013",
//                "65258-Distrito_Municipal_5-1000-2013");
        List<String> values5 = ImmutableList.of("41374-Cidade_de_Maputo-2013-3001", "21882-Boane-1000-3001", "65253-Aeroporto-1000-2013", "65254-Distrito_Municipal_1-1000-2013", "65257-Distrito_Municipal_4-1000-2013", "21885-Marracuene-1000-3001", "65256-Distrito_Municipal_3-1000-2013", "65252-Cidade_da_Matola-1000-3001", "21886-Matutuine-1000-3001", "65255-Distrito_Municipal_2-1000-2013", "65258-Distrito_Municipal_5-1000-2013");
        experimentMap.put(5, values5);

//        6 (Incorrect)
//        Set<String> values6 = ImmutableList.of("22709-Mobbar-1000-1999",
//                "190951-Lake_chad-1999-3001",
//                "190950-Abadam-1999-3001",
//                "90969-Gubio-1999-3001",
//                "191407-Mobbar-1999-300");
        List<String> values6 = ImmutableList.of("22709-Mobbar-1000-1999", "23120-Geidam-1000-3001", "190951-Lake_chad-1999-3001", "23124-Yunusari-1000-3001", "190950-Abadam-1999-3001", "190954-Kukawa-1999-3001", "190969-Gubio-1999-3001", "190976-Nganzai-1999-3001", "22707-Kukawa-1000-1999", "22705-Kaga-1000-1999", "190958-Guzamala-1999-3001", "191407-Mobbar-1999-3001", "190998-Magumeri-1999-3001");
        experimentMap.put(6, values6);

//        7 (Incorrect)
//        Set<String> values7 = ImmutableList.of("14438-Kakameoka-1000-2011",
//                "190471-Kakameoka-2011-3001",
//                "190472-Madingo_Kayes-2011-3001");
        List<String> values7 = ImmutableList.of("14438-Kakameoka-1000-2011", "14441-Mvouti-1000-3001", "190471-Kakameoka-2011-3001", "14452-Louvakou-1000-2011", "14440-Madingo_Kayes-1000-2011", "14450-Kibangou-1000-2011", "190472-Madingo_Kayes-2011-3001", "190470-Banba-2011-3001", "190474-Hinda-2011-3001", "14439-Loandjili_(pointe_Noire)-1000-2011", "190477-Louvakou-2011-3001");
        experimentMap.put(7, values7);

//        8 (Correct)
//        Set<String> values8 = ImmutableList.of("22617-Song-1000-1999",
//                "191126-Gombi-1999-3001",
//                "191111-Song-1999-3001");
        List<String> values8 = ImmutableList.of("22617-Song-1000-1999", "22618-Yola-1000-1999", "191123-Demsa-1999-3001", "22611-Maiha-1000-3001", "191126-Gombi-1999-3001", "22604-Fufore-1000-3001", "22616-Shelleng-1000-3001", "191141-Yola_South-1999-3001", "22606-Girie-1000-3001", "191111-Song-1999-3001", "22608-Hong-1000-3001", "191139-Yola_North-1999-3001");
        experimentMap.put(8, values8);


//        9 (Correct)
//        TODO(nickrobison): Find the one with the hole in the middle
//        Set<String> values9 = ImmutableList.of("22589-Aba-1000-1999",
//                "191375-Aba_South-1999-3001",
//                "91375-Aba_South-1999-3001");
        List<String> values9 = ImmutableList.of("22589-Aba-1000-1999", "191380-Ugwunagbo-1999-3001", "22602-Osisioma_Ngwa-1000-3001", "191375-Aba_South-1999-3001", "22597-Oboma_Ngwa-1000-1999", "191374-Aba_North-1999-3001", "191363-Oboma_Ngwa-1999-3001");
        experimentMap.put(9, values9);

//        10 (Incorrect)
//        Set<String> values0 = ImmutableList.of("22657-Oyi-1000-1999",
//                "191283-Ayamelum-1999-3001",
//                "191304-Anambra_East-1999-3001",
//                "191316-Oyi-1999-3001");
        List<String> values0 = ImmutableList.of("22657-Oyi-1000-1999", "22774-Uzo-Uwani-1000-3001", "22644-Anambra_West-1000-3001", "22646-Awka_North-1000-3001", "22654-Onitsha_North-1000-1999", "22761-Ezeagu-1000-3001", "22650-Njikoka-1000-1999", "191283-Ayamelum-1999-3001", "191304-Anambra_East-1999-3001", "22648-Idemili-1000-1999", "191317-Dunukofia-1999-3001", "191316-Oyi-1999-3001");
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
        BEARING(1),
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
