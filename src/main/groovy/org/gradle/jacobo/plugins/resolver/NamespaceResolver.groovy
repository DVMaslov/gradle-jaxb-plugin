package org.gradle.jacobo.plugins.resolver

import org.gradle.api.logging.Logging
import org.gradle.api.logging.Logger

import org.gradle.jacobo.schema.BaseSchemaDocument
import org.gradle.jacobo.plugins.structures.NamespaceData

class NamespaceResolver {
  static final Logger log = Logging.getLogger(NamespaceResolver.class)
  
  public List<NamespaceData> resolve(def documents) {
    log.info("resolving(grouping) '{}' documents by their unique namespaces",
	     documents.size)
    def namespaces = []
    documents.each { document ->
      def namespace = namespaces.find { it.namespace == document.xsdNamespace }
      if (namespace) {
	namespace.documents << document
	return true
      }
      namespace = new NamespaceData(document.xsdNamespace, [document])
      namespaces << namespace
    }
    return namespaces
  }
}