package org.opencps.statistic.rest.engine;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.opencps.statistic.model.OpencpsDossierStatistic;
import org.opencps.statistic.rest.dto.CommonRequest;
import org.opencps.statistic.rest.dto.DossierStatisticData;
import org.opencps.statistic.rest.dto.GetDossierData;
import org.opencps.statistic.rest.dto.GetDossierRequest;
import org.opencps.statistic.rest.dto.GetDossierResponse;
import org.opencps.statistic.rest.dto.GovAgencyData;
import org.opencps.statistic.rest.dto.GovAgencyRequest;
import org.opencps.statistic.rest.dto.GovAgencyResponse;
import org.opencps.statistic.rest.dto.ServiceInfoResponse;
import org.opencps.statistic.rest.facade.OpencpsCallDossierRestFacadeImpl;
import org.opencps.statistic.rest.facade.OpencpsCallGovAgencyRestFacadeImpl;
import org.opencps.statistic.rest.facade.OpencpsCallRestFacade;
import org.opencps.statistic.rest.facade.OpencpsCallServiceInfoRestFacadeImpl;
import org.opencps.statistic.rest.service.DossierStatisticDomainUpdateServiceImpl;
import org.opencps.statistic.rest.service.DossierStatisticGovUpdateServiceImpl;
import org.opencps.statistic.rest.service.DossierStatisticUpdateService;
import org.opencps.statistic.rest.util.DossierStatisticConfig;
import org.opencps.statistic.rest.util.DossierStatisticConstants;
import org.opencps.statistic.rest.util.DossierStatusContants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.messaging.BaseSchedulerEntryMessageListener;
import com.liferay.portal.kernel.messaging.DestinationNames;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.module.framework.ModuleServiceLifecycle;
import com.liferay.portal.kernel.scheduler.SchedulerEngineHelper;
import com.liferay.portal.kernel.scheduler.TimeUnit;
import com.liferay.portal.kernel.scheduler.TriggerFactory;
import com.liferay.portal.kernel.scheduler.TriggerFactoryUtil;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

import opencps.statistic.common.webservice.exception.UpstreamServiceFailedException;
import opencps.statistic.common.webservice.exception.UpstreamServiceTimedOutException;

@Component(immediate = true, service = DossierStatisticEngine.class)
public class DossierStatisticEngine extends BaseSchedulerEntryMessageListener {

	private final static Logger LOG = LoggerFactory.getLogger(DossierStatisticEngine.class);

	private SchedulerEngineHelper _schedulerEngineHelper;

	private OpencpsCallRestFacade<GovAgencyRequest, GovAgencyResponse> callService = new OpencpsCallGovAgencyRestFacadeImpl();

	private OpencpsCallRestFacade<GetDossierRequest, GetDossierResponse> callDossierRestService = new OpencpsCallDossierRestFacadeImpl();

	private OpencpsCallRestFacade<CommonRequest, ServiceInfoResponse> callServiceInfo = new OpencpsCallServiceInfoRestFacadeImpl();

	private DossierStatisticUpdateService<DossierStatisticData, OpencpsDossierStatistic> updateGovService = new DossierStatisticGovUpdateServiceImpl();

	private DossierStatisticUpdateService<HashMap<String, DossierStatisticData>, OpencpsDossierStatistic> updateDomainService = new DossierStatisticDomainUpdateServiceImpl();

	public static final int TYPE_1_ALL_GOV_ALL_DOMAIN = 1;

	public static final int TYPE_2_EACH_GOV_ALL_DOMAIN_AND_EACH_GOV_EACH_DOMAIN = 2;

	//public static final int TYPE_3_EACH_GOV_EACH_DOMAIN = 3;

	public static final int TYPE_4_ALL_GOV_EACH_DOMAIN = 4;

	@Override
	protected void doReceive(Message message) throws Exception {

		LOG.info("START getDossierStatistic(): " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

		Company company = CompanyLocalServiceUtil.getCompanyByMx(PropsUtil.get(PropsKeys.COMPANY_DEFAULT_WEB_ID));

		List<Group> groups = GroupLocalServiceUtil.getCompanyGroups(company.getCompanyId(), QueryUtil.ALL_POS,
				QueryUtil.ALL_POS);

		List<Group> sites = new ArrayList<Group>();

		for (Group group : groups) {
			if (group.getType() == 1 && group.isSite()) {
				sites.add(group);
			}
		}

		for (Group site : sites) {

			LOG.info(site.getName(DossierStatisticConstants.VIETNAM));

			/* Run TYPE_1 */
			calulateDossierStatistic(TYPE_1_ALL_GOV_ALL_DOMAIN, site.getGroupId(), company.getCompanyId());

			/* Run TYPE_2 */
			calulateDossierStatistic(TYPE_2_EACH_GOV_ALL_DOMAIN_AND_EACH_GOV_EACH_DOMAIN, site.getGroupId(),company.getCompanyId());

			/* Run TYPE_3 */
			calulateDossierStatistic(TYPE_4_ALL_GOV_EACH_DOMAIN, site.getGroupId(), company.getCompanyId());

		}

	}

	private void calulateDossierStatistic(int type, long groupId, long companyId)
			throws UpstreamServiceTimedOutException, UpstreamServiceFailedException, SystemException, PortalException {
		LocalDate cal = LocalDate.now();

		int month = cal.getMonthValue();
		int year = cal.getYear();

		GetDossierResponse dossierResponse = new GetDossierResponse();

		HashMap<String, DossierStatisticData> domainStatisticData = new HashMap<String, DossierStatisticData>();

		DossierStatisticData govDossierStaticData = new DossierStatisticData();

		GetDossierRequest payload = new GetDossierRequest();

		/* TYPE_1_ALL_GOV_ALL_DOMAIN */

		if (type == TYPE_1_ALL_GOV_ALL_DOMAIN) {
			payload.setGroupId(groupId);

			dossierResponse = callDossierRestService.callRestService(payload);

			if (Validator.isNotNull(dossierResponse.getData())) {

				filterStatistic(domainStatisticData, govDossierStaticData, dossierResponse, month, year,
						StringPool.BLANK, StringPool.BLANK, false, groupId, companyId);
			}

			updateGovService.updateDossierStatistic(govDossierStaticData);
		}

		if (type == TYPE_2_EACH_GOV_ALL_DOMAIN_AND_EACH_GOV_EACH_DOMAIN) {

			GovAgencyRequest agencyRequest = new GovAgencyRequest();

			agencyRequest.setGroupId(groupId);

			GovAgencyResponse agencyResponse = callService.callRestService(agencyRequest);

			if (Validator.isNotNull(agencyResponse.getData())) {

				for (GovAgencyData agencyData : agencyResponse.getData()) {
					payload.setGroupId(groupId);
					payload.setGovAgencyCode(agencyData.getItemCode());

					dossierResponse = callDossierRestService.callRestService(payload);

					if (Validator.isNotNull(dossierResponse.getData())) {
						filterStatistic(domainStatisticData, govDossierStaticData, dossierResponse, month, year,
								agencyData.getItemCode(), agencyData.getItemName(), true, groupId, companyId);
					}

					updateGovService.updateDossierStatistic(govDossierStaticData);
					updateDomainService.updateDossierStatistic(domainStatisticData);

				}

			}

		}

		if (type == TYPE_4_ALL_GOV_EACH_DOMAIN) {
			payload.setGroupId(groupId);

			dossierResponse = callDossierRestService.callRestService(payload);

			if (Validator.isNotNull(dossierResponse.getData())) {
				filterStatistic(domainStatisticData, govDossierStaticData, dossierResponse, month, year,
						StringPool.BLANK, StringPool.BLANK, true, groupId, companyId);
			}

			updateDomainService.updateDossierStatistic(domainStatisticData);
		}

	}

	private void filterStatistic(HashMap<String, DossierStatisticData> domainStatisticData,
			DossierStatisticData govDossierStaticData, GetDossierResponse dossierResponse, int month, int year,
			String govAgencyCode, String govAgencyName, boolean isSetDomain, long groupId, long companyId)
			throws UpstreamServiceFailedException, UpstreamServiceTimedOutException {

		int totalCount = dossierResponse.getTotal();
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

		for (GetDossierData dossierData : dossierResponse.getData()) {

			CommonRequest request = new CommonRequest();
			request.setGroupId(dossierData.getGroupId());
			request.setKeyword(dossierData.getServiceCode());

			ServiceInfoResponse serviceInfoResponse = callServiceInfo.callRestService(request);

			DossierStatisticData dossierStatisticData = new DossierStatisticData();

			if (domainStatisticData.containsKey(serviceInfoResponse.getDomainCode())) {
				dossierStatisticData = domainStatisticData.get(serviceInfoResponse.getDomainCode());
			} else {
				domainStatisticData.put(serviceInfoResponse.getDomainCode(), dossierStatisticData);
			}

			dossierStatisticData.setTotalCount(dossierStatisticData.getTotalCount() + 1);

			/* DENIED */
			if (dossierData.getDossierStatus().contentEquals(DossierStatusContants.DOSSIER_STATUS_DENIED)) {
				deniedCount = deniedCount + 1;

				dossierStatisticData.setDeniedCount(dossierStatisticData.getDeniedCount() + 1);
			}

			/* CANCELLED */
			if (dossierData.getDossierStatus().contentEquals(DossierStatusContants.DOSSIER_STATUS_CANCELLED)) {
				cancelledCount = cancelledCount + 1;

				dossierStatisticData.setCancelledCount(dossierStatisticData.getCancelledCount() + 1);
			}

			/* PROCESS */
			if (!dossierData.getDossierStatus().contentEquals(DossierStatusContants.DOSSIER_STATUS_DENIED)
					&& !dossierData.getDossierStatus().contentEquals(DossierStatusContants.DOSSIER_STATUS_CANCELLED)) {

				processCount = processCount + 1;

				dossierStatisticData.setProcessCount(dossierStatisticData.getProcessCount() + 1);

				/* REVEICED */
				if (Validator.isNotNull(dossierData.getReceiveDate())
						&& dossierData.getReceiveDate().after(getFirstDay())) {
					receivedCount = receivedCount + 1;

					dossierStatisticData.setReceivedCount(dossierStatisticData.getReceivedCount() + 1);

					/* ONLINE */
					if (dossierData.getOnline()) {
						onlineCount = onlineCount + 1;

						dossierStatisticData.setOnlineCount(dossierStatisticData.getOnlineCount() + 1);
					}
				} else {
					/* REMAINING */
					remainingCount = remainingCount + 1;

					dossierStatisticData.setRemainingCount(dossierStatisticData.getRemainingCount() + 1);

				}

				/* RELEASED */
				if (dossierData.getDossierStatus().contentEquals(DossierStatusContants.DOSSIER_STATUS_DONE)
						|| dossierData.getDossierStatus().contentEquals(DossierStatusContants.DOSSIER_STATUS_RELEASING)
						|| dossierData.getDossierStatus().contentEquals(DossierStatusContants.DOSSIER_STATUS_POSTING)
						|| dossierData.getDossierStatus()
								.contentEquals(DossierStatusContants.DOSSIER_STATUS_UNRESOLVED)) {

					releaseCount = releaseCount + 1;

					dossierStatisticData.setReleaseCount(dossierStatisticData.getReleaseCount() + 1);

					/* DONE */
					if (dossierData.getDossierStatus().contentEquals(DossierStatusContants.DOSSIER_STATUS_DONE)
							|| dossierData.getDossierStatus()
									.contentEquals(DossierStatusContants.DOSSIER_STATUS_POSTING)) {
						doneCount = doneCount + 1;

						dossierStatisticData.setDoneCount(dossierStatisticData.getDoneCount() + 1);

						/* BETIME */
						if (Validator.isNotNull(dossierData.getDueDate())
								&& Validator.isNotNull(dossierData.getExtendDate())
								&& dossierData.getExtendDate().after(dossierData.getDueDate())) {

							betimesCount = betimesCount + 1;

							dossierStatisticData.setBetimesCount(dossierStatisticData.getBetimesCount() + 1);

						} else {
							/* ONTIMECOUNT */
							if (Validator.isNull(dossierData.getDueDate())
									|| (Validator.isNotNull(dossierData.getReleaseDate()) && dossierData.getReleaseDate().before(dossierData.getDueDate()))) {

								ontimeCount = onlineCount + 1;

								dossierStatisticData.setOntimeCount(dossierStatisticData.getOntimeCount() + 1);
							}
						}
					}

					/* RELEASING */
					if (dossierData.getDossierStatus().contentEquals(DossierStatusContants.DOSSIER_STATUS_RELEASING)) {
						releasingCount = releasingCount + 1;

						dossierStatisticData.setReleasingCount(dossierStatisticData.getReleasingCount() + 1);
					}

					/* UNRESOLVED */
					if (dossierData.getDossierStatus().contentEquals(DossierStatusContants.DOSSIER_STATUS_UNRESOLVED)) {
						unresolvedCount = unresolvedCount + 1;

						dossierStatisticData.setUnresolvedCount(dossierStatisticData.getUnresolvedCount() + 1);
					}
				} else if (dossierData.getDossierStatus().contentEquals(DossierStatusContants.DOSSIER_STATUS_WAITING)) {
					/* WAITING */
					waitingCount = waitingCount + 1;

					dossierStatisticData.setWaitingCount(dossierStatisticData.getWaitingCount() + 1);

				} else {
					/* PROCESSING */
					processingCount = processingCount + 1;

					dossierStatisticData.setProcessingCount(dossierStatisticData.getProcessingCount() + 1);

					/* INTEROPERATING */
					if (dossierData.getDossierStatus()
							.contentEquals(DossierStatusContants.DOSSIER_STATUS_INTEROPERATING)) {
						interoperatingCount = interoperatingCount + 1;

						dossierStatisticData.setInteroperatingCount(dossierStatisticData.getInteroperatingCount() + 1);
					}

					/* UNDUE */
					if (Validator.isNull(dossierData.getDueDate()) || dossierData.getDueDate().after(new Date())) {
						undueCount = undueCount + 1;

						dossierStatisticData.setUndueCount(dossierStatisticData.getUndueCount() + 1);
					}
				}
			}

			dossierStatisticData.setOvertimeOutside(0);
			dossierStatisticData.setOvertimeInside(
					dossierStatisticData.getOvertimeCount() - dossierStatisticData.getOvertimeOutside());
			dossierStatisticData
					.setOvertimeCount((dossierStatisticData.getDoneCount() + dossierStatisticData.getReleasingCount())
							- (dossierStatisticData.getOntimeCount() + dossierStatisticData.getBetimesCount()));

			dossierStatisticData
					.setOverdueCount(dossierStatisticData.getProcessingCount() - dossierStatisticData.getUndueCount());

			int domainSubTotal = dossierStatisticData.getUndueCount() + dossierStatisticData.getOverdueCount()
					+ dossierStatisticData.getBetimesCount() + dossierStatisticData.getOntimeCount()
					+ dossierStatisticData.getOvertimeCount();

			if (domainSubTotal != 0) {

				int domainOntimePercentage = (dossierStatisticData.getUndueCount()
						+ dossierStatisticData.getBetimesCount() + dossierStatisticData.getOntimeCount()) * 100
						/ domainSubTotal;

				dossierStatisticData.setOntimePercentage(domainOntimePercentage);
			}

			dossierStatisticData.setMonth(month);
			dossierStatisticData.setYear(year);
			dossierStatisticData.setGovAgencyCode(govAgencyCode);
			dossierStatisticData.setGovAgencyName(govAgencyName);

			if (isSetDomain) {
				dossierStatisticData.setDomainCode(serviceInfoResponse.getDomainCode());
				dossierStatisticData.setDomainName(serviceInfoResponse.getDomainName());
			}

			dossierStatisticData.setCompanyId(companyId);
			dossierStatisticData.setGroupId(groupId);

			domainStatisticData.put(serviceInfoResponse.getDomainCode(), dossierStatisticData);

		}

		logAsFormattedJson(LOG, domainStatisticData);

		overtimeOutside = 0;

		overtimeInside = overtimeCount - overtimeOutside;

		overtimeCount = (doneCount + releasingCount) - (ontimeCount + betimesCount);

		overdueCount = processingCount - undueCount;

		int subTotal = undueCount + overdueCount + betimesCount + ontimeCount + overtimeCount;

		if (subTotal != 0) {
			ontimePercentage = (undueCount + betimesCount + ontimeCount) * 100 / subTotal;
		}

		govDossierStaticData.setTotalCount(totalCount);
		govDossierStaticData.setDeniedCount(deniedCount);
		govDossierStaticData.setCancelledCount(cancelledCount);
		govDossierStaticData.setProcessCount(processCount);
		govDossierStaticData.setReceivedCount(receivedCount);
		govDossierStaticData.setOnlineCount(onlineCount);
		govDossierStaticData.setReleaseCount(releaseCount);
		govDossierStaticData.setBetimesCount(betimesCount);
		govDossierStaticData.setOntimeCount(ontimeCount);
		govDossierStaticData.setOvertimeCount(overtimeCount);
		govDossierStaticData.setOvertimeInside(overtimeInside);
		govDossierStaticData.setOvertimeOutside(overtimeOutside);
		govDossierStaticData.setReleaseCount(releaseCount);
		govDossierStaticData.setProcessingCount(processingCount);
		govDossierStaticData.setUnresolvedCount(unresolvedCount);
		govDossierStaticData.setUndueCount(undueCount);
		govDossierStaticData.setOverdueCount(overdueCount);
		govDossierStaticData.setInteroperatingCount(interoperatingCount);
		govDossierStaticData.setWaitingCount(waitingCount);
		govDossierStaticData.setOntimePercentage(ontimePercentage);

		govDossierStaticData.setMonth(month);
		govDossierStaticData.setYear(year);
		govDossierStaticData.setGovAgencyCode(govAgencyCode);
		govDossierStaticData.setGovAgencyName(govAgencyName);

		govDossierStaticData.setCompanyId(companyId);
		govDossierStaticData.setGroupId(groupId);

		logAsFormattedJson(LOG, govDossierStaticData);
	}

	private static Date getFirstDay() {
		LocalDateTime localDateTime = LocalDateTime.now();

		localDateTime.with(TemporalAdjusters.firstDayOfMonth());
		localDateTime.with(LocalTime.MIN);

		Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();

		return Date.from(instant);
	}

	public static void logAsFormattedJson(Logger logger, Object obj) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			String contentFormatted = mapper.writeValueAsString(obj);
			logger.info("Content: \n {}", contentFormatted);
		} catch (JsonProcessingException e) {
			logger.info("Error printing REST request! {}", e);
		}
	}

	private void getDossierStatistic(int type, long groupId) {

		GovAgencyRequest agencyRequest = new GovAgencyRequest();

		agencyRequest.setGroupId(groupId);

		try {
			GovAgencyResponse agencyResponse = callService.callRestService(agencyRequest);

			LocalDate cal = LocalDate.now();

			int month = cal.getMonthValue();
			int year = cal.getYear();

			for (GovAgencyData agencyData : agencyResponse.getData()) {
				// LOG.info("GovAgencyCode " + agencyData.getItemCode());

				/* 2. Get dossier in GOV */

				GetDossierRequest payload = new GetDossierRequest();

				if (type != TYPE_1_ALL_GOV_ALL_DOMAIN) {
					payload.setGovAgencyCode(agencyData.getItemCode());
				}
				payload.setGroupId(groupId);

				/* Get List dossier in month of a govagency */
				GetDossierResponse dossierResponse = callDossierRestService.callRestService(payload);

				/* Store count in HashMap */

				HashMap<String, DossierStatisticData> domainStatisticData = new HashMap<String, DossierStatisticData>();

				int totalCount = dossierResponse.getTotal();
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

				if (dossierResponse.getTotal() > 0) {

					totalCount = dossierResponse.getTotal();

					for (GetDossierData dossierData : dossierResponse.getData()) {

						CommonRequest request = new CommonRequest();
						request.setGroupId(dossierData.getGroupId());
						request.setKeyword(dossierData.getServiceCode());

						// LOG.info("GROUPID_" + dossierData.getGroupId());
						// LOG.info("GROUPID_" + dossierData.getServiceCode());

						ServiceInfoResponse serviceInfoResponse = callServiceInfo.callRestService(request);

						DossierStatisticData dossierStatisticData = new DossierStatisticData();

						if (domainStatisticData.containsKey(serviceInfoResponse.getDomainCode())) {
							dossierStatisticData = domainStatisticData.get(serviceInfoResponse.getDomainCode());
						} else {
							domainStatisticData.put(serviceInfoResponse.getDomainCode(), dossierStatisticData);
						}

						dossierStatisticData.setTotalCount(dossierStatisticData.getTotalCount() + 1);

						/* DENIED */
						if (dossierData.getDossierStatus().contentEquals(DossierStatusContants.DOSSIER_STATUS_DENIED)) {
							deniedCount = deniedCount + 1;

							dossierStatisticData.setDeniedCount(dossierStatisticData.getDeniedCount() + 1);
						}

						/* CANCELLED */
						if (dossierData.getDossierStatus()
								.contentEquals(DossierStatusContants.DOSSIER_STATUS_CANCELLED)) {
							cancelledCount = cancelledCount + 1;

							dossierStatisticData.setCancelledCount(dossierStatisticData.getCancelledCount() + 1);
						}

						/* PROCESS */
						if (!dossierData.getDossierStatus().contentEquals(DossierStatusContants.DOSSIER_STATUS_DENIED)
								&& !dossierData.getDossierStatus()
										.contentEquals(DossierStatusContants.DOSSIER_STATUS_CANCELLED)) {

							processCount = processCount + 1;

							dossierStatisticData.setProcessCount(dossierStatisticData.getProcessCount() + 1);

							/* REVEICED */
							if (Validator.isNotNull(dossierData.getReceiveDate())
									&& dossierData.getReceiveDate().after(getFirstDay())) {
								receivedCount = receivedCount + 1;

								dossierStatisticData.setReceivedCount(dossierStatisticData.getReceivedCount() + 1);

								/* ONLINE */
								if (dossierData.getOnline()) {
									onlineCount = onlineCount + 1;

									dossierStatisticData.setOnlineCount(dossierStatisticData.getOnlineCount() + 1);
								}
							} else {
								/* REMAINING */
								remainingCount = remainingCount + 1;

								dossierStatisticData.setRemainingCount(dossierStatisticData.getRemainingCount() + 1);

							}

							/* RELEASED */
							if (dossierData.getDossierStatus().contentEquals(DossierStatusContants.DOSSIER_STATUS_DONE)
									|| dossierData.getDossierStatus()
											.contentEquals(DossierStatusContants.DOSSIER_STATUS_RELEASING)
									|| dossierData.getDossierStatus()
											.contentEquals(DossierStatusContants.DOSSIER_STATUS_POSTING)
									|| dossierData.getDossierStatus()
											.contentEquals(DossierStatusContants.DOSSIER_STATUS_UNRESOLVED)) {

								releaseCount = releaseCount + 1;

								dossierStatisticData.setReleaseCount(dossierStatisticData.getReleaseCount() + 1);

								/* DONE */
								if (dossierData.getDossierStatus()
										.contentEquals(DossierStatusContants.DOSSIER_STATUS_DONE)
										|| dossierData.getDossierStatus()
												.contentEquals(DossierStatusContants.DOSSIER_STATUS_POSTING)) {
									doneCount = doneCount + 1;

									dossierStatisticData.setDoneCount(dossierStatisticData.getDoneCount() + 1);

									/* BETIME */
									if (Validator.isNotNull(dossierData.getDueDate())
											&& Validator.isNotNull(dossierData.getExtendDate())
											&& dossierData.getExtendDate().after(dossierData.getDueDate())) {

										betimesCount = betimesCount + 1;

										dossierStatisticData
												.setBetimesCount(dossierStatisticData.getBetimesCount() + 1);

									} else {
										/* ONTIMECOUNT */
										if (Validator.isNull(dossierData.getDueDate())
												|| dossierData.getReleaseDate().before(dossierData.getDueDate())) {

											ontimeCount = onlineCount + 1;

											dossierStatisticData
													.setOntimeCount(dossierStatisticData.getOntimeCount() + 1);
										}
									}
								}

								/* RELEASING */
								if (dossierData.getDossierStatus()
										.contentEquals(DossierStatusContants.DOSSIER_STATUS_RELEASING)) {
									releasingCount = releasingCount + 1;

									dossierStatisticData
											.setReleasingCount(dossierStatisticData.getReleasingCount() + 1);
								}

								/* UNRESOLVED */
								if (dossierData.getDossierStatus()
										.contentEquals(DossierStatusContants.DOSSIER_STATUS_UNRESOLVED)) {
									unresolvedCount = unresolvedCount + 1;

									dossierStatisticData
											.setUnresolvedCount(dossierStatisticData.getUnresolvedCount() + 1);
								}
							} else if (dossierData.getDossierStatus()
									.contentEquals(DossierStatusContants.DOSSIER_STATUS_WAITING)) {
								/* WAITING */
								waitingCount = waitingCount + 1;

								dossierStatisticData.setWaitingCount(dossierStatisticData.getWaitingCount() + 1);

							} else {
								/* PROCESSING */
								processingCount = processingCount + 1;

								dossierStatisticData.setProcessingCount(dossierStatisticData.getProcessingCount() + 1);

								/* INTEROPERATING */
								if (dossierData.getDossierStatus()
										.contentEquals(DossierStatusContants.DOSSIER_STATUS_INTEROPERATING)) {
									interoperatingCount = interoperatingCount + 1;

									dossierStatisticData
											.setInteroperatingCount(dossierStatisticData.getInteroperatingCount() + 1);
								}

								/* UNDUE */
								if (Validator.isNull(dossierData.getDueDate())
										|| dossierData.getDueDate().after(new Date())) {
									undueCount = undueCount + 1;

									dossierStatisticData.setUndueCount(dossierStatisticData.getUndueCount() + 1);
								}
							}
						}

						dossierStatisticData.setOvertimeOutside(0);
						dossierStatisticData.setOvertimeInside(
								dossierStatisticData.getOvertimeCount() - dossierStatisticData.getOvertimeOutside());
						dossierStatisticData.setOvertimeCount((dossierStatisticData.getDoneCount()
								+ dossierStatisticData.getReleasingCount())
								- (dossierStatisticData.getOntimeCount() + dossierStatisticData.getBetimesCount()));

						dossierStatisticData.setOverdueCount(
								dossierStatisticData.getProcessingCount() - dossierStatisticData.getUndueCount());

						int domainSubTotal = dossierStatisticData.getUndueCount()
								+ dossierStatisticData.getOverdueCount() + dossierStatisticData.getBetimesCount()
								+ dossierStatisticData.getOntimeCount() + dossierStatisticData.getOvertimeCount();

						if (domainSubTotal != 0) {

							int domainOntimePercentage = (dossierStatisticData.getUndueCount()
									+ dossierStatisticData.getBetimesCount() + dossierStatisticData.getOntimeCount())
									* 100 / domainSubTotal;

							dossierStatisticData.setOntimePercentage(domainOntimePercentage);
						}

						dossierStatisticData.setMonth(month);
						dossierStatisticData.setYear(year);
						dossierStatisticData.setGovAgencyCode(dossierData.getGovAgencyCode());
						dossierStatisticData.setGovAgencyName(dossierData.getGovAgencyName());
						dossierStatisticData.setDomainCode(serviceInfoResponse.getDomainCode());
						dossierStatisticData.setDomainName(serviceInfoResponse.getDomainName());

						domainStatisticData.put(serviceInfoResponse.getDomainCode(), dossierStatisticData);

					}

					logAsFormattedJson(LOG, domainStatisticData);

					overtimeOutside = 0;

					overtimeInside = overtimeCount - overtimeOutside;

					overtimeCount = (doneCount + releasingCount) - (ontimeCount + betimesCount);

					overdueCount = processingCount - undueCount;

					int subTotal = undueCount + overdueCount + betimesCount + ontimeCount + overtimeCount;

					if (subTotal != 0) {
						ontimePercentage = (undueCount + betimesCount + ontimeCount) * 100 / subTotal;
					}

					DossierStatisticData govDossierStaticData = new DossierStatisticData();

					govDossierStaticData.setTotalCount(totalCount);
					govDossierStaticData.setDeniedCount(deniedCount);
					govDossierStaticData.setCancelledCount(cancelledCount);
					govDossierStaticData.setProcessCount(processCount);
					govDossierStaticData.setReceivedCount(receivedCount);
					govDossierStaticData.setOnlineCount(onlineCount);
					govDossierStaticData.setReleaseCount(releaseCount);
					govDossierStaticData.setBetimesCount(betimesCount);
					govDossierStaticData.setOntimeCount(ontimeCount);
					govDossierStaticData.setOvertimeCount(overtimeCount);
					govDossierStaticData.setOvertimeInside(overtimeInside);
					govDossierStaticData.setOvertimeOutside(overtimeOutside);
					govDossierStaticData.setReleaseCount(releaseCount);
					govDossierStaticData.setProcessingCount(processingCount);
					govDossierStaticData.setUnresolvedCount(unresolvedCount);
					govDossierStaticData.setUndueCount(undueCount);
					govDossierStaticData.setOverdueCount(overdueCount);
					govDossierStaticData.setInteroperatingCount(interoperatingCount);
					govDossierStaticData.setWaitingCount(waitingCount);
					govDossierStaticData.setOntimePercentage(ontimePercentage);

					govDossierStaticData.setMonth(month);
					govDossierStaticData.setYear(year);
					govDossierStaticData.setGovAgencyCode(agencyData.getItemCode());
					govDossierStaticData.setGovAgencyName(agencyData.getItemName());

					logAsFormattedJson(LOG, govDossierStaticData);

					updateGovService.updateDossierStatistic(govDossierStaticData);
					updateDomainService.updateDossierStatistic(domainStatisticData);

				}

			}

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private void doUpdateStatistic() {
		LOG.info("START doUpdateStatistic(): " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

		GovAgencyRequest agencyRequest = new GovAgencyRequest();

		agencyRequest.setGroupId(
				GetterUtil.getLong(DossierStatisticConfig.get(DossierStatisticConstants.OPENCPS_GROUP_CONFIG)));

		try {
			/* 1. Get GovAgency List */
			GovAgencyResponse agencyResponse = callService.callRestService(agencyRequest);

			for (GovAgencyData agencyData : agencyResponse.getData()) {
				LOG.info("GovAgencyCode " + agencyData.getItemCode());

				/* 2. Get dossier in GOV */

				GetDossierRequest payload = new GetDossierRequest();

				payload.setGovAgencyCode(agencyData.getItemCode());

				GetDossierResponse dossierResponse = new GetDossierResponse();

				dossierResponse = callDossierRestService.callRestService(payload);

				int totalCount = dossierResponse.getTotal();

				/* Set dossier status = denied */
				payload.setDossierStatus(DossierStatusContants.DOSSIER_STATUS_DENIED);

				int deniedCount = callDossierRestService.callRestService(payload).getTotal();

				/* Set dossier status = cancelled */
				payload.setDossierStatus(DossierStatusContants.DOSSIER_STATUS_CANCELLED);

				int cancelledCount = callDossierRestService.callRestService(payload).getTotal();

				int processCount = totalCount - (deniedCount + cancelledCount);

				LOG.info("totalCount" + totalCount + "deniedCount_" + deniedCount + "cancelledCount_" + cancelledCount
						+ "processCount_" + processCount);

				/* reset dossier status */
				payload.setDossierStatus(StringPool.BLANK);

				/* add condition received */
				payload.setReceived(true);

				int receivedCount = callDossierRestService.callRestService(payload).getTotal();

				/* add condition online */
				payload.setOnline(true);
				payload.setOnlineValue(true);

				int onlineCount = callDossierRestService.callRestService(payload).getTotal();

				int onegateCount = receivedCount - onlineCount;

				int remainingCount = processCount - receivedCount;

				LOG.info("receivedCount" + receivedCount + "onlineCount" + onlineCount + "onegateCount" + onegateCount
						+ "remainingCount" + remainingCount);

				/* reset received, online */
				payload.setReceived(false);
				payload.setOnline(false);
				payload.setOnlineValue(false);

				/* add release count */
				payload.setReleased(true);
				int releaseCount = callDossierRestService.callRestService(payload).getTotal();

				/* reset dossier release */
				payload.setReleased(false);

				/* add dossier status = done */
				payload.setDossierStatus(DossierStatusContants.DOSSIER_STATUS_DONE);

				int doneIsDone = callDossierRestService.callRestService(payload).getTotal();

				/* add dossier status = done */
				payload.setDossierStatus(DossierStatusContants.DOSSIER_STATUS_POSTING);

				int doneIsPosting = callDossierRestService.callRestService(payload).getTotal();

				int doneCount = doneIsDone + doneIsPosting;

				LOG.info("releaseCount" + releaseCount + "doneCount" + doneCount);

				/* set dossier status = unresolved */
				payload.setDossierStatus(DossierStatusContants.DOSSIER_STATUS_UNRESOLVED);

				int unresolvedCount = callDossierRestService.callRestService(payload).getTotal();

				int releasingCount = receivedCount - (doneCount + unresolvedCount);

				LOG.info("releasingCount" + releasingCount + "unresolvedCount" + unresolvedCount);

				/* reset dossier status */
				payload.setDossierStatus(StringPool.BLANK);

				/* set betime */
				payload.setBetime(true);
				payload.setReleased(true);

				int betimesCount = callDossierRestService.callRestService(payload).getTotal();

				/* set ontime */
				payload.setBetime(false);
				payload.setOntime(true);

				int ontimeCount = callDossierRestService.callRestService(payload).getTotal();

				int overtimeCount = releaseCount - (ontimeCount + betimesCount);

				LOG.info("overtimeCount" + overtimeCount + "ontimeCount" + ontimeCount + "betimesCount" + betimesCount);

				/* reset release, ontime, betime */
				payload.setBetime(false);
				payload.setOntime(false);
				payload.setReleased(false);

				/* TODO update this value later, need more logic from BA */
				int overtimeOutside = 0;

				int overtimeInside = overtimeCount - overtimeOutside;

				/* set dossier status = procesing */
				payload.setDossierStatus(DossierStatusContants.DOSSIER_STATUS_PROCESING);

				int processingIsPosting = callDossierRestService.callRestService(payload).getTotal();

				/* set dossier status = interoperating */
				payload.setDossierStatus(DossierStatusContants.DOSSIER_STATUS_INTEROPERATING);

				int processingIsInteroperating = callDossierRestService.callRestService(payload).getTotal();

				int processingCount = processingIsPosting + processingIsInteroperating;

				LOG.info("overtimeInside" + overtimeInside + "processingIsPosting" + processingIsPosting
						+ "processingIsInteroperating" + processingIsInteroperating + "processingCount"
						+ processingCount);

				/* reset dossier status */
				payload.setDossierStatus(StringPool.BLANK);
				payload.setUndue(true);

				int undueCount = callDossierRestService.callRestService(payload).getTotal();

				/* reset undue */
				payload.setUndue(false);

				int overdueCount = processingCount - undueCount;

				int waitingCount = processCount - (releaseCount + processingCount);

				double ontimePercentage = 0.0;

				if (releaseCount != 0) {
					ontimePercentage = (betimesCount + onlineCount) * 100 / releaseCount;
				}

				LOG.info("undueCount" + undueCount + "overdueCount" + overdueCount + "waitingCount" + waitingCount
						+ "ontimePercentage" + ontimePercentage);

			}

		} catch (Exception e) {
			LOG.info(e.getMessage());
		}

	}

	@Activate
	@Modified
	protected void activate() {
		schedulerEntryImpl.setTrigger(TriggerFactoryUtil.createTrigger(getEventListenerClass(), getEventListenerClass(),
				45, TimeUnit.SECOND));
		_schedulerEngineHelper.register(this, schedulerEntryImpl, DestinationNames.SCHEDULER_DISPATCH);
	}

	@Deactivate
	protected void deactivate() {
		_schedulerEngineHelper.unregister(this);
	}

	@Reference(target = ModuleServiceLifecycle.PORTAL_INITIALIZED, unbind = "-")
	protected void setModuleServiceLifecycle(ModuleServiceLifecycle moduleServiceLifecycle) {
	}

	@Reference(unbind = "-")
	protected void setSchedulerEngineHelper(SchedulerEngineHelper schedulerEngineHelper) {

		_schedulerEngineHelper = schedulerEngineHelper;
	}

	@Reference(unbind = "-")
	protected void setTriggerFactory(TriggerFactory triggerFactory) {
	}
}
