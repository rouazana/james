/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.                  *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.transport.mailets;

import org.apache.james.test.mock.mailet.MockMail;
import org.apache.james.test.mock.mailet.MockMailContext;
import org.apache.james.test.mock.mailet.MockMailetConfig;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.Mailet;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import junit.framework.TestCase;

public class RemoveAllMailAttributesTest extends TestCase {

    private MimeMessage mockedMimeMessage;

    private Mail mockedMail;

    private Mailet mailet;

    public RemoveAllMailAttributesTest(String arg0)
            throws UnsupportedEncodingException {
        super(arg0);
    }

    private void setupMockedMail(MimeMessage m) throws ParseException {
        mockedMail = new MockMail();
        mockedMail.setMessage(m);
        mockedMail.setRecipients(Arrays.asList(new MailAddress[] {
                new MailAddress("test@james.apache.org"),
                new MailAddress("test2@james.apache.org") }));
        mockedMail.setAttribute("org.apache.james.test.junit", "true");

    }

    private void setupMailet() throws MessagingException {
        mailet = new RemoveAllMailAttributes();
        MockMailetConfig mci = new MockMailetConfig("Test",
                new MockMailContext());
        mailet.init(mci);
    }

    // test if ToProcessor works
    public void testRemoveAllMailAttributes() throws MessagingException {
        setupMockedMail(mockedMimeMessage);
        setupMailet();

        // check if the mail has a attribute
        assertTrue(mockedMail.getAttributeNames().hasNext());

        mailet.service(mockedMail);

        // check if all was removed
        assertFalse(mockedMail.getAttributeNames().hasNext());
    }

}
