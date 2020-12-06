/**
 * 
 */
package de.champonthis.ena.eke.proxy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import de.champonthis.ena.eke.proxy.model.FetchInfo;

/**
 * @author Lurkars
 *
 */
@Repository
public interface FetchInfoRepository extends JpaRepository<FetchInfo, Long>, QuerydslPredicateExecutor<FetchInfo> {

}
