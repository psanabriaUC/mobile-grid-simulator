#!/bin/bash
#@Author: Matias Hirsch
#this script performs some basic checkings of the profile generated with the profile_mixer.jar

function check_accumulated_usage {
	#get accumulated usage directly from the mixed profile
	first_new_battery_event_time=$(cat $1 | grep "NEW_BATTERY_STATE_NODE" | head -1 | cut -d ";" -f2)
	left_node_event_time=$(cat $1 | grep "LEFT_NODE" | cut -d ";" -f2)
	profile_acc_usage=`expr $left_node_event_time - $first_new_battery_event_time`;
	
	#get accumulated usage from the log file of the mixed profile
	log_acc_usage=0;
	for usage in $(cat $2 | grep "Acc. usage (in millis)" | cut -d ":" -f2); do
	    log_acc_usage=`expr $log_acc_usage + $usage`;	
	done;
	
	#compare both accumulated usages
	if [ "`expr $profile_acc_usage - $log_acc_usage`" -ne 0 ]; then
		echo "ERROR: incorrect accumulated usage in file " $1 >> $2	
	fi
}

function check_progresstime {
   previousTime=0
   for currentTime in $(cat $1 | cut -d ";" -f2); do
	if [ -n "$currentTime" ]; then	
	   if [ $currentTime -le $previousTime ]; then
	      echo "ERROR: incorrect progress at time "$currentTime " in file " $1 >> $2
	   fi	
	   previousTime=$currentTime;
        fi
	
   done;
}

function check_progress_battery_percentage {
   previousBattPercentage=10000000
   for currentBattPercentage in $(cat $1 | grep "NEW_BATTERY_STATE_NODE" | cut -d ";" -f4); do
	if [ -n "$currentBattPercentage" ]; then	
	   if [ $currentBattPercentage -ge $previousBattPercentage ]; then
	      echo "ERROR: incorrect progress of battery percentage "$currentBattPercentage " in file " $1 >> $2
	   fi	
	   previousBattPercentage=$currentBattPercentage;
        fi	
   done;
}

function check_battcpu_initial_synchronization {
	for synchronization_line in $(cat $3 | grep "profile_baterry"); do
	   start_sync=`echo $synchronization_line | cut -d ";" -f2`	   
	   if [ -n "$start_sync" ]; then
	      if [[ -z "$(cat $1 | grep $start_sync)" || -z "$(cat $2 | grep $start_sync)" ]]; then
		 echo "ERROR: initial time of battery and cpu samples set not properly synchronized. Timestamp " $start_sync " in files " $1 " and " $2 >> $3
	      fi;	      
           fi;
	done;
}


check_accumulated_usage $1 $3;
has_error=$(cat $3 | grep "ERROR:");
if [ -z  "$has_error" ];
then 
	check_progresstime $1 $3;
	has_error=$(cat $3 | grep "ERROR:");
	if [ -z  "$has_error" ];
	then 
		check_progress_battery_percentage $1 $3;
		has_error=$(cat $3 | grep "ERROR:");
		if [ -z  "$has_error" ];
		then
			check_progresstime $2 $3
			has_error=$(cat $3 | grep "ERROR:");
			if [ -z "$has_error" ];
			then
				check_battcpu_initial_synchronization $1 $2 $3
			fi
		fi
	fi
fi


