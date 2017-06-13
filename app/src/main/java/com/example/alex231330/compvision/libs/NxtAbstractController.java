package com.example.alex231330.compvision.libs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Egor Suvorov on 25.07.2014.
 */
public abstract class NxtAbstractController implements NxtController {
    protected MessageWriteListener mMessageWriteListener = null;
    protected boolean mIsConnected;

    public NxtAbstractController() {
        mIsConnected = false;
    }

    @Override
    public boolean isConnected() {
        return mIsConnected;
    }

    @Override
    public void setMessageWriteListener(MessageWriteListener listener) {
        mMessageWriteListener = listener;
    }
}
