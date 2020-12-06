/**
 * 
 */
package de.champonthis.ena.eke.proxy.buisnesslogic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import de.champonthis.ena.eke.proxy.model.DiagnosisKey;
import de.champonthis.ena.eke.proxy.model.FetchInfo;
import de.champonthis.ena.eke.proxy.model.QDiagnosisKey;
import de.champonthis.ena.eke.proxy.model.QFetchInfo;
import de.champonthis.ena.eke.proxy.model.TemporaryExposureKeyExportOuterClass.TemporaryExposureKey;
import de.champonthis.ena.eke.proxy.model.TemporaryExposureKeyExportOuterClass.TemporaryExposureKeyExport;
import de.champonthis.ena.eke.proxy.model.validation.CountryValidator;
import de.champonthis.ena.eke.proxy.repository.DiagnosisKeyRepository;
import de.champonthis.ena.eke.proxy.repository.FetchInfoRepository;

/**
 * @author Lurkars
 *
 */
@Component
public class DiagnosisKeyManager {

	/**
	 * logger
	 */
	private Logger logger = LogManager.getLogger(DiagnosisKeyManager.class);

	public static final String EXPORT_FILE_HEADER = "EK Export v1    ";
	public static final String EXPORT_BINARY_FILE_NAME = "export.bin";
	public static final int DAYS_BACK = 14;

	@Autowired
	private FetchInfoRepository fetchInfoRepository;
	@Autowired
	private DiagnosisKeyRepository diagnosisKeyRepository;

	private QFetchInfo qFetchInfo = QFetchInfo.fetchInfo;
	private QDiagnosisKey qDiagnosisKey = QDiagnosisKey.diagnosisKey;

	@Value("${ena-eke-proxy.daily-url}")
	private String dailyUrl;
	@Value("${ena-eke-proxy.hourly-url}")
	private String hourlyUrl;
	@Value("${ena-eke-proxy.supported-countries}")
	private List<String> supportedCounties;
	@Value("${ena-eke-proxy.source}")
	private String source;

	/**
	 * @return the supportedCounties
	 */
	public List<String> getSupportedCounties() {
		return supportedCounties;
	}

	/**
	 * 
	 * @param diagnosisKey
	 * @return
	 */
	public DiagnosisKey save(DiagnosisKey diagnosisKey) {
		return diagnosisKeyRepository.save(diagnosisKey);
	}

	/**
	 * 
	 * @param diagnosisKeys
	 * @return
	 */
	public List<DiagnosisKey> saveAll(List<DiagnosisKey> diagnosisKeys) {
		return diagnosisKeyRepository.saveAll(diagnosisKeys);
	}

	/**
	 * 
	 * @param countryCode
	 * @param date
	 * @param pageable
	 * @return
	 */
	public Page<DiagnosisKey> getDaily(String countryCode, Instant date, Pageable pageable) {
		return diagnosisKeyRepository.findAll(
				qDiagnosisKey.countryCode.eq(countryCode)
						.and(qDiagnosisKey.submissionTimestamp
								.after(date.truncatedTo(ChronoUnit.DAYS).minusNanos(1)))
						.and(qDiagnosisKey.submissionTimestamp.before(
								date.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS))),
				pageable);
	}

	/**
	 * 
	 * @param countryCode
	 * @param date
	 * @param hour
	 * @param pageable
	 * @return
	 */
	public Page<DiagnosisKey> getHourly(String countryCode, Instant date, int hour,
			Pageable pageable) {
		return diagnosisKeyRepository.findAll(qDiagnosisKey.countryCode.eq(countryCode)
				.and(qDiagnosisKey.submissionTimestamp.after(date.truncatedTo(ChronoUnit.DAYS)
						.plus(hour, ChronoUnit.HOURS).minusNanos(1)))
				.and(qDiagnosisKey.submissionTimestamp.before(
						date.truncatedTo(ChronoUnit.DAYS).plus(hour + 1, ChronoUnit.HOURS))),
				pageable);
	}

	/**
	 * 
	 * @param diagnosisKey
	 * @param out
	 * @throws IOException
	 */
	public void writeToBinary(DiagnosisKey diagnosisKey, OutputStream out) throws IOException {
		out.write(Base64.getDecoder().decode(diagnosisKey.getKeyData()));
		out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
				.putInt(diagnosisKey.getRollingStartIntervalNumber()).array());
		out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
				.putInt(diagnosisKey.getRollingPeriod()).array());
		out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
				.putInt(diagnosisKey.getDaysSinceOnsetOfSymptoms()).array());
	}

	/**
	 * 
	 * @param zipInputStream
	 * @param submissionTimestamp
	 * @throws IOException
	 */
	public void importZip(ZipInputStream zipInputStream, String countryCode,
			Instant submissionTimestamp, String source) throws IOException {
		ZipEntry zipEntry;
		while ((zipEntry = zipInputStream.getNextEntry()) != null) {
			if (!zipEntry.isDirectory() && zipEntry.getName().equals(EXPORT_BINARY_FILE_NAME)) {
				ByteArrayOutputStream clone = new ByteArrayOutputStream();
				zipInputStream.transferTo(clone);
				ByteArrayInputStream in = new ByteArrayInputStream(clone.toByteArray());

				in.skip(EXPORT_FILE_HEADER.getBytes(StandardCharsets.UTF_8).length);

				TemporaryExposureKeyExport export = TemporaryExposureKeyExport.parseFrom(in);
				for (TemporaryExposureKey temporaryExposureKey : export.getKeysList()) {
					try {
						save(new DiagnosisKey(temporaryExposureKey, countryCode,
								submissionTimestamp, source));
					} catch (Exception e) {
						logger.error("Failed to insert TemporaryExposureKey: "
								+ temporaryExposureKey.toString());
						e.printStackTrace();
					}
				}
			}
		}

	}

	/**
	 * 
	 * @param countryCode
	 * @param urlString
	 * @param submissionTimestamp
	 */
	boolean fetch(String urlString, Instant submissionTimestamp, String countryCode,
			String source) {
		try {
			URL url = new URL(urlString);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			if (connection.getResponseCode() != 200) {
				logger.warn("Not found '" + urlString + "'");
				return false;
			}
			logger.debug("Start " + url.toString());
			importZip(new ZipInputStream(connection.getInputStream()), countryCode,
					submissionTimestamp, source);
			if (logger.isTraceEnabled()) {
				for (DiagnosisKey diagnosisKey : diagnosisKeyRepository
						.findAll(qDiagnosisKey.submissionTimestamp.eq(submissionTimestamp))) {
					logger.trace(diagnosisKey.toString());
				}
			}
			logger.debug("finish '" + urlString + "'");
			return true;
		} catch (IOException e) {
			logger.error("Error on fetching '" + urlString + "'");
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 
	 * @param date
	 * @param countryCode
	 * @return
	 */
	boolean fetchDate(Instant date, String countryCode) {
		return fetch(
				String.format(dailyUrl, countryCode,
						DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.from(ZoneOffset.UTC))
								.format(date)),
				date.truncatedTo(ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS), countryCode, source);
	}

	/**
	 * 
	 * @param dateTime
	 * @param hour
	 * @param countryCode
	 * @return
	 */
	boolean fetchHour(Instant dateTime, int hour, String countryCode) {
		dateTime = dateTime.truncatedTo(ChronoUnit.DAYS).plus(hour, ChronoUnit.HOURS);
		return fetch(
				String.format(hourlyUrl, countryCode, DateTimeFormatter.ISO_LOCAL_DATE
						.withZone(ZoneId.from(ZoneOffset.UTC)).format(dateTime), hour),
				dateTime, countryCode, source);
	}

	/**
	 * 
	 * @param date
	 * @return
	 */
	public boolean isFetched(Instant date, String countryCode) {
		date = date.truncatedTo(ChronoUnit.DAYS);
		return fetchInfoRepository.exists(qFetchInfo.countryCode.eq(countryCode).and(
				qFetchInfo.timestamp.between(date.minusNanos(1), date.plus(1, ChronoUnit.DAYS)))
				.and(qFetchInfo.daily.isTrue()));
	}

	/**
	 * 
	 * @param dateTime
	 * @return
	 */
	public boolean isFetched(Instant dateTime, int hour, String countryCode) {
		dateTime = dateTime.truncatedTo(ChronoUnit.DAYS);
		dateTime = dateTime.plus(hour, ChronoUnit.HOURS);
		return isFetched(dateTime, countryCode)
				|| fetchInfoRepository.exists(qFetchInfo.countryCode.eq(countryCode)
						.and(qFetchInfo.timestamp.between(dateTime.minusNanos(1),
								dateTime.plus(1, ChronoUnit.HOURS)))
						.and(qFetchInfo.daily.isFalse()));
	}

	/**
	 * every hour: fetch missed days, fetch new keys, clearing old keys it's
	 * fetching info
	 */
	@Scheduled(cron = "0 0 * * * *")
	void cronJob() {
		for (String countryCode : getSupportedCounties()) {
			if (!CountryValidator.isValidCountryCode(countryCode)) {
				logger.warn("Invalid country code in 'ena-eke-proxy.supported-countries': "
						+ countryCode);
				continue;
			}
			// fetch past days
			for (int dayBack = DAYS_BACK; dayBack > 0; dayBack--) {
				Instant day = Instant.now().minus(dayBack, ChronoUnit.DAYS);
				day = day.truncatedTo(ChronoUnit.DAYS);
				if (!isFetched(day, countryCode)) {
					if (fetchDate(day, countryCode)) {
						// delete daily fetch if exists
						fetchInfoRepository.deleteAll(fetchInfoRepository
								.findAll(qFetchInfo.daily.isFalse().and(qFetchInfo.timestamp
										.between(day, day.plus(1, ChronoUnit.DAYS)))));
						
						fetchInfoRepository.save(
								new FetchInfo(day.plus(12, ChronoUnit.HOURS), true, countryCode));
					}
				}
			}

			// fetch current day hourly
			Instant day = Instant.now().truncatedTo(ChronoUnit.DAYS);
			int currentHour = Instant.now().atOffset(ZoneOffset.UTC).get(ChronoField.HOUR_OF_DAY);
			for (int hour = 0; hour <= currentHour; hour++) {
				if (!isFetched(day, hour, countryCode)) {
					if (fetchHour(day, hour, countryCode)) {
						fetchInfoRepository.save(new FetchInfo(day.plus(hour, ChronoUnit.HOURS),
								false, countryCode));
					}
				}
			}
		}

		// clear old keys
		diagnosisKeyRepository
				.deleteAll(diagnosisKeyRepository.findAll(qDiagnosisKey.submissionTimestamp
						.before(Instant.now().minus(DAYS_BACK, ChronoUnit.DAYS)
								.truncatedTo(ChronoUnit.DAYS).minusNanos(1))));
		// clear old fetch info
		fetchInfoRepository.deleteAll(fetchInfoRepository
				.findAll(qFetchInfo.timestamp.before(Instant.now().minus(DAYS_BACK, ChronoUnit.DAYS)
						.truncatedTo(ChronoUnit.DAYS).minusNanos(1))));
	}

	/**
	 * 
	 * @param ctxStartEvt
	 */
	@EventListener
	public void handleContextRefreshedEvent(ContextRefreshedEvent ctxStartEvt) {
		cronJob();
	}

}
