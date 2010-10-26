package uk.org.elsie.osgi.promises;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public interface PromisesService {
	Promise when(Object obj, Callback callback, Callback errback, Callback progressback);
	Promise when(Object obj, Callback callback, Callback errback);
	Promise when(Object obj, Callback callback);
	Promise whenEach(Object[] promises);
	Promise whenEach(Collection<Object> promises);
	Deferred defer();
	Promise reject(Object reason);
	Promise ref(Object obj);
	boolean isPromise(Object obj);
	boolean isFailure(Object obj);
	Object waitFor(Object obj) throws InterruptedException, ExecutionException;
	Object waitFor(Object obj, long timeout, TimeUnit units) throws InterruptedException, ExecutionException, TimeoutException;
	ScheduledExecutorService getScheduledExecutorService();
	void setScheduledExecutorService(ScheduledExecutorService executorService);
	void unsetScheduledExecutorService(ScheduledExecutorService executorService);
	Promise delay(long time, TimeUnit units);
	Promise delay(long time, TimeUnit units, Object input);
	FailureCollectorService getFailureCollectorService();
	void setFailureCollectorService(FailureCollectorService failureCollector);
	void unsetFailureCollectorService(FailureCollectorService failureCollector);
}
