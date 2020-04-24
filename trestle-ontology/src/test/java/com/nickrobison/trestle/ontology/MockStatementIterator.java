package com.nickrobison.trestle.ontology;

import org.eclipse.rdf4j.repository.RepositoryResult;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Iterator;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by nickrobison on 4/19/20.
 */
@SuppressWarnings("unchecked")
public class MockStatementIterator {

    public static <T> RepositoryResult<T> mockResult(T... values) {
        final RepositoryResult<T> result = Mockito.mock(RepositoryResult.class);
        Iterator<T> mockIterator = mock(Iterator.class);
        when(result.iterator()).thenAnswer(answer -> mockIterator);
        if (values.length == 0) {
            when(mockIterator.hasNext()).thenReturn(false);
        } else if (values.length == 1) {
            when(mockIterator.hasNext()).thenReturn(true, false);
            when(mockIterator.next()).thenReturn(values[0]);
        } else {
            // build boolean array for hasNext()
            Boolean[] hasNextResponses = new Boolean[values.length];
            for (int i = 0; i < hasNextResponses.length -1 ; i++) {
                hasNextResponses[i] = true;
            }
            hasNextResponses[hasNextResponses.length - 1] = false;
            when(mockIterator.hasNext()).thenReturn(true, hasNextResponses);
            T[] valuesMinusTheFirst = Arrays.copyOfRange(values, 1, values.length);
            when(mockIterator.next()).thenReturn(values[0], valuesMinusTheFirst);
        }
        return result;
    }
}
