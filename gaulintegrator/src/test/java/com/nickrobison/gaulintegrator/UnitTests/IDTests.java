package com.nickrobison.gaulintegrator.UnitTests;

import com.nickrobison.gaulintegrator.common.ObjectID;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by nrobison on 5/6/16.
 */
public class IDTests {

    @Test
    public void testObjectIDCreation() {
        ObjectID newObject = new ObjectID();
        assertNotNull(newObject.getID());
        assertEquals(ObjectID.IDVersion.SIMPLE, newObject.getVersion());
    }

    @Test
    public void testUUIDSerialization() {
        UUID u = UUID.randomUUID();
        ObjectID testObjID = new ObjectID(u, ObjectID.IDVersion.SIMPLE);
        UUID u2 = testObjID.getID();
        assertEquals(u, u2);
    }

    @Test
    public void testObjectIDEquality() {
        UUID u = UUID.randomUUID();
        ObjectID objID1 = new ObjectID(u, ObjectID.IDVersion.SIMPLE);
        ObjectID objID2 = new ObjectID(u, ObjectID.IDVersion.SIMPLE);
        assertEquals("Objects with same UUID are not equal", objID1, objID2);
        ObjectID objID3 = new ObjectID();
        assertNotEquals("Objects with different UUIDs are equal", objID1, objID3);
        assertNotEquals("Objects with different UUIDs are equal", objID2, objID3);
    }

    @Test
    public void testObjectIDToString() {
        UUID u = UUID.randomUUID();
        ObjectID objID = new ObjectID(u, ObjectID.IDVersion.SIMPLE);
        assertEquals(u.toString(), objID.toString());
    }

    @Test
    public void testHierarchicalCreation() {
        UUID u = UUID.randomUUID();
        ObjectID obj1 = new ObjectID(u, ObjectID.IDVersion.HIERARCHICAL);
        ObjectID obj2 = new ObjectID(u, ObjectID.IDVersion.SIMPLE);
        assertNotEquals("Different Versions, should not be equal", obj1, obj2);
    }
}
