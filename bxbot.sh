#!/bin/bash

#set -x

#
# Bare bones script for starting BX-bot on Linux/OSX systems.
#
# Could be made better, but will do for now...
#
# You need the Java runtime installed - Oracle JDK 1.8 is supported.
#
# This script expects all the jar files to live in the LIB_DIR.
#
# You can change the bxbot_core var to the version you want to run; it has been defaulted to the current release.
#
# You can start, stop, and query the bot's status: ./bxbot.sh [start|stop|status]
#
LIB_DIR=./libs

# log4j2 config file location
log4j2_config=./config/log4j2.xml

# The BX-bot core jar (Spring Boot app containing all the dependencies)
bxbot_core=bxbot-app-0.5-beta.3-SNAPSHOT.jar

# PID file for checking if bot is running
PID_FILE=./.bxbot.pid

# Process args passed to script
case "$1" in
   'start')
       if [[ -e ${PID_FILE} ]]; then
          pid=$(cat ${PID_FILE});
          echo "BX-bot is already running with PID: $pid"
       else
          echo "Starting BX-bot..."
          java -Xmx64m -Xss256k -Dlog4j.configurationFile=file:${log4j2_config} -jar ${LIB_DIR}/${bxbot_core} 2>&1 >/dev/null &

          echo "BX-bot started with PID: $!"
          echo $! > ${PID_FILE}
       fi
       ;;

    'stop')
       if [[ -e ${PID_FILE} ]]; then
          pid=$(cat ${PID_FILE});
       else
          echo "BX-bot is not running. Nothing to stop."
          exit
       fi
       echo "Stopping BX-bot instance running with PID: $pid ..."
       kill ${pid}
       sleep 1
       pid=`ps -aef | grep ${pid} | grep -v grep`
       if [[ ${pid} -gt 1 ]]; then
          echo "Failed to stop BX-bot. Kill action required!"
       else
          echo "BX-bot has stopped."
          rm ${PID_FILE}
       fi
       ;;

   'status')
      if [[ -e ${PID_FILE} ]]; then
         pid=$(cat ${PID_FILE});
         echo "BX-bot is running with PID: $pid"
      else
         echo "BX-bot is not running."
      fi
      ;;

   *)
         echo "Invalid arg. Usage: $0 [start|stop|status]"
      ;;
esac
