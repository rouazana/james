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

package org.apache.james.smtpserver.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.ExtensibleHandler;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPRetCode;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.hook.HookResult;
import org.apache.james.smtpserver.hook.HookResultHook;
import org.apache.james.smtpserver.hook.HookReturnCode;

/**
 * Abstract class which Handle hooks.
 * 
 */
public abstract class AbstractHookableCmdHandler<Hook> implements CommandHandler, ExtensibleHandler {


    private List<Hook> hooks;
    private List rHooks;

    /**
     * Handle command processing
     * 
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(org.apache.james.smtpserver.SMTPSession,
     *      java.lang.String, java.lang.String)
     */
    public SMTPResponse onCommand(SMTPSession session, String command,
            String parameters) {
        SMTPResponse response = doFilterChecks(session, command, parameters);

        if (response == null) {

            response = processHooks(session, command, parameters);
            if (response == null) {
                return doCoreCmd(session, command, parameters);
            } else {
                return response;
            }
        } else {
            return response;
        }

    }

    /**
     * Process all hooks for the given command
     * 
     * @param session
     *            the SMTPSession object
     * @param command
     *            the command
     * @param parameters
     *            the paramaters
     * @return SMTPResponse
     */
    private SMTPResponse processHooks(SMTPSession session, String command,
            String parameters) {
        List<Hook> hooks = getHooks();
        if (hooks != null) {
            int count = hooks.size();
            for (int i = 0; i < count; i++) {
                Hook rawHook = hooks.get(i);
                session.getLogger().debug("executing hook " + rawHook.getClass().getName());
                HookResult hRes = callHook(rawHook, session, parameters);
                if (rHooks != null) {
                    for (int i2 = 0; i2 < rHooks.size(); i2++) {
                        Object rHook = rHooks.get(i2);
                        session.getLogger().debug("executing hook " + rHook);
                        hRes = ((HookResultHook) rHook).onHookResult(session, hRes, rawHook);
                    }
                }
                SMTPResponse res = calcDefaultSMTPResponse(hRes);
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }

    /**
     * Must be implemented by hookable cmd handlers to make the effective call to an hook.
     * 
     * @param rawHook the hook
     * @param session the session
     * @param parameters the parameters
     * @return the HookResult, will be calculated using HookResultToSMTPResponse.
     */
    protected abstract HookResult callHook(Hook rawHook, SMTPSession session, String parameters);

    /**
     * Convert the HookResult to SMTPResponse using default values. Should be override for using own values
     * 
     * @param result HookResult
     * @return SMTPResponse
     */
    public static SMTPResponse calcDefaultSMTPResponse(HookResult result) {
        if (result != null) {
            int rCode = result.getResult();
            String smtpRetCode = result.getSmtpRetCode();
            String smtpDesc = result.getSmtpDescription();
    
            if (rCode == HookReturnCode.DENY) {
                if (smtpRetCode == null)
                    smtpRetCode = SMTPRetCode.TRANSACTION_FAILED;
                if (smtpDesc == null)
                    smtpDesc = "Email rejected";
    
                return new SMTPResponse(smtpRetCode, smtpDesc);
            } else if (rCode == HookReturnCode.DENYSOFT) {
                if (smtpRetCode == null)
                    smtpRetCode = SMTPRetCode.LOCAL_ERROR;
                if (smtpDesc == null)
                    smtpDesc = "Temporary problem. Please try again later";
    
                return new SMTPResponse(smtpRetCode, smtpDesc);
            } else if (rCode == HookReturnCode.OK) {
                if (smtpRetCode == null)
                    smtpRetCode = SMTPRetCode.MAIL_OK;
                if (smtpDesc == null)
                    smtpDesc = "Command accepted";
    
                return new SMTPResponse(smtpRetCode, smtpDesc);
            } else {
                // Return null as default
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Execute Syntax checks and return a SMTPResponse if a syntax error was
     * detected, otherwise null.
     * 
     * @param session
     * @param command
     * @param parameters
     * @return
     */
    protected abstract SMTPResponse doFilterChecks(SMTPSession session,
            String command, String parameters);

    /**
     * Execute the core commandHandling.
     * 
     * @param session
     * @param command
     * @param parameters
     * @return
     */
    protected abstract SMTPResponse doCoreCmd(SMTPSession session,
            String command, String parameters);
    

    /**
     * @see org.apache.james.smtpserver.ExtensibleHandler#getMarkerInterfaces()
     */
    public List<Class<?>> getMarkerInterfaces() {
        List<Class<?>> classes = new ArrayList<Class<?>>(2);
        classes.add(getHookInterface());
        classes.add(HookResultHook.class);
        return classes;
    }

    /**
     * Return the interface which hooks need to implement to hook in
     * 
     * @return interface
     */
    protected abstract Class<Hook> getHookInterface();

    /**
     * @see org.apache.james.smtpserver.ExtensibleHandler#wireExtensions(java.lang.Class,
     *      java.util.List)
     */
    public void wireExtensions(Class interfaceName, List extension) {
        if (getHookInterface().equals(interfaceName)) {
            this.hooks = extension;
        } else if (HookResultHook.class.equals(interfaceName)) {
            this.rHooks = extension;
        }

    }

    /**
     * Return a list which holds all hooks for the cmdHandler
     * 
     * @return
     */
    protected List<Hook> getHooks() {
        return hooks;
    }

}