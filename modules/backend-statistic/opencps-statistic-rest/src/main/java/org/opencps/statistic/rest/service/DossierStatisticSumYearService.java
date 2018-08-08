package org.opencps.statistic.rest.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.opencps.statistic.rest.dto.DomainResponse;
import org.opencps.statistic.rest.dto.DossierStatisticData;
import org.opencps.statistic.rest.dto.DossierStatisticRequest;
import org.opencps.statistic.rest.dto.DossierStatisticResponse;
import org.opencps.statistic.rest.dto.GovAgencyData;
import org.opencps.statistic.rest.dto.GovAgencyRequest;
import org.opencps.statistic.rest.dto.GovAgencyResponse;
import org.opencps.statistic.rest.engine.DossierStatisticEngine;
import org.opencps.statistic.rest.facade.OpencpsCallGovAgencyRestFacadeImpl;
import org.opencps.statistic.rest.facade.OpencpsCallRestFacade;
import org.opencps.statistic.rest.util.DossierStatisticUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

import opencps.statistic.common.webservice.exception.UpstreamServiceFailedException;
import opencps.statistic.common.webservice.exception.UpstreamServiceTimedOutException;

public class DossierStatisticSumYearService {

	private final static Logger LOG = LoggerFactory.getLogger(DossierStatisticEngine.class);

	private DossierStatisticFinderService dossierStatisticFinderService = new DossierStatisticFinderServiceImpl();

	private DossierStatisticUpdateService<DossierStatisticData> updateGovService = new DossierStatisticGovUpdateServiceImpl();

	private OpencpsCallRestFacade<GovAgencyRequest, GovAgencyResponse> callService = new OpencpsCallGovAgencyRestFacadeImpl();

	public void caculateSumYear(long companyId, long groupId, int year)
			throws PortalException, UpstreamServiceTimedOutException, UpstreamServiceFailedException {
		
		//LOG.info("RUN#1" + groupId + "year" + year);
		/* filter all */
		filterSumYear(companyId, groupId, year, false, false);

		//LOG.info("RUN#2" + groupId + "year" + year);
		/* filter domain, agency = null */
		filterSumYear(companyId, groupId, year, true, false);
		
		//LOG.info("RUN#3" + groupId + "year" + year);
		/* filter domain = null, agency != null */
		filterSumYear(companyId, groupId, year, false, true);
		
		//LOG.info("RUN#4" + groupId + "year" + year);
		/* filter domain != null, agency != null */
		filterSumYear(companyId, groupId, year, true, true);

	}

	private void filterSumYear(long companyId, long groupId, int year, boolean isDomain, boolean isAgency)
			throws UpstreamServiceTimedOutException, UpstreamServiceFailedException {

		DossierStatisticRequest dossierStatisticRequest = new DossierStatisticRequest();

		dossierStatisticRequest.setMonth(-1);
		dossierStatisticRequest.setYear(year);
		dossierStatisticRequest.setGroupId(groupId);
		dossierStatisticRequest.setStart(QueryUtil.ALL_POS);
		dossierStatisticRequest.setEnd(QueryUtil.ALL_POS);

		/* case domain != null && agency = null */
		if (isDomain && !isAgency) {
			/* statistic by all */

			DossierStatisticResponse dossierStatisticResponse;

			List<DomainResponse> domainResponses = getDomain(groupId);
			
			DossierStatisticUtils.logAsFormattedJson(LOG, domainResponses);

			for (DomainResponse domainResponse : domainResponses) {

				try {
					dossierStatisticRequest.setDomain(domainResponse.getItemCode());
					
					DossierStatisticUtils.logAsFormattedJson(LOG, dossierStatisticRequest);

					dossierStatisticResponse = dossierStatisticFinderService
							.finderDossierStatistic(dossierStatisticRequest);

					Optional<List<DossierStatisticData>> dossierStatisticData = Optional
							.ofNullable(dossierStatisticResponse.getDossierStatisticData());

					dossierStatisticData.ifPresent(source -> {
						if (dossierStatisticData.get().size() > 0) {
							DossierStatisticData latestMonthStatisticData = source.get(0);

							try {
								getDetailData(companyId, groupId, 0, year, domainResponse.getItemCode(),
										domainResponse.getItemName(), StringPool.BLANK, StringPool.BLANK, source,
										latestMonthStatisticData);
							} catch (SystemException e) {
								e.printStackTrace();
							} catch (PortalException e) {
								e.printStackTrace();
							}
						}

					});

				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		}

		/* case domain = null && agency = null */
		if (!isDomain && !isAgency) {
			/* statistic by all */

			DossierStatisticResponse dossierStatisticResponse;

			try {
				dossierStatisticResponse = dossierStatisticFinderService
						.finderDossierStatistic(dossierStatisticRequest);

				Optional<List<DossierStatisticData>> dossierStatisticData = Optional
						.ofNullable(dossierStatisticResponse.getDossierStatisticData());

				dossierStatisticData.ifPresent(source -> {

					if (dossierStatisticData.get().size() > 0) {
						DossierStatisticData latestMonthStatisticData = source.get(0);

						try {
							getDetailData(companyId, groupId, 0, year, StringPool.BLANK, StringPool.BLANK,
									StringPool.BLANK, StringPool.BLANK, source, latestMonthStatisticData);
						} catch (SystemException e) {
							e.printStackTrace();
						} catch (PortalException e) {
							e.printStackTrace();
						}
					}

				});

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		/* case domain = null && agency != null */
		if (!isDomain && isAgency) {
			GovAgencyRequest agencyRequest = new GovAgencyRequest();

			agencyRequest.setGroupId(groupId);

			GovAgencyResponse agencyResponse = callService.callRestService(agencyRequest);

			Optional<List<GovAgencyData>> govDataList = Optional.ofNullable(agencyResponse.getData());

			govDataList.ifPresent(source -> {
				for (GovAgencyData data : source) {

					dossierStatisticRequest.setGovAgencyCode(data.getItemCode());

					DossierStatisticResponse dossierStatisticResponse;

					try {
						dossierStatisticResponse = dossierStatisticFinderService
								.finderDossierStatistic(dossierStatisticRequest);

						Optional<List<DossierStatisticData>> dossierStatisticData = Optional
								.ofNullable(dossierStatisticResponse.getDossierStatisticData());

						dossierStatisticData.ifPresent(source2 -> {
							if (dossierStatisticData.get().size() > 0) {

								DossierStatisticData latestMonthStatisticData = source2.get(0);

								try {
									getDetailData(companyId, groupId, 0, year, StringPool.BLANK, StringPool.BLANK,
											data.getItemCode(), data.getItemName(), source2, latestMonthStatisticData);
								} catch (SystemException e) {

									e.printStackTrace();
								} catch (PortalException e) {

									e.printStackTrace();
								}
							}

						});

					} catch (PortalException e) {

						e.printStackTrace();
					}

				}
			});

		}

		/* case domain != null && agency != null */

		if (isAgency && isDomain) {
			GovAgencyRequest agencyRequest = new GovAgencyRequest();

			agencyRequest.setGroupId(groupId);

			GovAgencyResponse agencyResponse = callService.callRestService(agencyRequest);

			Optional<List<GovAgencyData>> govDataList = Optional.ofNullable(agencyResponse.getData());

			List<DomainResponse> domainResponses = getDomain(groupId);

			govDataList.ifPresent(source -> {
				for (GovAgencyData data : source) {
					dossierStatisticRequest.setGovAgencyCode(data.getItemCode());

					if (!domainResponses.isEmpty()) {

						for (DomainResponse domainResponse : domainResponses) {

							dossierStatisticRequest.setDomain(domainResponse.getItemCode());

							DossierStatisticResponse dossierStatisticResponse;

							try {
								dossierStatisticResponse = dossierStatisticFinderService
										.finderDossierStatistic(dossierStatisticRequest);

								Optional<List<DossierStatisticData>> dossierStatisticData = Optional
										.ofNullable(dossierStatisticResponse.getDossierStatisticData());

								dossierStatisticData.ifPresent(source2 -> {
									if (dossierStatisticData.get().size() > 0) {

										DossierStatisticData latestMonthStatisticData = source2.get(0);

										try {
											getDetailData(companyId, groupId, 0, year, domainResponse.getItemCode(),
													domainResponse.getItemName(), data.getItemCode(),
													data.getItemName(), source2, latestMonthStatisticData);
										} catch (SystemException e) {

											e.printStackTrace();
										} catch (PortalException e) {

											e.printStackTrace();
										}
									}

								});

							} catch (PortalException e) {

								e.printStackTrace();
							}
						}
					}

				}
			});

		}

	}

	private List<DomainResponse> getDomain(long groupId) {
		List<DomainResponse> domainResponses = new ArrayList<DomainResponse>();

		DossierStatisticRequest dossierStatisticRequest = new DossierStatisticRequest();

		dossierStatisticRequest.setMonth(-1);
		dossierStatisticRequest.setYear(LocalDate.now().getYear());
		dossierStatisticRequest.setGroupId(groupId);
		dossierStatisticRequest.setStart(QueryUtil.ALL_POS);
		dossierStatisticRequest.setEnd(QueryUtil.ALL_POS);

		DossierStatisticResponse dossierStatisticResponse;

		try {
			dossierStatisticResponse = dossierStatisticFinderService.finderDossierStatistic(dossierStatisticRequest);

			Optional<List<DossierStatisticData>> optional = Optional
					.ofNullable(dossierStatisticResponse.getDossierStatisticData());

			optional.ifPresent(source -> {
				for (DossierStatisticData dossierStatisticData : source) {
					DomainResponse domainResponse = new DomainResponse();
					if (Validator.isNotNull(dossierStatisticData.getDomainCode())
							&& Validator.isNotNull(dossierStatisticData.getDomainName())) {

						domainResponse.setItemCode(dossierStatisticData.getDomainCode());
						domainResponse.setItemName(dossierStatisticData.getDomainName());

						domainResponses.add(domainResponse);
					}

				}
			});

		} catch (PortalException e) {
			e.printStackTrace();
		}

		/* remove duplicate */
		Set<DomainResponse> hs = new HashSet<>();
		hs.addAll(domainResponses);
		domainResponses.clear();
		domainResponses.addAll(hs);

		return domainResponses;
	}

	private void getDetailData(long companyId, long groupId, int month, int year, String domainCode, String domainName,
			String govAgencyCode, String govAgencyName, List<DossierStatisticData> source, DossierStatisticData latest)
			throws SystemException, PortalException {

		DossierStatisticData dossierStatisticData = new DossierStatisticData();

		int totalCount = 0;
		int deniedCount = 0;
		int cancelledCount = 0;
		int processCount = 0;
		int remainingCount = 0;
		int receivedCount = 0;
		int onlineCount = 0;
		int releaseCount = 0;
		int betimesCount = 0;
		int ontimeCount = 0;
		int overtimeCount = 0;
		int overtimeInside = 0;
		int overtimeOutside = 0;
		int doneCount = 0;
		int releasingCount = 0;
		int unresolvedCount = 0;
		int processingCount = 0;
		int undueCount = 0;
		int overdueCount = 0;
		int interoperatingCount = 0;
		int waitingCount = 0;
		int ontimePercentage = 0;

		for (DossierStatisticData data : source) {
			deniedCount = deniedCount + data.getDeniedCount();
			cancelledCount = cancelledCount + data.getCancelledCount();
			receivedCount = receivedCount + data.getReceivedCount();
			onlineCount = onlineCount + data.getOnlineCount();
			releaseCount = releaseCount + data.getReleaseCount();
			unresolvedCount = unresolvedCount + data.getUnresolvedCount();

			betimesCount = betimesCount + data.getBetimesCount();
			ontimeCount = ontimeCount + data.getOntimeCount();
			overtimeCount = overtimeCount + data.getOvertimeCount();
			overtimeOutside = overtimeOutside + data.getOvertimeOutside();
			overtimeInside = overtimeInside + data.getOvertimeInside();
		}

		/* value get in the latest month */
		processingCount = latest.getProcessingCount();
		undueCount = latest.getUndueCount();
		overdueCount = latest.getOverdueCount();
		interoperatingCount = latest.getInteroperatingCount();
		waitingCount = latest.getWaitingCount();

		processCount = releaseCount + processingCount;

		totalCount = processCount + deniedCount + cancelledCount;

		remainingCount = processCount - receivedCount;

		doneCount = releaseCount - (releasingCount + unresolvedCount);

		ontimePercentage = (undueCount + betimesCount + ontimeCount) * 100
				/ (undueCount + betimesCount + ontimeCount + overdueCount + overtimeCount);

		dossierStatisticData.setTotalCount(totalCount);
		dossierStatisticData.setDeniedCount(deniedCount);
		dossierStatisticData.setCancelledCount(cancelledCount);
		dossierStatisticData.setProcessCount(processCount);
		dossierStatisticData.setReceivedCount(receivedCount);
		dossierStatisticData.setOnlineCount(onlineCount);
		dossierStatisticData.setReleaseCount(releaseCount);
		dossierStatisticData.setBetimesCount(betimesCount);
		dossierStatisticData.setOntimeCount(ontimeCount);
		dossierStatisticData.setOvertimeCount(overtimeCount);
		dossierStatisticData.setOvertimeInside(overtimeInside);
		dossierStatisticData.setOvertimeOutside(overtimeOutside);
		dossierStatisticData.setReleaseCount(releaseCount);
		dossierStatisticData.setProcessingCount(processingCount);
		dossierStatisticData.setUnresolvedCount(unresolvedCount);
		dossierStatisticData.setUndueCount(undueCount);
		dossierStatisticData.setOverdueCount(overdueCount);
		dossierStatisticData.setInteroperatingCount(interoperatingCount);
		dossierStatisticData.setWaitingCount(waitingCount);
		dossierStatisticData.setOntimePercentage(ontimePercentage);
		dossierStatisticData.setMonth(month);
		dossierStatisticData.setYear(year);
		dossierStatisticData.setGovAgencyCode(govAgencyCode);
		dossierStatisticData.setGovAgencyName(govAgencyName);

		dossierStatisticData.setDoneCount(doneCount);
		dossierStatisticData.setRemainingCount(remainingCount);

		dossierStatisticData.setCompanyId(companyId);
		dossierStatisticData.setGroupId(groupId);

		/* add to database */

		updateGovService.updateDossierStatistic(dossierStatisticData);
	}
}
