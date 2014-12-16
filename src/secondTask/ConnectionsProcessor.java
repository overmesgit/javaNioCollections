package secondTask;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by user on 12/12/14.
 */
public class ConnectionsProcessor implements Runnable{
    private Selector requestSelector = Selector.open();
    private ByteBuffer buffer = ByteBuffer.allocate(100);

    private AtomicBoolean isWorked = new AtomicBoolean(true);

    ConnectionsProcessor() throws IOException {
    }

    public void stop() {
        isWorked.set(false);
    }

    public void addChannel(SocketChannel channel){
        try {
            channel.register(requestSelector, SelectionKey.OP_READ);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        List<Request> requestList = new ArrayList<>();
        while(isWorked.get()){
            int selectedKeysCount = 0;
            try {
                selectedKeysCount = requestSelector.select(100);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (selectedKeysCount > 0) {
                Iterator<SelectionKey> keyIterator = requestSelector.selectedKeys().iterator();
                while(keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    SocketChannel channel = (SocketChannel)key.channel();
                    if (key.isReadable()) {
                        Request request = processRequest(channel);
                        if (request != null) requestList.add(request);
                    }
                    keyIterator.remove();
                }
            }

            if (requestList.size() > 0 ) {
                sendResponse(requestList);
                requestList.clear();
            }
        }
    }

    private void sendResponse(List<Request> requestList) {
        Iterator<SelectionKey> keyIterator = requestSelector.keys().iterator();

        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            SocketChannel channel = (SocketChannel) key.channel();
            try {
                SocketAddress remoteAddress = channel.getRemoteAddress();
                for (Request request : requestList) {
                    if (!request.address.equals(remoteAddress)) {
                        byte[] bytes = request.message.getBytes(Charset.forName("UTF-8"));
                        ByteBuffer currentBuffer = ByteBuffer.allocate(bytes.length);
                        currentBuffer.put(bytes);
                        currentBuffer.flip();
                        channel.write(currentBuffer);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private Request processRequest(SocketChannel channel) {
        int bytesRead;
        Request result = null;

        try {
            buffer.clear();
            SocketAddress remoteAddress = channel.getRemoteAddress();
            if ((bytesRead = channel.read(buffer)) > 0) {
                buffer.flip();
                CharBuffer message = Charset.defaultCharset().decode(buffer);
                buffer.clear();
                result = new Request(remoteAddress, message.toString());
            }
            if (bytesRead < 0) {
                channel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;

    }
}

class Request {
    public SocketAddress address;
    public String message;

    Request(SocketAddress address, String message) {
        this.address = address;
        this.message = message;
    }
}
