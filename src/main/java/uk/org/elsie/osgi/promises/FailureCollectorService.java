package uk.org.elsie.osgi.promises;

import java.lang.ref.Reference;

public interface FailureCollectorService {

	public abstract Reference<Object> failed(Object failure);

	public abstract void collected(Reference<Object> ref);

}