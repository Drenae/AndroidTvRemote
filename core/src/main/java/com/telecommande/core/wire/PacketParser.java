package com.telecommande.core.wire;

import java.io.IOException;
import java.io.InputStream;

public abstract class PacketParser extends Thread {
    private final InputStream mInputStream;
    private volatile boolean isAbort = false;

    private static final int MAX_EXPECTED_PACKET_LENGTH = 8192;

    public PacketParser(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        mInputStream = inputStream;
    }

    @Override
    public void run() {
        System.out.println(getName() + ": Starting packet parsing loop.");
        int packetLength;
        int totalBytesReadForPacket;

        while (!isAbort) {
            try {
                System.out.println(getName() + ": Attempting to read packet length (1 byte)...");
                packetLength = mInputStream.read();

                if (packetLength == -1) {
                    System.out.println(getName() + ": Stream closed (returned -1) while reading packet length. Stopping.");
                    isAbort = true;
                    continue;
                }

                System.out.println(getName() + ": Received raw packet length byte: " + packetLength);

                if (packetLength > MAX_EXPECTED_PACKET_LENGTH) {
                    System.err.println(getName() + ": Error - Packet length " + packetLength + " exceeds MAX_EXPECTED_PACKET_LENGTH " + MAX_EXPECTED_PACKET_LENGTH + ". Corrupted data likely. Stopping.");
                    isAbort = true;
                    continue;
                }

                if (packetLength == 0) {
                    System.out.println(getName() + ": Received packet length 0. Processing as empty message.");
                    messageBufferReceived(new byte[0]);
                    continue;
                }

                System.out.println(getName() + ": Expecting packet of size: " + packetLength);
                byte[] buffer = new byte[packetLength];
                totalBytesReadForPacket = 0;

                while (totalBytesReadForPacket < packetLength && !isAbort) {
                    int remainingBytes = packetLength - totalBytesReadForPacket;
                    int bytesReadThisCycle = mInputStream.read(buffer, totalBytesReadForPacket, remainingBytes);

                    if (bytesReadThisCycle < 0) {
                        System.err.println(getName() + ": Stream closed unexpectedly while reading packet data. Expected " + packetLength + " bytes, but stream ended after " + totalBytesReadForPacket + " bytes. Stopping.");
                        isAbort = true;
                        throw new IOException("Stream closed unexpectedly while reading packet data. Expected " + packetLength + " bytes, got " + totalBytesReadForPacket + " before stream end.");
                    }

                    totalBytesReadForPacket += bytesReadThisCycle;
                }

                if (isAbort) {
                    System.out.println(getName() + ": Abort requested during packet body read. Discarding partial packet.");
                    continue;
                }

                messageBufferReceived(buffer);

            } catch (IOException e) {
                if (!isAbort) {
                    System.err.println(getName() + ": IOException in run loop: " + e.getMessage());
                }
                isAbort = true;
            } catch (Exception e) {
                if (!isAbort) {
                    System.err.println(getName() + ": Unexpected " + e.getClass().getSimpleName() + " in run loop: " + e.getMessage());
                }
                isAbort = true;
            }
        }

        System.out.println(getName() + ": Packet parsing loop finished.");
    }

    public void abort() {
        System.out.println(getName() + ": Abort requested.");
        isAbort = true;
    }

    public abstract void messageBufferReceived(byte[] buf);
}
