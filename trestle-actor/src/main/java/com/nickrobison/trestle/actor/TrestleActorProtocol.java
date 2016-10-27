package com.nickrobison.trestle.actor;

import com.nickrobison.trestle.exporter.ITrestleExporter;

import java.io.Serializable;
import java.time.temporal.Temporal;
import java.util.List;

/**
 * Created by nrobison on 10/25/16.
 */
public class TrestleActorProtocol implements Serializable {

    private static final long serialVersionUID = 42L;


    public static class SayHello implements Serializable {
        private static final long serialVersionUID = 42L;
        private final String hello;

        public SayHello(String name) {
            this.hello = name;
        }

        public String getHello() {
            return this.hello;
        }
    }

    public static class HelloStatus implements Serializable {
        private static final long serialVersionUID = 42L;
        public HelloStatus() {}
    }

    public static class SpatialIntersectObject<T> implements Serializable {
        private static final long serialVersionUID = 42L;
        private final T object;
        private final double buffer;

        public SpatialIntersectObject(T inputObject, double buffer) {
            object = inputObject;
            this.buffer = buffer;
        }

        public T getObject() {
            return this.object;
        }

        public double getBuffer() {
            return this.buffer;
        }
    }

    public static class SpatialIntersect implements Serializable {
        private static final long serialVersionUID = 42L;
        private final Class<?> clazz;
        private final double buffer;
        private final String wkt;
        private final String classString;

        public SpatialIntersect(String dataset, String wkt, double buffer) {
            this.clazz = null;
            this.classString = dataset;
            this.wkt = wkt;
            this.buffer = buffer;
        }

        public SpatialIntersect(Class<?> inputClass, String wkt, double buffer) {
            this.clazz = inputClass;
            this.wkt = wkt;
            this.buffer = buffer;
            this.classString = "";
        }

        public Class<?> getClazz() {
            return this.clazz;
        }

        public double getBuffer() {
            return this.buffer;
        }

        public String getWkt() {
            return this.wkt;
        }

        public String getClassString() { return this.classString; }
    }

    public static class TemporalSpatialIntersect extends SpatialIntersect {
        private final Temporal atTemporal;

        public TemporalSpatialIntersect(Class<?> inputClass, String wkt, double buffer, Temporal atTemporal) {
            super(inputClass, wkt, buffer);
            this.atTemporal = atTemporal;
        }

        public TemporalSpatialIntersect(String dataset, String wkt, double buffer, Temporal atTemporal) {
            super(dataset, wkt, buffer);
            this.atTemporal = atTemporal;
        }

        public Temporal getTemporal() {
            return this.atTemporal;
        }
    }

    public static class GetRelatedObjects<T> implements Serializable {
        private static final long serialVersionUID = 42L;
        private final Class<T> clazz;
        private final String id;
        private final double cutoff;


        public GetRelatedObjects(Class<T> clazz, String objectID, double cutoff) {
            this.clazz = clazz;
            this.id = objectID;
            this.cutoff = cutoff;
        }

        public Class<T> getClazz() {
            return this.clazz;
        }

        public String getId() {
            return this.id;
        }

        public double getCutoff() {
            return this.cutoff;
        }
    }

    public static class ExportData<T> implements Serializable {
        private static final long serialVersionUID = 42L;
        private final Class<T> clazz;
        private final List<String> objectIDs;
        private final ITrestleExporter.DataType dataType;

        public ExportData(Class<T> datasetClass, List<String> objectIDs, ITrestleExporter.DataType dataType) {
            this.clazz = datasetClass;
            this.objectIDs = objectIDs;
            this.dataType = dataType;
        }

        public Class<T> getClazz() {
            return clazz;
        }

        public List<String> getObjectIDs() {
            return objectIDs;
        }

        public ITrestleExporter.DataType getDataType() {
            return dataType;
        }
    }

    public static class GetDatasets<T> implements Serializable{
        private static final long serialVersionUID = 42L;
        public GetDatasets(){}
    }

    public static class ReadObject<T> implements Serializable{
        private static final long serialVersionUID = 42L;
        private final Class<T> clazz;
        private final String objectID;

        public ReadObject(Class<T> clazz, String objectID) {
            this.clazz = clazz;
            this.objectID = objectID;
        }

        public Class<T> getClazz() {
            return clazz;
        }

        public String getObjectID() {
            return objectID;
        }
    }
}
