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



package org.apache.james.vut.api;

import java.util.Collection;
import java.util.Map;


/**
 * Expose virtualusertable management functionality through JMX.
 * 
 */
public interface VirtualUserTableManagementMBean {
    
    /**
     * Add regex mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     */
    void addRegexMapping(String user, String domain, String regex) throws Exception;
    
    /**
     * Remove regex mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     */
    void removeRegexMapping(String user,String domain, String regex) throws Exception;
    
    /***
     * Add address mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param address the address.
     */
    void addAddressMapping(String user, String domain, String address) throws Exception;
    
    /**
     * Remove address mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param address
     */
    void removeAddressMapping(String user,String domain, String address) throws Exception;
    
    /**
     * Add error mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param error
     */
    void addErrorMapping(String user, String domain, String error) throws Exception;

    /**
     * Remove error mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param error
     * @return true if successfully
     */
    void removeErrorMapping(String user,String domain, String error) throws Exception;
    
    /**
     * Return the explicit mapping stored for the given user and domain. Return null
     * if no mapping was found
     *
     * @param user the username
     * @param domain the domain
     * @return the collection which holds the mappings. 
     */
    Collection<String> getUserDomainMappings(String user, String domain) throws Exception;
    
    /**
    * Try to identify the right method based on the prefix of the mapping and add it.
    * 
    * @param user the username. Null if no username should be used
    * @param domain the domain. Null if no domain should be used
    * @param mapping the mapping.
    */
    void addMapping(String user, String domain, String mapping) throws Exception;
    
    /**
     * Try to identify the right method based on the prefix of the mapping and remove it.
     *
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param mapping the mapping.
     */
    void removeMapping(String user, String domain, String mapping) throws Exception;
    

    /**
     * Return a Map which holds all mappings. The key is the user@domain and the value is a Collection 
     * which holds all mappings
     * 
     * @return Map which holds all mappings
     */
    Map<String,Collection<String>> getAllMappings() throws Exception;
}