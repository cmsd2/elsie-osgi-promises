package uk.org.elsie.osgi.promises;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.org.elsie.osgi.promises.internal.DeferredImpl;
import uk.org.elsie.osgi.promises.internal.Progress;

public class PromisesServiceImpl implements PromisesService {
	
	private static Log log = LogFactory.getLog(PromisesServiceImpl.class);

	private ScheduledExecutorService executor;
	private boolean rejectImmediately = false;
	private FailureCollectorService failureCollector;
	
	public PromisesServiceImpl() {
		log.info("Creating promises service");
	}
	
	public Object perform(Promise obj, Callback async) {
		try {
			Object value;
			value = async.callback(obj);
			if(isPromise(value)) {
				return value;
			} else {
				Deferred d = defer();
				d.resolve(value);
				return d.getPromise();
			}
		} catch (Exception e) {
			Deferred d = defer();
			d.reject(e);
			return d.getPromise();
		}
	}
	
	public Object whenPromise(Promise obj, final Callback resolvedcallback, final Callback rejectback, final Callback progressback) {
		return perform(obj, new Callback() {
				@Override
				public Object callback(Object input) {
					return ((Promise)input).then(resolvedcallback, rejectback, progressback);
				}
			});
	}
	
	public Promise when(Object obj, Callback resolvedcallback, Callback errback, Callback progressback) {
		if(isPromise(obj)) {
			return ref(whenPromise((Promise) obj, resolvedcallback, errback, progressback));
		} else if(isFailure(obj)) {
			return ref(errback.callback(obj));
		} else {
			return ref(resolvedcallback.callback(obj));
		}
	}
	
	public Promise when(Object obj, Callback resolvedcallback, Callback errback) {
		return when(obj, resolvedcallback, errback, null);
	}
	
	public Promise when(Object obj, Callback resolvedcallback) {
		return when(obj, resolvedcallback, null, null);
	}
	
	public Promise whenEach(final Object[] promises) {
		final DeferredImpl d = defer();
		final int[] fulfilled = new int[] { 0 };
		final Object[] results = new Object[promises.length];
		final int[] i = new int[1];

		for(i[0] = 0; i[0] < promises.length; i[0]++) {
			when(promises[i[0]], new Callback() {
				@Override
				public Object callback(Object input) {
					results[i[0]] = input;
					fulfilled[0]++;
					if(d.getProgressCallback() != null)
						d.getProgressCallback().callback(new Progress(fulfilled[0], promises.length));
					if(fulfilled[0] == promises.length) {
						d.resolve(results);
					}
					return null;
				}
			}, new Callback() {
				@Override
				public Object callback(Object input) {
					d.reject(input);
					return null;
				}
				
			}, null);
		}
		return d.getPromise();
	}
	
	public Promise whenEach(Collection<Object> promises) {
		return whenEach(promises.toArray());
	}
	
	public Object waitFor(Object obj) throws InterruptedException,
			ExecutionException {
		try {
			return waitFor(obj, 0, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			throw new RuntimeException("this shouldn't happen", e);
		}
	}
	
	public Object waitFor(Object obj, long timeout, TimeUnit units) throws InterruptedException,
			ExecutionException, TimeoutException {
		if(isPromise(obj)) {
			final Object monitor = new Object();
			final boolean[] finished = new boolean[] { false };
			final boolean[] error = new boolean[] { false };
			final Object[] result = new Object[] { null };
			
			((Promise) obj).then(new Callback() {
				@Override
				public Object callback(Object value) {
					synchronized (monitor) {
						finished[0] = true;
						result[0] = value;
						monitor.notify();
					}
					return null;
				}	
			}, new Callback() {
				@Override
				public Object callback(Object value) {
					synchronized (monitor) {
						finished[0] = error[0] = true;
						result[0] = value;
						monitor.notify();
					}
					return null;
				}	
			}, null);
			
			if(timeout == 0) {
				synchronized (monitor) {
					while(!finished[0]) {
						monitor.wait();
					}
				}
			} else {
				long millis = TimeUnit.MILLISECONDS.convert(timeout, units);

				synchronized (monitor) {
					if(!finished[0]) {
						monitor.wait(millis);
					}
				}

				if(!finished[0]) {
					throw new TimeoutException();
				}
			}

			return result[0];
		} else {
			return obj;
		}
	}

	public DeferredImpl defer() {
		return createDeferred(null);
	}
	
	public Deferred defer(Canceller canceller) {
		return createDeferred(canceller);
	}

	public Promise reject(Object reason) {
		Deferred d = defer();
		d.reject(reason);
		return d.getPromise();
	}

	public Promise ref(Object obj) {
		if(isPromise(obj)) {
			return (Promise)obj;
		} else {
			Deferred d = defer();
			d.resolve(obj);
			return d.getPromise();
		}
	}

	public boolean isPromise(Object obj) {
		return obj instanceof Promise;
	}
	
	public boolean isFailure(Object obj) {
		return obj instanceof Failure;
	}
	
	protected DeferredImpl createDeferred(Canceller canceller) {
		return new DeferredImpl(this, canceller, rejectImmediately);
	}
	
	public synchronized ScheduledExecutorService getScheduledExecutorService() {
		return executor;
	}
	
	public synchronized void setScheduledExecutorService(ScheduledExecutorService executor) {
		log.info("Setting scheduled executor");
		this.executor = executor;
	}
	
	public synchronized void unsetScheduledExecutorService(ScheduledExecutorService executor) {
		if(this.executor == executor) {
			log.info("Unsetting scheduled executor");
			this.executor = null;
		}
	}
	
	public boolean getRejectImmediately() {
		return rejectImmediately;
	}
	
	public synchronized FailureCollectorService getFailureCollectorService() {
		return failureCollector;
	}
	
	public synchronized void setFailureCollectorService(FailureCollectorService failureCollector) {
		log.info("Set failure-collector");
		this.failureCollector = failureCollector;
	}
	
	public synchronized void unsetFailureCollectorService(FailureCollectorService failureCollector) {
		if(this.failureCollector == failureCollector) {
			log.info("Unset failure-collector");
			this.failureCollector = null;
		}
	}
	
	/**
	 * In a multithreaded environment setting this to true
	 * introduces a race between the thread creating the
	 * promise and adding callbacks, and the thread running
	 * them.
	 * In the case that a promise is rejected before any
	 * error-backs are added, if this is true, the failure
	 * would be raised immediately and get missed by any
	 * error callback added afterward.
	 * If this is left as false, failures wait to be collected.
	 * To help detect this, a failure collector prints
	 * uncollected failures when they're ready for GC.
	 * @param reject
	 */
	public void setRejectImmediately(boolean reject) {
		this.rejectImmediately = reject;
	}
	
	public Promise delay(long time, TimeUnit units) {
		return delay(time, units, null);
	}

	public Promise delay(long delay, TimeUnit units, final Object input) {
		final Deferred d = defer();
		final ScheduledFuture<Object> future = executor.schedule(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				log.debug("delayed task called");
				d.resolve(input);
				return null;
			}
		}, delay, units);
		d.setCanceller(new Canceller() {
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return future.cancel(mayInterruptIfRunning);
			}
		});
		
		return d.getPromise();
	}
}
