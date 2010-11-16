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
package org.apache.james.pop3server.netty;

import javax.annotation.Resource;
import javax.net.ssl.SSLContext;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.pop3server.POP3HandlerConfigurationData;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.impl.AbstractSSLAwareChannelPipelineFactory;
import org.apache.james.services.MailServer;
import org.apache.james.socket.netty.AbstractConfigurableAsyncServer;
import org.apache.james.socket.netty.ConnectionCountHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * NIO POP3 Server which use Netty
 * 
 *
 */
public class POP3Server extends AbstractConfigurableAsyncServer implements POP3ServerMBean{
    /**
     * The number of bytes to read before resetting the connection timeout
     * timer. Defaults to 20 KB.
     */
    private int lengthReset = 20 * 1024;

    /**
     * The configuration data to be passed to the handler
     */
    private POP3HandlerConfigurationData theConfigData = new POP3HandlerConfigurationDataImpl();

    private final ConnectionCountHandler countHandler = new ConnectionCountHandler();
    
    private ProtocolHandlerChain handlerChain;

    private MailServer mailServer;

    
    public void setProtocolHandlerChain(ProtocolHandlerChain handlerChain) {
        this.handlerChain = handlerChain;
    }

    @Resource(name="mailserver")
    public final void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }
    @Override
    protected int getDefaultPort() {
        return 110;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.socket.ServerMBean#getServiceType()
     */
    public String getServiceType() {
        return "POP3 Service";
    }

    @Override
    protected void doConfigure(final HierarchicalConfiguration configuration) throws ConfigurationException {
        super.doConfigure(configuration);
        HierarchicalConfiguration handlerConfiguration = configuration.configurationAt("handler");
        lengthReset = handlerConfiguration.getInteger("lengthReset", lengthReset);
        if (getLogger().isInfoEnabled()) {
            getLogger().info("The idle timeout will be reset every " + lengthReset + " bytes.");
        }
    }

  

    /**
     * A class to provide POP3 handler configuration to the handlers
     */
    private class POP3HandlerConfigurationDataImpl implements POP3HandlerConfigurationData {

        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#getHelloName()
         */
        public String getHelloName() {
            if (POP3Server.this.getHelloName() == null) {
                return POP3Server.this.mailServer.getHelloName();
            } else {
                return POP3Server.this.getHelloName();
            }
        }

        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#getResetLength()
         */
        public int getResetLength() {
            return POP3Server.this.lengthReset;
        }

        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#isStartTLSSupported()
         */
        public boolean isStartTLSSupported() {
            return POP3Server.this.isStartTLSSupported();
        }
    }

    @Override
    protected ChannelPipelineFactory createPipelineFactory(ChannelGroup group) {
        return new POP3ChannelPipelineFactory(getTimeout(), connectionLimit, connPerIP, group);
    }

    private final class POP3ChannelPipelineFactory extends AbstractSSLAwareChannelPipelineFactory {

        public POP3ChannelPipelineFactory(int timeout, int maxConnections,
                int maxConnectsPerIp, ChannelGroup group) {
            super(timeout, maxConnections, maxConnectsPerIp, group);
        }

        @Override
		public ChannelPipeline getPipeline() throws Exception {
			ChannelPipeline pipeLine =  super.getPipeline();
			pipeLine.addBefore("coreHandler", "countHandler", countHandler);
			return pipeLine;
		}

		@Override
        protected SSLContext getSSLContext() {
            return POP3Server.this.getSSLContext();

        }

        @Override
        protected boolean isSSLSocket() {
            return POP3Server.this.isSSLSocket();
        }

        @Override
        protected OneToOneEncoder createEncoder() {
            return new POP3ResponseEncoder();

        }

        @Override
        protected ChannelUpstreamHandler createHandler() {
            return new POP3ChannelUpstreamHandler(handlerChain, theConfigData, getLogger(), getSSLContext(), getEnabledCipherSuites());

        }
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.socket.ServerMBean#getCurrentConnections()
     */
	public int getCurrentConnections() {
		return countHandler.getCurrentConnectionCount();
	}

    @Override
    protected String getDefaultJMXName() {
        return "pop3server";
    }

}
