package com.telecommande.core.discovery;

import java.util.List;

public interface TvDiscoveryListener {

    void onDiscoveryStarted();

    void onDiscoveryStopped();

    void onTvFound(DiscoveredTv tv);

    void onTvLost(DiscoveredTv tv);

    void onDiscoveryError(String message, int errorCode);
}