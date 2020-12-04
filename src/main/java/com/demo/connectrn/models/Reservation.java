package com.demo.connectrn.models;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POJO representing a customer's reservation of a particular table at a particular time. Also defines the record to be
 * persisted in DynamoDB.
 * 
 * Note: The time and table fields are chosen as the hash/range key in order to facilitate fast queries of tables by
 * time without needing a secondary index.
 * 
 * @author sean
 */
@DynamoDBTable(tableName = "Reservations")
@Schema(title = "A customer's reservation of a particular table at a particular time.")
public class Reservation {

	/** Handy single field identifier. */
	private String id;

	/** Hour for which the table is reserved. */
	@DynamoDBHashKey
	private int time;

	/** ID of the table being reserved. */
	@DynamoDBRangeKey
	private String table;

	/** Number of people to be occupying the reserved table. */
	private int persons;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public Reservation withTime(int time) {
		setTime(time);
		return this;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public int getPersons() {
		return persons;
	}

	public void setPersons(int persons) {
		this.persons = persons;
	}

	@Override
	public String toString() {
		return "Reservation [" + (id != null ? "id=" + id + ", " : "") + "time=" + time + ", "
				+ (table != null ? "table=" + table + ", " : "") + "persons=" + persons + "]";
	}

}