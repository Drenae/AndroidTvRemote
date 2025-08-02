package com.telecommande.core.discovery;

import java.net.InetAddress;

public class DiscoveredTv {
    private final String serviceName;
    private final String friendlyName;
    private final InetAddress hostAddress;
    private final int port;

    public DiscoveredTv(String serviceName, String friendlyName, InetAddress hostAddress, int port) {
        this.serviceName = serviceName;
        this.friendlyName = friendlyName;
        this.hostAddress = hostAddress;
        this.port = port;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public InetAddress getHostAddress() {
        return hostAddress;
    }

    public String getIpAddress() {
        return hostAddress != null ? hostAddress.getHostAddress() : null;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "DiscoveredTv{" +
                "friendlyName='" + friendlyName + '\'' +
                ", ipAddress='" + getIpAddress() + '\'' +
                ", port=" + port +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscoveredTv that = (DiscoveredTv) o;
        return serviceName != null ? serviceName.equals(that.serviceName) : that.serviceName == null;
    }

    @Override
    public int hashCode() {
        return serviceName != null ? serviceName.hashCode() : 0;
    }
}
