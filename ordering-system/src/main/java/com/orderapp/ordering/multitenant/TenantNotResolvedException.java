package com.orderapp.ordering.multitenant;

public class TenantNotResolvedException extends RuntimeException {
	public TenantNotResolvedException(String message) {
		super(message);
	}
}

