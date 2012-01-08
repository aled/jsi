This release contains the following directories/files.

doc/           - documentation
lib/           - contains the sil-0.44.2b library that is not available through maven
pom.xml        - maven build configuration. See BUILD.txt.
src/           - source code to the JSI library. Test code is in the com.infomatiq.jsi.test package.

The target directory is normally created as part of the build. However, the following are 
included in jsi-1.0-full.zip.

target/apidocs      - javadocs
target/classes      - compiled non-test classes
target/dependencies - libraries that are downloaded by maven during the build 
target/jsi-1.0.jar  - the jsi library
target/test-classes - compiled test classes

