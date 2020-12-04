package com.demo.connectrn.models;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Simple POJO representing a reservable table in the restaurant.
 * 
 * @author sean
 */
@Schema(title = "Information about an available table")
public class Table {
	private String id;
	private int capacity;

	public Table(String id, int capacity) {
		this.id = id;
		this.capacity = capacity;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public String toString() {
		return "Table [" + (id != null ? "id=" + id + ", " : "") + "capacity=" + capacity + "]";
	}

}