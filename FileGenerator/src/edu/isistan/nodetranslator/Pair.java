package edu.isistan.nodetranslator;

public class Pair<T, D> {

	private T time;
	private D data;
	
	public Pair(T time, D data) {
		super();
		this.time = time;
		this.data = data;
	}

	public T getTime() {
		return this.time;
	}

	public void setTime(T time) {
		this.time = time;
	}

	public D getData() {
		return this.data;
	}

	public void setData(D data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return this.time + ";" + this.data;
	}
}
