#!/system/bin/sh

savedMode=$1
interfaceName=$2

echo 0 > /proc/sys/net/ipv4/ip_forward
iptables -t nat -D POSTROUTING 1
ip link set $interfaceName down
ip addr flush dev $interfaceName
ip rule del from all lookup main

setprop sys.usb.config $savedMode

exit 0