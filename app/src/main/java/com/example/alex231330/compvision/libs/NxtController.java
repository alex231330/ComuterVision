package com.example.alex231330.compvision.libs;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Egor Suvorov on 25.07.2014.
 */
public interface NxtController {
    final public int MAX_DIRECT_COMMAND_LENGTH = 64;

    void setMessageWriteListener(MessageWriteListener listener);

    void connect() throws IOException;

    void disconnect();

    boolean isConnected();

    public interface MessageWriteListener {
        public void onMessageWrite(NxtBluetoothController controller, byte[] message);
    }

    public abstract byte[] sendDirectCommand(byte[] command) throws IOException, InterruptedException;
}
