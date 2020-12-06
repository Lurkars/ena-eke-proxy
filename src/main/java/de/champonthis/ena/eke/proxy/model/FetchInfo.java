/**
 * 
 */
package de.champonthis.ena.eke.proxy.model;

import java.io.Serializable;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import de.champonthis.ena.eke.proxy.model.FetchInfo.FetchInfoId;

/**
 * @author Lurkars
 *
 */
@Entity
@IdClass(FetchInfoId.class)
public class FetchInfo {

	@Id
	@Column(unique = true)
	private final Instant timestamp;
	@Id
	private final boolean daily;
	private final String countryCode;

	/**
	 * 
	 */
	public FetchInfo() {
		this.timestamp = null;
		this.daily = false;
		this.countryCode = "";
	}

	/**
	 * @param timestamp
	 * @param daily
	 */
	public FetchInfo(Instant timestamp, boolean daily, String countryCode) {
		this.timestamp = timestamp;
		this.daily = daily;
		this.countryCode = countryCode;
	}

	/**
	 * @return the timestamp
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the daily
	 */
	public boolean isDaily() {
		return daily;
	}

	/**
	 * @return the countryCode
	 */
	public String getCountryCode() {
		return countryCode;
	}

	public static class FetchInfoId implements Serializable {

		private static final long serialVersionUID = 1L;
		private Instant timestamp;
		private boolean daily;

		/**
		 * 
		 */
		public FetchInfoId() {
		}

		/**
		 * @param timestamp
		 * @param daily
		 */
		public FetchInfoId(Instant timestamp, boolean daily) {
			this.timestamp = timestamp;
			this.daily = daily;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			FetchInfoId fetchInfoId = (FetchInfoId) o;
			return timestamp.equals(fetchInfoId.timestamp) && daily == fetchInfoId.daily;
		}
	}

}
