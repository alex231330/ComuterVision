package com.example.alex231330.compvision.libs;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NxtBluetoothController extends NxtAbstractController {
    public static BluetoothDevice findDeviceByName(String name) {
        BluetoothAdapter adapter;
        adapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices;
        pairedDevices = adapter.getBondedDevices();
        BluetoothDevice result = null;
        for (BluetoothDevice device : pairedDevices) {
            String deviceName = device.getName();
            if (deviceName.equals(name)) {
                if (result != null) {
                    throw new IllegalArgumentException("There are more than one device with name '" + name + "'");
                }
                result = device;
            }
        }
        if (result == null) {
            throw new NoSuchElementException();
        }
        return result;
    }

    final protected UUID NXT_SERVER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // NXT's server UUID

    protected BluetoothDevice mBtDevice;
    protected BluetoothSocket mSocket;
    protected Thread mListenerThread;
    protected ArrayList<BlockingQueue<byte[]>> receivedResponses;

    public NxtBluetoothController(BluetoothDevice btDevice) {
        super();
        mBtDevice = btDevice;
    }

    @Override
    public synchronized void connect() throws IOException {
        if (isConnected())
            throw new IllegalStateException("Already connected");

        try {
            Method m = BluetoothDevice.class.getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            mSocket = (BluetoothSocket) m.invoke(mBtDevice, NXT_SERVER_UUID);
        } catch (NoSuchMethodException e) {
            mSocket = mBtDevice.createRfcommSocketToServiceRecord(NXT_SERVER_UUID);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        mSocket.connect();
        mIsConnected = true;
        receivedResponses = new ArrayList<BlockingQueue<byte[]>>();
        for (int i = 0; i < 256; i++)
            receivedResponses.add(new LinkedBlockingQueue<byte[]>());
        mListenerThread = new Thread(new ListenerThread());
        mListenerThread.start();
    }

    @Override
    public synchronized void disconnect() {
        if (!isConnected())
            throw new IllegalStateException("Not connected");
        try {
            mSocket.close();
        } catch (IOException e) {
        }
        mIsConnected = false;
        while (mListenerThread.isAlive()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        mSocket = null;
        receivedResponses = null;
    }

    static private void readBytes(InputStream stream, byte[] buffer, int len) throws IOException {
        if (len < 0 || buffer.length < len)
            throw new IllegalArgumentException("len should be <= buffer.length");
        int bytesRead = 0;
        while (bytesRead < len) {
            int curRead = stream.read(buffer, bytesRead, len - bytesRead);
            if (curRead == -1) break;
            bytesRead += curRead;
        }
    }

    class ListenerThread implements Runnable {
        public void run() {
            final int bufferSize = MAX_DIRECT_COMMAND_LENGTH;
            final byte[] buffer = new byte[bufferSize];

            try {
                InputStream stream = mSocket.getInputStream();

                while (true) {
                    readBytes(stream, buffer, 2);

                    int len = buffer[0] | (buffer[1] << 8);
                    assert len <= 64;
                    readBytes(stream, buffer, len);

                    byte[] response = new byte[len];
                    System.arraycopy(buffer, 0, response, 0, len);

                    if (buffer.length >= 2) {
                        if (buffer[0] == 0x02) {
                            receivedResponses.get(buffer[1] & 0xFF).add(response);
                            continue;
                        } else if (buffer[0] == (byte) 0x80 && buffer[1] == 0x09) {
                            if (mMessageWriteListener != null) {
                                mMessageWriteListener.onMessageWrite(NxtBluetoothController.this, response);
                            }
                            continue;
                        }
                    }

                    final StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < len; i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(String.format("0x%02X", buffer[i]));
                    }
                    Log.w("NxtBluetoothController", "Received invalid message: " + sb.toString());
                }
            } catch (IOException e) {
            }
        }
    }

    private final byte[] toSend = new byte[MAX_DIRECT_COMMAND_LENGTH + 2];

    @Override
    public byte[] sendDirectCommand(byte[] command) throws IOException, InterruptedException {
        if (!isConnected())
            throw new IllegalStateException("Not connected");

        if (command.length < 2 || command.length > 64)
            throw new IllegalArgumentException("command.length should be >= 2 && <= MAX_DIRECT_COMMAND_LENGTH");
        if (command[0] != (byte) 0x00 && command[0] != (byte) 0x80)
            throw new IllegalArgumentException("command[0] should be either 0x00 or 0x80");

        synchronized (this) {
            toSend[0] = (byte) (command.length & 0xFF);
            toSend[1] = (byte) ((command.length >> 8) & 0xFF);
            System.arraycopy(command, 0, toSend, 2, command.length);
            mSocket.getOutputStream().write(toSend, 0, 2 + command.length);
        }

        if (command[0] == (byte) 0x80) return null; // Response is not required
        return receivedResponses.get(command[1] & 0xFF).take();
    }
}
