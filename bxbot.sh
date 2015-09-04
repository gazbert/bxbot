#!/bin/bash

#set -x

#
# TODO clean this up and create windows .bat equivalent
#
# Bare bones script for starting BX-bot on linux systems.
#
# Could be made better, but will do for now...
#
# You need the Java runtime installed - Oracle JDK 1.8 is supported.
#
# This script expects *all* the jar files to live in the LIB_DIR.
#
# Change the bxbot_core, log4j, javamail, and gson (if you are using the inbuilt Exchange Adapters) vars to
# the versions you want to run. They have been given sensible defaults.
#
# Set bxbot_strategies var if you have a jar with your own Trading Strategies.
#
# Set bxbot_exchanges var if you have a jar with your own Exchange Adapters.
#
# If you are using the inbuit Exchange Adapters, you will need to add your trade keys in the appropriate trading
# config file, e.g. RESOURCES_DIR/btce/btce-config.properties
#

LIB_DIR=./libs
RESOURCES_DIR=./resources

# log4j (mandatory)
log4j=log4j-1.2.17.jar

# javamail (mandatory)
javamail=javax.mail-1.5.4.jar

# GSON (optional - only needed if you use the inbuilt Exchange Adapters; the script assumes you are)
gson=gson-2.3.1.jar

# The BX-bot core jar (mandatory)
bxbot_core=bxbot-core-1.0-SNAPSHOT.jar

# Your Trading Strategies (optional)
# Needed if you're not using the sample included with the bot OR you have not included your strats in the bxbot_core jar.
bxbot_strategies=

# Your Exchange Adapters (optional)
# Needed if you're not using the inbuilt Exchange Adapters OR you have not included your adapters in the bxbot_core jar.
bxbot_exchanges=

# Runtime classpath
CLASSPATH=${LIB_DIR}/${log4j}:${LIB_DIR}/${javamail}:${LIB_DIR}/${gson}:${LIB_DIR}/${bxbot_core}:\
${LIB_DIR}/${bxbot_strategies}:${LIB_DIR}/${bxbot_exchanges}:${RESOURCES_DIR}

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
          java -cp ${CLASSPATH} com.gazbert.bxbot.core.BXBot 2>&1 >/dev/null &

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