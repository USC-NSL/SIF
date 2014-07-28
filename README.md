# SIF
*A Selective Instrumentation Framework for Mobile Apps*

## Project Details
SIF was developed by Shuai Hao in collaboration with Ding Li, William G.J. Halfond, and Ramesh Govindan at [USC](http://www.usc.edu). A thorough description of SIF can be found at the Networked Systems Lab [project page for SIF](http://nsl.cs.usc.edu/Projects/SIF). Any technical questions can be directed to shuai.hao@gmail.com.

## Running SIF

```./sif.sh foo.apk SIFScript.java``` 

The instrumented app will be named as ```foo-sif.apk```.

Note that SIF is currently instantiated for Android platform and only tested on Ubuntu machine (12.04), Android 4.1.2. Users may require proper Android development environment setup before using SIF. We are adding some automated procedure to check such environment before running SIF.
