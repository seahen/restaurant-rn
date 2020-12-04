package com.demo.connectrn.models;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response data structure packaging which reservation requests were successful and which were not.
 * 
 * @author sean
 */
@Schema(title = "Response to reservation requests.")
public class ReservationResponse {

	private List<Reservation> successful = new ArrayList<>();
	private List<ReservationRequest> failed = new ArrayList<>();

	public List<Reservation> getSuccessful() {
		return successful;
	}

	public void setSuccessful(List<Reservation> successful) {
		this.successful = successful;
	}

	public List<ReservationRequest> getFailed() {
		return failed;
	}

	public void setFailed(List<ReservationRequest> failed) {
		this.failed = failed;
	}

	@Override
	public String toString() {
		return "ReservationResponse [" + (successful != null ? "successful=" + successful + ", " : "")
				+ (failed != null ? "failed=" + failed : "") + "]";
	}

}
