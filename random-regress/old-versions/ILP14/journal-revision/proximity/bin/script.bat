@echo off

REM $Id: script.bat 3658 2007-10-15 16:29:11Z schapira $
REM
REM Part of the open-source Proximity system (see LICENSE for copyright
REM and license information).
REM
REM Runs Proximity application, by setting the appropriate classpath
REM

if not exist "%PROX_HOME%\proximity.jar" goto noProxHome

call "%PROX_HOME%\bin\classpath.bat"

rem Slurp the command line arguments. Copied from the Ant startup script.
set PROX_CMD_LINE_ARGS=%1
if ""%1""=="""" goto runApplication
shift
:setupArgs
if ""%1""=="""" goto runApplication
set PROX_CMD_LINE_ARGS=%PROX_CMD_LINE_ARGS% %1
shift
goto setupArgs

:runApplication
java -classpath %classpath% kdl.prox.app.PythonScript %PROX_CMD_LINE_ARGS%
goto End

:noProxHome
echo PROX_HOME is set incorrectly or Proximity could not be located: '%PROX_HOME%'. Please set PROX_HOME.
goto End

:End
