package async;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

public class Listener {

    private Queue<CompletableFuture<Object>> waitings = new ConcurrentLinkedQueue<>();

    public void receive(Object input) {
        final CompletableFuture<Object> poll = waitings.poll();
        if (poll != null) {
            poll.complete(input);
        }
    }

    public Future<Object> getNext() {
        final CompletableFuture<Object> future = new CompletableFuture<>();
        waitings.add(future);
        return future;
    }
}
