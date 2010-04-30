/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.socket.netty;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * {@link ChannelUpstreamHandler} which limit connections per IP
 * 
 * This handler must be used as singleton when adding it to the {@link ChannelPipeline} to work correctly
 *
 */
@ChannelPipelineCoverage("all")
public class ConnectionPerIpLimitUpstreamHandler extends SimpleChannelUpstreamHandler{

    private final Map<String, Integer> connections = new HashMap<String, Integer>();
    private final int maxConnectionsPerIp;
    
    public ConnectionPerIpLimitUpstreamHandler(int maxConnectionsPerIp) {
        this.maxConnectionsPerIp = maxConnectionsPerIp;
    }
    
    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

        if (maxConnectionsPerIp > 0) {
            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.getChannel().getRemoteAddress();
            String remoteIp = remoteAddress.getAddress().getHostAddress();
            synchronized (connections) {
                Integer count = connections.get(remoteIp);

                if (count == null) {
                    count = new Integer(1);
                    connections.put(remoteIp, count);
                } else {
                    count++;
                    if (count > maxConnectionsPerIp) {
                        ctx.getChannel().close();
                        count--;
                    }
                    connections.put(remoteIp, count);
                }
               
            }
            
        }
        
        super.channelOpen(ctx, e);
    }
    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        if (maxConnectionsPerIp > 0) {
            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.getChannel().getRemoteAddress();
            String remoteIp = remoteAddress.getAddress().getHostAddress();
            synchronized (connections) {
                Integer count = connections.get(remoteIp);
                if (count != null) {
                    count--;
                    connections.put(remoteIp, count);
                }              
            }
        }
        super.channelClosed(ctx, e);
    }
}