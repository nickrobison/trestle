package com.nickrobison.trestle.reasoner.engines.exporter;

import com.nickrobison.trestle.exporter.ITrestleExporter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.temporal.Temporal;
import java.util.List;

/**
 * Created by nickrobison on 2/19/18.
 */
public interface ITrestleDataExporter {
    /**
     * Export TrestleObject at the specified valid/database temporal
     *
     * @param inputClass - {@link Class} to parse
     * @param objectID - {@link List} of objectID strings to return
     * @param exportType - {@link ITrestleExporter.DataType} export datatype of file
     * @param <T> - Generic type parameter
     * @return - {@link File} of type {@link ITrestleExporter.DataType}
     * @throws IOException - Throws if it can't create the file
     */
    <T> File exportDataSetObjects(Class<T> inputClass, List<String> objectID, ITrestleExporter.DataType exportType) throws IOException;

    /**
     * Export TrestleObject at the specified valid/database temporal
     *
     * @param inputClass - {@link Class} to parse
     * @param objectID   - {@link List} of objectID strings to return
     * @param validAt    - {@link Temporal} of validAt time
     * @param databaseAt - {@link Temporal} of databaseAt time
     * @param exportType - {@link ITrestleExporter.DataType} export datatype of file
     * @param <T>        - Generic type parameter
     * @return - {@link File} of type {@link ITrestleExporter.DataType}
     * @throws IOException - Throws if it can't create the file
     */
    <T> File exportDataSetObjects(Class<T> inputClass, List<String> objectID, @Nullable Temporal validAt, @Nullable Temporal databaseAt, ITrestleExporter.DataType exportType) throws IOException;
}
