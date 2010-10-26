package uk.org.elsie.osgi.promises;

import java.util.concurrent.Future;

public interface Promise extends Future<Object>, Canceller {
	Object then(Callback callback, Callback errback, Callback progressBack);
	
	Object then(Callback callback, Callback errback);
	
	Object then(Callback callback);

	Object addCallback(Callback cb);
	
	Object addErrback(Callback eb);
	
	Object addBoth(Callback cb, Callback eb);
	
	Object addCallbacks(Callback cb, Callback eb);
}
