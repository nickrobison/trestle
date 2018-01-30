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

    public UserExperimentResult() {

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
}
