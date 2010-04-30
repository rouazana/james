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

package org.apache.james.jcr;

import java.io.File;

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.userrepository.MockUsersRepositoryTest;
import org.xml.sax.InputSource;

public class JcrUserRepositoryTest extends MockUsersRepositoryTest {

    private static final String JACKRABBIT_HOME = "target/jackrabbit";
    private RepositoryImpl repository;
        
    protected UsersRepository getUsersRepository() throws Exception {
        JCRUsersRepository repos = new JCRUsersRepository(repository);
        repos.setLog(new SimpleLog("MockLog"));
        return repos;
    }

    protected void setUp() throws Exception {
        File home = new File(JACKRABBIT_HOME);
        if (home.exists()) {
            delete(home);
        }
        RepositoryConfig config = RepositoryConfig.create(new InputSource(this.getClass().getClassLoader().getResourceAsStream("test-repository.xml")), JACKRABBIT_HOME);
        repository = RepositoryImpl.create(config);
        super.setUp();
    }

    private void delete(File file) {
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            for (int i = 0; i < contents.length; i++) {
                delete(contents[i]);
            }
        } 
        file.delete();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        repository.shutdown();
    }
}