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


package org.apache.james.smtpserver.protocol.core.fastfail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.internet.ParseException;

import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.smtpserver.protocol.BaseFakeDNSService;
import org.apache.james.smtpserver.protocol.BaseFakeSMTPSession;
import org.apache.james.smtpserver.protocol.DNSService;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.mailet.MailAddress;

public class DNSRBLHandlerTest extends TestCase {

    private DNSService mockedDnsServer;

    private SMTPSession mockedSMTPSession;

    private String remoteIp = "127.0.0.2";

    private boolean relaying = false;   
    
    public static final String RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME = "org.apache.james.smtpserver.rbl.blocklisted";
    
    public static final String RBL_DETAIL_MAIL_ATTRIBUTE_NAME = "org.apache.james.smtpserver.rbl.detail";

    protected void setUp() throws Exception {
        super.setUp();
        setupMockedDnsServer();
        setRelayingAllowed(false);
    }

    /**
     * Set the remoteIp
     * 
     * @param remoteIp The remoteIP to set
     */
    private void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    /**
     * Set relayingAllowed
     * 
     * @param relaying true or false
     */
    private void setRelayingAllowed(boolean relaying) {
        this.relaying = relaying;
    }

    /**
     * Setup the mocked dnsserver
     *
     */
    private void setupMockedDnsServer() {
        mockedDnsServer  = new BaseFakeDNSService() {

            public Collection<String> findTXTRecords(String hostname) {
                List<String> res = new ArrayList<String>();
                if (hostname == null) {
                    return res;
                }
                ;
                if ("2.0.0.127.bl.spamcop.net.".equals(hostname)) {
                    res.add("Blocked - see http://www.spamcop.net/bl.shtml?127.0.0.2");
                }
                return res;
            }

            public InetAddress getByName(String host)
                    throws UnknownHostException {
                if ("2.0.0.127.bl.spamcop.net.".equals(host)) {
                    return InetAddress.getByName("127.0.0.1");
                } else if ("3.0.0.127.bl.spamcop.net.".equals(host)) {
                    return InetAddress.getByName("127.0.0.1");
                } else if ("1.0.168.192.bl.spamcop.net.".equals(host)) {
                    throw new UnknownHostException(host);
                }
                throw new UnsupportedOperationException("getByName("+host+") not implemented in DNSRBLHandlerTest mock");
            }
        };
        
    }

    /**
     * Setup mocked smtpsession
     */
    private void setupMockedSMTPSession(final MailAddress rcpt) {
        mockedSMTPSession = new BaseFakeSMTPSession() {
            HashMap<String,Object> state = new HashMap<String,Object>();
            HashMap<String,Object> connectionState = new HashMap<String,Object>();
            
            public String getRemoteIPAddress() {
                return remoteIp;
            }

            public Map<String,Object> getState() {
                return state;
            }

            public boolean isRelayingAllowed() {
                return relaying;
            }

            public boolean isAuthSupported() {
                return false;
            }

            public int getRcptCount() {
                return 0;
            }

            public Map<String,Object> getConnectionState() {       
                return connectionState;
            }

        };
    }

    // ip is blacklisted and has txt details
    public void testBlackListedTextPresent() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();
       
        setupMockedSMTPSession(new MailAddress("any@domain"));
        rbl.setDNSService(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertEquals("Details","Blocked - see http://www.spamcop.net/bl.shtml?127.0.0.2",
               mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNotNull("Blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip is blacklisted and has txt details but we don'T want to retrieve the txt record
    public void testGetNoDetail() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();
        setupMockedSMTPSession(new MailAddress("any@domain"));
        rbl.setDNSService(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(false);
        rbl.onConnect(mockedSMTPSession);
        assertNull("No details",mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNotNull("Blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip is allowed to relay
    public void testRelayAllowed() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();
        setRelayingAllowed(true);
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSService(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull("No details",mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNull("Not blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip not on blacklist
    public void testNotBlackListed() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        setRemoteIp("192.168.0.1");
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSService(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull("No details",mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNull("Not blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip on blacklist without txt details
    public void testBlackListedNoTxt() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        setRemoteIp("127.0.0.3");
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSService(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull(mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNotNull("Blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip on whitelist
    public void testWhiteListed() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        setRemoteIp("127.0.0.2");
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSService(mockedDnsServer);

        rbl.setWhitelist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull(mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNull("Not blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }
    
    public void testInvalidConfig() {
        boolean exception = false;
        DNSRBLHandler rbl = new DNSRBLHandler();
        try {
            rbl.configure(new Configuration() {
				
				public Configuration subset(String prefix) {
					return null;
				}
				
				public void setProperty(String key, Object value) {
					
				}
				
				public boolean isEmpty() {
					return true;
				}
				
				public String[] getStringArray(String key) {
					return null;
				}
				
				public String getString(String key, String defaultValue) {
					return null;
				}
				
				public String getString(String key) {
					return null;
				}
				
				public Short getShort(String key, Short defaultValue) {
					return null;
				}
				
				public short getShort(String key, short defaultValue) {
					return 0;
				}
				
				public short getShort(String key) {
					return 0;
				}
				
				public Object getProperty(String key) {
					return null;
				}
				
				public Properties getProperties(String key) {
					return null;
				}
				
				public Long getLong(String key, Long defaultValue) {
					return null;
				}
				
				public long getLong(String key, long defaultValue) {
					return 0;
				}
				
				public long getLong(String key) {
					return 0;
				}
				
				@SuppressWarnings("unchecked")
                public List getList(String key, List defaultValue) {
					return null;
				}
				
				@SuppressWarnings("unchecked")
                public List getList(String key) {
					return null;
				}
				
				@SuppressWarnings("unchecked")
                public Iterator getKeys(String prefix) {
					return null;
				}
				
				@SuppressWarnings("unchecked")
                public Iterator getKeys() {
					return null;
				}
				
				public Integer getInteger(String key, Integer defaultValue) {
					return null;
				}
				
				public int getInt(String key, int defaultValue) {
					return 0;
				}
				
				public int getInt(String key) {
					return 0;
				}
				
				public Float getFloat(String key, Float defaultValue) {
					return null;
				}
				
				public float getFloat(String key, float defaultValue) {
					return 0;
				}
				
				public float getFloat(String key) {
					return 0;
				}
				
				public Double getDouble(String key, Double defaultValue) {
					return null;
				}
				
				public double getDouble(String key, double defaultValue) {
					return 0;
				}
				
				public double getDouble(String key) {
					return 0;
				}
				
				public Byte getByte(String key, Byte defaultValue) {
					return null;
				}
				
				public byte getByte(String key, byte defaultValue) {
					return 0;
				}
				
				public byte getByte(String key) {
					return 0;
				}
				
				public Boolean getBoolean(String key, Boolean defaultValue) {
					return null;
				}
				
				public boolean getBoolean(String key, boolean defaultValue) {
					return false;
				}
				
				public boolean getBoolean(String key) {
					return false;
				}
				
				public BigInteger getBigInteger(String key, BigInteger defaultValue) {
					return null;
				}
				
				public BigInteger getBigInteger(String key) {
					return null;
				}
				
				public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
					return null;
				}
				
				public BigDecimal getBigDecimal(String key) {
					return null;
				}
				
				public boolean containsKey(String key) {
					return false;
				}
				
				public void clearProperty(String key) {
					
				}
				
				public void clear() {
					
				}
				
				public void addProperty(String key, Object value) {
					
				}
			});
        } catch (ConfigurationException e) {
            exception = true;
        }
        
        assertTrue("Invalid config",exception);
    }

}
