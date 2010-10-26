package uk.org.elsie.osgi.promises.internal;

import uk.org.elsie.osgi.promises.Callback;

public class Listener {
	private Callback callback;
	private Callback errback;
	private Callback progressback;
	private DeferredImpl deferred;
	
	public Listener(Callback callback, Callback errback, Callback progressback, DeferredImpl deferred) {
		this.callback = callback;
		this.errback = errback;
		this.progressback = progressback;
		this.deferred = deferred;
	}
	
	public Callback getCallback() {
		return callback;
	}
	
	public Callback getErrback() {
		return errback;
	}
	
	public Callback getProgressback() {
		return progressback;
	}
	
	public DeferredImpl getDeferred() {
		return deferred;
	}
}
