package com.ericdmartell.maga.utils;

public class MAGAException extends RuntimeException{
	public MAGAException(Exception e) {
		super(e);
	}
	public MAGAException(String msg, Exception e) {
		super(msg, e);
	}

	public MAGAException(String msg) {
		super(msg);
	}

}
