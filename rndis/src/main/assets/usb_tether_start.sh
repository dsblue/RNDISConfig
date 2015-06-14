#!/system/bin/sh

ipAddress=$1
interfaceName=$2

setprop sys.usb.config 'rndis,adb'
until [ "$(getprop sys.usb.state)" = 'rndis,adb' ] ; do sleep 1 ; done

exit 0

ip rule add from all lookup main
ip addr flush dev $interfaceName
ip addr add $ipAddress dev $interfaceName
ip link set $interfaceName up
iptables -t nat -I POSTROUTING 1 -o rmnet0 -j MASQUERADE
echo 1 > /proc/sys/net/ipv4/ip_forward
