# cdsp-tools packaging as part of c3ds-projects

JTOOLS_REL := cdsp-tools/cdsp-tools.jar cdsp-tools/cdsp-tools cdsp-tools/cdsp-tools.cmd

# cdsp-tools JAR itself

.PHONY: cdsp-tools/cdsp-tools.jar
cdsp-tools/cdsp-tools.jar: java
	cd cdsp-tools ; mvn package
	cp cdsp-tools/target/cdsp-tools-0.666-SNAPSHOT-jar-with-dependencies.jar cdsp-tools/cdsp-tools.jar

rel: $(JTOOLS_REL)
rel-sdk: $(JTOOLS_REL)
