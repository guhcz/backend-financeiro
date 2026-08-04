package com.personal.backend_financeiro.exception;

public class ResourceInUseException extends RuntimeException {

	public ResourceInUseException(String message) {
		super(message);
	}

}
