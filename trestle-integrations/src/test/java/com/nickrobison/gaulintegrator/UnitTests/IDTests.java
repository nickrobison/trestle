package com.nickrobison.gaulintegrator.UnitTests;

import com.nickrobison.gaulintegrator.common.ObjectID;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(objID1, objID2, "Objects with same UUID are not equal");
        ObjectID objID3 = new ObjectID();
        assertNotEquals(objID1, objID3, "Objects with different UUIDs are equal");
        assertNotEquals(objID2, objID3, "Objects with different UUIDs are equal");
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
        assertNotEquals(obj1, obj2, "Different Versions, should not be equal");
    }
}
