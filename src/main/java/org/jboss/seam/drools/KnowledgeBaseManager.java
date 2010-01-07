package org.jboss.seam.drools;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.security.auth.login.Configuration;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderErrors;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.event.knowledgebase.KnowledgeBaseEventListener;
import org.drools.io.ResourceFactory;
import org.drools.template.ObjectDataCompiler;
import org.jboss.seam.drools.events.KnowledgeBuilderErrorsEvent;
import org.jboss.seam.drools.events.RuleResourceAddedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager component for a Drools KnowledgeBase.
 * 
 * @author Tihomir Surdilovic
 */
public class KnowledgeBaseManager
{
   private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseManager.class);
   private static final String RESOURCE_TYPE_URL = "url";
   private static final String RESOURCE_TYPE_FILE = "file";
   private static final String RESOURCE_TYPE_CLASSPATH = "classpath";
   
   private KnowledgeBaseManagerConfig kbaseManagerConfig;
   private KnowledgeBase kbase;

   @Inject
   BeanManager manager;
   
   @Inject 
   public KnowledgeBaseManager(KnowledgeBaseManagerConfig kbaseManagerConfig) {
      this.kbaseManagerConfig = kbaseManagerConfig;
   }

   @Produces
   @ApplicationScoped
   public KnowledgeBase getKBase()
   {
      return kbase;
   }

   public void disposeKBase(@Disposes KnowledgeBase kbase)
   {
      kbase = null;
   }

   @PostConstruct
   private void createKBase() throws Exception
   {
      KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(kbaseManagerConfig.getKnowledgeBuilderConfiguration());
      
      for (String nextResource : kbaseManagerConfig.getRuleResources())
      {
         addResource(kbuilder, nextResource);
      }
      
      KnowledgeBuilderErrors kbuildererrors = kbuilder.getErrors();
      if (kbuildererrors.size() > 0)
      {
         for (KnowledgeBuilderError kbuildererror : kbuildererrors)
         {
            log.error(kbuildererror.getMessage());
         }
         manager.fireEvent(new KnowledgeBuilderErrorsEvent(kbuildererrors));
      }

      kbase = KnowledgeBaseFactory.newKnowledgeBase(kbaseManagerConfig.getKnowledgeBaseConfiguration());
      kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());

      if (kbaseManagerConfig.getEventListeners() != null)
      {
         for (String eventListener : kbaseManagerConfig.getEventListeners())
         {
            addEventListener(kbase, eventListener);
         }
      }
   } 
   
   private void addEventListener(org.drools.KnowledgeBase kbase, String eventListener) {
      try {
         @SuppressWarnings("unchecked")
         Class eventListenerClass = Class.forName(eventListener);
         Object eventListenerObject = eventListenerClass.newInstance();
        
         if(eventListenerObject instanceof KnowledgeBaseEventListener) {
            kbase.addEventListener((KnowledgeBaseEventListener) eventListenerObject);
         } else {
            log.debug("Event Listener " + eventListener + " is not of type KnowledgeBaseEventListener");
         }
      } catch(Exception e) {
         log.error("Error adding event listener " + e.getMessage());
      }
   }
   
   protected void addResource(KnowledgeBuilder kbuilder, String resource) throws Exception
   {
      if(kbaseManagerConfig.isValidResource(resource)) {
         ResourceType resourceType = ResourceType.getResourceType(kbaseManagerConfig.getResourceType(resource));
         if(kbaseManagerConfig.isRuleTemplate(resource)) {
            @SuppressWarnings("unchecked")
            Bean<TemplateDataProvider> templateDataProviderBean = (Bean<TemplateDataProvider>) manager.getBeans(kbaseManagerConfig.getTemplateData(resource)).iterator().next();

            TemplateDataProvider templateDataProvider = (TemplateDataProvider) manager.getReference(templateDataProviderBean, Configuration.class, manager.createCreationalContext(templateDataProviderBean));

            InputStream templateStream = this.getClass().getClassLoader().getResourceAsStream(kbaseManagerConfig.getRuleResource(resource));
            if (templateStream == null)
            {
               throw new IllegalStateException("Could not locate rule resource: " + kbaseManagerConfig.getRuleResource(resource));
            }

            ObjectDataCompiler converter = new ObjectDataCompiler();
            String drl = converter.compile(templateDataProvider.getTemplateData(), templateStream);
            templateStream.close();
            log.debug("Generated following DRL from template: " + drl);
            Reader rdr = new StringReader(drl);

            kbuilder.add(ResourceFactory.newReaderResource(rdr), resourceType);
         } else {
            if (kbaseManagerConfig.getResourcePath(resource).equals(RESOURCE_TYPE_URL))
            {
               kbuilder.add(ResourceFactory.newUrlResource(kbaseManagerConfig.getRuleResource(resource)), resourceType);
               manager.fireEvent(new RuleResourceAddedEvent(kbaseManagerConfig.getRuleResource(resource)));
            }
            else if (kbaseManagerConfig.getResourcePath(resource).equals(RESOURCE_TYPE_FILE))
            {
               kbuilder.add(ResourceFactory.newFileResource(kbaseManagerConfig.getRuleResource(resource)), resourceType);
               manager.fireEvent(new RuleResourceAddedEvent(kbaseManagerConfig.getRuleResource(resource)));
            }
            else if (kbaseManagerConfig.getResourcePath(resource).equals(RESOURCE_TYPE_CLASSPATH))
            {
               kbuilder.add(ResourceFactory.newClassPathResource(kbaseManagerConfig.getRuleResource(resource)), resourceType);
               manager.fireEvent(new RuleResourceAddedEvent(kbaseManagerConfig.getRuleResource(resource)));
            }
            else
            {
               log.error("Invalid resource path: " + kbaseManagerConfig.getResourcePath(resource));
            }
         }
      } else {
         log.error("Invalid resource definition: " + resource);         
      }
   }
}