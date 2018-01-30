package com.nickrobison.trestle.server.models;

import javax.persistence.*;
import java.util.List;

/**
 * Created by nickrobison on 1/29/18.
 */
@Entity
@Table(name = "EVALUATION_USERS")
public class UserExperimentResult {

    @Id
    @Column(name = "ID", unique = true, nullable = false)
    private Long userId;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "USER_ID", referencedColumnName = "ID")
    private List<ExperimentResult> experimentResults;
    @Embedded
    private UserDemographics demographics;

    public UserExperimentResult() {
//        Not used
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<ExperimentResult> getExperimentResults() {
        return experimentResults;
    }

    public void setExperimentResults(List<ExperimentResult> experimentResults) {
        this.experimentResults = experimentResults;
    }

    public UserDemographics getDemographics() {
        return demographics;
    }

    public void setDemographics(UserDemographics demographics) {
        this.demographics = demographics;
    }

    @Entity
    @Table(name = "EVALUATION_RESULTS")
    public static class ExperimentResult {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "ID", unique = true, nullable = false)
        private Long id;
        @Column(name = "USER_ID")
        private Long userId;
        private Integer expNumber;
        private Integer expState;
        private Long expTime;
        @Column(name = "HAS_UNION")
        private Boolean union;
        @Column(name = "UNION_OF")
        private String unionOf;
        private Integer sliderEvents;
        private Integer mapMoves;

        public ExperimentResult() {
//            Not used
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Integer getExpNumber() {
            return expNumber;
        }

        public void setExpNumber(Integer expNumber) {
            this.expNumber = expNumber;
        }

        public Integer getExpState() {
            return expState;
        }

        public void setExpState(Integer expState) {
            this.expState = expState;
        }

        public Long getExpTime() {
            return expTime;
        }

        public void setExpTime(Long expTime) {
            this.expTime = expTime;
        }

        public Boolean getUnion() {
            return union;
        }

        public void setUnion(Boolean union) {
            this.union = union;
        }

        public String getUnionOf() {
            return this.unionOf;
        }

        public void setUnionOf(String unionOf) {
            this.unionOf = unionOf;
        }

        public Integer getSliderEvents() {
            return sliderEvents;
        }

        public void setSliderEvents(Integer sliderEvents) {
            this.sliderEvents = sliderEvents;
        }

        public Integer getMapMoves() {
            return mapMoves;
        }

        public void setMapMoves(Integer mapMoves) {
            this.mapMoves = mapMoves;
        }
    }

    @Embeddable
    public static class UserDemographics {

        private Integer age;
        private String education;
        private Boolean geospatial;
        private Boolean publicHealth;


        public UserDemographics() {
//            Not used
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getEducation() {
            return education;
        }

        public void setEducation(String education) {
            this.education = education;
        }

        public Boolean getGeospatial() {
            return geospatial;
        }

        public void setGeospatial(Boolean geospatial) {
            this.geospatial = geospatial;
        }

        public Boolean getPublicHealth() {
            return publicHealth;
        }

        public void setPublicHealth(Boolean publicHealth) {
            this.publicHealth = publicHealth;
        }
    }
}
