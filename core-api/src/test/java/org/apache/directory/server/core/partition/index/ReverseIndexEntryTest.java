/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.partition.index;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.UUID;

import org.apache.directory.server.core.api.partition.index.ReverseIndexEntry;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests the {@link ReverseIndexEntry} class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReverseIndexEntryTest
{

    private ReverseIndexEntry<String> indexEntry;


    @Before
    public void setUp()
    {
        indexEntry = new ReverseIndexEntry<String>();
    }


    @Test
    public void testSetGetId()
    {
        assertNull( indexEntry.getId() );

        indexEntry.setId( UUID.fromString( "00000000-0000-0000-0000-000000000001" ) );
        assertEquals( UUID.fromString( "00000000-0000-0000-0000-000000000001" ), indexEntry.getId() );
    }


    @Test
    public void testSetGetValue()
    {
        assertNull( indexEntry.getValue() );

        indexEntry.setValue( "test" );
        assertEquals( "test", indexEntry.getValue() );
    }


    @Test
    public void testSetGetObject()
    {
        assertNull( indexEntry.getEntry() );

        indexEntry.setEntry( new DefaultEntry() );
        assertEquals( new DefaultEntry(), indexEntry.getEntry() );
    }


    @Test
    public void testSetGetTuple()
    {
        assertNotNull( indexEntry.getTuple() );
        assertNull( indexEntry.getTuple().getKey() );
        assertNull( indexEntry.getTuple().getValue() );

        indexEntry.setTuple( new Tuple<UUID, String>( UUID.fromString( "00000000-0000-0000-0000-000000000001" ), "a" ), new DefaultEntry() );
        assertEquals( new Tuple<UUID, String>( UUID.fromString( "00000000-0000-0000-0000-000000000001" ), "a" ), indexEntry.getTuple() );
        assertEquals( new DefaultEntry(), indexEntry.getEntry() );
    }


    @Test
    public void testClear()
    {
        indexEntry.setTuple( new Tuple<UUID, String>( UUID.fromString( "00000000-0000-0000-0000-000000000001" ), "a" ), new DefaultEntry() );
        indexEntry.clear();

        assertNull( indexEntry.getId() );
        assertNull( indexEntry.getValue() );
        assertNull( indexEntry.getEntry() );
        assertNotNull( indexEntry.getTuple() );
        assertNull( indexEntry.getTuple().getKey() );
        assertNull( indexEntry.getTuple().getValue() );
    }


    @Test
    public void testCopy()
    {
        // prepare index entry
        indexEntry.setTuple( new Tuple<UUID, String>( UUID.fromString( "00000000-0000-0000-0000-000000000001" ), "a" ), new DefaultEntry() );

        // create empty index entry and assert empty values
        ReverseIndexEntry<String> indexEntry2 = new ReverseIndexEntry<String>();
        assertNull( indexEntry2.getId() );
        assertNull( indexEntry2.getValue() );
        assertNull( indexEntry2.getEntry() );
        assertNotNull( indexEntry2.getTuple() );
        assertNull( indexEntry2.getTuple().getKey() );
        assertNull( indexEntry2.getTuple().getValue() );

        // copy values and assert non-empty values
        indexEntry2.copy( indexEntry );
        assertEquals( UUID.fromString( "00000000-0000-0000-0000-000000000001" ), indexEntry2.getId() );
        assertEquals( "a", indexEntry2.getValue() );
        assertEquals( new DefaultEntry(), indexEntry2.getEntry() );
        assertEquals( new Tuple<UUID, String>( UUID.fromString( "00000000-0000-0000-0000-000000000001" ), "a" ), indexEntry2.getTuple() );
    }


    @Test
    public void testToString()
    {
        indexEntry.setTuple( new Tuple<UUID, String>( UUID.fromString( "00000000-0000-0000-0000-000000054321" ), "asdfghjkl" ), new DefaultEntry() );
        assertTrue( indexEntry.toString().contains( "asdfghjkl" ) );
        assertTrue( indexEntry.toString().contains( "54321" ) );
    }

}
