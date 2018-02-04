#!/bin/bash

#set -x

#
# Bare bones script for starting BX-bot on Linux/OSX systems.
#
# Could be made better, but will do for now...
#
# You need the Java 8 JRE installed.
#
# This script expects all the jar files to live in the lib_dir.
#
# You can change the bxbot_jar var to the version you want to run; it has been defaulted to the current release.
#
# You can start, stop, and query the bot's status: ./bxbot.sh [start|stop|status]
#
lib_dir=./libs

# log4j2 config file location
log4j2_config=./config/log4j2.xml

# The BX-bot 'fat' jar (Spring Boot app containing all the dependencies)
bxbot_jar=bxbot-app-0.8.7-SNAPSHOT.jar

# PID file for checking if bot is running
pid_file=./.bxbot.pid

# Process args passed to script
case "$1" in
   'start')
       if [[ -e ${pid_file} ]]; then
          pid=$(cat ${pid_file});
          echo "BX-bot is already running with PID: $pid"
       else
          echo "Starting BX-bot..."
          java -Xmx64m -Xss256k -Dlog4j.configurationFile=file:${log4j2_config} -jar ${lib_dir}/${bxbot_jar} 2>&1 >/dev/null &

          echo "BX-bot started with PID: $!"
          echo $! > ${pid_file}
       fi
       ;;

    'stop')
       if [[ -e ${pid_file} ]]; then
          pid=$(cat ${pid_file});
       else
          echo "BX-bot is not running. Nothing to stop."
          exit
       fi
       echo "Stopping BX-bot instance running with PID: $pid ..."
       kill ${pid}
       sleep 1
       pid=`ps -aef | grep ${pid} | grep -v grep`
       if [[ ${pid} -gt 1 ]]; then
          echo "Failed to stop BX-bot. Manual kill required!"
       else
          echo "BX-bot has stopped."
          rm ${pid_file}
       fi
       ;;

   'status')
      if [[ -e ${pid_file} ]]; then
         pid=$(cat ${pid_file});
         echo "BX-bot is running with PID: $pid"
      else
         echo "BX-bot is not running."
      fi
      ;;

   *)
         echo "Invalid args. Usage: $0 [start|stop|status]"
      ;;
esac
