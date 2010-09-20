**************
To run the Selenium tests for GateIn
**************

From mvn command line:
* Start a GateIn server (Tomcat or JBoss)
* Start the server
* Launch the tests:
** mvn install -Pselenium to run the html recorded Scripts generated in Java during the process

Changing the port (JBoss is using 4444 in default config):
** mvn install -Pselenium -Dselenium.port=6666

Changing the browser (firefox, safari, iexplorer, opera):
** mvn install -Pselenium -Dselenium.browser=safari

From Eclipse:
* Start a GateIn server (Tomcat or JBoss)
* Start the Selenium server ( GateIn server (Tomcat or JBoss)
** Main class: org.openqa.selenium.server.SeleniumServer
** Parameter: -userExtensions ${project_loc}/src/suite/user-extensions.js
** Some .launch files are available in src/eclipse
* Run any test like a unit test


**************
Informations:
**************

* suite/ contains the recorded tests
** They can be edited using the Selenium IDE
* src/java/main/java/ contains a generator to create the same tests in Java
** One Test per Selenium test
* target/generated/test contains the generated tests

******************************
Known Issues:
******************************
* Known


******************
* Archetype usage
******************

mvn archetype:generate -DarchetypeGroupId=org.exoplatform.utils.selegen -DarchetypeArtifactId=exo-selegen-archetype -DarchetypeVersion=0.9.1-SNAPSHOT -DgroupId=org.exoplatform.test -DartifactId=selenium

