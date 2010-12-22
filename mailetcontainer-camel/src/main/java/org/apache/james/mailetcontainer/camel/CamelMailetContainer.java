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
package org.apache.james.mailetcontainer.camel;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;
import org.apache.commons.logging.Log;
import org.apache.james.mailetcontainer.lib.AbstractMailetContainer;
import org.apache.james.mailetcontainer.lib.MailetConfigImpl;
import org.apache.james.mailetcontainer.lib.MatcherMailetPair;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.Matcher;


/**
 * {@link AbstractMailetContainer} implementation which use Camel DSL for the {@link Matcher} / {@link Mailet} routing
 *
 */
public class CamelMailetContainer extends AbstractMailetContainer implements CamelContextAware{

    private CamelContext context;


    private ProducerTemplate producerTemplate;
    
    private final UseLatestAggregationStrategy aggr = new UseLatestAggregationStrategy();


    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.api.MailProcessor#service(org.apache.mailet.Mail)
     */
    public void service(Mail mail) throws MessagingException {
        try {
            producerTemplate.sendBody(getEndpoint(), mail);
            
         } catch (CamelExecutionException ex) {
             throw new MessagingException("Unable to process mail " + mail.getName(),ex);
         }        
     }


    /*
     * (non-Javadoc)
     * @see org.apache.camel.CamelContextAware#getCamelContext()
     */
    public CamelContext getCamelContext() {
        return context;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.camel.CamelContextAware#setCamelContext(org.apache.camel.CamelContext)
     */
    public void setCamelContext(CamelContext context) {
        this.context = context;
    }


    /**
     * Return the endpoint for the processorname. 
     * 
     * This will return a "direct" endpoint. 
     * 
     * @param processorName
     * @return endPoint
     */
    protected String getEndpoint() {
        return "direct:processor." + getName();
    }
    
    
    @PostConstruct
    public void init() throws Exception {
        producerTemplate = context.createProducerTemplate();

        super.init();
    }



    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.lib.AbstractMailetContainer#setupRouting(java.util.List)
     */
    protected void setupRouting(List<MatcherMailetPair> pairs) throws MessagingException {
        try {
            context.addRoutes(new MailetContainerRouteBuilder(pairs));
        } catch (Exception e) {
            throw new MessagingException("Unable to setup routing for MailetMatcherPairs", e);
        }
    }


    /**
     * {@link RouteBuilder} which construct the Matcher and Mailet routing use Camel DSL
     *
     */
    private final class MailetContainerRouteBuilder extends RouteBuilder {

        private List<MatcherMailetPair> pairs;

        public MailetContainerRouteBuilder(List<MatcherMailetPair> pairs) {
            this.pairs = pairs;
        }
        
        @Override
        public void configure() throws Exception {
            Processor disposeProcessor = new DisposeProcessor();
            Processor removePropsProcessor = new RemovePropertiesProcessor();
            Processor completeProcessor = new CompleteProcessor();
            String name = getName();
            Log logger = getLogger();

            RouteDefinition processorDef = from(getEndpoint()).routeId(name).inOnly()
            // store the logger in properties
            .setProperty(MatcherSplitter.LOGGER_PROPERTY, constant(getLogger()));
            
            for (int i = 0; i < pairs.size(); i++) {
                MatcherMailetPair pair = pairs.get(i);
                Matcher matcher = pair.getMatcher();
                Mailet mailet = pair.getMailet();
                
                String onMatchException = null;
                MailetConfig mailetConfig = mailet.getMailetConfig();
                
                if (mailetConfig instanceof MailetConfigImpl) {
                    onMatchException = ((MailetConfigImpl) mailetConfig).getInitAttribute("onMatchException");
                }
                
                MailetProcessor mailetProccessor = new MailetProcessor(mailet, logger, CamelMailetContainer.this);
                // Store the matcher to use for splitter in properties
                processorDef
                    .setProperty(MatcherSplitter.MATCHER_PROPERTY, constant(matcher)).setProperty(MatcherSplitter.ON_MATCH_EXCEPTION_PROPERTY, constant(onMatchException)).setProperty(MatcherSplitter.MAILETCONTAINER_PROPERTY, constant(CamelMailetContainer.this))
                   
                    // do splitting of the mail based on the stored matcher
                    .split().method(MatcherSplitter.class).aggregationStrategy(aggr).parallelProcessing()
                       
                    .choice().when(new MatcherMatch()).process(mailetProccessor).end()
                    
                    .choice().when(new MailStateEquals(Mail.GHOST)).process(disposeProcessor).stop().otherwise().process(removePropsProcessor).end()

                    .choice().when(new MailStateNotEquals(name)).process(completeProcessor).stop().end();
            }
                
          

            
            
            Processor terminatingMailetProcessor = new MailetProcessor(new TerminatingMailet(), getLogger(), CamelMailetContainer.this);

            
            processorDef
                // start choice
                .choice()
             
                // when the mail state did not change till yet ( the end of the route) we need to call the TerminatingMailet to
                // make sure we don't fall into a endless loop
                .when(new MailStateEquals(name)).process(terminatingMailetProcessor).stop()
                
                   
                // dispose when needed
                .when(new MailStateEquals(Mail.GHOST)).process(disposeProcessor).stop()
                
                 // this container is complete
                .otherwise().process(completeProcessor).stop();
            
                       
        }

        

        private final class RemovePropertiesProcessor implements Processor {

            public void process(Exchange exchange) throws Exception {
                exchange.removeProperty(MatcherSplitter.ON_MATCH_EXCEPTION_PROPERTY);
                exchange.removeProperty(MatcherSplitter.MATCHER_PROPERTY);
            }
        }
        


        private final class CompleteProcessor implements Processor {
            
            public void process(Exchange ex) throws Exception {
                getLogger().debug("End of mailetcontainer" + getName() + " reached");
                ex.setProperty(Exchange.ROUTE_STOP, true);
            }
        }
        
    }



}
