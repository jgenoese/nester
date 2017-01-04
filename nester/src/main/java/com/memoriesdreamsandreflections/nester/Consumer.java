package com.memoriesdreamsandreflections.nester;

public interface Consumer<R>
{
	public void handleRollup(Object[] categories, R row);
	
	public void handleDetail(Object[] categories, R row);
}
