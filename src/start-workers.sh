#!/usr/bin/env bash
grep '^[^#].*' |
while read user host; do
    ssh -l $user $host $1 &
    echo ran $1 on $user@$host
done
