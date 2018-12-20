package async;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;


public class TestListener {

    public static final int DELIVERER_DELAY = 5;
    private Listener subject;
    private Executor executor;


    @Before
    public void setUp() {
        subject = new Listener();
        executor = Executors.newSingleThreadExecutor();
    }
    private class Deliverer implements Runnable {
        private final Object toDeliver;
        private Exception e;

        private Deliverer(Object toDeliver) {
            this.toDeliver = toDeliver;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(DELIVERER_DELAY);
                TestListener.this.subject.receive(toDeliver);
            } catch (Exception e) {
                this.e = e;
            }
        }

        public Exception getE() {
            return e;
        }
    }

    @Test
    public void shouldRetrieveAsynchronouslyPassedObject() throws InterruptedException, ExecutionException, TimeoutException {
        final Future<Object> next = subject.getNext();

        final Object data = new Object();
        executor.execute(new Deliverer(data));

        final Object o = next.get(DELIVERER_DELAY * 2, TimeUnit.MILLISECONDS);
        assertThat(o).isEqualTo(o);
    }


    @Test
    public void shouldNotFailOnNoRegistrations() throws InterruptedException {
        final Object data = new Object();

        final Deliverer command = new Deliverer(data);
        executor.execute(command);
        Thread.sleep(DELIVERER_DELAY * 2);
        assertThat(command.getE()).isNull();
    }

    @Test
    public void shouldRetrieveObjectsInSequence() throws InterruptedException, ExecutionException, TimeoutException {
        final int endExclusive = 5;
        final List<Future<Object>> results = IntStream.range(0, endExclusive).boxed()
                .map(it -> subject.getNext())
                .collect(Collectors.toList());

        final List<Object> data = IntStream.range(0, endExclusive).boxed()
                .map(it -> new Object())
                .collect(Collectors.toList());


        data.stream().map(Deliverer::new).forEach(executor::execute);

        for (int i = 0; i < results.size(); i++) {
            final Future<Object> objectFuture = results.get(i);
            assertThat(objectFuture.get(DELIVERER_DELAY * 2, TimeUnit.MILLISECONDS)).isEqualTo(data.get(i));
        }
    }

    @Test(expected = TimeoutException.class)
    public void shouldTimeoutOnGetIfNoDataIsSent() throws InterruptedException, ExecutionException, TimeoutException {
        final Future<Object> result = subject.getNext();
        result.get(DELIVERER_DELAY * 2, TimeUnit.MILLISECONDS);
    }

}
