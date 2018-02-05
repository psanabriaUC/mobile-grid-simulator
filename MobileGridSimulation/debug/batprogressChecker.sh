function check_progressbat {
   previousbat_int=10000000
   previousbat_dec=0
   for currentbat in $(cat $1 | cut -d ";" -f4); do
	currentbat_int=$(echo $currentbat | cut -d "." -f1)	
	currentbat_dec=$(echo $currentbat | cut -d "." -f2)	
	
	if [ "$currentbat_int" -eq "$previousbat_int" -a "$currentbat_dec" -gt "$previousbat_dec" -o "$currentbat_int" -gt "$previousbat_int" ]; 
	then
	   echo "ERROR: incorrect progress at bat status "$currentbat " in file " $1 >> $2
	fi	
	previousbat_int=$currentbat_int;
	previousbat_dec=$currentbat_dec;
   done;
}

echo "" > errorBatprogress
check_progressbat $1 errorBatprogress
