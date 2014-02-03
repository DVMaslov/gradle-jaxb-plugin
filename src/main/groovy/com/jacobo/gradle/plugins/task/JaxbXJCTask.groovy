package com.jacobo.gradle.plugins.task

import org.gradle.api.logging.Logging
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Input
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException

import com.jacobo.gradle.plugins.JaxbPlugin
import com.jacobo.gradle.plugins.model.TreeManager
import com.jacobo.gradle.plugins.model.TreeNode
import com.jacobo.gradle.plugins.structures.NamespaceData

/**
 * @author jacobono
 * Created: Tue Dec 04 09:01:34 EST 2012
 */

class JaxbXJCTask extends DefaultTask {
  static final Logger log = Logging.getLogger(JaxbXJCTask.class)

  @Input
  TreeManager manager

  /**
   * If an xsd namespace has dependencies, references episode files from
   * this directory. Must write own episode file to this directory
   */
  @OutputDirectory
  File episodeDirectory

  /**
   * User can create custom bindings, they would be in this directory
   */
  @OutputDirectory
  File customBindingDirectory

  /**
   * directory where the generated java files from xjc would go
   * Usually <pre><project-root>/src/main/java</pre>
   */
  @OutputDirectory
  File generatedFilesDirectory

  @TaskAction
  void start() {
    def treeManager = getManager()
    log.lifecycle("jaxb: attempting to parse '{}' nodes in tree, base nodes are '{}'",
		  treeManager.managedNodes.size(), treeManager.currentNodeRow)
    parseNodes(treeManager.currentNodeRow)
  }

  def parseNodes(baseTreeNodes) {
    def nextRow = baseTreeNodes as Set
    while(nextRow) {
      log.info("parsing '{}' nodes '{}'", nextRow.size(), nextRow)
      nextRow.each { node -> parseNode(node) }
      nextRow = manager.nextNodeRow(nextRow)
    }
  }

  def parseNode(TreeNode<NamespaceData> node) {
    log.info("gathering information for node '{}'", node)
    def episodeBindings = this.resolveEpisodeFiles(node)
    def episodeName = this.convertNamespaceToEpisodeName(node.data.namespace)
    // def episodePath = project.jaxb.jaxbEpisodeDirectory +
    //   "/${episodeName}.episode"
    def xsdFiles = node.data.filesToParse()
    def parseFiles = this.xsdFilesListToString(xsdFiles)
    def bindings = this.transformBindingListToString(project.jaxb.bindingIncludes)


    xjc(project.jaxb.jaxbSchemaDestinationDirectory, project.jaxb.extension,
	project.jaxb.removeOldOutput, project.jaxb.header,
	project.jaxb.xsdDirectoryForGraph, parseFiles,
	project.jaxb.bindingIncludes, project.jaxb.jaxbBindingDirectory,
	bindings, episodeBindings, project.jaxb.jaxbEpisodeDirectory,
	episodeName)
  }

  def xjc(destinationDirectory, extension, removeOldOutput, header,
	  schemasDirectory, xsdFiles, customBinding, bindingDirectory,
	  bindings, episodeBindings, episodesDirectory, episodeName) {
    ant.taskdef (name : 'xjc', 
		 classname : 'com.sun.tools.xjc.XJCTask',
		 classpath : project.configurations[JaxbPlugin.JAXB_CONFIGURATION_NAME].asPath
		)

    ant.xjc(destdir : destinationDirectory,
	    extension : extension,
	    removeOldOutput : removeOldOutput,
	    header : header)
    {
      //TODO maybe should put the produces in there?
      //produces (dir : destinationDirectory)
      schema (dir : schemasDirectory,
	      includes : xsdFiles )
      if (!customBinding.isEmpty()) {
	binding(dir : bindingDirectory, includes : bindings)
      }
      episodeBindings.each { episode ->
	log.info("binding with file {}.episode", episode)
	binding (dir : episodesDirectory,
		 includes : "${episode}.episode")
      }
      log.info("episode file is {}", episodeName)
      arg(value : '-episode')
      arg(value: episodeName)
      arg(value : '-verbose')
      arg(value : '-npa')
    }
  }

  /**
   * @param Tree Node with NamespaceData as a data object
   * @return List of episode names to bind with.
   * Based off of the current Node looking to parse.  Node's parents
   * form the core episode bindings, but the node, along with each
   * one of its parents can depend on external namespaces, and these
   * can be duplicated, so a Set of dependentNamespaces is constructed
   * and then each namespace is converted to it episode file name.
   */
  def resolveEpisodeFiles(TreeNode<NamespaceData> node) { 
    def dependentNamespaces = [] as Set
    // must check for current nodes external dependencies.  Could have 0
    // parents and only possible external dependencies lay in this node
    if (node.data.hasExternalDependencies) {
      log.info("node '{}' has external dependencies", node)
      dependentNamespaces.addAll(node.data.dependentExternalNamespaces)
    }

    def parents = manager.getParents(node)
    // returns empty set if no parents found
    if (parents) {
      dependentNamespaces.addAll(parents.data.namespace)
      // check if any parent has external dependencies
      parents.each { parent ->
	log.info("node '{}' depends on Parent node '{}'", node, parent)
	if (parent.data.hasExternalDependencies) {
	  log.info("parent node '{}' has external dependencies", parent)
	  dependentNamespaces.addAll(parent.data.dependentExternalNamespaces)
	}
      }
    }

    log.info("node '{}' depends on a total of '{}' namespaces", node,
	     dependentNamespaces.size())
    def episodeBindings = dependentNamespaces.collect { dependentNamespace ->
      this.convertNamespaceToEpisodeName(dependentNamespace)
    }
    if(episodeBindings)
      checkEpisodeFilesExist(episodeBindings, node.data.namespace)
    return episodeBindings
  }

  /**
   * Make sure episode files exist.  If not throw an error, letting the
   * user know what is wrong instead of making them sift through info
   * or debug output from command line
   * @param episodeNames -> list of episode file names to check for existence of
   * @param namespace -> the namespace that depends on these episode
   */
  def checkEpisodeFilesExist(List episodeNames, String namespace) {
    log.info("checking existence of '{}' episode files", episodeNames.size())
    episodeNames.each { episode ->
      def file = new File(getEpisodeDirectory(), "${episode}.episode")
      if (file.exists())
	return

      def exceptionString = """Episode file '${file}' doesn't exist! Namespace
'${namespace}' depends on this episode file.  Make sure that the project this
namespace depends on has been parsed and has generated an episode file"""
      throw new RuntimeException(exceptionString)
    }
  }

  /**
   * @param List of File objects
   * @return String fileNames -  the list of included schemas to undergo
   * parsing via xjc. File name ONLY, space separated.
  */
  public String xsdFilesListToString(List<File> filesToParse) {
    String fileNames = filesToParse.name.join(" ")
    log.info("xjc file list input is '{}'", fileNames)
    return fileNames
  }


  /**
   * @param bindings a list of user defined bindings
   * @return String binding list
   * List transformed into a string so the xjc task can accordingly
   * process it Takes a binding list (user defined) and turns it into an
   * appropriate string for the xjc ant task to use
   */
  public String transformBindingListToString(List bindings) { 
    String bindingNames = bindings.join(" ")
    log.info("list of custom bindings for xjc input is '{}'", bindingNames)
    return bindingNames
  }

  /**
   * Method that converts the namespace into an appropriate episode file
   * name that the file system accepts
   * TODO: conventions could be better
   */
  public convertNamespaceToEpisodeName(String namespace) { 
    def episodeName = namespace.replace("http://", "")
    episodeName = episodeName.replace(":", "-")
    episodeName = episodeName.replace("/", "-")
    log.info("converted namespace '{}' to episode name '{}'", namespace,
	     episodeName)
    return  episodeName
  }
}