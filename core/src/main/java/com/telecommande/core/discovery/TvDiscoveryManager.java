package com.telecommande.core.discovery;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class TvDiscoveryManager {
    private static final String TAG = "TvDiscoveryManager";
    public static final String SERVICE_TYPE_ANDROID_TV_REMOTE = "_androidtvremote2._tcp.";

    private final Context context;
    private final NsdManager nsdManager;
    private final TvDiscoveryListener discoveryListener;

    private NsdManager.DiscoveryListener nsdDiscoveryListener;
    private WifiManager.MulticastLock multicastLock;

    private final Map<String, NsdServiceInfo> servicesToResolve = new HashMap<>();
    private final Map<String, DiscoveredTv> resolvedServices = new HashMap<>();


    public TvDiscoveryManager(Context context, TvDiscoveryListener listener) {
        this.context = context.getApplicationContext();
        this.nsdManager = (NsdManager)
                this.context.getSystemService(Context.NSD_SERVICE);
        this.discoveryListener = listener;
    }

    public void startDiscovery() {
        if (nsdDiscoveryListener != null) {
            Log.w(TAG, "Discovery already active. Stopping previous one.");
            stopDiscoveryInternal(false);
        }

        Log.d(TAG, "Starting TV discovery for service type: " + SERVICE_TYPE_ANDROID_TV_REMOTE);
        acquireMulticastLock();
        servicesToResolve.clear();
        resolvedServices.clear();

        initializeNsdDiscoveryListener();
        try {
            nsdManager.discoverServices(
                    SERVICE_TYPE_ANDROID_TV_REMOTE,
                    NsdManager.PROTOCOL_DNS_SD,
                    nsdDiscoveryListener
            );
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error starting discovery (listener already registered or other issue): " + e.getMessage(), e);
            if (discoveryListener != null) {
                discoveryListener.onDiscoveryError("Failed to start discovery: " + e.getMessage(), 0);
            }
            releaseMulticastLock();
        }
    }

    public void stopDiscovery() {
        stopDiscoveryInternal(true);
    }

    private void stopDiscoveryInternal(boolean notifyListener) {
        Log.d(TAG, "Stopping TV discovery.");
        if (nsdDiscoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(nsdDiscoveryListener);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Error stopping discovery (listener might not be registered): " + e.getMessage());
            } finally {
                nsdDiscoveryListener = null;
            }
        }
        servicesToResolve.clear();

        releaseMulticastLock();
        if (notifyListener && discoveryListener != null) {
            discoveryListener.onDiscoveryStopped();
        }
    }

    private void initializeNsdDiscoveryListener() {
        nsdDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "NSD Discovery Started: " + regType);
                if (discoveryListener != null) {
                    discoveryListener.onDiscoveryStarted();
                }
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.i(TAG, "NSD Service Found: Name=" + service.getServiceName() + ", Type=" + service.getServiceType());

                String currentServiceType = service.getServiceType();
                if (currentServiceType == null) {
                    Log.w(TAG, "Service found with null type. Ignoring.");
                    return;
                }
                if (!currentServiceType.endsWith(".")) {
                    currentServiceType += ".";
                }

                if (!SERVICE_TYPE_ANDROID_TV_REMOTE.equalsIgnoreCase(currentServiceType)) {
                    Log.d(TAG, "Ignoring service with type: " + service.getServiceType() + " (Expected: " + SERVICE_TYPE_ANDROID_TV_REMOTE + ")");
                    return;
                }

                synchronized (servicesToResolve) {
                    if (servicesToResolve.containsKey(service.getServiceName()) || resolvedServices.containsKey(service.getServiceName())) {
                        Log.d(TAG, "Service " + service.getServiceName() + " is already being resolved or has been resolved. Skipping.");
                        return;
                    }
                    servicesToResolve.put(service.getServiceName(), service);
                }

                Log.d(TAG, "Resolving service: " + service.getServiceName());
                nsdManager.resolveService(service, initializeResolveListener(service.getServiceName()));
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.i(TAG, "NSD Service Lost: " + service.getServiceName());
                DiscoveredTv lostTv = null;
                synchronized (servicesToResolve) {
                    servicesToResolve.remove(service.getServiceName());
                    lostTv = resolvedServices.remove(service.getServiceName());
                }

                if (discoveryListener != null && lostTv != null) {
                    discoveryListener.onTvLost(lostTv);
                } else if (discoveryListener != null && service.getServiceName() != null) {
                    Log.w(TAG, "Service lost but was not in resolved list or details incomplete: " + service.getServiceName());
                    discoveryListener.onTvLost(new DiscoveredTv(service.getServiceName(), service.getServiceName(), null, 0));
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "NSD Discovery Stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "NSD Start Discovery Failed: Type=" + serviceType + ", ErrorCode=" + errorCode);
                if (discoveryListener != null) {
                    discoveryListener.onDiscoveryError("Start Discovery Failed. Error: " + errorCode, errorCode);
                }
                stopDiscoveryInternal(false);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "NSD Stop Discovery Failed: Type=" + serviceType + ", ErrorCode=" + errorCode);
                if (discoveryListener != null) {
                    discoveryListener.onDiscoveryError("Stop Discovery Failed. Error: " + errorCode, errorCode);
                }
                releaseMulticastLock();
            }
        };
    }

    private NsdManager.ResolveListener initializeResolveListener(final String serviceNameKey) {
        return new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "NSD Resolve Failed for " + (serviceInfo != null ? serviceInfo.getServiceName() : serviceNameKey) + ": ErrorCode=" + errorCode);
                synchronized (servicesToResolve) {
                    servicesToResolve.remove(serviceNameKey);
                }
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.i(TAG, "NSD Service Resolved: Name=" + serviceInfo.getServiceName() +
                        ", Host=" + serviceInfo.getHost() +
                        ", Port=" + serviceInfo.getPort());

                synchronized (servicesToResolve) {
                    servicesToResolve.remove(serviceNameKey);
                    if (resolvedServices.containsKey(serviceInfo.getServiceName())) {
                        Log.d(TAG, "Service " + serviceInfo.getServiceName() + " was already resolved and notified. Skipping duplicate notification.");
                        return;
                    }
                }

                String friendlyName = serviceInfo.getServiceName();
                InetAddress hostAddress = serviceInfo.getHost();
                int port = serviceInfo.getPort();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Map<String, byte[]> attributes = serviceInfo.getAttributes();
                    if (attributes != null && attributes.containsKey("fn")) {
                        byte[] fnBytes = attributes.get("fn");
                        if (fnBytes != null) {
                            try {
                                friendlyName = new String(fnBytes, "UTF-8");
                                Log.d(TAG, "Friendly name from TXT record: " + friendlyName);
                            } catch (UnsupportedEncodingException e) {
                                Log.w(TAG, "Failed to decode friendly name from TXT record", e);
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Cannot access TXT attributes on API < 21 for friendly name. Using service name.");
                }

                if (hostAddress == null) {
                    Log.w(TAG, "Resolved service " + serviceInfo.getServiceName() + " has null host address. Skipping.");
                    return;
                }

                DiscoveredTv tv = new DiscoveredTv(serviceInfo.getServiceName(), friendlyName, hostAddress, port);
                Log.d(TAG, "Successfully resolved and processed: " + tv.toString());

                if (discoveryListener != null) {
                    synchronized(servicesToResolve) {
                        resolvedServices.put(serviceInfo.getServiceName(), tv);
                    }
                    discoveryListener.onTvFound(tv);
                }
            }
        };
    }

    private void acquireMulticastLock() {
        if (multicastLock == null) {
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                multicastLock = wifi.createMulticastLock(TAG + ".MulticastLock");
                multicastLock.setReferenceCounted(true);
            } else {
                Log.e(TAG, "WifiManager not available, cannot create MulticastLock.");
                return;
            }
        }
        try {
            if (!multicastLock.isHeld()) {
                multicastLock.acquire();
                Log.d(TAG, "MulticastLock acquired.");
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException acquiring MulticastLock: " + se.getMessage() + ". Check CHANGE_WIFI_MULTICAST_STATE permission.", se);
        } catch (Exception e) {
            Log.e(TAG, "Error acquiring MulticastLock: " + e.getMessage(), e);
        }
    }

    private void releaseMulticastLock() {
        if (multicastLock != null && multicastLock.isHeld()) {
            try {
                multicastLock.release();
                Log.d(TAG, "MulticastLock released.");
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MulticastLock: " + e.getMessage(), e);
            }
        }
    }
}