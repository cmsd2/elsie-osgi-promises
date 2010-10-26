package uk.org.elsie.osgi.promises;

public interface Callback {
	public static Callback Identity = new Callback() {
		@Override
		public Object callback(Object input) {
			return input;
		}
	};

	public Object callback(Object input);
}
