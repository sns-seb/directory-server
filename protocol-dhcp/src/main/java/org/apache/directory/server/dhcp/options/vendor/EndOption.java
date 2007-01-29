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

/**
 * The end option marks the end of valid information in the vendor
 * field.  Subsequent octets should be filled with pad options.
 * 
 * The code for the end option is 255, and its length is 1 octet.
 */
package org.apache.directory.server.dhcp.options.vendor;


import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EndOption extends DhcpOption
{
    public EndOption()
    {
        super( 255, 1 );
    }


    public void writeTo( ByteBuffer out )
    {
        out.put( ( byte ) 0xFF );
    }


    protected void valueToByteBuffer( ByteBuffer out )
    {
        /**
         * This option has no value
         */
    }
}
