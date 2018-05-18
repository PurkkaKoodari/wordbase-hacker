package net.pietu1998.wordbasehacker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class RootShell implements AutoCloseable {

    private Process root;
    private Writer commandWriter;
    private BufferedReader resultReader;

    public RootShell() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("su");
        pb.redirectErrorStream(true);
        root = pb.start();
        commandWriter = new OutputStreamWriter(root.getOutputStream());
        resultReader = new BufferedReader(new InputStreamReader(root.getInputStream()));
    }

    public synchronized void sendCommand(String command) throws IOException {
        commandWriter.write(command + "\n");
        commandWriter.flush();
    }

    public synchronized String readResultLine() throws IOException {
        return resultReader.readLine();
    }

    @Override
    public void close() throws IOException {
        sendCommand("exit");
        final Thread current = Thread.currentThread();
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {}
            current.interrupt();
        }).start();
        try {
            root.waitFor();
        } catch (InterruptedException e) {
            root.destroy();
        }
        commandWriter.close();
        resultReader.close();
    }
}
