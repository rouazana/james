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
package org.apache.james.modules.mailbox;

import com.google.inject.name.Names;
import org.apache.james.adapter.mailbox.store.UserRepositoryAuthenticator;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPathLocker;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.mailbox.cassandra.*;
import org.apache.james.mailbox.cassandra.mail.CassandraModSeqProvider;
import org.apache.james.mailbox.cassandra.mail.CassandraUidProvider;
import org.apache.james.mailbox.store.Authenticator;
import org.apache.james.mailbox.store.NoMailboxPathLocker;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class CassandraMailboxModule extends AbstractModule {

    public static final String MAILBOXMANAGER_NAME = "mailboxmanager";

    @Override
    protected void configure() {
        bind(MailboxManager.class).annotatedWith(Names.named(MAILBOXMANAGER_NAME)).to(CassandraMailboxManager.class);

        bind(SubscriptionManager.class).to(CassandraSubscriptionManager.class);
        bind(MessageMapperFactory.class).to(CassandraMailboxSessionMapperFactory.class);

        bind(MailboxPathLocker.class).to(NoMailboxPathLocker.class);
        bind(Authenticator.class).to(UserRepositoryAuthenticator.class);

        bind(new TypeLiteral<ModSeqProvider<CassandraId>>(){}).to(new TypeLiteral<CassandraModSeqProvider>(){});
        bind(new TypeLiteral<UidProvider<CassandraId>>(){}).to(new TypeLiteral<CassandraUidProvider>(){});
    }
}