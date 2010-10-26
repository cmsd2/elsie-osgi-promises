package uk.org.elsie.osgi.promises.internal;

import uk.org.elsie.osgi.promises.Callback;
import uk.org.elsie.osgi.promises.Promise;

public abstract class AbstractPromise implements Promise {

	@Override
	public Object addCallback(Callback cb) {
		return then(cb, null, null);
	}

	@Override
	public Object addErrback(Callback eb) {
		return then(null, eb, null);
	}

	@Override
	public Object addBoth(Callback cb, Callback eb) {
		return then(cb, eb, null);
	}

	@Override
	public Object addCallbacks(Callback cb, Callback eb) {
		return then(cb, eb, null);
	}
}
