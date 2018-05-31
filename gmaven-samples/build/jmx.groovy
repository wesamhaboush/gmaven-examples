#!/usr/bin/env groovy

import javax.management.MBeanServerConnection  
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL  
  
import com.sun.tools.attach.VirtualMachine  
  
def getPid(processName) {
    'ps -A'.execute()
           .text
           .split('\n')
           .find { it.contains processName.toLowerCase() }?.split()?.first() as String
}

def static MBeanServerConnection retrieveServerConnection(String pid) {
   def connectorAddressStr = "com.sun.management.jmxremote.localConnectorAddress"  
   def jmxUrl = retrieveUrlForPid(pid, connectorAddressStr)  
   def jmxConnector = JMXConnectorFactory.connect(jmxUrl)  
   return jmxConnector.getMBeanServerConnection()  
}  
  
def static JMXServiceURL retrieveUrlForPid(String pid, String connectorAddressStr)  
{  
   def vm = VirtualMachine.attach(pid)  
   def connectorAddress =  
      vm.getAgentProperties().getProperty(connectorAddressStr)  
   if (connectorAddress == null)  
   {  
      def agent = vm.getSystemProperties().getProperty("java.home") +  
          File.separator + "lib" + File.separator + "management-agent.jar"  
      vm.loadAgent(agent)  
  
      connectorAddress =  
         vm.getAgentProperties().getProperty(connectorAddressStr)  
   }  
  
   return new JMXServiceURL(connectorAddress);  
}  

def pid = getPid("tomcat")  
println "attaching to tomcat process with id: ${pid}"
def mbeans = retrieveServerConnection(pid)
def fullIndexerMBean = new GroovyMBean(mbeans, 'LaLaLa:type=Indexer,name=FullIndexer')

println fullIndexerMBean

println "re-indexing admin"
fullIndexerMBean.reindexAdmin()
println "re-indexing all projects"
fullIndexerMBean.reindexAllProjects("")

def bulkIndexServiceMBean = new GroovyMBean(mbeans, 'LaLaLa:type=ElasticSearch,name=BulkIndexService')

println "bulk index service mbean:" 
println bulkIndexServiceMBean

println "re-indexing users" 
bulkIndexServiceMBean.reIndexUsers()
println "re-indexing organizations" 
bulkIndexServiceMBean.reIndexOrganisations()
println "re-indexing projects" 
bulkIndexServiceMBean.reIndexProjects()
println "re-indexing tasks" 
bulkIndexServiceMBean.reIndexTasks()
println "re-indexing tenders" 
bulkIndexServiceMBean.reIndexTenders()
