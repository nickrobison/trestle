package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.Ignore;
import com.nickrobison.trestle.annotations.IndividualIdentifier;
import com.nickrobison.trestle.annotations.OWLClassName;
import com.nickrobison.trestle.annotations.Spatial;
import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.EndTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.types.TemporalType;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Created by nrobison on 7/29/16.
 */
@OWLClassName(className = "GAUL_Test")
class GAULMethodTest {

    public int adm0_code;
    private String adm0_name;
    @Spatial
    public String test_name;

    @Ignore
    public LocalDateTime defaultTime;
    private String privateField;
    //        @DefaultTemporalProperty(type = TemporalType.POINT, scope= TemporalScope.EXISTS, duration = 0, unit = ChronoUnit.YEARS)
    private LocalDateTime intervalStart;
    private LocalDateTime intervalEnd;

    public GAULMethodTest() {
        this.adm0_code = 4326;
        this.test_name = "new_test";
        this.adm0_name = "test region";
        this.defaultTime = LocalDateTime.of(1998, 3, 26, 0, 0);
        this.privateField = "don't read me";
        this.intervalStart = LocalDateTime.of(1989, 3, 26, 0, 0);
        this.intervalEnd = this.intervalStart.plusYears(5);
    }

    public GAULMethodTest(int adm0_code, String adm0_name, String test_name, LocalDateTime defaultTime, LocalDateTime intervalStart, LocalDateTime intervalEnd) {
        this.adm0_code = adm0_code;
        this.adm0_name = adm0_name;
        this.test_name = test_name;
        this.defaultTime = defaultTime;
        this.intervalStart = intervalStart;
        this.privateField = "don't read me";
        this.intervalEnd = intervalEnd;
    }

    @IndividualIdentifier
    public String getName() {
        return "string_from_method";
    }

    public int getAdm0_code() {
        return this.adm0_code;
    }

    public String getadm0_name() {
        return this.adm0_name;
    }

    @DefaultTemporalProperty(type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
    public LocalDateTime getTime() {
        return this.defaultTime;
    }

    @StartTemporalProperty(type = TemporalType.INTERVAL)
    public LocalDateTime getStart() {
        return this.intervalStart;
    }

    @EndTemporalProperty()
    public LocalDateTime getEnd() {
        return this.intervalEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GAULMethodTest that = (GAULMethodTest) o;

        if (getAdm0_code() != that.getAdm0_code()) return false;
        if (!adm0_name.equals(that.adm0_name)) return false;
        if (!test_name.equals(that.test_name)) return false;
        if (!defaultTime.equals(that.defaultTime)) return false;
        if (!privateField.equals(that.privateField)) return false;
        if (!intervalStart.equals(that.intervalStart)) return false;
        return intervalEnd.equals(that.intervalEnd);

    }

    @Override
    public int hashCode() {
        int result = getAdm0_code();
        result = 31 * result + adm0_name.hashCode();
        result = 31 * result + test_name.hashCode();
        result = 31 * result + defaultTime.hashCode();
        result = 31 * result + privateField.hashCode();
        result = 31 * result + intervalStart.hashCode();
        result = 31 * result + intervalEnd.hashCode();
        return result;
    }
}
