/*
 * JBoss, Home of Professional Open Source
 * Copyright ${year}, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */ 
package org.jboss.seam.drools.test.ksession;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotSame;

import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.drools.runtime.StatefulKnowledgeSession;
import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.drools.KnowledgeBaseProducer;
import org.jboss.seam.drools.qualifiers.config.DefaultConfig;
import org.jboss.seam.drools.qualifiers.config.MVELDialectConfig;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Archives;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.weld.extensions.resources.ResourceProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class KSessionTest
{
   @Deployment
   public static JavaArchive createTestArchive()
   {
      String pkgPath = KSessionTest.class.getPackage().getName().replaceAll("\\.", "/");
      JavaArchive archive = Archives.create("test.jar", JavaArchive.class)
      .addPackages(true, new KSessionTestFiler(), KnowledgeBaseProducer.class.getPackage())
      .addPackages(true, ResourceProvider.class.getPackage())
      .addClass(KSessionTestRules.class)
      .addResource(pkgPath + "/ksessiontest.drl", ArchivePaths.create("ksessiontest.drl"))
      .addResource(pkgPath + "/kbuilderconfig.properties", ArchivePaths.create("kbuilderconfig.properties"))
      .addResource(pkgPath + "/kbaseconfig.properties", ArchivePaths.create("kbaseconfig.properties"))
      .addManifestResource(pkgPath + "/KSessionTest-beans.xml", ArchivePaths.create("beans.xml"));
      //System.out.println(archive.toString(Formatters.VERBOSE));
      return archive;
   }
   
   @Inject @Default @DefaultConfig StatefulKnowledgeSession ksession;
   @Inject @Default @MVELDialectConfig StatefulKnowledgeSession mvelksession;
   @Inject @Default @MVELDialectConfig StatefulKnowledgeSession mvelksession2;
   
   @Test
   public void testKSession()
   {
      assertNotNull(ksession);
      assertTrue(ksession.getId() >= 0);
      
      assertNotNull(mvelksession);
      assertTrue(mvelksession.getId() >= 0);
      
      assertNotSame(ksession, mvelksession);
      assertSame(mvelksession, mvelksession2);
   }
}
