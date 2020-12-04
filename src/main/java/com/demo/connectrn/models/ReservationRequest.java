package com.demo.connectrn.models;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request data structure for making reservations.
 * 
 * @author sean
 */
@Schema(title = "Request to make a reservation at a particular time for number of people")
public class ReservationRequest {
	private int time;
	private int persons;

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public ReservationRequest withTime(int time) {
		setTime(time);
		return this;
	}

	public int getPersons() {
		return persons;
	}

	public void setPersons(int persons) {
		this.persons = persons;
	}

	public ReservationRequest withPersons(int persons) {
		setPersons(persons);
		return this;
	}

	@Override
	public String toString() {
		return "ReservationRequest [time=" + time + ", persons=" + persons + "]";
	}

}