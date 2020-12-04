package com.demo.connectrn;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jvnet.hk2.annotations.Service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.demo.connectrn.models.Reservation;
import com.demo.connectrn.models.Table;

/**
 * Data Access Object for restaurant data being persisted in DynamoDB.
 * 
 * @author sean
 */
@Service
public class RestaurantDao {
	private static final Logger LOG = Logger.getLogger(RestaurantDao.class.getName());

	/**
	 * Reservable Tables. Note: This could be loaded from a config service, or another dynamodb table.
	 */
	private static final List<Table> RESERVABLE_TABLES = Arrays
			.asList(new Table("Apple", 1), new Table("Banana", 2), new Table("Cherry", 3), new Table("Date", 4));

	private DynamoDBMapper dynamoDbMapper;

	public RestaurantDao() {
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
		dynamoDbMapper = new DynamoDBMapper(client);
	}

	/** Get the list of all tables in the restaurant. */
	public List<Table> getReservableTables() {
		return RESERVABLE_TABLES;
	}

	/**
	 * Get the reservable tables which are NOT reserved at {@code time}
	 * 
	 * @param time Exclude tables already reserved at this time.
	 * @return A list (possibly empty) of unreserved tables. Never null.
	 */
	public List<Table> getFreeTablesAtTime(int time) {
		// Query reservations at that time.
		Reservation hashKeyHolder = new Reservation().withTime(time);
		PaginatedQueryList<Reservation> reservationsAtTime = dynamoDbMapper.query(Reservation.class,
				new DynamoDBQueryExpression<Reservation>().withHashKeyValues(hashKeyHolder));
		Set<String> bookedTableIds = reservationsAtTime.stream().map(Reservation::getTable).collect(Collectors.toSet());
		return getReservableTables().stream().filter(table -> !bookedTableIds.contains(table.getId()))
				.collect(Collectors.toList());
	}

	/** Fetch every record in a given table. */
	public <T> List<T> getAll(Class<T> clazz) {
		return dynamoDbMapper.scan(clazz, new DynamoDBScanExpression());
	}

	/**
	 * Save a new reservation to DynamoDB, but only if a record with the same keys (time & table) does not already exist
	 * 
	 * @param res New reservation record to save.
	 * @return True if successful, false if a conflicting reservation already exists.
	 */
	public boolean commitNewReservation(Reservation res) {
		try {
			// The save expression is checked atomically
			dynamoDbMapper.save(res,
					new DynamoDBSaveExpression().withExpected(
							Collections.singletonMap("id", new ExpectedAttributeValue().withExists(false))));
			return true;
		} catch (ConditionalCheckFailedException ex) {
			LOG.info("Attempt to save " + res + " failed, reservation already exists.");
			return false;
		}
	}

	/**
	 * Delete a reservation by ID.
	 * 
	 * @param reservationId ID of reservation to remove.
	 * @return True if record existed and was deleted, otherwise false.
	 */
	public boolean deleteReservation(String reservationId) {
		PaginatedScanList<Reservation> scanResult = dynamoDbMapper.scan(Reservation.class,
				new DynamoDBScanExpression().withFilterExpression("id = :id").withExpressionAttributeValues(
						Collections.singletonMap(":id", new AttributeValue().withS(reservationId))));
		if (scanResult.isEmpty()) {
			return false;
		} else {
			dynamoDbMapper.delete(scanResult.iterator().next()); // there will be only one.
			return true;
		}
	}

}
