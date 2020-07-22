package com.crossman;

import java.io.IOException;

public interface Authority {
	public boolean isAuthorized(Auth auth);
	public void setAuthorized(Auth auth) throws IOException;
}
