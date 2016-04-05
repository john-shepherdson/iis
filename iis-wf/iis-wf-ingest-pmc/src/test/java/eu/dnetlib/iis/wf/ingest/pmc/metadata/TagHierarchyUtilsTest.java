package eu.dnetlib.iis.wf.ingest.pmc.metadata;

import static eu.dnetlib.iis.wf.ingest.pmc.metadata.TagHierarchyUtils.*;
import static org.junit.Assert.*;

import java.util.Stack;

import org.junit.Test;

/**
 * @author mhorst
 * @author madryk
 */
public class TagHierarchyUtilsTest {

    
    //------------------------ TESTS --------------------------
    
    @Test
    public void testIsWithinElement() {
        Stack<String> parents = new Stack<String>();
        parents.add("ref-list");
        parents.add("ref");
        
        assertTrue(isWithinElement("name", "name", parents, "ref"));
        assertFalse(isWithinElement("name", "name", parents, "ref-list"));
    }
    
    @Test
    public void testHasAmongParents() {
        Stack<String> parents = new Stack<String>();
        parents.add("ref-list");
        parents.add("ref");
        parents.add("something");
        parents.add("name");
        assertTrue(
                hasAmongParents("surname", "surname", parents, "name", "something", "ref", "ref-list"));
        assertTrue(hasAmongParents("surname", "surname", parents, "name", "ref", "ref-list"));
        assertTrue(hasAmongParents("surname", "surname", parents, "name", "ref"));
        assertTrue(hasAmongParents("name", "name", parents, "name"));
        assertTrue(hasAmongParents("name", "name", parents, "ref"));
        assertTrue(hasAmongParents("name", "name", parents, "ref-list"));
        assertFalse(hasAmongParents("surname", "surname", parents, "ref", "name"));
        assertFalse(hasAmongParents("surname", "surname", parents, "ref-list", "ref"));
        assertFalse(hasAmongParents("name", "name", parents, "xxx"));
    }

}
