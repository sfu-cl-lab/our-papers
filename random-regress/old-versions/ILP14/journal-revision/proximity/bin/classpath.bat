@echo off

REM $Id: classpath.bat 3783 2007-11-18 19:44:12Z schapira $
REM
REM Part of the open-source Proximity system (see LICENSE for copyright
REM and license information).

set classpath="%PROX_HOME%\proximity.jar"
set classpath=%classpath%;"%PROX_HOME%\lib\colt.jar"
set classpath=%classpath%;"%PROX_HOME%\lib\commons-collections-3.1.jar"
set classpath=%classpath%;"%PROX_HOME%\lib\jdom.jar"
set classpath=%classpath%;"%PROX_HOME%\lib\jung-1.7.6.jar"
set classpath=%classpath%;"%PROX_HOME%\lib\junit.jar"
set classpath=%classpath%;"%PROX_HOME%\lib\jython.jar"
set classpath=%classpath%;"%PROX_HOME%\lib\log4j-1.2.14.jar"
set classpath=%classpath%;"%PROX_HOME%\lib\mantissa-7.0.jar"
set classpath=%classpath%;"%PROX_HOME%\lib\piccolo.jar"
set classpath=%classpath%;"%PROX_HOME%\lib\piccolox.jar"
set classpath=%classpath%;"%PROX_HOME%\lib\spin.jar"
set classpath=%classpath%;"%PROX_HOME%\lib\xercesImpl.jar"
