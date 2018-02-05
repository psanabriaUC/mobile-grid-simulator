load "defaults.p"
set encoding iso_8859_1
set terminal postscript eps monochrome noenhanced defaultplex "Arial" 12
#set term pdf
set out "cpu2.eps"
#set terminal png
set autoscale

set style histogram
set grid noxtics ytics
#set yrange [0.0:0.5]
set key top right horizontal
#set key right top
set key outside 
#set boxwidth 0.7 relative
set xtics rotate by -90 border in scale 1,0.5 nomirror font "Arial,9"
#set bmargin at screen 0.5

#set ytics 8500,1000,12000
set ylabel "CPU Usage"
set xlabel "time in seconds"
set datafile separator ";"
#fs solid 0 lw 1 lc rgb "gray" lt 1
#with points pt 5 ps 0.2
plot "" using ($2/1000):4 with steps ls 2 title "cpu usage"

