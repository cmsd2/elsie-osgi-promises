package uk.org.elsie.osgi.promises.internal;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.org.elsie.osgi.promises.Callback;
import uk.org.elsie.osgi.promises.Canceller;
import uk.org.elsie.osgi.promises.Deferred;
import uk.org.elsie.osgi.promises.FailureCollectorService;
import uk.org.elsie.osgi.promises.Promise;
import uk.org.elsie.osgi.promises.PromisesService;

public class DeferredImpl implements Future<Object>, Canceller, Deferred {
	private static Log log = LogFactory.getLog(DeferredImpl.class);

	private boolean isError = false;
	private boolean rejectImmediately;
	private Canceller canceller;
	private Promise promise;
	private PromisesService promises;
	private volatile boolean cancelled = false;
	private volatile boolean finished = false;
	private Object result = null;
	private List<Listener> waiting = new ArrayList<Listener> ();
	private Reference<Object> collectable = null;
	
	private class DeferredPromise extends AbstractPromise {
		
		public DeferredPromise() {
		}

		@Override
		public Object then(Callback callback, Callback errback,
				Callback progressback) {
			return DeferredImpl.this.then(callback, errback, progressback);
		}
		
		@Override
		public Object then(Callback callback, Callback errback) {
			return DeferredImpl.this.then(callback, errback, null);
		}
		
		@Override
		public Object then(Callback callback) {
			return DeferredImpl.this.then(callback, null, null);
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return DeferredImpl.this.cancel(mayInterruptIfRunning);
		}

		@Override
		public Object get() throws InterruptedException, ExecutionException {
			return DeferredImpl.this.get();
		}

		@Override
		public Object get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			return DeferredImpl.this.get(timeout, unit);
		}

		@Override
		public boolean isCancelled() {
			return DeferredImpl.this.isCancelled();
		}

		@Override
		public boolean isDone() {
			return DeferredImpl.this.isDone();
		}
	}
	
	public DeferredImpl(PromisesService promises, Canceller canceller, boolean rejectImmediately) {
		this.rejectImmediately = rejectImmediately;
		this.promises = promises;
		this.canceller = canceller;
		this.promise = new DeferredPromise();
	}
	
	public synchronized void notifyAllListeners(Object value) {
		if(finished) {
			throw new RuntimeException("already finished");
		}
		
		result = value;
		finished = true;
		FailureCollectorService failureCollector = promises.getFailureCollectorService();

		if(isError && waiting.isEmpty()) {
			if(rejectImmediately) {
				throw new RuntimeException(result.toString(), (result instanceof Exception) ? ((Exception)result) : null);
			} else if(failureCollector != null) {
				collectable = failureCollector.failed(result);
			} else {
				log.warn("Uncollected error" + result.toString(), (result instanceof Exception) ? ((Exception)result) : null);
			}
		}
		
		for(Listener o : waiting) {
			notifyListener(o);
		}
	}

	protected void notifyListener(final Listener listener) {
		final Callback cb = !isError ? listener.getCallback() : listener.getErrback();
		
		FailureCollectorService failureCollector = promises.getFailureCollectorService();
		if(isError && collectable != null && failureCollector != null) {
			failureCollector.collected(collectable);
			collectable = null;
		}
		
		if(cb != null) {
			enqueue(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					try {
						Object newResult = cb.callback(result);
						if(newResult != null && promises.isPromise(newResult)) {
							((Promise) newResult).then(
									listener.getDeferred().getResolveCallback(), 
									listener.getDeferred().getRejectCallback(),
									listener.getDeferred().getProgressCallback());
						} else if(promises.isFailure(newResult)) {
							listener.getDeferred().reject(newResult);
						} else {
							listener.getDeferred().resolve(newResult);
						}
					} catch (Exception e) {
						listener.getDeferred().reject(e);
					}
					return null;
				}
			});
		} else {
			if(isError) {
				listener.getDeferred().reject(result);
			} else {
				listener.getDeferred().resolve(result);
			}
		}
	}
	
	public Callback getResolveCallback() {
		return new Callback() {
			@Override
			public Object callback(Object input) {
				DeferredImpl.this.resolve(input);
				return null;
			}
		};
	}
	
	public Callback getRejectCallback() {
		return new Callback() {
			@Override
			public Object callback(Object input) {
				DeferredImpl.this.reject(input);
				return null;
			}
		};
	}
	
	public Callback getProgressCallback() {
		return new Callback() {
			@Override
			public Object callback(Object input) {
				DeferredImpl.this.progress(input);
				return null;
			}
		};
	}

	public void resolve(Object value) {
		notifyAllListeners(value);
	}
	
	public synchronized void rejectNow(Object reason) {
		isError = true;
		notifyAllListeners(reason);
	}
	
	public void rejectLater(final Object reason) {
		enqueue(new Callable<Object> () {
			@Override
			public Object call() throws Exception {
				rejectNow(reason);
				return null;
			}
		});
	}

	public void reject(Object reason) {
		if(rejectImmediately) {
			rejectNow(reason);
		} else {
			rejectLater(reason);
		}
	}
	
	public synchronized void progress(Object update) {
		for(Listener o : waiting) {
			Callback cb = o.getProgressback();
			if(cb != null) {
				cb.callback(update);
			}
		}
	}

	public void setCanceller(Canceller canceller) {
		this.canceller = canceller;
	}
	
	public synchronized boolean cancel(boolean mayInterruptIfRunning) {
		if(canceller != null && !cancelled && !finished) {
			Object result = "cancelled";
			try {
				cancelled = canceller.cancel(mayInterruptIfRunning);
			} catch (Exception e) {
				cancelled = true;
				result = e;
			}

			if(cancelled) {
				rejectNow(result);
			}
			return true;
		} else {
			return false;
		}
	}
	
	public synchronized Object then(Callback resolvedCallback, Callback errorCallback, Callback progressCallback) {
		DeferredImpl retDeferred = new DeferredImpl(promises, promise, rejectImmediately);
		Listener listener = new Listener(resolvedCallback, errorCallback, progressCallback, retDeferred);
		if(finished) {
			notifyListener(listener);
		} else {
			waiting.add(listener);
		}
		return retDeferred.getPromise();
	}
	
	public Object then(Callback resolvedCallback) {
		return then(resolvedCallback, null, null);
	}
	
	public Object then(Callback resolvedCallback, Callback errorCallback) {
		return then(resolvedCallback, errorCallback, null);
	}

	public Promise getPromise() {
		return promise;
	}

	public Object get() throws InterruptedException, ExecutionException {
		return promises.waitFor(promise);
	}

	public Object get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		return promises.waitFor(promise, timeout, unit);
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public boolean isDone() {
		return finished;
	}
	
	private Future<Object> enqueue(Callable<Object> callable) {
		return promises.getScheduledExecutorService().submit(callable);
	}
}
