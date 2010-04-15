package org.jboss.seam.drools;

import java.util.Iterator;
import java.util.Properties;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.event.KnowledgeRuntimeEventManager;
import org.drools.event.process.ProcessEventListener;
import org.drools.event.rule.AgendaEventListener;
import org.drools.event.rule.WorkingMemoryEventListener;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.StatelessKnowledgeSession;
import org.jboss.seam.drools.bootstrap.DroolsExtension;
import org.jboss.seam.drools.config.DroolsConfiguration;
import org.jboss.seam.drools.qualifiers.Scanned;
import org.jboss.seam.drools.utils.ConfigUtils;
import org.jboss.weld.extensions.resources.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Tihomir Surdilovic
 */
public class KnowledgeSessionProducer
{
   private static final Logger log = LoggerFactory.getLogger(KnowledgeSessionProducer.class);

   @Inject
   BeanManager manager;
   
   @Inject
   ResourceProvider resourceProvider;
   
   @Inject
   DroolsExtension droolsExtension;

   @Produces
   public StatefulKnowledgeSession produceStatefulSession(KnowledgeBase kbase,DroolsConfiguration ksessionConfig) throws Exception
   {
      StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession(getConfig(ksessionConfig.getKnowledgeSessionConfigPath()), null);
      addEventListeners(ksession);
      addWorkItemHandlers(ksession);

      return ksession;
   }

   @Produces
   @Scanned
   public StatefulKnowledgeSession produceScannedStatefulSession(@Scanned KnowledgeBase kbase, DroolsConfiguration ksessionConfig) throws Exception
   {
      StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession(getConfig(ksessionConfig.getKnowledgeSessionConfigPath()), null);
      addEventListeners(ksession);
      addWorkItemHandlers(ksession);

      return ksession;
   }

   @Produces
   @Scanned
   public StatelessKnowledgeSession produceScannedStatelessSession(@Scanned KnowledgeBase kbase, DroolsConfiguration ksessionConfig) throws Exception
   {
      StatelessKnowledgeSession ksession = kbase.newStatelessKnowledgeSession(getConfig(ksessionConfig.getKnowledgeSessionConfigPath()));
      addEventListeners(ksession);

      return ksession;
   }

   @Produces
   public StatelessKnowledgeSession produceStatelessSession(KnowledgeBase kbase, DroolsConfiguration ksessionConfig) throws Exception
   {
      StatelessKnowledgeSession ksession = kbase.newStatelessKnowledgeSession(getConfig(ksessionConfig.getKnowledgeSessionConfigPath()));
      addEventListeners(ksession);

      return ksession;
   }

   public void disposeStatefulSession(@Disposes StatefulKnowledgeSession session)
   {
      session.dispose();
   }
   
   
   private KnowledgeSessionConfiguration getConfig(String knowledgeSessionConfigPath) throws Exception
   {
      KnowledgeSessionConfiguration droolsKsessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
      if (knowledgeSessionConfigPath != null && knowledgeSessionConfigPath.endsWith(".properties"))
      {
         Properties ksessionProp = ConfigUtils.loadProperties(resourceProvider, knowledgeSessionConfigPath);
         droolsKsessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration(ksessionProp);
         log.debug("KnowledgeSessionConfiguration loaded: " + knowledgeSessionConfigPath);
      }
      else
      {
         log.warn("Invalid config type: " + knowledgeSessionConfigPath);
      }
      return droolsKsessionConfig;
   }

   private void addEventListeners(KnowledgeRuntimeEventManager ksession)
   {
      Iterator<Object> iter = droolsExtension.getKsessionEventListenerSet().iterator();
      while (iter.hasNext())
      {
         Object eventListenerInstance = iter.next();

         if (eventListenerInstance instanceof WorkingMemoryEventListener)
         {
            ksession.addEventListener((WorkingMemoryEventListener) eventListenerInstance);
         }
         else if (eventListenerInstance instanceof AgendaEventListener)
         {
            ksession.addEventListener((AgendaEventListener) eventListenerInstance);
         }
         else if (eventListenerInstance instanceof ProcessEventListener)
         {
            ksession.addEventListener((ProcessEventListener) eventListenerInstance);
         }
         else
         {
            log.debug("Invalid Event Listener: " + eventListenerInstance);
         }
      }
   }

   private void addWorkItemHandlers(StatefulKnowledgeSession ksession)
   {
      Iterator<String> iter = droolsExtension.getWorkItemHandlers().keySet().iterator();
      while (iter.hasNext())
      {
         String name = iter.next();
         ksession.getWorkItemManager().registerWorkItemHandler(name, droolsExtension.getWorkItemHandlers().get(name));
      }
   }
}
