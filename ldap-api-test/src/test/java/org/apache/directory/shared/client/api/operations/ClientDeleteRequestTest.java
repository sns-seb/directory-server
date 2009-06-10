/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.client.api.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.messages.DeleteResponse;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for client delete operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith(SiRunner.class)
@CleanupLevel(Level.METHOD)
@ApplyLdifs( {
    "dn: cn=parent,ou=system\n" +
    "objectClass: person\n" +
    "cn: parent_cn\n" +
    "sn: parent_sn\n" + 
    
    "\n" +
    
    "dn: cn=child1,cn=parent,ou=system\n" +
    "objectClass: person\n" +
    "cn: child1_cn\n" +
    "sn: child1_sn\n" + 
    
    "\n" +
    
    "dn: cn=child2,cn=parent,ou=system\n" +
    "objectClass: person\n" +
    "cn: child2_cn\n" +
    "sn: child2_sn\n" + 
    
    "\n" +
    
    "dn: cn=grand_child11,cn=child1,cn=parent,ou=system\n" +
    "objectClass: person\n" +
    "cn: grand_child11_cn\n" +
    "sn: grand_child11_sn\n" + 
    
    "\n" +
    
    "dn: cn=grand_child12,cn=child1,cn=parent,ou=system\n" +
    "objectClass: person\n" +
    "cn: grand_child12_cn\n" +
    "sn: grand_child12_sn\n"
})
public class ClientDeleteRequestTest
{
    public static LdapService ldapService;
    
    private LdapConnection connection;
    
    private CoreSession session;
    
    @Before
    public void setup() throws Exception
    {
        connection = new LdapConnection( "localhost", ldapService.getPort() );

        LdapDN bindDn = new LdapDN( "uid=admin,ou=system" );
        connection.bind( bindDn.getUpName(), "secret" );
        
        session = ldapService.getDirectoryService().getAdminSession();
    }
    
    @After
    public void clean() throws Exception
    {
        connection.close();
    }
    
    
    @Test
    public void testDeleteLeafNode() throws Exception
    {
        LdapDN dn = new LdapDN( "cn=grand_child12,cn=child1,cn=parent,ou=system" );
        
        assertTrue( session.exists( dn ) );
        
        DeleteResponse response = connection.delete( dn.getUpName() );
        assertNotNull( response );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        assertFalse( session.exists( dn ) );
    }
    
    
    @Test
    public void testDeleteNonLeafFailure() throws Exception
    {
        LdapDN dn = new LdapDN( "cn=child1,cn=parent,ou=system" ); // has children
        assertTrue( session.exists( dn ) );
 
        DeleteResponse response = connection.delete( dn.getUpName() );
        assertNotNull( response );
        assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_NON_LEAF, response.getLdapResult().getResultCode() );
        
        assertTrue( session.exists( dn ) );
    }
    

    @Ignore( "this method is failing, need to figure out the issue" )
    @Test
    public void testDeleteWithCascadeControl() throws Exception
    {
        LdapDN dn = new LdapDN( "cn=parent,ou=system" );
        
        assertTrue( session.exists( dn ) );
        
        DeleteResponse response = connection.delete( dn, true );
        assertNotNull( response );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        assertFalse( session.exists( dn ) );
    }
    
    
    /**
     * this method uses reflection to test deleteChildren method without using the
     * convenient method delete( dn, true ), cause the convenient method checks 
     * whether the server supports the CascadeControl.
     * 
     * Cause ADS supports this control, delete(dn, true) will never call the method
     * deleteChildren() (which has private scope) 
     * To test the manual deletion of the entries in the absence of this CascadeControl
     * reflection was used to invoke the private method deleteChildren().
     * 
     */
    @Test
    public void testDeleteWithoutCascadeControl() throws Exception
    {
        LdapDN dn = new LdapDN( "cn=parent,ou=system" );
        
        assertTrue( session.exists( dn ) );

        Method deleteChildrenMethod = connection.getClass().getDeclaredMethod( "deleteChildren", LdapDN.class, Map.class );
        deleteChildrenMethod.setAccessible( true );
        
        DeleteResponse response = ( DeleteResponse ) deleteChildrenMethod.invoke( connection, dn, null );
        assertNotNull( response );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        assertFalse( session.exists( dn ) );
    }
}
