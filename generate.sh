#!/bin/bash

KEY="API-Key-of-Voicerss"
LANG="de-de"

function download {
  file=$1
  message=$2
  echo $message
  if [ ! -e $file ]; then
    wget -q -O $file "http://api.voicerss.org/?key=$KEY&hl=$LANG&f=44khz_16bit_mono&src=$message"
    sleep 250
  fi
}

function daytime {
  h=$1
  m=$2
  if [ $h -ge 6 -a $h -lt 12 ]; then
    download $h-$m.mp3 "Guten morgen, es ist $h Uhr $m"
  elif [ $h -ge 6 -a $h -lt 18 ]; then
    download $h-$m.mp3 "Guten Tag, es ist $h Uhr $m"
  elif [ $h -ge 6 -a $h -lt 21 ]; then
    download $h-$m.mp3 "Du solltest schlafen, denn es ist $h Uhr $m"
  else
    download $h-$m.mp3 "Auch im Traumland ist es jetzt $h Uhr $m"
  fi
}

for ((h=0;h<24;h++)); do 
  for ((m=0;m<60;m++)); do 
    if [ $m -eq 0 ]; then
    download $h-$m.mp3 "Es ist jetzt genau $h Uhr"
    else
      NUM=$(shuf -i 1-10 -n 1)
      if [ $NUM -lt 3 ]; then
        download $h-$m.mp3 "Es ist $h Uhr $m"
      elif [ $NUM -lt 5 ]; then
        download $h-$m.mp3 "Hei Maus, es ist $h Uhr $m"
      elif [ $NUM -lt 7 ]; then
        download $h-$m.mp3 "Kleine Prinzessin, es ist $h Uhr $m"
      elif [ $NUM -lt 8 ]; then
        download $h-$m.mp3 "Weil du es bist, sag ich dir wieviel Uhr es ist, $h Uhr $m"
      else
        daytime $h $m
      fi
    fi
  done
done

wget -q -O ungeduld.mp3 "http://api.voicerss.org/?key=$KEY&hl=$LANG&f=44khz_16bit_mono&src=Nicht so ungeduldig!"
