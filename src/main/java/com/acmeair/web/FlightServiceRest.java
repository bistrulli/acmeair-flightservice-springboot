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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

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

import ctrlmnt.ControllableService;
import ctrlmnt.CtrlMNT;
import ctrlmnt.MonitoringThread;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/")
public class FlightServiceRest extends ControllableService {

	private static final AtomicInteger users = new AtomicInteger(0);

	private static final Logger logger = Logger.getLogger(FlightServiceRest.class.getName());

	@Autowired
	private FlightService flightService;

	@Autowired
	private SecurityUtils secUtils;

	@Value("${ms.hw}")
	private Float hw;

	@Value("${ms.name}")
	private String msname;

	@Value("${ms.iscgroup}")
	private String iscgroup;

	@Value("${ms.stime.queryflights}")
    private long stimeQueryFlights;

	@Value("${ms.stime.getrewardmiles}")
    private long stimeGetRewardMiles;

    private MonitoringThread monitor = null;

	public FlightServiceRest() {
		CtrlMNT mnt = new CtrlMNT(this);
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(mnt, 0, 50, TimeUnit.MILLISECONDS);

		monitor = new MonitoringThread();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(monitor, 0, 30, TimeUnit.SECONDS);
	}

	/**
	 * Get flights.
	 */
	@RequestMapping(value = "/queryflights", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String getTripFlights(@RequestParam String fromAirport, @RequestParam String toAirport,
			@RequestParam Date fromDate, @RequestParam Date returnDate, @RequestParam Boolean oneWay)
			throws ParseException {

		ControllableService.activeRequests.incrementAndGet();
		long startTime = System.currentTimeMillis(); // TODO nanotime

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

		this.doWork(this.stimeQueryFlights);
		
		logger.info("New request arrived. Total:" + ControllableService.requestCount.addAndGet(1));
		long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime; // Elapsed time in milliseconds
        logger.info("Single request service time: " + elapsedTime + " ms");

        logger.info("Current serviceTimeSum: " + ControllableService.serviceTimesSum.addAndGet(elapsedTime) + " ms");
        ControllableService.activeRequests.decrementAndGet();

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

		ControllableService.activeRequests.incrementAndGet();
		long startTime = System.currentTimeMillis(); // TODO nanotime

		if (secUtils.secureServiceCalls()) {
			String body = "flightSegment=" + flightSegment;
			secUtils.verifyBodyHash(body, headerSigBody);
			secUtils.verifyFullSignature("POST", "/getrewardmiles", headerId, headerDate, headerSigBody, headerSig);
		}

		Long miles = flightService.getRewardMiles(flightSegment);
		RewardMilesResponse result = new RewardMilesResponse();
		result.miles = miles;

		this.doWork(this.stimeGetRewardMiles);

		logger.info("New request arrived. Total:" + ControllableService.requestCount.addAndGet(1));
		long endTime = System.currentTimeMillis();
	    long elapsedTime = endTime - startTime; // Elapsed time in milliseconds
	    logger.info("Single request service time: " + elapsedTime + " ms");

	    logger.info("Current serviceTimeSum: " + ControllableService.serviceTimesSum.addAndGet(elapsedTime) + " ms");
	    ControllableService.activeRequests.decrementAndGet();
		
		return result;
	}

	@RequestMapping("/")
	public String checkStatus() {
		return "OK";
	}

	@Override
	public Float getHw() {
		return this.hw;
	}

	@Override
	public void setHw(Float hw) {
		this.hw = hw;
	}

	@Override
	public String getName() {
		return this.msname;
	}

	@Override
	public void egress() {
		FlightServiceRest.users.decrementAndGet();
	}

	@Override
	public Integer getUser() {
		return FlightServiceRest.users.get();
	}

	@Override
	public void ingress() {
		FlightServiceRest.users.incrementAndGet();
	}

	public String getIscgroup() {
		return iscgroup;
	}
}