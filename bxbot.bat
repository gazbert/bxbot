@echo off

REM
REM TODO - Work in progress...
REM
REM Bare bones script for starting BX-bot on Windows systems.
REM
REM Could be made better, but will do for now...
REM
REM You need the Java 8 JRE installed.
REM
REM This script expects all the jar files to live in the lib_dir.
REM
REM You can change the bxbot_jar var to the version you want to run; it has been defaulted to the current release.
REM
REM You can start, stop, and query the bot's status: bxbot.bat [start|stop|status]
REM
SET lib_dir=.\libs

REM log4j2 config file location
SET log4j2_config=.\config\log4j2.xml

REM The BX-bot 'fat' jar (Spring Boot app containing all the dependencies)
SET bxbot_jar=bxbot-app-0.8.7-SNAPSHOT.jar

REM PID file for checking if bot is running
SET pid_file=.\.bxbot.pid

REM Process args passed to script. Ouch. Is there a Windows equivalent of a Bash 'case' ?
IF %1.==. GOTO:invalidArgs
IF "%1"=="start" GOTO:start
IF "%1"=="stop" GOTO:stop
IF "%1"=="status" GOTO:status
IF NOT "%1"=="status" GOTO:invalidArgs

:start
REM TODO - check if bot is already running before trying to start it!
SET START_TIME=%time%
ECHO Starting BX-bot...
START "BX-bot - %START_TIME%" java -Xmx64m -Xss256k -Dlog4j.configurationFile=%log4j2_config% -jar %lib_dir%\%bxbot_jar%
FOR /F "tokens=2" %%i in ('TASKLIST /NH /FI "WINDOWTITLE eq BX-bot - %START_TIME%"' ) DO (SET PID=%%i)
ECHO %PID% > %pid_file%
ECHO BX-bot started with PID: %PID%
GOTO:EOF

:stop
IF NOT EXIST %pid_file% (
    ECHO BX-bot is not running. Nothing to stop.
) ELSE (
    FOR /f %%a IN (%pid_file%) DO (
    	ECHO Stopping BX-bot instance running with PID: %%a ...
   		taskkill /F /PID %%a
        DEL %pid_file%
        EXIT /b
    )
)
GOTO:EOF

:status
IF EXIST %pid_file% (
    FOR /f %%a IN (%pid_file%) DO (
       ECHO BX-bot is running with PID: %%a
       EXIT /b
    )
) ELSE (
    ECHO BX-bot is not running.
)
GOTO:EOF

:invalidArgs
ECHO Invalid args. Usage: bxbot.bat [start^|stop^|status]
GOTO:EOF
