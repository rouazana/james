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

package org.apache.james.jmap.api.access;

import java.util.Objects;
import java.util.UUID;

public class AccessToken {

    public static AccessToken fromString(String tokenString) {
        return new AccessToken(UUID.fromString(tokenString));
    }

    private final UUID token;

    private AccessToken(UUID token) {
        this.token = token;
    }
    
    public static AccessToken random() {
        return new AccessToken(UUID.randomUUID());
    }

    public String serialize() {
        return token.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o != null
            && o instanceof AccessToken
            && Objects.equals(this.token, ((AccessToken)o).token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }
}
