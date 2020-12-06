/**
 * 
 */
package de.champonthis.ena.eke.proxy.model;

import java.time.Instant;
import java.util.Base64;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import org.apache.tomcat.util.buf.HexUtils;

import de.champonthis.ena.eke.proxy.model.TemporaryExposureKeyExportOuterClass.TemporaryExposureKey;
import de.champonthis.ena.eke.proxy.model.TemporaryExposureKeyExportOuterClass.TemporaryExposureKey.ReportType;
import de.champonthis.ena.eke.proxy.model.validation.ValidCountry;
import de.champonthis.ena.eke.proxy.model.validation.ValidRollingStartIntervalNumber;
import de.champonthis.ena.eke.proxy.model.validation.ValidSubmissionTimestamp;

/**
 * @author Lurkars
 *
 */
@Entity
public class DiagnosisKey {

	public static final long ROLLING_PERIOD_MINUTES_INTERVAL = 10;

	/**
	 * According to "Setting Up an Exposure Notification Server" by Apple, exposure
	 * notification servers are expected to reject any diagnosis keys that do not
	 * have a rolling period of a certain fixed value. See
	 * https://developer.apple.com/documentation/exposurenotification/setting_up_an_exposure_notification_server
	 */
	public static final int KEY_DATA_LENGTH = 16;
	public static final int KEY_BASE64_DATA_LENGTH = 24;
	public static final int MIN_ROLLING_PERIOD = 0;
	public static final int MAX_ROLLING_PERIOD = 144;
	public static final int MIN_DAYS_SINCE_ONSET_OF_SYMPTOMS = -14;
	public static final int MAX_DAYS_SINCE_ONSET_OF_SYMPTOMS = 4000;
	public static final int MIN_TRANSMISSION_RISK_LEVEL = 0;
	public static final int MAX_TRANSMISSION_RISK_LEVEL = 8;
	public static final int ISO_CONTRYCODE_MIN = 2;
	public static final int ISO_CONTRYCODE_MAX = 3;

	@Id
	@Size(min = KEY_BASE64_DATA_LENGTH, max = KEY_BASE64_DATA_LENGTH)
	@Column(length = KEY_BASE64_DATA_LENGTH, unique = true)
	private final String keyData;
	@ValidRollingStartIntervalNumber
	private final int rollingStartIntervalNumber;
	@Min(MIN_ROLLING_PERIOD)
	@Max(MAX_ROLLING_PERIOD)
	private final int rollingPeriod;
	@Min(MIN_TRANSMISSION_RISK_LEVEL)
	@Max(MAX_TRANSMISSION_RISK_LEVEL)
	private final int transmissionRiskLevel;
	private final ReportType reportType;
	@Min(MIN_DAYS_SINCE_ONSET_OF_SYMPTOMS)
	@Max(MAX_DAYS_SINCE_ONSET_OF_SYMPTOMS)
	private final int daysSinceOnsetOfSymptoms;
	@ValidSubmissionTimestamp
	private final Instant submissionTimestamp;
	@Size(min = ISO_CONTRYCODE_MIN, max = ISO_CONTRYCODE_MAX)
	@Column(length = ISO_CONTRYCODE_MAX)
	@ValidCountry
	private final String countryCode;
	private final String source;

	/**
	 * 
	 */
	public DiagnosisKey() {
		this.keyData = null;
		this.rollingStartIntervalNumber = 0;
		this.rollingPeriod = 0;
		this.transmissionRiskLevel = 0;
		this.reportType = null;
		this.daysSinceOnsetOfSymptoms = 0;
		this.submissionTimestamp = null;
		this.countryCode = "";
		this.source = "";
	}

	/**
	 * 
	 * @param temporaryExposureKey
	 * @param countryCode
	 * @param submissionTimestamp
	 */
	public DiagnosisKey(TemporaryExposureKey temporaryExposureKey, String countryCode,
			Instant submissionTimestamp, String source) {
		this.keyData = Base64.getEncoder().encodeToString(temporaryExposureKey.getKeyData().toByteArray());
		this.rollingStartIntervalNumber = temporaryExposureKey.getRollingStartIntervalNumber();
		this.rollingPeriod = temporaryExposureKey.getRollingPeriod();
		this.transmissionRiskLevel = temporaryExposureKey.hasTransmissionRiskLevel()
				? temporaryExposureKey.getTransmissionRiskLevel()
				: 0;
		this.reportType = temporaryExposureKey.getReportType();
		this.daysSinceOnsetOfSymptoms = temporaryExposureKey.hasDaysSinceOnsetOfSymptoms()
				? temporaryExposureKey.getDaysSinceOnsetOfSymptoms()
				: null;
		this.submissionTimestamp = submissionTimestamp;
		this.countryCode = countryCode;
		this.source = source;
	}

	/**
	 * @return the keyData
	 */
	public String getKeyData() {
		return keyData;
	}

	/**
	 * @return the rollingStartIntervalNumber
	 */
	public int getRollingStartIntervalNumber() {
		return rollingStartIntervalNumber;
	}

	/**
	 * @return the rollingPeriod
	 */
	public int getRollingPeriod() {
		return rollingPeriod;
	}

	/**
	 * @return the transmissionRiskLevel
	 */
	public int getTransmissionRiskLevel() {
		return transmissionRiskLevel;
	}

	/**
	 * @return the reportType
	 */
	public ReportType getReportType() {
		return reportType;
	}

	/**
	 * @return the daysSinceOnsetOfSymptoms
	 */
	public int getDaysSinceOnsetOfSymptoms() {
		return daysSinceOnsetOfSymptoms;
	}

	/**
	 * @return the submissionTimestamp
	 */
	public Instant getSubmissionTimestamp() {
		return submissionTimestamp;
	}

	/**
	 * @return the countryCode
	 */
	public String getCountryCode() {
		return countryCode;
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	@Override
	public String toString() {

		return "DiagnosisKey{" + "keyData=" + HexUtils.toHexString(Base64.getDecoder().decode(keyData))
				+ ", rollingStartIntervalNumber=" + rollingStartIntervalNumber + ", rollingPeriod=" + rollingPeriod
				+ ", transmissionRiskLevel=" + transmissionRiskLevel + ", reportType=" + reportType
				+ ", daysSinceOnsetOfSymptoms=" + daysSinceOnsetOfSymptoms + ", submissionTimestamp="
				+ submissionTimestamp + ", countryCode=" + countryCode + ", source=" + source + '}';
	}

}
