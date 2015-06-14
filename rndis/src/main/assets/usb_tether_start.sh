#!/system/bin/sh

ipAddress=$1
interfaceName=$2

setprop sys.usb.config 'rndis,adb'
until [ "$(getprop sys.usb.state)" = 'rndis,adb' ] ; do sleep 1 ; done

ip rule add from all lookup main
ip addr flush dev $interfaceName
ip addr add $ipAddress dev $interfaceName
ip link set $interfaceName up
iptables -t nat -I POSTROUTING 1 -o rmnet0 -j MASQUERADE
echo 1 > /proc/sys/net/ipv4/ip_forward

#dnsmasq --pid-file=/cache/usb_tether_dnsmasq.pid --interface=$interfaceName --bind-interfaces --bogus-priv --filterwin2k --no-resolv --domain-needed --server=8.8.8.8 --server=8.8.4.4 --cache-size=1000 --dhcp-range=192.168.137.2,192.168.137.254,255.255.255.0,192.168.137.255 --dhcp-lease-max=253 --dhcp-authoritative --dhcp-leasefile=/cache/usb_tether_dnsmasq.leases < /dev/null

exit 0

#netcfg rndis0 dhcp
#route add default gw 192.168.0.1 dev rndis0
#ifconfig rndis0 192.168.2.2 netmask 255.255.255.0 up
#route add default gw 192.168.2.1 dev rndis0
#iptables -F
#iptables -F -t nat
#setprop net.dns1 8.8.8.8
#setprop "net.gprs.http-proxy" ""