package com.demo.connectrn.api;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.demo.connectrn.RestaurantDao;
import com.demo.connectrn.models.Reservation;
import com.demo.connectrn.models.ReservationRequest;
import com.demo.connectrn.models.ReservationResponse;
import com.demo.connectrn.models.Table;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

// Simplifying Assumptions:
// - Parties don't want to be split across different tables.
// - Parties don't want to share a table with another party.
// - Can't choose arbitrary start time, only on the hour.
// - Can't choose end time, we allocate 1 hour for all reservations.
// - We simulate only a single day. The pattern could be easily extended by adding more records for each day.
// - No authentication. With only a single customer in this scenario, and customer/owner both having full
// rights, we can ignore it for this exercise. Also the reason for omitting fields tracking which customer made
// the reservation.

// Note: I chose to use DynamoDB to store reservations in order to highlight some of the atomic update
// capabilities it does have that I mentioned earlier. (However whether or not it's cost effective is another
// matter) I could imagine a similar mechanism being used to create a ShiftAssignment record (or to update a
// ShiftRequest record status to FILLED) conditionally based on it not already being filled. If multiple records
// need to be atomically updated at the same time, DynamoDB transactions would be used.

@Path("/v1")
public class ReservationApi {

	private static final Logger LOG = Logger.getLogger(ReservationApi.class.getName());

	@Inject
	private RestaurantDao dao;

	/**
	 * Make one or more reservations. Each reservation succeeds or fails independently, check return value to see which
	 * ones were successful.
	 *
	 * Note: If we wanted them to succeed or fail as a group, we could use DynamoDB transactions instead of a simple
	 * atomic conditional save.
	 * 
	 * @param reservationRequests List of requests to attempt.
	 * @return
	 */
	@PUT
	@Operation(summary = "Make one or more reservations.")
	@Path("/reservations")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ReservationResponse makeReservations(List<ReservationRequest> reservationRequests) {
		ReservationResponse response = new ReservationResponse();
		for (ReservationRequest req : reservationRequests) {
			Reservation res = makeReservation(req);
			if (res != null) {
				response.getSuccessful().add(res);
			} else {
				response.getFailed().add(req);
			}
		}
		return response;
	}

	/**
	 * Make a reservation, attempting three times if persisting failes due to concurrent updates or inconsistent reads.
	 * 
	 * @param req Reservation Request
	 * @return Reservation request that was persisted if successful, {@code null} if couldn't make the reservation.
	 */
	protected Reservation makeReservation(ReservationRequest req) {

		// Try three times in case of concurrent update or stale reads.
		for (int attempt = 0; attempt < 3; attempt++) {
			// Fetch available tables at this time.
			List<Table> freeTables = dao.getFreeTablesAtTime(req.getTime());

			// Choose the smallest table that will fit the party.
			Optional<Table> chosenTable = freeTables.stream().sorted(Comparator.comparing(Table::getCapacity))
					.filter(table -> table.getCapacity() >= req.getPersons()).findFirst();
			if (!chosenTable.isPresent()) {
				// No table big enough is available at that time!
				return null; // Bail out right away, no need to retry in this case.
			} else {
				// Try to actually make the reservation!
				Reservation res = new Reservation();
				res.setId(UUID.randomUUID().toString());
				res.setPersons(req.getPersons());
				res.setTime(req.getTime());
				res.setTable(chosenTable.get().getId());
				if (dao.commitNewReservation(res)) {
					return res; // Success!
				} else {
					continue; // Retry
				}
			}
		}
		LOG.warning("Giving up after third attempt to commit a reservation.");
		return null;
	}

	/**
	 * Cancel a specific reservation.
	 * 
	 * @param id ID of the reservation to cancel.
	 */
	@DELETE
	@Path("/reservations/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Cancel a specific reservation")
	@ApiResponses({ @ApiResponse(responseCode = "204", description = "Cancellation was successful."),
			@ApiResponse(responseCode = "404", description = "Reservation was not found to be cancelled.") })
	public Response cancelReservation(@PathParam("id") String id) {
		if (dao.deleteReservation(id)) {
			return Response.noContent().build();
		} else {
			return Response.status(Status.NOT_FOUND).build();
		}
	}

	/**
	 * Get current user's reservations.
	 * 
	 * @return List (possibly empty) of reservations made by the current user.
	 */
	@GET
	@Path("/users/me/reservations")
	@Operation(summary = "Get current user's reservations.")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Reservation> getCurrentReservations() {
		// Since we have only one possible user, ALL reservations are this customer's reservations.
		return dao.getAll(Reservation.class);
	}

	/**
	 * Check which tables are free and of what capacity.
	 * 
	 * @param time Point in time at which to check table status.
	 * @return List of tables (possibly empty) that are free at {@code time}.
	 */
	@GET
	@Path("/tables/free")
	@Operation(summary = "Check which tables are free and of what capacity.")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Table> getFreeTables(@QueryParam("time") int time) {
		return dao.getFreeTablesAtTime(time);
	}

}
