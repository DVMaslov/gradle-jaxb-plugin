# Gradle JAXB Plugin Changelog

## 1.3.6
- Fix to issue #27 - when there are binding files, will not parse xjc
  in a dependency order.  Will just use the folder and glob pattern
  '**/*.xsd'

## 1.3.5

- plugin package/group name changed from `org.gradle.jacobo.plugins`
to `com.github.jacobono`
- ant `xjc` task is passing in the epsiode file to generate through
  `arg value` instead of `arg line` to prevent errors when file paths
  have spaces in them.
- changing calls from member .size to .size()

## 1.3.4

- Plugin id changed fram 'jaxb' to 'com.github.jacobono.jaxb'
- Included in gradle plugin repo

## 1.3.3

- Backwards compatibility with java 1.6
- now available via bintray's jcenter

## 1.3.2

- Adding additional args to plugin DSL

## 1.3.1

- full groovy doc and detalied documention

## 1.3

- Major refactoring to use the gradle-xsd-wsdl-slurping common library
  I refactored the common logic and interfaces to. 
- Major refactoring of classes to improve readability
- Adding Service delegate Objects
- Using Guice for Dependency Injection
- Complete code makeover
- package renaming to one with base `org.gradle.jacobo.plugins`
- adding Test Fixtures to properly test the different edge cases of this plugin
- Tasks were renamed
- Added support for xsd projects to declare they depend on another xsd
  project and have that project's `xjc` task run prior. 
