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
package org.apache.james.jmap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

public class BypassOnPostFilterTest {

    private HttpServletRequest mockedRequest;
    private Filter nestedFilter;
    private BypassOnPostFilter tested;
    private FilterChain filterChain;
    
    @Before
    public void setup() throws Exception {
        mockedRequest = mock(HttpServletRequest.class);
        nestedFilter = mock(Filter.class);
        tested = new BypassOnPostFilter(nestedFilter);
        filterChain = mock(FilterChain.class);
    }
    
    @Test
    public void filterShouldCallNestedFilterOnGet() throws Exception {
        when(mockedRequest.getMethod())
            .thenReturn("GET");
        
        tested.doFilter(mockedRequest, null, filterChain);
        
        verify(nestedFilter).doFilter(mockedRequest, null, filterChain);
    }
    
    @Test
    public void filterShouldNotCallDirectlyChainOnGet() throws Exception {
        when(mockedRequest.getMethod())
            .thenReturn("GET");
        
        tested.doFilter(mockedRequest, null, filterChain);
        
        verify(filterChain, never()).doFilter(mockedRequest, null);
    }
    
    @Test
    public void filterShouldNotCallNestedFilterOnPost() throws Exception {
        when(mockedRequest.getMethod())
            .thenReturn("POST");
        
        tested.doFilter(mockedRequest, null, filterChain);
        
        verify(nestedFilter, never()).doFilter(mockedRequest, null, filterChain);
    }
    
    @Test
    public void filterShouldCallChainOnPost() throws Exception {
        when(mockedRequest.getMethod())
            .thenReturn("POST");
        
        tested.doFilter(mockedRequest, null, filterChain);
        
        verify(filterChain).doFilter(mockedRequest, null);
    }
    
}
