/*******************************************************************************
 * Copyright (c) 2013 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.acmeair.web;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.acmeair.securityutils.SecurityUtils;
import com.acmeair.service.FlightService;

@RestController
@RequestMapping("/")
public class FlightServiceRest {

	@Autowired
	private FlightService flightService;

	@Autowired
	private SecurityUtils secUtils;

	@Value("${ms.hw}")
	private Float hw;

	private static final AtomicInteger users = new AtomicInteger(0);

	/**
	 * Get flights.
	 */

	@RequestMapping(value = "/queryflights", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String getTripFlights(@RequestParam String fromAirport, @RequestParam String toAirport,
			@RequestParam Date fromDate, @RequestParam Date returnDate, @RequestParam Boolean oneWay)
			throws ParseException {

		String options = "";
		
		System.out.println(fromAirport);
		System.out.println(toAirport);
		System.out.println(fromDate);
		System.out.println(returnDate);

		List<String> toFlights = flightService.getFlightByAirportsAndDepartureDate(fromAirport, toAirport, fromDate);

		if (!oneWay) {

			List<String> retFlights = flightService.getFlightByAirportsAndDepartureDate(toAirport, fromAirport,
					returnDate);

			// TODO: Why are we doing it like this?
			options = "{\"tripFlights\":" + "[{\"numPages\":1,\"flightsOptions\": " + toFlights
					+ ",\"currentPage\":0,\"hasMoreOptions\":false,\"pageSize\":10}, "
					+ "{\"numPages\":1,\"flightsOptions\": " + retFlights
					+ ",\"currentPage\":0,\"hasMoreOptions\":false,\"pageSize\":10}], " + "\"tripLegs\":2}";
		} else {
			options = "{\"tripFlights\":" + "[{\"numPages\":1,\"flightsOptions\": " + toFlights
					+ ",\"currentPage\":0,\"hasMoreOptions\":false,\"pageSize\":10}], " + "\"tripLegs\":1}";
		}
		
		this.doWork(90l);
		return options;
	}

	/**
	 * Get reward miles for flight segment.
	 */
	@RequestMapping(value = "/getrewardmiles", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = "application/json")
	public RewardMilesResponse getRewardMiles(@RequestHeader(name = "acmeair-id", required = false) String headerId,
			@RequestHeader(name = "acmeair-date", required = false) String headerDate,
			@RequestHeader(name = "acmeair-sig-body", required = false) String headerSigBody,
			@RequestHeader(name = "acmeair-signature", required = false) String headerSig,
			@RequestParam String flightSegment

	) {

		if (secUtils.secureServiceCalls()) {
			String body = "flightSegment=" + flightSegment;
			secUtils.verifyBodyHash(body, headerSigBody);
			secUtils.verifyFullSignature("POST", "/getrewardmiles", headerId, headerDate, headerSigBody, headerSig);
		}

		Long miles = flightService.getRewardMiles(flightSegment);
		RewardMilesResponse result = new RewardMilesResponse();
		result.miles = miles;
		
		this.doWork(50l);
		return result;
	}

	@RequestMapping("/")
	public String checkStatus() {
		return "OK";
	}
	
	private void doWork(long stime) {
		FlightServiceRest.users.incrementAndGet();
		Double isTime = Long.valueOf(stime).doubleValue();
		Float d = (float) (isTime.floatValue() * (FlightServiceRest.users.floatValue() / this.hw));
		try {
			TimeUnit.MILLISECONDS.sleep(Math.max(Math.round(d), Math.round(isTime)));
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			FlightServiceRest.users.decrementAndGet();
		}
	}
}