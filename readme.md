[![Build Status](https://www.travis-ci.org/aled/jsi.png?branch=master)](https://www.travis-ci.org/aled/jsi)

Overview
--------
The Java Spatial Index project aims to maintain a high performance Java version of the RTree spatial indexing algorithm as described in the 1984 paper "R-trees: A Dynamic Index Structure for Spatial Searching" by Antonin Guttman.  (<a href="http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.131.7887&amp;rep=rep1&amp;type=pdf">PDF on CiteSeerX</a>)

The JSI spatial index is deliberately limited in features,
      and does a small number of things well. It particular, it is fast.

The code is open source, and released under the <a href="http://www.gnu.org/copyleft/lesser.html">GNU Lesser General Public License</a>, version 2.1 or later.

Usage
-----
It is highly recommended to start by looking at the jsi-examples repository at <a href=https://github.com/aled/jsi-examples>https://github.com/aled/jsi-examples</a>.

Briefly, you need to initialize the RTree like this:

    // Create and initialize an rtree
    SpatialIndex si = new RTree();
    si.init(null);

Then add some rectangles; each one has an ID.

    final Rectangle[] rects = new Rectangle[100];
    rects[0] = new Rectangle(0, 10, 0, 10);
    rects[1] = new Rectangle(0, 11, 1, 20);
    si.add(rects[0], 0);
    si.add(rects[1], 1);
    ...

and finally query for the 3 nearest rectangles to (36.3, 84.3) by calling the nearestN() method.

    si.nearestN(
      new Point(36.3f, 84.3f),      // the point for which we want to find nearby rectangles
      new TIntProcedure() {         // a procedure whose execute() method will be called with the results
        public boolean execute(int i) {
          log.info("Rectangle " + i + " " + rects[i] + ", distance=" + rects[i].distance(p));
          return true;              // return true here to continue receiving results
        }
      },
      3,                            // the number of nearby rectangles to find
      Float.MAX_VALUE               // Don't bother searching further than this. MAX_VALUE means search everything
    );

A binary distribution that contains the JSI jar and all the runtime dependencies is available from <a href=http://sourceforge.net/projects/jsi/files>http://sourceforge.net/projects/jsi/files</a>.

Alternatively, maven users can use this repository in their pom.xml:

    <repository>
      <id>jsi.sourceforge.net</id>
      <name>sourceforge jsi repository</name>
      <url>http://sourceforge.net/projects/jsi/files/m2_repo</url>
    </repository>

Building
-------
To build the JSI library from source, install maven 3 and run the following:

    % cd <location-of-pom.xml>
    % mvn package

This will generate the binary package (jsi-x.y.z.jar) in the target subdirectory.

The following is a list of useful maven targets:

    eclipse:eclipse (generate eclipse project files; see below)
    clean 
    compile
    test
    -Dtest=ReferenceCompareTest_10000 test
    package	
    site
    assembly:single (create package only)
    site:deploy
    deploy
  
To import the project into eclipse, run mvn eclipse:eclipse, and then set the `M2_REPO` variable
in `Window -> Preferences -> Java -> Build Path -> Classpath Variables` to point to your local maven repository (e.g. `~/.m2/repository`)


Testing
-------

These are the steps needed to check that the JSI library is working correctly. 
Note this will take a very long time to run:

    % cd <location-of-pom.xml>
    % mvn test [This runs a short and quick test]
    % mvn -Dtest=ReferenceCompareTest_1000 test [Long test]
    % mvn -Dtest=ReferenceCompareTest_10000 test [Very long test]
    % mvn -Dtest=ReferenceCompareTest_100000 test [Ridiculously long test]

If any errors occur, please raise an issue at https://github.com/aled/jsi/issues

