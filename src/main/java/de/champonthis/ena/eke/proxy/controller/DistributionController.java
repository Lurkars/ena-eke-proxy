/**
 * 
 */
package de.champonthis.ena.eke.proxy.controller;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.net.MediaType;

import de.champonthis.ena.eke.proxy.buisnesslogic.DiagnosisKeyManager;
import de.champonthis.ena.eke.proxy.model.DiagnosisKey;

/**
 * @author Lurkars
 *
 */
@Controller
@RequestMapping("/version/v1/diagnosis-keys/country/{countryCode}/date/{dateString}")
public class DistributionController {

	@Autowired
	private DiagnosisKeyManager diagnosisKeyManager;

	/**
	 * 
	 * @param countryCode
	 * @param dateString
	 * @param page
	 * @param size
	 * @param request
	 * @param response
	 */
	@GetMapping
	public void getDailyKeys(@PathVariable("countryCode") String countryCode,
			@PathVariable("dateString") String dateString,
			@RequestParam("page") Optional<Integer> page,
			@RequestParam("size") Optional<Integer> size, HttpServletRequest request,
			HttpServletResponse response) {

		if (!diagnosisKeyManager.getSupportedCounties().contains(countryCode)) {
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);
		}

		Instant date = LocalDate.parse(dateString).atStartOfDay().toInstant(ZoneOffset.UTC);
		try {
			Page<DiagnosisKey> keys = diagnosisKeyManager.getDaily(countryCode, date, PageRequest
					.of(page.orElse(0), size.orElse(500), Sort.by("rollingStartIntervalNumber")));

			if (keys.hasContent()) {
				response.setContentLengthLong(size.orElse(500) * 28);
				response.setContentType(MediaType.APPLICATION_BINARY.toString());
				for (DiagnosisKey diagnosisKey : keys.getContent()) {
					diagnosisKeyManager.writeToBinary(diagnosisKey, response.getOutputStream());
				}
			} else {
				if (diagnosisKeyManager.isFetched(date, countryCode)) {
					throw new ResponseStatusException(HttpStatus.NO_CONTENT);
				}
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT);
		}

	}

	/**
	 * 
	 * @param countryCode
	 * @param dateString
	 * @param hour
	 * @param page
	 * @param size
	 * @param request
	 * @param response
	 */
	@GetMapping("/hour/{hour}")
	public void getHourlyKeys(@PathVariable("countryCode") String countryCode,
			@PathVariable("dateString") String dateString, @PathVariable("hour") Integer hour,
			@RequestParam("page") Optional<Integer> page,
			@RequestParam("size") Optional<Integer> size, HttpServletRequest request,
			HttpServletResponse response) {

		if (!diagnosisKeyManager.getSupportedCounties().contains(countryCode)) {
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);
		}

		Instant date = LocalDate.parse(dateString).atStartOfDay().toInstant(ZoneOffset.UTC);
		try {
			Page<DiagnosisKey> keys = diagnosisKeyManager.getHourly(countryCode, date, hour,
					PageRequest.of(page.orElse(0), size.orElse(500),
							Sort.by("rollingStartIntervalNumber")));

			if (keys.hasContent()) {
				response.setContentLengthLong(size.orElse(500) * 28);
				response.setContentType(MediaType.APPLICATION_BINARY.toString());
				for (DiagnosisKey diagnosisKey : keys.getContent()) {
					diagnosisKeyManager.writeToBinary(diagnosisKey, response.getOutputStream());
				}
			} else {
				if (diagnosisKeyManager.isFetched(date, hour, countryCode)) {
					throw new ResponseStatusException(HttpStatus.NO_CONTENT);
				}
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT);
		}

	}

}
