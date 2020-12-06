/**
 * 
 */
package de.champonthis.ena.eke.proxy.controller;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import de.champonthis.ena.eke.proxy.buisnesslogic.DiagnosisKeyManager;
import de.champonthis.ena.eke.proxy.model.DiagnosisKey;
import de.champonthis.ena.eke.proxy.model.TemporaryExposureKeyExportOuterClass.TemporaryExposureKey;
import de.champonthis.ena.eke.proxy.model.TemporaryExposureKeyExportOuterClass.TemporaryExposureKey.ReportType;

/**
 * @author Lurkars
 *
 */
@Controller
@RequestMapping("/version/v1/diagnosis-keys")
public class SubmissionController {

	@Autowired
	private DiagnosisKeyManager diagnosisKeyManager;

	/**
	 * logger
	 */
	private Logger logger = LogManager.getLogger(SubmissionController.class);

	@Value("${ena-eke-proxy.debug.submission-keys:}")
	private List<String> debugSubmissionKeys;

	/**
	 * 
	 * @param authorizationHeader
	 * @param request
	 * @param response
	 */
	@PostMapping
	public void getDailyKeys(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = true) String authorizationHeader,
			HttpServletRequest request, HttpServletResponse response) {

		// TODO for submission now, hard coded keys are provided, submission key should
		// be validated against real key server
		boolean validAuthorizationHeader = debugSubmissionKeys == null;
		if (!validAuthorizationHeader) {
			for (String debugSubmissionKey : debugSubmissionKeys) {
				if (debugSubmissionKey.equalsIgnoreCase(authorizationHeader)) {
					validAuthorizationHeader = true;
					break;
				}
			}
		}
		if (!validAuthorizationHeader) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}

		// TODO how to choose country code?
		String countryCode = diagnosisKeyManager.getSupportedCounties().get(0);

		List<DiagnosisKey> diagnosisKeys = Lists.newArrayList();
		try {
			while (request.getInputStream().available() > 0) {
				TemporaryExposureKey temporaryExposureKey = TemporaryExposureKey.newBuilder()
						.setKeyData(ByteString.copyFrom(request.getInputStream().readNBytes(16)))
						.setReportType(ReportType.SELF_REPORT)
						.setRollingStartIntervalNumber(
								ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
										.put(request.getInputStream().readNBytes(4)).getInt(0))
						.setRollingPeriod(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
								.put(request.getInputStream().readNBytes(4)).getInt(0))
						.setDaysSinceOnsetOfSymptoms(
								ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
										.put(request.getInputStream().readNBytes(4)).getInt(0))
						.build();

				diagnosisKeys.add(new DiagnosisKey(temporaryExposureKey, countryCode, Instant.now(),
						"ESP-ENA"));

				logger.trace("received key : " + diagnosisKeys.get(diagnosisKeys.size() - 1));
			}
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}

		diagnosisKeyManager.saveAll(diagnosisKeys);
		response.setStatus(HttpStatus.OK.value());
	}

}
