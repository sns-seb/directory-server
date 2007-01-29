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
 * The time offset field specifies the offset of the client's subnet in
 * seconds from Coordinated Universal Time (UTC).  The offset is
 * expressed as a two's complement 32-bit integer.  A positive offset
 * indicates a location east of the zero meridian and a negative offset
 * indicates a location west of the zero meridian.
 * 
 * The code for the time offset option is 2, and its length is 4 octets.
 */
package org.apache.directory.server.dhcp.options.vendor;


import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class TimeOffset extends DhcpOption
{
    private byte[] timeOffset;


    public TimeOffset(byte[] timeOffset)
    {
        super( 2, 4 );
        this.timeOffset = timeOffset;
    }


    protected void valueToByteBuffer( ByteBuffer out )
    {
        out.put( timeOffset );
    }
}
