package me.xethh.tools.jvault;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Scanner;

public class LockForWaiting {
    private final Thread thread;
    private final PipedOutputStream os;

    public LockForWaiting() {
        try {
            var is = new PipedInputStream();
            os = new PipedOutputStream(is);
            thread = new Thread(() -> {
                var scanner = new Scanner(is);
                scanner.nextLine();
            });
            thread.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void done() {
        try {
            this.os.write("Done".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean waitForComplete() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return true;

    }

    public static void main(String[] args) throws InterruptedException {
        LockForWaiting lockForWaiting = new LockForWaiting();
        new Thread(() -> {
            lockForWaiting.waitForComplete();
            System.out.println(lockForWaiting.waitForComplete());
        }).start();
        var t = new Thread(() -> {
            try {
                Thread.sleep(Duration.ofSeconds(3).toMillis());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lockForWaiting.done();
        });
        t.start();
        t.join();
        System.out.println("Finished");
    }


}
