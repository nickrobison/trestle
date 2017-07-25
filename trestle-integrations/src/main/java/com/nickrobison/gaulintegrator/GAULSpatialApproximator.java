package com.nickrobison.gaulintegrator;

import com.esri.core.geometry.*;
import com.nickrobison.trestle.datasets.GAULObject;

import java.time.LocalDate;
import java.util.*;


/**
 * Created by detwiler on 7/11/17.
 */
public class GAULSpatialApproximator
{
    //   Spatial operators
    private static final OperatorFactoryLocal instance = OperatorFactoryLocal.getInstance();
    private static final OperatorIntersection operatorIntersection = (OperatorIntersection) instance.getOperator(Operator.Type.Intersection);
    private static final OperatorUnion operatorUnion = (OperatorUnion) instance.getOperator(Operator.Type.Union);

    public static boolean isApproxEqual(GAULObject inputObj, GAULObject matchObj, SpatialReference inputSR, double matchThreshold)
    {
        return isApproxEqual(inputObj.getShapePolygon(),matchObj.getShapePolygon(),inputSR,matchThreshold);
    }

    public static GAULSetMatch getApproxEqualUnion(List<GAULObject> conceptObjs, SpatialReference inputSR, double matchThreshold)
    {
        Map<Polygon,GAULObject> polyObjMap = new HashMap<Polygon,GAULObject>();
        TemporallyDividedObjs dividedObjs = divideObjectsTemporally(conceptObjs);
        Set<GAULObject> earlyObjs = dividedObjs.getEarlyObjs();
        Set<GAULObject> lateObjs = dividedObjs.getLateObjs();

        Set<Polygon> earlyPolys = new HashSet<Polygon>();
        for (GAULObject earlyObject : earlyObjs) {
            earlyPolys.add(earlyObject.getShapePolygon());
        }

        Set<Polygon> latePolys = new HashSet<Polygon>();
        for (GAULObject lateObject : lateObjs) {
            latePolys.add(lateObject.getShapePolygon());
        }

        PolySetMatch polySetMatch = getApproxEqualUnion(earlyPolys,latePolys,inputSR,matchThreshold);
        if(polySetMatch==null)
            return null;

        // determine objects that go with match
        Set<GAULObject> earlyResult = new HashSet<GAULObject>();
        Set<GAULObject> lateResult = new HashSet<GAULObject>();
        for(GAULObject earlyObj : earlyObjs)
        {
            if(polySetMatch.getEarlyPolys().contains(earlyObj.getShapePolygon()))
            {
                earlyResult.add(earlyObj);

            }
        }
        for(GAULObject lateObj : lateObjs)
        {
            if(polySetMatch.getLatePolys().contains(lateObj.getShapePolygon()))
            {
                lateResult.add(lateObj);

            }
        }

        GAULSetMatch gaulSetMatch = new GAULSetMatch(earlyResult,lateResult);
        return gaulSetMatch;
    }

    /*
    public static GAULSetMatch getApproxEqualUnion(List<GAULObject> conceptObjs, SpatialReference inputSR, double matchThreshold)
    {
        Map<Polygon,GAULObject> polyObjMap = new HashMap<Polygon,GAULObject>();

        TemporallyDividedObjs dividedObjs = divideObjectsTemporally(conceptObjs);
        Set<GAULObject> earlyObjs = dividedObjs.getEarlyObjs();
        Set<GAULObject> lateObjs = dividedObjs.getLateObjs();

        Set<Polygon> earlyPolys = new HashSet<Polygon>();
        for (GAULObject earlyObject : earlyObjs) {
            Polygon earlyPoly = earlyObject.getShapePolygon();
            earlyPolys.add(earlyPoly);
            polyObjMap.put(earlyPoly,earlyObject);
        }

        Set<Polygon> latePolys = new HashSet<Polygon>();
        for (GAULObject lateObject : lateObjs) {
            Polygon latePoly = lateObject.getShapePolygon();
            latePolys.add(latePoly);
            polyObjMap.put(latePoly,lateObject);
        }

        PolySetMatch polySetMatch = getApproxEqualUnion(earlyPolys,latePolys,inputSR,matchThreshold);
        if(polySetMatch==null)
            return null;

        // determine objects that go with match
        Set<GAULObject> earlyResult = new HashSet<GAULObject>();
        Set<GAULObject> lateResult = new HashSet<GAULObject>();
        for(Polygon earlyPoly : polySetMatch.getEarlyPolys())
        {
            earlyResult.add(polyObjMap.get(earlyPoly));
        }
        for(Polygon latePoly : polySetMatch.getLatePolys())
        {
            lateResult.add(polyObjMap.get(latePoly));
        }

        GAULSetMatch gaulSetMatch = new GAULSetMatch(earlyResult,lateResult);
        return gaulSetMatch;
    }
    */

    private static TemporallyDividedObjs divideObjectsTemporally(List<GAULObject> inputObjects)
    {
        // what to do if there are more than 2 time intervals represented?

        Collections.sort(inputObjects,new Comparator<GAULObject>(){
            @Override
            public int compare(final GAULObject lhs,GAULObject rhs) {
                // return 1 if rhs should be before lhs
                // return -1 if lhs should be before rhs
                // return 0 otherwise

                if(lhs.getStartDate().isEqual(rhs.getStartDate()) && lhs.getEndDate().isEqual(rhs.getEndDate()))
                    return 0;
                else if(rhs.getEndDate().isBefore(lhs.getStartDate())||rhs.getEndDate().isEqual(lhs.getStartDate()))
                    return 1;
                else if(lhs.getEndDate().isBefore(rhs.getStartDate())||lhs.getEndDate().isEqual(rhs.getStartDate()))
                    return -1;

                // should not get to here
                return 0;
            }
        });

        Set<GAULObject> earlyObjs = new HashSet<GAULObject>();
        Set<GAULObject> lateObjs = new HashSet<GAULObject>();
        LocalDate currEndDate = null;
        for(GAULObject currObject : inputObjects)
        {
            if(currEndDate==null)
                currEndDate = currObject.getEndDate();

            // if object has the same end date as currEndDate, put in earlyObjs
            if(currObject.getEndDate().isEqual(currEndDate))
                earlyObjs.add(currObject);
                // if object has a start date after the
            else if(currObject.getStartDate().isAfter(currEndDate)||currObject.getStartDate().isEqual(currEndDate))
                lateObjs.add(currObject);
        }

        TemporallyDividedObjs results = new TemporallyDividedObjs(earlyObjs,lateObjs);
        return  results;
    }

    /**
     * Test to see if two polygons occupy approximately the same space
     * @param inputPoly one polygon in test
     * @param matchPoly the other polygon in test
     * @param inputSR the spatial reference (for use when computing geometry of intersection)
     * @param matchThreshold a measure of how close the two polygons have to be to assert that they are approximately equal
     * @return true if (area of intersection)/(area of larger input polygon) gt matchTreshold, false otherwise
     */
    private static boolean isApproxEqual(Polygon inputPoly, Polygon matchPoly, SpatialReference inputSR, double matchThreshold)
    {
        // compute some basic spatial info
        Double inputPolyArea = inputPoly.calculateArea2D();
        Double matchPolyArea = matchPoly.calculateArea2D();
        Double greaterArea = inputPolyArea >= matchPolyArea ? inputPolyArea : matchPolyArea;

        // first compute the intersection and its area
        Geometry intersectionGeom = OperatorIntersection.local().execute(inputPoly,matchPoly,inputSR,null);
        Double geomArea = intersectionGeom.calculateArea2D();

        // now check (area of intersection)/(area of larger region)
        Double areaRatio = geomArea/greaterArea;

        // test to see if greater than threshold
        if(areaRatio >= matchThreshold)
        {
            // found approximate equality
            return true;
        }

        return false;
    }

    /**
     * Note, this is not yet correct. It currently will only test for unions in one direction and makes no test
     * that it is comparing objects from one time interval to next
     * @param inputPolys set of early polygon in test
     * @param matchPolys set of late polygons in test
     * @param inputSR spatial reference (for use in computing geometry of unions)
     * @param matchThreshold a measure of how close the two polygons have to be to assert that they are approximately equal
     * @return set of matchPolys whose union isApproxEqual to the inputPoly
     */
    private static PolySetMatch getApproxEqualUnion(Set<Polygon> inputPolys, Set<Polygon> matchPolys, SpatialReference inputSR, double matchThreshold)
    {
        // get all subsets of inputPolys
        Set<Set<Polygon>> allInputSets = powerSet(inputPolys);
        for(Set<Polygon> inputSet : allInputSets)
        {
            if(inputSet.size()<1)
                continue;

            // use union to create input polygon
            List inputGeomList = new ArrayList(inputSet);
            SimpleGeometryCursor inputGeomCurs = new SimpleGeometryCursor(inputGeomList);
            GeometryCursor unionInputGeoms = OperatorUnion.local().execute(inputGeomCurs, inputSR, null);
            Geometry unionInputGeom = null;
            while ((unionInputGeom = unionInputGeoms.next()) != null)
            {

                // get all subsets of matchPolys
                Set<Set<Polygon>> allMatchSets = powerSet(matchPolys);
                for (Set<Polygon> matchSet : allMatchSets) {
                    if (matchSet.size() < 1)
                        continue;

                    // generate union of polygons in set
                    List matchGeomList = new ArrayList(matchSet);
                    SimpleGeometryCursor matchGeomCurs = new SimpleGeometryCursor(matchGeomList);
                    GeometryCursor unionMatchGeoms = OperatorUnion.local().execute(matchGeomCurs, inputSR, null);
                    Geometry unionMatchGeom = null;
                    while ((unionMatchGeom = unionMatchGeoms.next()) != null) {
                        if (isApproxEqual((Polygon) unionInputGeom, (Polygon) unionMatchGeom, inputSR, matchThreshold))
                            return new PolySetMatch(inputSet,matchSet);
                    }
                }
            }
        }

        return null;
    }

    private static Set<Set<Polygon>> powerSet(Set<Polygon> originalSet) {
        Set<Set<Polygon>> sets = new HashSet<Set<Polygon>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<Polygon>());
            return sets;
        }
        List<Polygon> list = new ArrayList<Polygon>(originalSet);
        Polygon head = list.get(0);
        Set<Polygon> rest = new HashSet<Polygon>(list.subList(1, list.size()));
        for (Set<Polygon> set : powerSet(rest)) {
            Set<Polygon> newSet = new HashSet<Polygon>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }

    private static class TemporallyDividedObjs
    {
        private Set<GAULObject> earlyObjs;
        private Set<GAULObject> lateObjs;

        public TemporallyDividedObjs(Set<GAULObject> earlyObjs, Set<GAULObject> lateObjs)
        {
            this.earlyObjs = earlyObjs;
            this.lateObjs = lateObjs;
        }

        public Set<GAULObject> getEarlyObjs() {
            return earlyObjs;
        }

        public Set<GAULObject> getLateObjs() {
            return lateObjs;
        }
    }

    private static class PolySetMatch
    {
        private Set<Polygon> earlyPolys;
        private Set<Polygon> latePolys;

        public PolySetMatch(Set<Polygon> earlyPolys, Set<Polygon> latePolys)
        {
            this.earlyPolys = earlyPolys;
            this.latePolys = latePolys;
        }

        public Set<Polygon> getEarlyPolys() {
            return earlyPolys;
        }

        public Set<Polygon> getLatePolys() {
            return latePolys;
        }
    }

    public static class GAULSetMatch
    {
        private Set<GAULObject> earlyGaulObjs;
        private Set<GAULObject> lateGaulObjs;

        public GAULSetMatch(Set<GAULObject> earlyPolys, Set<GAULObject> latePolys)
        {
            this.earlyGaulObjs = earlyPolys;
            this.lateGaulObjs = latePolys;
        }

        public Set<GAULObject> getEarlyGaulObjs() {
            return earlyGaulObjs;
        }

        public Set<GAULObject> getLateGaulObjs() {
            return lateGaulObjs;
        }
    }
}
