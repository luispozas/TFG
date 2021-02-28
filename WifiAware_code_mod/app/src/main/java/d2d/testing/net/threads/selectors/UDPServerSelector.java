package d2d.testing.net.threads.selectors;

import android.net.Network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Iterator;

import d2d.testing.MainActivity;
import d2d.testing.utils.Logger;
import d2d.testing.net.threads.workers.EchoWorker;

import static java.lang.Thread.sleep;

public class UDPServerSelector extends AbstractSelector {
    private DatagramChannel mDatagramChannel;
    private int mPortUDP;
    private InetAddress mLocalAddress;

    public UDPServerSelector(MainActivity mainActivity, InetAddress localAddress, int port) throws IOException {
        super(mainActivity, RTSPServerSelector.getInstance().mConManager);
        mPortUDP = port;
        mLocalAddress = localAddress;
        mWorker = new EchoWorker();
        mWorker.start();
    }


    @Override
    protected void onClientDisconnected(SelectableChannel socketChannel) {

    }

    @Override
    protected void initiateConnection() {
        try {
            Network net = RTSPServerSelector.getInstance().mLastNet;
            if(!mConManager.bindProcessToNetwork(net)) throw new IOException("Error bind to net");
            mDatagramChannel = (DatagramChannel) DatagramChannel.open().configureBlocking(false);
            mDatagramChannel.socket().bind(new InetSocketAddress(mLocalAddress, mPortUDP));
            mStatusUDP = STATUS_LISTENING;
            this.addChangeRequest(new ChangeRequest(mDatagramChannel, ChangeRequest.REGISTER, SelectionKey.OP_READ));
            mConManager.bindProcessToNetwork(null);
            Logger.d("ClientSelector: initiateConnection as server listening UDP on port " + mLocalAddress.getHostAddress() + ":" + mPortUDP);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SelectableChannel addConnectionUDP(InetAddress address, int port) throws IOException {

            DatagramChannel datagramChannel =  (DatagramChannel) DatagramChannel.open().configureBlocking(false);
            datagramChannel.connect(new InetSocketAddress(address.getHostAddress(), port));
            addChangeRequest(new ChangeRequest(datagramChannel, ChangeRequest.REGISTER, SelectionKey.OP_READ));
            mConnections.add(datagramChannel);

            Logger.d("ClientSelector: initiateConnection UDP client 'connected' to " + address.getHostAddress() + ":" + port);

        return datagramChannel;
    }

    @Override
    public void send(byte[] data) {
        Logger.d("UDPServerSelector: sending " + data.length + "bytes to " + mConnections.size());
        for (SelectableChannel socket : mConnections) {
            this.send(socket,data);
        }
    }
}