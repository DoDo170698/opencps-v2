package org.opencps.dossiermgt.action.impl;

import com.liferay.counter.kernel.service.CounterLocalServiceUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.NoSuchUserException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.SortFactoryUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.HttpMethod;

import org.opencps.auth.utils.APIDateTimeUtils;
import org.opencps.communication.model.Notificationtemplate;
import org.opencps.communication.service.NotificationQueueLocalServiceUtil;
import org.opencps.communication.service.NotificationtemplateLocalServiceUtil;
import org.opencps.datamgt.model.DictCollection;
import org.opencps.datamgt.model.DictItem;
import org.opencps.datamgt.service.DictCollectionLocalServiceUtil;
import org.opencps.datamgt.service.DictItemLocalServiceUtil;
import org.opencps.datamgt.util.HolidayUtils;
import org.opencps.dossiermgt.action.DossierActions;
import org.opencps.dossiermgt.action.DossierFileActions;
import org.opencps.dossiermgt.action.PaymentFileActions;
import org.opencps.dossiermgt.action.util.AutoFillFormData;
import org.opencps.dossiermgt.action.util.DeliverableNumberGenerator;
import org.opencps.dossiermgt.action.util.DocumentTypeNumberGenerator;
import org.opencps.dossiermgt.action.util.DossierActionUtils;
import org.opencps.dossiermgt.action.util.DossierContentGenerator;
import org.opencps.dossiermgt.action.util.DossierMgtUtils;
import org.opencps.dossiermgt.action.util.DossierNumberGenerator;
import org.opencps.dossiermgt.action.util.DossierPaymentUtils;
import org.opencps.dossiermgt.action.util.PaymentUrlGenerator;
import org.opencps.dossiermgt.constants.ActionConfigTerm;
import org.opencps.dossiermgt.constants.DeliverableTypesTerm;
import org.opencps.dossiermgt.constants.DossierActionTerm;
import org.opencps.dossiermgt.constants.DossierActionUserTerm;
import org.opencps.dossiermgt.constants.DossierDocumentTerm;
import org.opencps.dossiermgt.constants.DossierFileTerm;
import org.opencps.dossiermgt.constants.DossierPartTerm;
import org.opencps.dossiermgt.constants.DossierStatusConstants;
import org.opencps.dossiermgt.constants.DossierSyncTerm;
import org.opencps.dossiermgt.constants.DossierTerm;
import org.opencps.dossiermgt.constants.PaymentFileTerm;
import org.opencps.dossiermgt.constants.ProcessActionTerm;
import org.opencps.dossiermgt.constants.ProcessStepRoleTerm;
import org.opencps.dossiermgt.constants.StepConfigTerm;
import org.opencps.dossiermgt.model.ActionConfig;
import org.opencps.dossiermgt.model.DeliverableType;
import org.opencps.dossiermgt.model.DocumentType;
import org.opencps.dossiermgt.model.Dossier;
import org.opencps.dossiermgt.model.DossierAction;
import org.opencps.dossiermgt.model.DossierActionUser;
import org.opencps.dossiermgt.model.DossierDocument;
import org.opencps.dossiermgt.model.DossierFile;
import org.opencps.dossiermgt.model.DossierMark;
import org.opencps.dossiermgt.model.DossierPart;
import org.opencps.dossiermgt.model.DossierTemplate;
import org.opencps.dossiermgt.model.PaymentConfig;
import org.opencps.dossiermgt.model.PaymentFile;
import org.opencps.dossiermgt.model.ProcessAction;
import org.opencps.dossiermgt.model.ProcessOption;
import org.opencps.dossiermgt.model.ProcessPlugin;
import org.opencps.dossiermgt.model.ProcessSequence;
import org.opencps.dossiermgt.model.ProcessStep;
import org.opencps.dossiermgt.model.ProcessStepRole;
import org.opencps.dossiermgt.model.ServiceConfig;
import org.opencps.dossiermgt.model.ServiceProcess;
import org.opencps.dossiermgt.model.ServiceProcessRole;
import org.opencps.dossiermgt.model.StepConfig;
import org.opencps.dossiermgt.rest.utils.OpenCPSConverter;
import org.opencps.dossiermgt.scheduler.InvokeREST;
import org.opencps.dossiermgt.scheduler.RESTFulConfiguration;
import org.opencps.dossiermgt.service.ActionConfigLocalServiceUtil;
import org.opencps.dossiermgt.service.DeliverableTypeLocalServiceUtil;
import org.opencps.dossiermgt.service.DocumentTypeLocalServiceUtil;
import org.opencps.dossiermgt.service.DossierActionLocalServiceUtil;
import org.opencps.dossiermgt.service.DossierActionUserLocalServiceUtil;
import org.opencps.dossiermgt.service.DossierDocumentLocalServiceUtil;
import org.opencps.dossiermgt.service.DossierFileLocalServiceUtil;
import org.opencps.dossiermgt.service.DossierLocalServiceUtil;
import org.opencps.dossiermgt.service.DossierMarkLocalServiceUtil;
import org.opencps.dossiermgt.service.DossierPartLocalServiceUtil;
import org.opencps.dossiermgt.service.DossierRequestUDLocalServiceUtil;
import org.opencps.dossiermgt.service.DossierSyncLocalServiceUtil;
import org.opencps.dossiermgt.service.DossierTemplateLocalServiceUtil;
import org.opencps.dossiermgt.service.PaymentConfigLocalServiceUtil;
import org.opencps.dossiermgt.service.PaymentFileLocalServiceUtil;
import org.opencps.dossiermgt.service.ProcessActionLocalServiceUtil;
import org.opencps.dossiermgt.service.ProcessOptionLocalServiceUtil;
import org.opencps.dossiermgt.service.ProcessPluginLocalServiceUtil;
import org.opencps.dossiermgt.service.ProcessSequenceLocalServiceUtil;
import org.opencps.dossiermgt.service.ProcessStepLocalServiceUtil;
import org.opencps.dossiermgt.service.ProcessStepRoleLocalServiceUtil;
import org.opencps.dossiermgt.service.ServiceConfigLocalServiceUtil;
import org.opencps.dossiermgt.service.ServiceProcessLocalServiceUtil;
import org.opencps.dossiermgt.service.ServiceProcessRoleLocalServiceUtil;
import org.opencps.dossiermgt.service.StepConfigLocalServiceUtil;
import org.opencps.dossiermgt.service.comparator.DossierFileComparator;
import org.opencps.usermgt.model.Employee;
import org.opencps.usermgt.service.EmployeeLocalServiceUtil;
import org.opencps.usermgt.service.util.OCPSUserUtils;

import backend.auth.api.exception.NotFoundException;

public class DossierActionsImpl implements DossierActions {

	public static final String SPECIAL_ACTION = "1100";
	public static final String AUTO_EVENT_SUBMIT = "submit";
	public static final String AUTO_EVENT_TIMMER = "timer";
	public static final String AUTO_EVENT_LISTENER = "listener";
	public static final String AUTO_EVENT_SPECIAL = "special";
	public static final String DOSSIER_SATUS_DC_CODE = "DOSSIER_STATUS";
	public static final String DOSSIER_SUB_SATUS_DC_CODE = "DOSSIER_SUB_STATUS";
	private static final long VALUE_CONVERT_DATE_TIMESTAMP = 1000 * 60 * 60 * 24;
	private static final long VALUE_CONVERT_HOUR_TIMESTAMP = 1000 * 60 * 60;
	private static final String EXTEND_ONE_VALUE = ".0";
	private static final String EXTEND_TWO_VALUE = ".00";
	
	

	@Override
	public JSONObject getDossiers(long userId, long companyId, long groupId, LinkedHashMap<String, Object> params,
			Sort[] sorts, int start, int end, ServiceContext serviceContext) {

		JSONObject result = JSONFactoryUtil.createJSONObject();

		Hits hits = null;

		SearchContext searchContext = new SearchContext();
		searchContext.setCompanyId(companyId);

		try {

			hits = DossierLocalServiceUtil.searchLucene(params, sorts, start, end, searchContext);
			
			result.put("data", hits.toList());

			long total = DossierLocalServiceUtil.countLucene(params, searchContext);

			result.put("total", total);

		} catch (Exception e) {
			_log.error(e);
		}

		return result;

	}

	@Override
	public JSONObject getDossiersTest(long userId, long companyId, long groupId, LinkedHashMap<String, Object> params,
			Sort[] sorts, int start, int end, ServiceContext serviceContext) {

		JSONObject result = JSONFactoryUtil.createJSONObject();

		Hits hits = null;

		SearchContext searchContext = new SearchContext();
		searchContext.setCompanyId(companyId);

		try {

			String statusCode = GetterUtil.getString(params.get(DossierTerm.STATUS));

			String subStatusCode = GetterUtil.getString(params.get(DossierTerm.SUBSTATUS));
			if (groupId == 55217) {
				hits = DossierLocalServiceUtil.searchLucene(params, sorts, start, end, searchContext);

				result.put("data", hits.toList());

				long total = DossierLocalServiceUtil.countLucene(params, searchContext);

				result.put("total", total);

				return result;
			}

			// Get collection with collection Code
			DictCollection dictCollection = DictCollectionLocalServiceUtil.fetchByF_dictCollectionCode("DOSSIER_STATUS",
					groupId);
			_log.info("dictCollection: "+dictCollection+" |groupId: "+groupId);

			// Get list dossier follow status Code
			if (Validator.isNotNull(statusCode) || Validator.isNotNull(subStatusCode)) {
//				_log.info("52" + sb.toString());

				DictItem dictItem = null;
				if (Validator.isNotNull(subStatusCode)) {
					dictItem = DictItemLocalServiceUtil.fetchByF_dictItemCode(subStatusCode,
							dictCollection.getDictCollectionId(), groupId);
				} else {
					dictItem = DictItemLocalServiceUtil.fetchByF_dictItemCode(statusCode,
							dictCollection.getDictCollectionId(), groupId);
				}

				long total = 0;
				if (dictItem != null) {
					_log.info("53");
					String metaData = dictItem.getMetaData();
					String specialStatus = StringPool.BLANK;
					if (Validator.isNotNull(metaData)) {
						_log.info("metaData: " + metaData);
						try {
							JSONObject metaJson = JSONFactoryUtil.createJSONObject(metaData);
							specialStatus = metaJson.getString("specialStatus");
//							_log.info("specialStatus: " + specialStatus);

						} catch (Exception e) {
							_log.debug(e);
							//_log.error(e);
						}
					}

					if (Validator.isNotNull(specialStatus) && Boolean.parseBoolean(specialStatus)) {
//						params.put(DossierTerm.DOSSIER_ACTION_ID, sb.toString());
						params.put(DossierTerm.FOLLOW, String.valueOf(true));
					} else {
						params.put(DossierTerm.FOLLOW, false);
					}

					hits = DossierLocalServiceUtil.searchLucene(params, sorts, start, end, searchContext);
					if (hits != null && hits.getLength() > 0) {
						result.put("data", hits.toList());
//						_log.info("hits.toList(): " + hits.toList().size());
						total = DossierLocalServiceUtil.countLucene(params, searchContext);
//						_log.info("total: " + total);
						result.put("total", total);
					}
				}
			} else { /* Get list dossier follow roles user login */

				List<DictItem> dictItems = DictItemLocalServiceUtil
						.findByF_dictCollectionId(dictCollection.getDictCollectionId());

				// Get list dossier follow each status
				if (dictItems != null && dictItems.size() > 0) {
					_log.info("dictItems: " + dictItems.size());
					List<Document> allDocsList = new ArrayList<Document>();
					long total = 0;
					for (DictItem dictItem : dictItems) {
						_log.info("dictItem: " + dictItem);
						String metaData = dictItem.getMetaData();
						_log.info("metaData: " + metaData);
						String specialStatus = StringPool.BLANK;
						if (Validator.isNotNull(metaData)) {
//							_log.info("metaData: " + metaData);
							try {
								JSONObject metaJson = JSONFactoryUtil.createJSONObject(metaData);
								specialStatus = metaJson.getString("specialStatus");
								_log.info("specialStatus: " + specialStatus);

							} catch (Exception e) {
								_log.debug(e);
								//_log.error(e);
							}
						}

//						statusCode = StringPool.BLANK;
						subStatusCode = StringPool.BLANK;
						// Get info status of dossier
						if (dictItem.getParentItemId() != 0) {
							subStatusCode = dictItem.getItemCode();
//							_log.info("subStatusCode: " + subStatusCode);
							DictItem parentDictItem = DictItemLocalServiceUtil.getDictItem(dictItem.getParentItemId());
							statusCode = parentDictItem.getItemCode();
//							_log.info("statusCode: " + statusCode);
						} else {
							statusCode = dictItem.getItemCode();
//							_log.info("statusCode: " + statusCode);
						}

						// Check permission user login
						boolean isPermission = checkPermission(statusCode, subStatusCode, groupId, userId);

						if (isPermission) {
//							_log.info("isPermission: " + isPermission);
//							_log.info("userId: " + userId);
							// _log.info("strdossierActionId: " +
							// sb.toString());

							if (Validator.isNotNull(specialStatus) && Boolean.parseBoolean(specialStatus)) {
								// Add params
								params.put(DossierTerm.STATUS, statusCode);
								params.put(DossierTerm.SUBSTATUS, subStatusCode);
								// params.put(DossierTerm.DOSSIER_ACTION_ID,
								// sb.toString());
								params.put(DossierTerm.FOLLOW, String.valueOf(true));

								hits = DossierLocalServiceUtil.searchLucene(params, sorts, -1, -1, searchContext);

								if (hits != null && hits.getLength() > 0) {
									long count = DossierLocalServiceUtil.countLucene(params, searchContext);
//									_log.info("count: " + count);
									if (dictItem.getParentItemId() != 0) {
										allDocsList.addAll(hits.toList());
//										_log.info("SizeList1: " + hits.toList().size());
										total += count;
									}
								}

							} else {
								params.put(DossierTerm.STATUS, statusCode);
								params.put(DossierTerm.SUBSTATUS, subStatusCode);
								params.put(DossierTerm.FOLLOW, String.valueOf(false));

								hits = DossierLocalServiceUtil.searchLucene(params, sorts, -1, -1, searchContext);

								if (hits != null && hits.getLength() > 0) {
									// allDocsList.addAll(hits.toList());
									// _log.info("SizeList2: " +
									// hits.toList().size());
									long count = DossierLocalServiceUtil.countLucene(params, searchContext);
									_log.info("count: " + count);
									if (dictItem.getParentItemId() != 0) {
										allDocsList.addAll(hits.toList());
										_log.info("SizeList2: " + hits.toList().size());
										total += count;
									}
								}
							}
						}
					}
					// Add all list dossier of multiple status
					result.put("data", allDocsList);
					result.put("total", total);
				}
			}

		} catch (Exception e) {
			_log.error(e);
		}

		return result;

	}

	@Override
	public JSONObject getDossierProcessList(long userId, long companyId, long groupId,
			LinkedHashMap<String, Object> params, Sort[] sorts, Integer start, Integer end,
			ServiceContext serviceContext) {

		SearchContext searchContext = new SearchContext();
		searchContext.setCompanyId(companyId);

		JSONObject result = JSONFactoryUtil.createJSONObject();
		Hits hits = null;

		try {

			hits = DossierLocalServiceUtil.searchLucene(params, sorts, start, end, searchContext);
//			_log.info("hits.toList():" +hits.toList());

			result.put("data", hits.toList());

			long total = DossierLocalServiceUtil.countLucene(params, searchContext);
//			_log.info("total:" +total);

			result.put("total", total);

			return result;

		} catch (Exception e) {
			_log.error(e);
		}

		return result;

	}

	@Override
	public Dossier addDossier(long groupId, long dossierId, String referenceUid, int counter, String serviceCode,
			String serviceName, String govAgencyCode, String govAgencyName, String applicantName,
			String applicantIdType, String applicantIdNo, Date applicantIdDate, String address, String cityCode,
			String cityName, String districtCode, String districtName, String wardCode, String wardName,
			String contactName, String contactTelNo, String contactEmail, String dossierTemplateNo, String dossierNote,
			String submissionNote, String applicantNote, String briefNote, String dossierNo, boolean submitting,
			Date correctingDate, String dossierStatus, String dossierStatusText, String dossierSubStatus,
			String dossierSubStatusText, long folderId, long dossierActionId, int viaPostal, String postalAddress,
			String postalCityCode, String postalCityName, String postalTelNo, String password, boolean notification,
			boolean online, String serverNo, ServiceContext context) throws PortalException {

		Dossier dossier = DossierLocalServiceUtil.updateDossier(groupId, dossierId, referenceUid, counter, serviceCode,
				serviceName, govAgencyCode, govAgencyName, applicantName, applicantIdType, applicantIdNo,
				applicantIdDate, address, cityCode, cityName, districtCode, districtName, wardCode, wardName,
				contactName, contactTelNo, contactEmail, dossierTemplateNo, dossierNote, submissionNote, applicantNote,
				briefNote, dossierNo, submitting, correctingDate, dossierStatus, dossierStatusText, dossierSubStatus,
				dossierSubStatusText, folderId, dossierActionId, viaPostal, postalAddress, postalCityCode,
				postalCityName, postalTelNo, password, notification, online, serverNo, context);

		return dossier;
	}

	@Override
	public Dossier getDossierDetail(long groupId, long dossierId, String referenceUid) throws PortalException {
		if (dossierId != 0) {
			return DossierLocalServiceUtil.fetchDossier(dossierId);
		} else {
			return DossierLocalServiceUtil.getByRef(groupId, referenceUid);
		}

	}

	@Override
	public Dossier removeDossier(long groupId, long dossierId, String referenceUid) throws PortalException {

		return DossierLocalServiceUtil.removeDossier(groupId, dossierId, referenceUid);
	}

	@Override
	public Dossier cancelDossier(long groupId, long dossierId, String referenceUid, ServiceContext context)
			throws PortalException {
		return DossierLocalServiceUtil.updateCancellingDate(groupId, dossierId, referenceUid, new Date(), context);
	}

	@Override
	public Dossier correctDossier(long groupId, long dossierId, String referenceUid, ServiceContext context)
			throws PortalException {
		return DossierLocalServiceUtil.updateCorrectingDate(groupId, dossierId, referenceUid, new Date(), context);
	}

	@Override
	public Dossier submitPostDossier(long groupId, long dossierId, String referenceUid, ServiceContext context)
			throws PortalException {
		return DossierLocalServiceUtil.updateEndosementDate(groupId, dossierId, referenceUid, new Date(), context);
	}

	@Override
	public Dossier submitDossier(long groupId, long dossierId, String referenceUid, ServiceContext context)
			throws PortalException {
		return DossierLocalServiceUtil.submitting(groupId, dossierId, referenceUid, context);
	}

	@Override
	public Dossier resetDossier(long groupId, long dossierId, String referenceUid, ServiceContext context)
			throws PortalException {
		return DossierLocalServiceUtil.reset(groupId, dossierId, referenceUid, context);
	}

	@Override
	public JSONArray getNextActions(long userId, long companyId, long groupId, LinkedHashMap<String, Object> params,
			Sort[] sorts, int start, int end, ServiceContext serviceContext) throws PortalException {

		JSONArray results = JSONFactoryUtil.createJSONArray();

		List<ProcessAction> lstProcessAction;

		Dossier dossier = null;

		DossierAction dossierAction = null;

		long serviceProcessId = 0;

		String stepCode;

		String actionCode = GetterUtil.getString(params.get(DossierActionTerm.ACTION_CODE));
		_log.info("actionCode =: " + actionCode);
		// TODO filter by Auto
		String auto = GetterUtil.getString(params.get(DossierActionTerm.AUTO));
		_log.info("auto =: " + auto);

		// auto != null ko check role, check dk preCondition = auto or submmit

		String referenceUid = GetterUtil.getString(params.get(DossierTerm.REFERENCE_UID));
		_log.info("referenceUid =: " + referenceUid);

		long dossierId = GetterUtil.getLong(params.get(DossierTerm.DOSSIER_ID));
		_log.info("dossierId =: " + dossierId);

		if (Validator.isNotNull(referenceUid)) {
			dossier = DossierLocalServiceUtil.getByRef(groupId, referenceUid);
		} else {
			//
			dossier = DossierLocalServiceUtil.fetchDossier(dossierId);
		}

		if (dossier != null) {

			long dossierActionId = dossier.getDossierActionId();

			if (Validator.isNotNull(actionCode)) {
				dossierAction = getDossierAction(dossier.getDossierId(), actionCode);
			} else {
				dossierAction = DossierActionLocalServiceUtil.fetchDossierAction(dossierActionId);
			}

			serviceProcessId = dossierAction != null ? dossierAction.getServiceProcessId() : 0;

			stepCode = dossierAction != null ? dossierAction.getStepCode() : StringPool.BLANK;

			boolean pending = dossierAction != null ? dossierAction.getPending() : false;

			if (Validator.isNotNull(stepCode)) {

				try {
					lstProcessAction = ProcessActionLocalServiceUtil.getProcessActionByG_SPID_PRESC(groupId,
							serviceProcessId, stepCode);
					for (ProcessAction processAction : lstProcessAction) {


						
						String[] preConditions = StringUtil.split(processAction.getPreCondition());

						String createDossierFiles = processAction.getCreateDossierFiles();

						String returnDossierFiles = processAction.getReturnDossierFiles();

						boolean checkPreCondition = DossierMgtUtils.checkPreCondition(preConditions, dossier);

						if (!checkPreCondition) {
							continue;
						}

						JSONObject result = JSONFactoryUtil.createJSONObject();

						String postStepCode = processAction.getPostStepCode();

						if (Validator.isNull(postStepCode)) {
							continue;
						}

						ProcessStep processStep = ProcessStepLocalServiceUtil.fetchBySC_GID(postStepCode, groupId,
								processAction.getServiceProcessId());

						List<ProcessStepRole> lstProcessStepRole = ProcessStepRoleLocalServiceUtil
								.findByP_S_ID(processStep.getProcessStepId());
						List<User> lstUser = new ArrayList<User>();

						if (Validator.isNotNull(auto) && ("submit".equals(processAction.getAutoEvent())
								|| "timer".equals(processAction.getAutoEvent()))) {
							// TODO ?
						} else {
							// Check roles

							if (lstProcessStepRole == null || lstProcessStepRole.isEmpty()) {
								// Search in ServiceProcessRole
								List<ServiceProcessRole> serviceProcessRoles = ServiceProcessRoleLocalServiceUtil
										.findByS_P_ID(serviceProcessId);

								for (ServiceProcessRole serviceProcessRole : serviceProcessRoles) {

									List<User> users = UserLocalServiceUtil
											.getRoleUsers(serviceProcessRole.getRoleId());

									if (users != null) {
										for (User user : users) {
											HashMap<String, Object> moderator = new HashMap<>();
											moderator.put("moderator", serviceProcessRole.getModerator());
											user.setModelAttributes(moderator);
										}

										lstUser.addAll(users);
									}

								}
							} else {
								for (ProcessStepRole processStepRole : lstProcessStepRole) {

									List<User> users = UserLocalServiceUtil.getRoleUsers(processStepRole.getRoleId());

									if (users != null) {
										for (User user : users) {
											HashMap<String, Object> moderator = new HashMap<>();
											moderator.put("moderator", processStepRole.getModerator());
											user.setModelAttributes(moderator);
										}

										lstUser.addAll(users);
									}
								}
							}

						}

						List<String> dossierFileTemplateNos = ListUtil.toList(StringUtil.split(createDossierFiles));

						List<String> returnDossierFileTemplateNos = ListUtil
								.toList(StringUtil.split(returnDossierFiles));

						_log.info("-----RETURN DOSSIER FILE TEMPLATE NOS----" + returnDossierFileTemplateNos.size());
						JSONArray createFiles = JSONFactoryUtil.createJSONArray();

						if (dossierFileTemplateNos != null && !dossierFileTemplateNos.isEmpty()) {

							DossierTemplate dossierTemplate = DossierTemplateLocalServiceUtil.getByTemplateNo(groupId,
									dossier.getDossierTemplateNo());

							List<DossierPart> dossierParts = DossierPartLocalServiceUtil.getByTemplateNo(groupId,
									dossierTemplate.getTemplateNo());

							if (dossierParts != null) {
								for (DossierPart dossierPart : dossierParts) {

									String fileTemplateNo = dossierPart.getFileTemplateNo();

									if (dossierFileTemplateNos.contains(fileTemplateNo)) {
										JSONObject createFile = JSONFactoryUtil.createJSONObject();
										createFile.put("dossierPartId", dossierPart.getDossierPartId());
										createFile.put("partNo", dossierPart.getPartNo());
										createFile.put("partName", dossierPart.getPartName());
										createFile.put("partTip", dossierPart.getPartTip());
										createFile.put("multiple", dossierPart.getMultiple());
										createFile.put("templateFileNo", dossierPart.getFileTemplateNo());

										long fileEntryId = 0;
										boolean eForm = false;
										String formData = StringPool.BLANK;
										String formScript = StringPool.BLANK;
										String docFileReferenceUid = StringPool.BLANK;
										boolean returned = false;
										int counter = 0;
										long dossierFileId = 0;

//										_log.info("------DO ACTION 4-5-2018------");
										
										if (dossierPart != null) {
											ServiceContext context = new ServiceContext();
											context.setScopeGroupId(dossierPart.getGroupId());
											context.setCompanyId(dossierPart.getCompanyId());
											
//											String deliverableTypeStr = dossierPart.getDeliverableType();
//											_log.info("--------DOSSIER PART NO-------" + dossierPart.getPartName());
//											
//											_log.info("-------DOSSIER PART SAMPLE DATA------" + dossierPart.getSampleData());
//											String formDataDeliverables = AutoFillFormData.sampleDataBinding(dossierPart.getSampleData(), dossierId, context);
//											_log.info("-------FORM DATA DELIVERABLE---------" + formDataDeliverables);
											
//											JSONObject formDataObj = JSONFactoryUtil.createJSONObject(formDataDeliverables);
//											String deliverableFormData = StringPool.BLANK;
//											JSONObject deliverableFormDataObj = JSONFactoryUtil.createJSONObject();											
										}

										
										//End add generate deliverable if has deliverable type
										if (Validator.isNull(dossierPart.getDeliverableType())) {
											
										List<DossierFile> dossierFilesResult = DossierFileLocalServiceUtil
												.getDossierFileByDID_FTNO_DPT(dossierId, fileTemplateNo, 2, false,
														QueryUtil.ALL_POS, QueryUtil.ALL_POS,
														new DossierFileComparator(false, "createDate", Date.class));

										if (dossierFilesResult != null && !dossierFilesResult.isEmpty()) {
											df: for (DossierFile dossierFile : dossierFilesResult) {
												if (dossierFile.getDossierPartNo().equals(dossierPart.getPartNo())) {
													eForm = dossierFile.getEForm();
													formData = dossierFile.getFormData();
													formScript = dossierFile.getFormScript();
													docFileReferenceUid = dossierFile.getReferenceUid();
													fileEntryId = dossierFile.getFileEntryId();
													if (returnDossierFileTemplateNos
															.contains(dossierFile.getFileTemplateNo())) {
														returned = true;
													}

													dossierFileId = dossierFile.getDossierFileId();

													break df;
												}
											}
										} else {
											eForm = Validator.isNotNull(dossierPart.getFormScript()) ? true : false;

											formData = AutoFillFormData.sampleDataBinding(dossierPart.getSampleData(),
													dossierId, serviceContext);
											formScript = dossierPart.getFormScript();

											if (returnDossierFileTemplateNos
													.contains(dossierPart.getFileTemplateNo())) {
												returned = true;
											}
												
// create Dossier File
	
												if (eForm) {
													DossierFileActions actions = new DossierFileActionsImpl();
	
													DossierFile dossierFile = null;
	
													try {
														dossierFile = DossierFileLocalServiceUtil
																.getDossierFileByDID_FTNO_DPT_First(dossierId,
																		fileTemplateNo, 2, false, new DossierFileComparator(
																				false, "createDate", Date.class));
													} catch (Exception e) {
														_log.debug(e);
														//_log.error(e);
													}
													if (Validator.isNull(dossierFile)) {
	
														dossierFile = actions.addDossierFile(groupId, dossierId,
																referenceUid, dossier.getDossierTemplateNo(),
																dossierPart.getPartNo(), fileTemplateNo,
																dossierPart.getPartName(), StringPool.BLANK, 0L, null,
																StringPool.BLANK, String.valueOf(false), serviceContext);
														_log.info("dossierFile create:" + dossierFile.getDossierPartNo()
																+ "Timer create :" + new Date());
													}
	
													docFileReferenceUid = dossierFile.getReferenceUid();
	
													dossierFileId = dossierFile.getDossierFileId();
												}
	
											}
											dossierFilesResult = DossierFileLocalServiceUtil
													.getDossierFileByDID_FTNO_DPT_NOT_NULL_FID(dossierId, fileTemplateNo, 2,
															0, false);

											counter = (dossierFilesResult != null && !dossierFilesResult.isEmpty())
													? dossierFilesResult.size() : 0;
											createFile.put("eform", eForm);
											createFile.put("dossierFileId", dossierFileId);
											createFile.put("formData", formData);
											createFile.put("formScript", formScript);
											createFile.put("referenceUid", docFileReferenceUid);
											createFile.put("counter", counter);
											createFile.put("returned", returned);
											createFile.put("fileEntryId", fileEntryId);
											createFiles.put(createFile);
										}
										else {
											if (dossierPart != null) {
												ServiceContext context = new ServiceContext();
												context.setScopeGroupId(dossierPart.getGroupId());
												context.setCompanyId(dossierPart.getCompanyId());
												
												String deliverableTypeStr = dossierPart.getDeliverableType();
//												_log.info("--------DOSSIER PART NO-------" + dossierPart.getPartName());
												
//												_log.info("-------DOSSIER PART SAMPLE DATA------" + dossierPart.getSampleData());
//												String formDataDeliverables = AutoFillFormData.sampleDataBinding(dossierPart.getSampleData(), dossierId, context);
////												_log.info("-------FORM DATA DELIVERABLE---------" + formDataDeliverables);
												
												JSONObject formDataObj;
//												String deliverableFormData = StringPool.BLANK;
//												JSONObject deliverableFormDataObj = JSONFactoryUtil.createJSONObject();
												
//												_log.info("------DELIVERABLE FORM DATA------" + deliverableFormData);
//												_log.info("------DELIVERABLE TYPE STR-------" + deliverableTypeStr);
												if (Validator.isNotNull(deliverableTypeStr)) {
													DeliverableType deliverableTypeObject = DeliverableTypeLocalServiceUtil.getByCode(groupId, deliverableTypeStr);
//													_log.info("Deliverable type: " + deliverableTypeObject);
													if (deliverableTypeObject != null) {
														String mappingData = deliverableTypeObject.getMappingData();
														JSONObject mappingDataObj = JSONFactoryUtil.createJSONObject(mappingData);
														if (mappingDataObj.has(DeliverableTypesTerm.DELIVERABLES_KEY)) {
															String deliverables = mappingDataObj.getString(DeliverableTypesTerm.DELIVERABLES_KEY);
															_log.info("--------DELIVERABLES----------" + deliverables);
															_log.info("--------HAS E SIGNATURE----------" + processAction.getESignature());
															_log.info("---------FILE TEMPLATE NO--------" + fileTemplateNo);
															if (Validator.isNull(deliverables)) {
																//Add one deliverable
																List<DossierFile> dossierFilesResult = DossierFileLocalServiceUtil
																		.getDossierFileByDID_FTNO_DPT(dossierId, fileTemplateNo, 2, false,
																				QueryUtil.ALL_POS, QueryUtil.ALL_POS,
																				new DossierFileComparator(false, "createDate", Date.class));
//																_log.info("DOSSIER FILE RESULT SIZE: " + dossierFilesResult.size());
																if (dossierFilesResult != null && !dossierFilesResult.isEmpty()) {
																	df: for (DossierFile dossierFile : dossierFilesResult) {
																		if (dossierFile.getDossierPartNo().equals(dossierPart.getPartNo())) {
																			eForm = dossierFile.getEForm();
																			formData = dossierFile.getFormData();
																			formScript = dossierFile.getFormScript();
																			docFileReferenceUid = dossierFile.getReferenceUid();
																			fileEntryId = dossierFile.getFileEntryId();
																			if (returnDossierFileTemplateNos
																					.contains(dossierFile.getFileTemplateNo())) {
																				returned = true;
																			}
						
																			dossierFileId = dossierFile.getDossierFileId();
						
																			break df;
																		}
																	}
																} else {
																	eForm = Validator.isNotNull(dossierPart.getFormScript()) ? true : false;
//																	_log.info("Sample data: " + dossierPart.getSampleData());
																	
																	formData = AutoFillFormData.sampleDataBinding(dossierPart.getSampleData(),
																			dossierId, serviceContext);
																	formScript = dossierPart.getFormScript();
																	
//																	_log.info("Dossier part: " + dossierPart.getPartNo());
//																	_log.info("Form data: " + formData);
																	
																	
																	if (returnDossierFileTemplateNos
																			.contains(dossierPart.getFileTemplateNo())) {
																		returned = true;
																	}
						
//																	if (Validator.isNotNull(formData)) {
											if (eForm) {
												DossierFileActions actions = new DossierFileActionsImpl();
						
																		DossierFile dossierFile = null;
						
																		try {
																			dossierFile = DossierFileLocalServiceUtil
																					.getDossierFileByDID_FTNO_DPT_First(dossierId,
																							fileTemplateNo, 2, false, new DossierFileComparator(
																									false, "createDate", Date.class));
																		} catch (Exception e) {
																			_log.debug(e);
																			//_log.error(e);
																		}
																		if (Validator.isNull(dossierFile)) {
						
																			dossierFile = actions.addDossierFile(groupId, dossierId,
																					referenceUid, dossier.getDossierTemplateNo(),
																					dossierPart.getPartNo(), fileTemplateNo,
																					dossierPart.getPartName(), StringPool.BLANK, 0L, null,
																					StringPool.BLANK, String.valueOf(false), serviceContext);
//																			_log.info("dossierFile create:" + dossierFile.getDossierPartNo()
//																					+ "Timer create :" + new Date());
																		}
						
																		docFileReferenceUid = dossierFile.getReferenceUid();
						
																		dossierFileId = dossierFile.getDossierFileId();
																		formDataObj = JSONFactoryUtil.createJSONObject(formData);
																		formDataObj.put("LicenceNo", dossierFile.getDeliverableCode());
																		formData = formDataObj.toJSONString();
																		dossierFile.setFormData(formData);
//																		_log.info("UPDATE FORM DATA GENERATE RESULT FILE");
																		DossierFileLocalServiceUtil.updateDossierFile(dossierFile);
//																		actions.updateDossierFileFormData(groupId, dossierId, docFileReferenceUid, formData, serviceContext);
																	}
																	else if (Validator.isNotNull(formData)) {
//																		_log.info("GENARATE DELIVERABLE FROM FORM DATA");
																		DeliverableType dlt = DeliverableTypeLocalServiceUtil.getByCode(groupId, dossierPart.getDeliverableType());
																		mappingData = dlt.getMappingData();
																		mappingDataObj = JSONFactoryUtil.createJSONObject(mappingData);
																		formDataObj = JSONFactoryUtil.createJSONObject(formData);
																		
																		if (mappingDataObj.has(DeliverableTypesTerm.DELIVERABLES_KEY)) {
																			deliverables = mappingDataObj.getString(DeliverableTypesTerm.DELIVERABLES_KEY);
																			if (Validator.isNull(deliverables)) {
																				_log.info("Form data plugin: " + formData);
																				DossierFile dossierFile = null;
																				
																				try {
																					dossierFile = DossierFileLocalServiceUtil
																							.getDossierFileByDID_FTNO_DPT_First(dossierId,
																									fileTemplateNo, 2, false, new DossierFileComparator(
																											false, "createDate", Date.class));
																				} catch (Exception e) {
																					_log.debug(e);
																					//_log.error(e);
																				}
																				DossierFileActions actions = new DossierFileActionsImpl();
																				
																				if (Validator.isNull(dossierFile)) {
																					dossierFile = actions.addDossierFile(groupId, dossierId,
																							referenceUid, dossier.getDossierTemplateNo(),
																							dossierPart.getPartNo(), fileTemplateNo,
																							dossierPart.getPartName(), StringPool.BLANK, 0L, null,
																							StringPool.BLANK, String.valueOf(false), serviceContext);
																					_log.info("dossierFile create:" + dossierFile.getDossierPartNo()
																							+ "Timer create :" + new Date());
																				}
								
																				docFileReferenceUid = dossierFile.getReferenceUid();
								
																				dossierFileId = dossierFile.getDossierFileId();
																				formDataObj = JSONFactoryUtil.createJSONObject(formData);
																				formDataObj.put("LicenceNo", dossierFile.getDeliverableCode());
				
																				_log.info("UPDATE FORM DATA GENERATE RESULT FILE");
																				actions.updateDossierFileFormData(groupId, dossierId, docFileReferenceUid, formData, serviceContext);																						
																			}
																			else {
																				if (formDataObj.has(deliverables)) {
																					formDataObj = JSONFactoryUtil.createJSONObject(formData);
																					JSONArray deliverablesArr = JSONFactoryUtil.createJSONArray(formDataObj.getString(deliverables));
																					dossierFilesResult = DossierFileLocalServiceUtil
																							.getDossierFileByDID_FTNO_DPT_NOT_NULL_FID(dossierId, fileTemplateNo, 2,
																									0, false);

																					counter = (dossierFilesResult != null && !dossierFilesResult.isEmpty())
																							? dossierFilesResult.size() : 0;

																					for (int i = 0; i < deliverablesArr.length(); i++) {
																						JSONObject newFormDataObj = JSONFactoryUtil.createJSONObject();

																						Iterator<?> keys = formDataObj.keys();

																						while( keys.hasNext() ) {
																						    String key = (String)keys.next();
																						    if (!key.equals(deliverables)) {
																						    	newFormDataObj.put(key, formDataObj.get(key));
																						    }
																						}							

																						JSONObject deliverableObj = deliverablesArr.getJSONObject(i);
																						
																						keys = deliverableObj.keys();

																						while( keys.hasNext() ) {
																						    String key = (String)keys.next();
																						    newFormDataObj.put(key, deliverableObj.get(key));
																						}	
																						
																						DossierFile dossierFile = null;
																						
												try {
													dossierFile = DossierFileLocalServiceUtil
															.getDossierFileByDID_FTNO_DPT_First(dossierId,
																	fileTemplateNo, 2, false, new DossierFileComparator(
																			false, "createDate", Date.class));
												} catch (Exception e) {
													_log.debug(e);
													//_log.error(e);
												}
												DossierFileActions actions = new DossierFileActionsImpl();
												if (Validator.isNull(dossierFile)) {
													dossierFile = actions.addDossierFile(groupId, dossierId,
															referenceUid, dossier.getDossierTemplateNo(),
															dossierPart.getPartNo(), fileTemplateNo,
															dossierPart.getPartName(), StringPool.BLANK, 0L, null,
															StringPool.BLANK, String.valueOf(false), serviceContext);
													// SimpleDate
													_log.info("dossierFile create:" + dossierFile.getDossierPartNo()
															+ "Timer create :" + new Date());
												}

												docFileReferenceUid = dossierFile.getReferenceUid();
										
																						dossierFileId = dossierFile.getDossierFileId();
																						formDataObj = JSONFactoryUtil.createJSONObject(formData);
																						formDataObj.put("LicenceNo", dossierFile.getDeliverableCode());
						
																						_log.info("UPDATE FORM DATA GENERATE RESULT FILE");
																						actions.updateDossierFileFormData(groupId, dossierId, docFileReferenceUid, newFormDataObj.toJSONString(), serviceContext);																						

																						createFile = JSONFactoryUtil.createJSONObject();
																						
																						createFile.put("eform", eForm);
																						createFile.put("dossierFileId", dossierFileId);
																						createFile.put("formData", formData);
																						createFile.put("formScript", formScript);
																						createFile.put("referenceUid", docFileReferenceUid);
																						createFile.put("counter", counter);
																						createFile.put("returned", returned);
																						createFile.put("fileEntryId", fileEntryId);
																						
																						createFiles.put(createFile);
																					}
																				}
																			}							
																						
																		}
																	}
																}
																dossierFilesResult = DossierFileLocalServiceUtil
																		.getDossierFileByDID_FTNO_DPT_NOT_NULL_FID(dossierId, fileTemplateNo, 2,
																				0, false);

																counter = (dossierFilesResult != null && !dossierFilesResult.isEmpty())
																		? dossierFilesResult.size() : 0;
																createFile.put("eform", eForm);
																createFile.put("dossierFileId", dossierFileId);
																createFile.put("formData", formData);
																createFile.put("formScript", formScript);
																createFile.put("referenceUid", docFileReferenceUid);
																createFile.put("counter", counter);
																createFile.put("returned", returned);
																createFile.put("fileEntryId", fileEntryId);
																createFiles.put(createFile);
															}
															else {
																List<DossierFile> dossierFilesResult = DossierFileLocalServiceUtil
																		.getDossierFileByDID_FTNO_DPT(dossierId, fileTemplateNo, 2, false,
																				QueryUtil.ALL_POS, QueryUtil.ALL_POS,
																				new DossierFileComparator(false, "createDate", Date.class));
						
																if (dossierFilesResult != null && !dossierFilesResult.isEmpty()) {
//																	dossierFilesResult = DossierFileLocalServiceUtil
//																			.getDossierFileByDID_FTNO_DPT_NOT_NULL_FID(dossierId, fileTemplateNo, 2,
//																					0, false);
//
//																	counter = (dossierFilesResult != null && !dossierFilesResult.isEmpty())
//																			? dossierFilesResult.size() : 0;
																	
																	_log.info("Dossier file result");
																	for (DossierFile dossierFile : dossierFilesResult) {
																		_log.info("Dossier file part: " + dossierFile.getDossierPartNo() + ", " + dossierPart.getPartNo());
																		if (dossierFile.getDossierPartNo().equals(dossierPart.getPartNo())) {
																			eForm = dossierFile.getEForm();
																			formData = dossierFile.getFormData();
																			formScript = dossierFile.getFormScript();
																			docFileReferenceUid = dossierFile.getReferenceUid();
																			fileEntryId = dossierFile.getFileEntryId();
																			if (returnDossierFileTemplateNos
																					.contains(dossierFile.getFileTemplateNo())) {
																				returned = true;
																			}
						
												dossierFileId = dossierFile.getDossierFileId();
																			createFile = JSONFactoryUtil.createJSONObject();
																			createFile.put("dossierPartId", dossierPart.getDossierPartId());
																			createFile.put("partNo", dossierPart.getPartNo());
																			createFile.put("partName", dossierPart.getPartName());
																			createFile.put("partTip", dossierPart.getPartTip());
																			createFile.put("multiple", dossierPart.getMultiple());
																			createFile.put("templateFileNo", dossierPart.getFileTemplateNo());
																			
																			createFile.put("eform", eForm);
																			createFile.put("dossierFileId", dossierFileId);
																			createFile.put("formData", formData);
																			createFile.put("formScript", formScript);
																			createFile.put("referenceUid", docFileReferenceUid);
																			createFile.put("counter", 1);
																			createFile.put("returned", returned);
																			createFile.put("fileEntryId", fileEntryId);
																			
																			createFiles.put(createFile);																
						
																		}
																	}
																} else {
																	eForm = Validator.isNotNull(dossierPart.getFormScript()) ? true : false;
//																	_log.info("Sample data: " + dossierPart.getSampleData());
																	
																	formData = AutoFillFormData.sampleDataBinding(dossierPart.getSampleData(),
																			dossierId, serviceContext);
																	formScript = dossierPart.getFormScript();
						
//																	_log.info("Dossier part: " + dossierPart.getPartNo());
//																	_log.info("Form data: " + formData);
																	
																	
																	if (returnDossierFileTemplateNos
																			.contains(dossierPart.getFileTemplateNo())) {
																		returned = true;
											}
																	if (eForm && !dossierPart.getESign()) {
																		DossierFileActions actions = new DossierFileActionsImpl();
																		
																		DossierFile dossierFile = null;
						
																		try {
																			dossierFile = DossierFileLocalServiceUtil
																					.getDossierFileByDID_FTNO_DPT_First(dossierId,
																							fileTemplateNo, 2, false, new DossierFileComparator(
																									false, "createDate", Date.class));
																		} catch (Exception e) {
																			_log.debug(e);
																			//_log.error(e);
																		}
																		if (Validator.isNull(dossierFile)) {
						
																			dossierFile = actions.addDossierFile(groupId, dossierId,
																					referenceUid, dossier.getDossierTemplateNo(),
																					dossierPart.getPartNo(), fileTemplateNo,
																					dossierPart.getPartName(), StringPool.BLANK, 0L, null,
																					StringPool.BLANK, String.valueOf(false), serviceContext);
//																			_log.info("dossierFile create:" + dossierFile.getDossierPartNo()
//																					+ "Timer create :" + new Date());
																		}
						
																		docFileReferenceUid = dossierFile.getReferenceUid();
						
																		dossierFileId = dossierFile.getDossierFileId();																		
																	}
																	else if (Validator.isNotNull(formData)) {
																		DossierFileActions actions = new DossierFileActionsImpl();
						
																		DossierFile dossierFile = null;

																		if (Validator.isNotNull(dossierPart.getDeliverableType())) {
																			DeliverableType dlt = DeliverableTypeLocalServiceUtil.getByCode(groupId, dossierPart.getDeliverableType());
																			if (dlt != null) {					
																					mappingData = dlt.getMappingData();
																					mappingDataObj = JSONFactoryUtil.createJSONObject(mappingData);
																					if (mappingDataObj.has(DeliverableTypesTerm.DELIVERABLES_KEY)) {
																						deliverables = mappingDataObj.getString(DeliverableTypesTerm.DELIVERABLES_KEY);
//																						_log.info("--------DELIVERABLES----------" + deliverables);
																						if (Validator.isNotNull(dossierFile)) {
//																							formData = dossierFile.getFormData();
										}

																						formDataObj = JSONFactoryUtil.createJSONObject(formData);
																						JSONArray deliverableListArr = JSONFactoryUtil.createJSONArray();
																													
																						if (Validator.isNull(deliverables)) {
																						}
																						else {
																							if (formDataObj.has(deliverables)) {
//																								_log.info("----GENERATE MANY DELIVERABLES----:" + formDataObj.toJSONString());
																								
																								JSONArray deliverablesArr = JSONFactoryUtil.createJSONArray(formDataObj.getString(deliverables));

																								for (int i = 0; i < deliverablesArr.length(); i++) {
																									JSONObject newFormDataObj = JSONFactoryUtil.createJSONObject();

																									Iterator<?> keys = formDataObj.keys();

																									while( keys.hasNext() ) {
																									    String key = (String)keys.next();
																									    if (!key.equals(deliverables)) {
																									    	newFormDataObj.put(key, formDataObj.get(key));
																									    }
																									}							

																									JSONObject deliverableObj = deliverablesArr.getJSONObject(i);
																									
																									keys = deliverableObj.keys();

																									while( keys.hasNext() ) {
																									    String key = (String)keys.next();
																									    newFormDataObj.put(key, deliverableObj.get(key));
																									}						
																																																																									
																									deliverableListArr.put(newFormDataObj);
																																											
																									dossierFile = actions.addDossierFile(groupId, dossierId,
																												referenceUid, dossier.getDossierTemplateNo(),
																												dossierPart.getPartNo(), fileTemplateNo,
																												dossierPart.getPartName(), StringPool.BLANK, 0L, null,
																												StringPool.BLANK, String.valueOf(false), serviceContext);
//																									_log.info("dossierFile create:" + dossierFile.getDossierPartNo()
//																												+ "Timer create :" + new Date());
																										
																									docFileReferenceUid = dossierFile.getReferenceUid();
																										
																									dossierFileId = dossierFile.getDossierFileId();

																									newFormDataObj.put("LicenceNo", DeliverableNumberGenerator.generateDeliverableNumber(groupId, companyId, dlt.getDeliverableTypeId()));

																									DossierFileLocalServiceUtil.updateFormData(groupId, dossierId, docFileReferenceUid, newFormDataObj.toJSONString(), serviceContext);
																								}
																							}
																						}
																						
																						formData = deliverableListArr.toJSONString();
																						
//																						_log.info("DELIVERABLES: " + formData);
																					}
																					else {
																						
																					}

																				} else {
																				}
																		}
																	}
//																	if (eForm) {
//																		DossierFileActions actions = new DossierFileActionsImpl();
//						
//																		DossierFile dossierFile = null;
//						
//																		try {
//																			dossierFile = DossierFileLocalServiceUtil
//																					.getDossierFileByDID_FTNO_DPT_First(dossierId,
//																							fileTemplateNo, 2, false, new DossierFileComparator(
//																									false, "createDate", Date.class));
//																		} catch (Exception e) {
//																		}
//																		if (Validator.isNull(dossierFile)) {
//						
//																			dossierFile = actions.addDossierFile(groupId, dossierId,
//																					referenceUid, dossier.getDossierTemplateNo(),
//																					dossierPart.getPartNo(), fileTemplateNo,
//																					dossierPart.getPartName(), StringPool.BLANK, 0L, null,
//																					StringPool.BLANK, String.valueOf(false), serviceContext);
//																			_log.info("dossierFile create:" + dossierFile.getDossierPartNo()
//																					+ "Timer create :" + new Date());
//																		}
//						
//																		docFileReferenceUid = dossierFile.getReferenceUid();
//						
//																		dossierFileId = dossierFile.getDossierFileId();
//																	}
						
										dossierFilesResult = DossierFileLocalServiceUtil
												.getDossierFileByDID_FTNO_DPT_NOT_NULL_FID(dossierId, fileTemplateNo, 2,
														0, false);

										counter = (dossierFilesResult != null && !dossierFilesResult.isEmpty())
												? dossierFilesResult.size() : 0;

										createFile.put("eform", eForm);
										createFile.put("dossierFileId", dossierFileId);
										createFile.put("formData", formData);
										createFile.put("formScript", formScript);
										createFile.put("referenceUid", docFileReferenceUid);
										createFile.put("counter", counter);
										createFile.put("returned", returned);
										createFile.put("fileEntryId", fileEntryId);
																	
																}
															}
														}
													}
												}
												else {
													//Do not config deliverable type
												}
											}											
										}
									}
								}
							}
						}
						result.put("pending", pending);
						result.put("processAction", processAction);
						result.put("lstUser", lstUser);
						result.put("createFiles", createFiles);
						results.put(result);
					}
				} catch (Exception e) {
					_log.error(e);
				}
			}
		}

		return results;
	}

	@Override
	public JSONArray getNextActionList(long userId, long companyId, long groupId, LinkedHashMap<String, Object> params,
			Sort[] sorts, Integer start, Integer end, ServiceContext serviceContext) {

		// TODO filter by Auto
//		String auto = GetterUtil.getString(params.get(DossierActionTerm.AUTO));
//		_log.info("auto =: " + auto);
		long dossierId = GetterUtil.getLong(params.get(DossierTerm.DOSSIER_ID));
//		_log.info("dossierId =: " + dossierId);

		DossierAction dossierAction = null;
		List<ProcessAction> processActionList = null;
		JSONArray results = null;
		Dossier dossier = DossierLocalServiceUtil.fetchDossier(dossierId);
//		_log.info("dossier: "+dossier);
		try {
			if (dossier != null) {
				long serviceProcessId = 0;
				String stepCode = StringPool.BLANK;
				boolean pending = false;
				long dossierActionId = dossier.getDossierActionId();
				// _log.info("dossierActionId: "+dossierActionId);
				if (dossierActionId > 0) {
					dossierAction = DossierActionLocalServiceUtil.fetchDossierAction(dossierActionId);
				}

				if (dossierAction != null) {
					serviceProcessId = dossierAction.getServiceProcessId();
					stepCode = dossierAction.getStepCode();
					pending = dossierAction.getPending();
				}

				if (Validator.isNotNull(stepCode) && serviceProcessId > 0) {
					DossierActionUser dActionUser = DossierActionUserLocalServiceUtil
							.getByDossierAndUser(dossierActionId, userId);
					// _log.info("User id: " + userId);
					// _log.info("Dossier action user:" );
					// GS.AnhTT_Process
					int enable = 2;
					if (dossier.getOriginality() == DossierTerm.ORIGINALITY_DVCTT) {
						if (dossier.getUserId() == userId && !pending) {
							enable = 1;
						}
					}
					if (dActionUser != null) {
						int assign = dActionUser.getAssigned();
						if (assign == 1 && !pending)
							enable = 1;
					}
					//Check if user if admin
					User checkAU = UserLocalServiceUtil.fetchUser(userId);
					if (checkAU != null) {
						List<Role> userRoles = checkAU.getRoles();
						boolean isAdmin = false;
						for (Role r : userRoles) {
							if ("Administrator".equalsIgnoreCase(r.getName())) {
								isAdmin = true;
								break;
							}
						}
						if (isAdmin) {
							enable = 1;
						}
					}
					// _log.info("Enable: " + enable);
					processActionList = ProcessActionLocalServiceUtil.getProcessActionByG_SPID_PRESC(groupId,
							serviceProcessId, stepCode);
					// _log.info("processActionList:
					// "+processActionList.size());
					if (processActionList != null && processActionList.size() > 0) {
						results = JSONFactoryUtil.createJSONArray();
						JSONObject data = null;
						long processActionId = 0;
						String actionCode;
						String actionName;
						String preStepCode;
						String postStepCode;
						String autoEvent;
						String preCondition;
						for (ProcessAction processAction : processActionList) {
							// _log.info("processAction: "+processAction);
							data = JSONFactoryUtil.createJSONObject();
							processActionId = processAction.getProcessActionId();
							actionCode = processAction.getActionCode();
							actionName = processAction.getActionName();
							preStepCode = processAction.getPreStepCode();
							postStepCode = processAction.getPostStepCode();
							autoEvent = processAction.getAutoEvent();
							preCondition = processAction.getPreCondition();
							// Check permission enable button
							if (processCheckEnable(preCondition, autoEvent, dossier, actionCode, groupId))
								data.put(ProcessActionTerm.ENABLE, enable);
							else
								data.put(ProcessActionTerm.ENABLE, 0);

							data.put(ProcessActionTerm.PROCESS_ACTION_ID, processActionId);
							data.put(ProcessActionTerm.ACTION_CODE, actionCode);
							data.put(ProcessActionTerm.ACTION_NAME, actionName);
							data.put(ProcessActionTerm.PRESTEP_CODE, preStepCode);
							data.put(ProcessActionTerm.POSTSTEP_CODE, postStepCode);
							data.put(ProcessActionTerm.AUTO_EVENT, autoEvent);
							data.put(ProcessActionTerm.PRE_CONDITION, preCondition);
							//
							results.put(data);
						}
					}
				} else {
					results = JSONFactoryUtil.createJSONArray();
					ProcessOption option = getProcessOption(dossier.getServiceCode(), dossier.getGovAgencyCode(),
							dossier.getDossierTemplateNo(), groupId);
					if (option != null) {
						serviceProcessId = option.getServiceProcessId();
					}
						processActionList = ProcessActionLocalServiceUtil
								.getByServiceStepCode(groupId, serviceProcessId, StringPool.BLANK);
					if (processActionList != null && processActionList.size() > 0) {
						JSONObject data = null;
						long processActionId = 0;
						String actionCode;
						String actionName;
						String preStepCode;
						String postStepCode;
						String autoEvent;
						String preCondition;
						for (ProcessAction processAction : processActionList) {
							// _log.info("processAction: "+processAction);
							data = JSONFactoryUtil.createJSONObject();
							processActionId = processAction.getProcessActionId();
							actionCode = processAction.getActionCode();
							actionName = processAction.getActionName();
							preStepCode = processAction.getPreStepCode();
							postStepCode = processAction.getPostStepCode();
							autoEvent = processAction.getAutoEvent();
							preCondition = processAction.getPreCondition();
							data.put(ProcessActionTerm.PROCESS_ACTION_ID, processActionId);
							data.put(ProcessActionTerm.ACTION_CODE, actionCode);
							data.put(ProcessActionTerm.ACTION_NAME, actionName);
							data.put(ProcessActionTerm.PRESTEP_CODE, preStepCode);
							data.put(ProcessActionTerm.POSTSTEP_CODE, postStepCode);
							data.put(ProcessActionTerm.AUTO_EVENT, autoEvent);
							data.put(ProcessActionTerm.PRE_CONDITION, preCondition);
							data.put(ProcessActionTerm.ENABLE, 1);
							//
							results.put(data);
						}
					}
				}
			}
		} catch (Exception e) {
			_log.error(e);
		}

		return results;
	}

	@Override
	public JSONObject getDetailNextActions(long userId, long companyId, long groupId,
			LinkedHashMap<String, Object> params, Sort[] sorts, Integer start, Integer end,
			ServiceContext serviceContext) {
		JSONObject result = JSONFactoryUtil.createJSONObject();

		// TODO filter by Auto
//		String auto = GetterUtil.getString(params.get(DossierActionTerm.AUTO));
//		_log.info("auto =: " + auto);
		long dossierId = GetterUtil.getLong(params.get(DossierTerm.DOSSIER_ID));
//		_log.info("dossierId =: " + dossierId);

		try {
			Dossier dossier = DossierLocalServiceUtil.fetchDossier(dossierId);
//			_log.info("dossier: "+dossier);
			if (dossier != null) {
				String dossierTempNo = dossier.getDossierTemplateNo();
				long processActionId = GetterUtil.getLong(params.get(ProcessActionTerm.PROCESS_ACTION_ID));
				ProcessAction processAction = ProcessActionLocalServiceUtil.fetchProcessAction(processActionId);

				//Process PaymentFile
				JSONObject payment = JSONFactoryUtil.createJSONObject();
				String postStepCode = StringPool.BLANK;
				long serviceProcessId = 0;
				if (processAction != null) {
					postStepCode = processAction.getPostStepCode();
					serviceProcessId = processAction.getServiceProcessId();
					payment.put(PaymentFileTerm.PAYMENT_REQUEST, processAction.getRequestPayment());
//					_log.info("Payment fee: " + payment);
					String paymentFeeData = processAction.getPaymentFee();
					PaymentFile paymentFile = PaymentFileLocalServiceUtil.getByDossierId(groupId, dossierId);
					String advanceAmount = StringPool.BLANK;
					String feeAmount = StringPool.BLANK;
					String serviceAmount = StringPool.BLANK;
					String shipAmount = StringPool.BLANK;
					String paymentFee = StringPool.BLANK;
					String paymentNote = StringPool.BLANK;
					int editable = 0;
					
					if (Validator.isNotNull(paymentFeeData)) {
						JSONObject jsonPaymentFee = JSONFactoryUtil.createJSONObject(paymentFeeData);
						
						advanceAmount = jsonPaymentFee.getString(PaymentFileTerm.ADVANCE_AMOUNT);
						feeAmount = jsonPaymentFee.getString(PaymentFileTerm.FEE_AMOUNT);
						serviceAmount = jsonPaymentFee.getString(PaymentFileTerm.SERVICE_AMOUNT);
						shipAmount = jsonPaymentFee.getString(PaymentFileTerm.SHIP_AMOUNT);
						paymentFee = jsonPaymentFee.getString(PaymentFileTerm.PAYMENT_FEE);
						paymentNote = jsonPaymentFee.getString(PaymentFileTerm.PAYMENT_NOTE);
						editable = GetterUtil.getInteger(jsonPaymentFee.getString(PaymentFileTerm.EDITABLE));						
					}
					if (paymentFile != null) {
						payment.put(PaymentFileTerm.ADVANCE_AMOUNT, paymentFile.getAdvanceAmount());
						payment.put(PaymentFileTerm.FEE_AMOUNT, paymentFile.getFeeAmount());
						payment.put(PaymentFileTerm.SERVICE_AMOUNT, paymentFile.getServiceAmount());
						payment.put(PaymentFileTerm.SHIP_AMOUNT, paymentFile.getShipAmount());
						payment.put(PaymentFileTerm.PAYMENT_FEE, paymentFile.getPaymentFee());
						payment.put(PaymentFileTerm.PAYMENT_NOTE, paymentFile.getPaymentNote());
						payment.put(PaymentFileTerm.EDITABLE, editable);
					} else {
						if (Validator.isNotNull(paymentFeeData)) {	
							JSONObject jsonPaymentFee = JSONFactoryUtil.createJSONObject(paymentFeeData);
							if (jsonPaymentFee != null) {
								payment.put(PaymentFileTerm.ADVANCE_AMOUNT, advanceAmount);
								payment.put(PaymentFileTerm.FEE_AMOUNT, feeAmount);
								payment.put(PaymentFileTerm.SERVICE_AMOUNT, serviceAmount);
								payment.put(PaymentFileTerm.SHIP_AMOUNT, shipAmount);
								payment.put(PaymentFileTerm.PAYMENT_FEE, paymentFee);
								payment.put(PaymentFileTerm.PAYMENT_NOTE, paymentNote);
								payment.put(PaymentFileTerm.EDITABLE, editable);

							} else {
							}
						} else {
							payment.put(PaymentFileTerm.ADVANCE_AMOUNT, 0);
							payment.put(PaymentFileTerm.FEE_AMOUNT, 0);
							payment.put(PaymentFileTerm.SERVICE_AMOUNT, 0);
							payment.put(PaymentFileTerm.SHIP_AMOUNT, 0);
							payment.put(PaymentFileTerm.PAYMENT_FEE, 0);
							payment.put(PaymentFileTerm.PAYMENT_NOTE, 0);
							payment.put(PaymentFileTerm.EDITABLE, editable);
						}
					}
					
					_log.info("Payment fee: " + payment);
					
					result.put("payment", payment);
				}

				ProcessStep processStep = ProcessStepLocalServiceUtil.fetchBySC_GID(postStepCode, groupId,
						serviceProcessId);
				if (processStep != null) {
					List<User> lstUser = new ArrayList<>();
					
					List<ProcessStepRole> processStepRoleList = ProcessStepRoleLocalServiceUtil
							.findByP_S_ID(processStep.getProcessStepId());
					if (Validator.isNotNull(processStep.getRoleAsStep())) {
						String[] steps = StringUtil.split(processStep.getRoleAsStep());
						for (String stepCode : steps) {
							if (stepCode.startsWith("!")) {
								int index = stepCode.indexOf("!");
								String stepCodePunc = stepCode.substring(index + 1);
								lstUser.addAll(processRoleAsStepDonedListUser(dossier, stepCodePunc, serviceProcessId, processStep));
							}
							else {
								lstUser.addAll(processRoleAsStepListUser(dossier, stepCode, serviceProcessId, processStep));								
							}
						}
					}
					else {
						if (processStepRoleList != null && !processStepRoleList.isEmpty()) {
							List<ProcessStepRole> lstStepRoles = new ArrayList<>();
							for (ProcessStepRole psr : processStepRoleList) {
								if (Validator.isNotNull(psr.getCondition())) {
									String[] conditions = StringUtil.split(psr.getCondition());
									
									if (DossierMgtUtils.checkPreCondition(conditions, dossier)) {
										lstStepRoles.add(psr);
									}
								}
								else {
									lstStepRoles.add(psr);
								}
							}
							lstUser.addAll(processRoleListUser(lstStepRoles, serviceProcessId));
						}						
					}
					if (lstUser != null && !lstUser.isEmpty()) {
						result.put("lstUser", lstUser);
					}
				}

				//Put receiving to next action
				if (processAction != null) {
					_log.info("Service process: " + serviceProcessId);
					ServiceProcess serviceProcess = ServiceProcessLocalServiceUtil.fetchServiceProcess(serviceProcessId);
					
					ProcessStep postStep = ProcessStepLocalServiceUtil.fetchBySC_GID(processAction.getPostStepCode(), groupId, serviceProcessId);
					
					JSONObject receivingObj = JSONFactoryUtil.createJSONObject();
					Date receiveDate = new Date();
					receivingObj.put(DossierTerm.RECEIVE_DATE, dossier.getReceiveDate() != null ? dossier.getReceiveDate().getTime() : receiveDate.getTime());
					Date dueDate = null;
					Double durationCount = serviceProcess.getDurationCount();
					if (Validator.isNotNull(String.valueOf(durationCount)) && durationCount > 0d) {
						dueDate = HolidayUtils.getDueDate(new Date(), serviceProcess.getDurationCount(), serviceProcess.getDurationUnit(), groupId);
					}
					
					receivingObj.put(DossierTerm.DUE_DATE, dueDate != null ? dueDate.getTime() : 0l);
					receivingObj.put("editable", DossierMgtUtils.isDueDateEditable(serviceProcess.getDueDatePattern()));
					_log.info("Receiving object: " + receivingObj.toJSONString());	
					if (postStep != null) {
						if (DossierTerm.DOSSIER_STATUS_NEW.equals(postStep.getDossierStatus())
								|| DossierTerm.DOSSIER_STATUS_PROCESSING.equals(postStep.getDossierStatus())
								&& dossier.getReceiveDate() == null) {
							result.put("receiving", receivingObj);													
						}
					}
				}
				//
				String createDossierFiles = StringPool.BLANK;
				String returnDossierFiles = StringPool.BLANK;
				if (processAction != null) {
					createDossierFiles = processAction.getCreateDossierFiles();
					returnDossierFiles = processAction.getReturnDossierFiles();
				}

				List<String> createFileTempNoList = ListUtil.toList(StringUtil.split(createDossierFiles));
				List<String> returnFileTempNoList = ListUtil.toList(StringUtil.split(returnDossierFiles));
				_log.info("-----RETURN DOSSIER FILE TEMPLATE NOS----" + returnFileTempNoList.size());

				if (returnFileTempNoList != null && !returnFileTempNoList.isEmpty()) {
					DossierTemplate dossierTemplate = DossierTemplateLocalServiceUtil.getByTemplateNo(groupId,
							dossier.getDossierTemplateNo());

					List<DossierPart> partList = DossierPartLocalServiceUtil.getByTemplateNo(groupId,
							dossierTemplate.getTemplateNo());

					List<DossierFile> returnFiles = new ArrayList<>();
					if (partList != null && partList.size() > 0) {
						for (DossierPart dossierPart : partList) {
							String fileTemplateNo = dossierPart.getFileTemplateNo();
							int dossierPartType = dossierPart.getPartType();
							if (returnFileTempNoList.contains(fileTemplateNo)) {
								List<DossierFile> fileList = DossierFileLocalServiceUtil.getDossierFileByDID_FTNO_DPT(
										dossierId, fileTemplateNo, dossierPartType, false);
								if (fileList != null && fileList.size() > 0) {
										returnFiles.addAll(fileList);
								}
							}
						}
					}
					result.put("returnFiles", returnFiles);
				}
			
				JSONArray createFiles = JSONFactoryUtil.createJSONArray();
				if (createFileTempNoList != null && !createFileTempNoList.isEmpty()) {
					DossierTemplate dossierTemplate = DossierTemplateLocalServiceUtil.getByTemplateNo(groupId,
							dossierTempNo);

					List<DossierPart> partList = DossierPartLocalServiceUtil.getByTemplateNo(groupId, dossierTemplate.getTemplateNo());

					if (partList != null && partList.size() > 0) {
						long fileEntryId = 0;
						boolean eForm = false;
						boolean multiple = false;
						String formData;
						String formScript;
						String docFileReferenceUid = StringPool.BLANK;
						int counter = 0;
						long dossierFileId = 0;
						for (DossierPart dossierPart : partList) {
							String fileTemplateNo = dossierPart.getFileTemplateNo();
							eForm = dossierPart.getEForm();
							multiple = dossierPart.getMultiple();
							
							if (createFileTempNoList.contains(fileTemplateNo)) {
								JSONObject createFile = JSONFactoryUtil.createJSONObject();
								createFile.put(DossierPartTerm.DOSSIERPART_ID, dossierPart.getDossierPartId());
								createFile.put(DossierPartTerm.PART_NO, dossierPart.getPartNo());
								createFile.put(DossierPartTerm.PART_NAME, dossierPart.getPartName());
								createFile.put(DossierPartTerm.PART_TIP, dossierPart.getPartTip());
								createFile.put(DossierPartTerm.MULTIPLE, dossierPart.getMultiple());
								createFile.put(DossierPartTerm.FILE_TEMPLATE_NO, fileTemplateNo);
								createFile.put(DossierPartTerm.PART_TYPE, dossierPart.getPartType());
								createFile.put(DossierPartTerm.EFORM, eForm);
								createFile.put(DossierPartTerm.MULTIPLE, multiple);
								
								//Get dossierMark
								DossierMark dossierMark = DossierMarkLocalServiceUtil.getDossierMarkbyDossierId(groupId,
										dossierId, dossierPart.getPartNo());
								if (dossierMark != null) {
									createFile.put(DossierPartTerm.FILE_MARK, dossierMark.getFileMark());
									createFile.put(DossierPartTerm.FILE_MARK, dossierMark.getFileMark());
									createFile.put(DossierPartTerm.FILE_COMMENT, dossierMark.getFileComment());
								} else {
									createFile.put(DossierPartTerm.FILE_MARK, 0);
									createFile.put(DossierPartTerm.FILE_MARK, 0);
									createFile.put(DossierPartTerm.FILE_COMMENT, StringPool.BLANK);
								}

								ServiceContext context = new ServiceContext();
								context.setScopeGroupId(dossierPart.getGroupId());
								context.setCompanyId(dossierPart.getCompanyId());

								String strDeliverableType = dossierPart.getDeliverableType();
								String sampleData = dossierPart.getSampleData();
								_log.info("--------DOSSIER PART NO-------" + dossierPart.getPartName());
								_log.info("-------DOSSIER PART SAMPLE DATA------" + dossierPart.getSampleData());
								String formDataDeliverables = AutoFillFormData.sampleDataBinding(sampleData, dossierId,
										context);
								_log.info("-------FORM DATA DELIVERABLE---------" + formDataDeliverables);

								JSONObject formDataObj = JSONFactoryUtil.createJSONObject(formDataDeliverables);

								// End add generate deliverable if has deliverable type
								if (Validator.isNull(strDeliverableType)) {
									List<DossierFile> dossierFilesResult = DossierFileLocalServiceUtil
											.getDossierFileByDID_FTNO_DPT(dossierId, fileTemplateNo, 2, false,
													QueryUtil.ALL_POS, QueryUtil.ALL_POS,
													new DossierFileComparator(false, "createDate", Date.class));
									_log.info("dossierFilesResult: "+dossierFilesResult.size());
									if (dossierFilesResult != null && !dossierFilesResult.isEmpty()) {
										_log.info("1");
										createFile = processFileResult(dossierFilesResult, createFile,
												dossierPart.getPartNo());
										_log.info("createFile: "+createFile.toJSONString());

									} else {
										createFile = processEFormByCreateFile(dossierPart, groupId, dossierId,
												sampleData, fileTemplateNo, dossierTempNo, createFile, serviceContext);
									}
									dossierFilesResult = DossierFileLocalServiceUtil
											.getDossierFileByDID_FTNO_DPT_NOT_NULL_FID(dossierId, fileTemplateNo, 2, 0,
													false);
									_log.info("dossierFilesResult1: "+dossierFilesResult.size());

									counter = (dossierFilesResult != null && !dossierFilesResult.isEmpty())
											? dossierFilesResult.size() : 0;
									createFile.put(DossierFileTerm.COUNTER, counter);
									createFiles.put(createFile);
								} else {
									DeliverableType deliverableTypeObject = DeliverableTypeLocalServiceUtil
											.getByCode(groupId, strDeliverableType);
									_log.info("Deliverable type: " + deliverableTypeObject);
									if (deliverableTypeObject != null) {
										String mappingData = deliverableTypeObject.getMappingData();
										JSONObject mappingDataObj = JSONFactoryUtil.createJSONObject(mappingData);
										if (mappingDataObj.has(DeliverableTypesTerm.DELIVERABLES_KEY)) {
											String deliverables = mappingDataObj
													.getString(DeliverableTypesTerm.DELIVERABLES_KEY);
											_log.info("--------DELIVERABLES----------" + deliverables);
											_log.info("--------HAS E SIGNATURE----------"
													+ processAction.getESignature());
											_log.info("---------FILE TEMPLATE NO--------" + fileTemplateNo);
											if (Validator.isNull(deliverables)) {
												// Add one deliverable
												List<DossierFile> dossierFilesResult = DossierFileLocalServiceUtil
														.getDossierFileByDID_FTNO_DPT(dossierId, fileTemplateNo, 2,
																false, QueryUtil.ALL_POS, QueryUtil.ALL_POS,
																new DossierFileComparator(false, "createDate",
																		Date.class));
												if (dossierFilesResult != null && !dossierFilesResult.isEmpty()) {
													createFile = processFileResult(dossierFilesResult, createFile,
															dossierPart.getPartNo());
												} else {
													formScript = dossierPart.getFormScript();
													eForm = Validator.isNotNull(formScript) ? true : false;
													formData = AutoFillFormData.sampleDataBinding(
															dossierPart.getSampleData(), dossierId, serviceContext);
													_log.info("Dossier part: " + dossierPart.getPartNo());
													_log.info("Form data: " + formData);

													if (eForm) {
														createFile = processEFormAndUpdateDossierFile(dossierPart,
																groupId, dossierId, formData, fileTemplateNo,
																dossierTempNo, formDataObj, createFile, serviceContext);
													} else if (Validator.isNotNull(formData)) {
														formDataObj = JSONFactoryUtil.createJSONObject(formData);
														if (mappingDataObj.has(DeliverableTypesTerm.DELIVERABLES_KEY)) {
															deliverables = mappingDataObj
																	.getString(DeliverableTypesTerm.DELIVERABLES_KEY);
															if (Validator.isNull(deliverables)) {
																createFile = processEFormAndUpdateFormData(dossierPart,
																		groupId, dossierId, formData, fileTemplateNo,
																		dossierTempNo, formDataObj, createFile,
																		serviceContext);
															} else {
																if (formDataObj.has(deliverables)) {
																	formDataObj = JSONFactoryUtil
																			.createJSONObject(formData);
																	JSONArray deliverablesArr = JSONFactoryUtil
																			.createJSONArray(formDataObj
																					.getString(deliverables));
																	dossierFilesResult = DossierFileLocalServiceUtil
																			.getDossierFileByDID_FTNO_DPT_NOT_NULL_FID(
																					dossierId, fileTemplateNo, 2, 0,
																					false);

																	counter = (dossierFilesResult != null
																			&& !dossierFilesResult.isEmpty())
																					? dossierFilesResult.size() : 0;

																	for (int i = 0; i < deliverablesArr.length(); i++) {
																		JSONObject newFormDataObj = JSONFactoryUtil
																				.createJSONObject();
																		Iterator<?> keys = formDataObj.keys();
																		while (keys.hasNext()) {
																			String key = (String) keys.next();
																			if (!key.equals(deliverables)) {
																				newFormDataObj.put(key,
																						formDataObj.get(key));
																			}
																		}

																		JSONObject deliverableObj = deliverablesArr
																				.getJSONObject(i);
																		keys = deliverableObj.keys();
																		while (keys.hasNext()) {
																			String key = (String) keys.next();
																			newFormDataObj.put(key,
																					deliverableObj.get(key));
																		}

																		createFile = processEFormAndUpdateFormData(
																				dossierPart, groupId, dossierId,
																				formData, fileTemplateNo, dossierTempNo,
																				formDataObj, createFile,
																				serviceContext);

																		createFile.put(DossierFileTerm.E_FORM, eForm);
																		createFile.put(DossierFileTerm.FORM_SCRIPT,
																				formScript);
																		createFile.put(DossierFileTerm.COUNTER,
																				counter);

																		createFiles.put(createFile);
																	}
																}
															}
														}
													}
												}
												dossierFilesResult = DossierFileLocalServiceUtil
														.getDossierFileByDID_FTNO_DPT_NOT_NULL_FID(dossierId,
																fileTemplateNo, 2, 0, false);

												counter = (dossierFilesResult != null && !dossierFilesResult.isEmpty())
														? dossierFilesResult.size() : 0;
												createFile.put(DossierFileTerm.COUNTER, counter);
												createFile.put("fileEntryId", fileEntryId);
												createFiles.put(createFile);
											} else {
												List<DossierFile> dossierFilesResult = DossierFileLocalServiceUtil
														.getDossierFileByDID_FTNO_DPT(dossierId, fileTemplateNo, 2,
																false, QueryUtil.ALL_POS, QueryUtil.ALL_POS,
																new DossierFileComparator(false, "createDate",
																		Date.class));

												if (dossierFilesResult != null && !dossierFilesResult.isEmpty()) {
													createFile = JSONFactoryUtil.createJSONObject();
													createFile = processFileResult(dossierFilesResult, createFile,
															dossierPart.getPartNo());
													createFile.put(DossierPartTerm.DOSSIERPART_ID,
															dossierPart.getDossierPartId());
													createFile.put(DossierPartTerm.PART_NO, dossierPart.getPartNo());
													createFile.put(DossierPartTerm.PART_NAME,
															dossierPart.getPartName());
													createFile.put(DossierPartTerm.PART_TIP, dossierPart.getPartTip());
													createFile.put(DossierPartTerm.MULTIPLE, dossierPart.getMultiple());
													createFile.put(DossierPartTerm.FILE_TEMPLATE_NO, fileTemplateNo);
													createFile.put(DossierFileTerm.COUNTER, 1);
													createFiles.put(createFile);
												} else {
													eForm = Validator.isNotNull(dossierPart.getFormScript()) ? true
															: false;
													formData = AutoFillFormData.sampleDataBinding(
															dossierPart.getSampleData(), dossierId, serviceContext);
													formScript = dossierPart.getFormScript();

													if (eForm && !dossierPart.getESign()) {
														DossierFileActions actions = new DossierFileActionsImpl();

														DossierFile dossierFile = null;

														try {
															dossierFile = DossierFileLocalServiceUtil
																	.getDossierFileByDID_FTNO_DPT_First(dossierId,
																			fileTemplateNo, 2, false,
																			new DossierFileComparator(false,
																					"createDate", Date.class));
														} catch (Exception e) {
															_log.debug(e);
															//_log.error(e);
														}
														if (Validator.isNull(dossierFile)) {

															dossierFile = actions.addDossierFile(groupId, dossierId,
																	StringPool.BLANK, dossier.getDossierTemplateNo(),
																	dossierPart.getPartNo(), fileTemplateNo,
																	dossierPart.getPartName(), StringPool.BLANK, 0L,
																	null, StringPool.BLANK, String.valueOf(false),
																	serviceContext);
														}

														docFileReferenceUid = dossierFile.getReferenceUid();

														dossierFileId = dossierFile.getDossierFileId();
													} else if (Validator.isNotNull(formData)) {
														DossierFileActions actions = new DossierFileActionsImpl();

														DossierFile dossierFile = null;

														if (Validator.isNotNull(dossierPart.getDeliverableType())) {
															DeliverableType dlt = DeliverableTypeLocalServiceUtil
																	.getByCode(groupId,
																			dossierPart.getDeliverableType());
															if (dlt != null) {
																mappingData = dlt.getMappingData();
																mappingDataObj = JSONFactoryUtil
																		.createJSONObject(mappingData);
																if (mappingDataObj
																		.has(DeliverableTypesTerm.DELIVERABLES_KEY)) {
																	deliverables = mappingDataObj.getString(
																			DeliverableTypesTerm.DELIVERABLES_KEY);
																	formDataObj = JSONFactoryUtil
																			.createJSONObject(formData);
																	JSONArray deliverableListArr = JSONFactoryUtil
																			.createJSONArray();

																	if (Validator.isNull(deliverables)) {
																	} else {
																		if (formDataObj.has(deliverables)) {

																			JSONArray deliverablesArr = JSONFactoryUtil
																					.createJSONArray(formDataObj
																							.getString(deliverables));
																			for (int i = 0; i < deliverablesArr
																					.length(); i++) {
																				JSONObject newFormDataObj = JSONFactoryUtil
																						.createJSONObject();
																				Iterator<?> keys = formDataObj.keys();
																				while (keys.hasNext()) {
																					String key = (String) keys.next();
																					if (!key.equals(deliverables)) {
																						newFormDataObj.put(key,
																								formDataObj.get(key));
																					}
																				}
																				JSONObject deliverableObj = deliverablesArr
																						.getJSONObject(i);
																				keys = deliverableObj.keys();
																				while (keys.hasNext()) {
																					String key = (String) keys.next();
																					newFormDataObj.put(key,
																							deliverableObj.get(key));
																				}
																				deliverableListArr.put(newFormDataObj);

																				dossierFile = actions.addDossierFile(
																						groupId, dossierId,
																						StringPool.BLANK,
																						dossier.getDossierTemplateNo(),
																						dossierPart.getPartNo(),
																						fileTemplateNo,
																						dossierPart.getPartName(),
																						StringPool.BLANK, 0L, null,
																						StringPool.BLANK,
																						String.valueOf(false),
																						serviceContext);

																				docFileReferenceUid = dossierFile
																						.getReferenceUid();
																				dossierFileId = dossierFile
																						.getDossierFileId();
																				newFormDataObj.put("LicenceNo",
																						DeliverableNumberGenerator
																								.generateDeliverableNumber(
																										groupId,
																										companyId,
																										dlt.getDeliverableTypeId()));

																				DossierFileLocalServiceUtil
																						.updateFormData(groupId,
																								dossierId,
																								docFileReferenceUid,
																								newFormDataObj
																										.toJSONString(),
																								serviceContext);
																			}
																		}
																	}

																	formData = deliverableListArr.toJSONString();
																}
															}
														}
													}

													dossierFilesResult = DossierFileLocalServiceUtil
															.getDossierFileByDID_FTNO_DPT_NOT_NULL_FID(dossierId,
																	fileTemplateNo, 2, 0, false);

													counter = (dossierFilesResult != null
															&& !dossierFilesResult.isEmpty())
																	? dossierFilesResult.size() : 0;

													createFile.put(DossierFileTerm.E_FORM, eForm);
													createFile.put(DossierFileTerm.DOSSIER_FILE_ID, dossierFileId);
													createFile.put(DossierFileTerm.FORM_DATA, formData);
													createFile.put(DossierFileTerm.FORM_SCRIPT, formScript);
													createFile.put(DossierFileTerm.REFERENCE_UID, docFileReferenceUid);
													createFile.put(DossierFileTerm.COUNTER, counter);
													createFile.put(DossierFileTerm.FILE_ENTRY_ID, fileEntryId);
												}
											}
										}
									}
								}
							}
						}
					}
				}
				result.put("processAction", processAction);
				result.put("createFiles", createFiles);
			}
		} catch (Exception e) {
			_log.error(e);
		}

		return result;
	}

	@Override
	public JSONArray getPayloadNextActions(long userId, long companyId, long groupId,
			LinkedHashMap<String, Object> params, Sort[] sorts, Integer start, Integer end,
			ServiceContext serviceContext) {
		
		long dossierId = GetterUtil.getLong(params.get(DossierTerm.DOSSIER_ID));
//		_log.info("dossierId =: " + dossierId);

		try {
			Dossier dossier = DossierLocalServiceUtil.fetchDossier(dossierId);
//			_log.info("dossier: " + dossier);
			if (dossier != null) {
				DossierAction dossierAction = DossierActionLocalServiceUtil
						.fetchDossierAction(dossier.getDossierActionId());

				JSONArray result = JSONFactoryUtil.createJSONArray();
				if (dossierAction != null) {
					String actionCode = params.containsKey(DossierActionTerm.ACTION_CODE) ? (String)params.get(DossierActionTerm.ACTION_CODE) : StringPool.BLANK;
					
					if (Validator.isNotNull(actionCode)) {
						ActionConfig actConfig = ActionConfigLocalServiceUtil.getByCode(groupId, actionCode);
						if (actConfig != null) {
							boolean extraForm = actConfig.getExtraForm();
							if (extraForm) {
								String formConfig = actConfig.getFormConfig();
//								_log.info("formConfig: " + formConfig);
								String sampleData = Validator.isNotNull(actConfig.getSampleData()) ? actConfig.getSampleData() : "{}";

								String formData = AutoFillFormData.sampleDataBinding(sampleData, dossierId,
										serviceContext);
								JSONObject formDataJson = JSONFactoryUtil.createJSONObject(formData);
								JSONObject formConfigObj = JSONFactoryUtil.createJSONObject(formConfig);
								
								JSONArray formConfigArr = formConfigObj.getJSONArray("fields");
								
//								_log.info("formConfigArr: " + formConfigArr);
								if (formConfigArr != null && formConfigArr.length() > 0) {
									int length = formConfigArr.length();
									for (int i = 0; i < length; i++) {
										JSONObject jsonObject = formConfigArr.getJSONObject(i);
										String value = formDataJson.getString(jsonObject.getString("fieldName"));
										jsonObject.put("value", value);
//										_log.info("formConfigArr: " + formConfigArr);
										result.put(jsonObject);
									}
								}
								return result;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
//			e.printStackTrace();
			_log.info(e);
		}
		return null;
	}

	private JSONObject processEFormAndUpdateFormData(DossierPart dossierPart, long groupId, long dossierId,
			String formData, String fileTemplateNo, String dossierTempNo, JSONObject formDataObj,
			JSONObject createFile, ServiceContext serviceContext) {

		String docFileReferenceUid = StringPool.BLANK;
		long dossierFileId  = 0;

		DossierFile dossierFile = null;
		try {
			dossierFile = DossierFileLocalServiceUtil.getDossierFileByDID_FTNO_DPT_First(dossierId, fileTemplateNo, 2,
					false, new DossierFileComparator(false, "createDate", Date.class));

			DossierFileActions actions = new DossierFileActionsImpl();
			if (Validator.isNull(dossierFile)) {
				dossierFile = actions.addDossierFile(groupId, dossierId, StringPool.BLANK, dossierTempNo,
						dossierPart.getPartNo(), fileTemplateNo, dossierPart.getPartName(), StringPool.BLANK, 0L, null,
						StringPool.BLANK, String.valueOf(false), serviceContext);
			}

			docFileReferenceUid = dossierFile.getReferenceUid();
			dossierFileId = dossierFile.getDossierFileId();
			//
			formDataObj = JSONFactoryUtil.createJSONObject(formData);
			formDataObj.put("LicenceNo", dossierFile.getDeliverableCode());

			_log.info("UPDATE FORM DATA GENERATE RESULT FILE");
			actions.updateDossierFileFormData(groupId, dossierId, docFileReferenceUid, formData, serviceContext);
		} catch (Exception e) {
			_log.debug(e);
			//_log.error(e);
		}
		createFile.put(DossierFileTerm.DOSSIER_FILE_ID, dossierFileId);
		createFile.put(DossierFileTerm.FORM_DATA, formData);
		createFile.put(DossierFileTerm.REFERENCE_UID, docFileReferenceUid);
		createFile.put(DossierFileTerm.FILE_ENTRY_ID, 0l);

		return createFile;
	}

	private JSONObject processEFormAndUpdateDossierFile(DossierPart dossierPart, long groupId, long dossierId,
			String formData, String fileTemplateNo, String dossierTempNo, JSONObject formDataObj,
			JSONObject createFile, ServiceContext serviceContext) {

		String docFileReferenceUid = StringPool.BLANK;
		long dossierFileId  = 0;

		// create Dossier File
		DossierFile dossierFile = null;
		try {
			dossierFile = DossierFileLocalServiceUtil.getDossierFileByDID_FTNO_DPT_First(dossierId, fileTemplateNo,
					2, false, new DossierFileComparator(false, "createDate", Date.class));

			if (Validator.isNull(dossierFile)) {
				DossierFileActions actions = new DossierFileActionsImpl();
				dossierFile = actions.addDossierFile(groupId, dossierId, StringPool.BLANK, dossierTempNo,
						dossierPart.getPartNo(), fileTemplateNo, dossierPart.getPartName(), StringPool.BLANK, 0L,
						null, StringPool.BLANK, String.valueOf(false), serviceContext);
				_log.info("dossierFile create:" + dossierFile.getDossierPartNo() + "Timer create :" + new Date());
			}

			docFileReferenceUid = dossierFile.getReferenceUid();
			dossierFileId = dossierFile.getDossierFileId();
			//Update DossierFile
			formDataObj = JSONFactoryUtil.createJSONObject(formData);
			formDataObj.put("LicenceNo", dossierFile.getDeliverableCode());
			formData = formDataObj.toJSONString();
			dossierFile.setFormData(formData);
			DossierFileLocalServiceUtil.updateDossierFile(dossierFile);
		} catch (Exception e) {
			_log.debug(e);
			//_log.error(e);
		}
		createFile.put(DossierFileTerm.DOSSIER_FILE_ID, dossierFileId);
		createFile.put(DossierFileTerm.FORM_DATA, formData);
		createFile.put(DossierFileTerm.REFERENCE_UID, docFileReferenceUid);
		createFile.put(DossierFileTerm.FILE_ENTRY_ID, 0l);

		return createFile;
	}

	private JSONObject processEFormByCreateFile(DossierPart dossierPart, long groupId, long dossierId, String sampleData,
			String fileTemplateNo, String dossierTempNo, JSONObject createFile, ServiceContext serviceContext) {

		String docFileReferenceUid = StringPool.BLANK;
		long dossierFileId  = 0;
		String formScript = dossierPart.getFormScript();
//		boolean eForm = Validator.isNotNull(formScript) ? true : false;
		boolean eForm = dossierPart.getEForm();
		String formData = eForm ? AutoFillFormData.sampleDataBinding(sampleData, dossierId, serviceContext) : StringPool.BLANK;

		// create Dossier File
		if (eForm) {
			DossierFile dossierFile = null;

			try {
				dossierFile = DossierFileLocalServiceUtil.getDossierFileByDID_FTNO_DPT_First(dossierId, fileTemplateNo,
						2, false, new DossierFileComparator(false, "createDate", Date.class));

				if (Validator.isNull(dossierFile)) {
					DossierFileActions actions = new DossierFileActionsImpl();
					dossierFile = actions.addDossierFile(groupId, dossierId, StringPool.BLANK, dossierTempNo,
							dossierPart.getPartNo(), fileTemplateNo, dossierPart.getPartName(), StringPool.BLANK, 0L,
							null, StringPool.BLANK, String.valueOf(false), serviceContext);
					_log.info("dossierFile create:" + dossierFile.getDossierPartNo() + "Timer create :" + new Date());
				}

				docFileReferenceUid = dossierFile.getReferenceUid();
				dossierFileId = dossierFile.getDossierFileId();
			} catch (Exception e) {
				_log.debug(e);
				//_log.error(e);
			}
		}
		createFile.put(DossierFileTerm.E_FORM, eForm);
		createFile.put(DossierFileTerm.DOSSIER_FILE_ID, dossierFileId);
		createFile.put(DossierFileTerm.FORM_DATA, formData);
		createFile.put(DossierFileTerm.FORM_SCRIPT, formScript);
		createFile.put(DossierFileTerm.REFERENCE_UID, docFileReferenceUid);
		createFile.put(DossierFileTerm.FILE_ENTRY_ID, 0l);

		return createFile;
		
	}
	public JSONArray getNextActionTimmer(long userId, long companyId, long groupId,
			LinkedHashMap<String, Object> params, Sort[] sorts, int start, int end, ServiceContext serviceContext)
			throws PortalException {

		JSONArray results = JSONFactoryUtil.createJSONArray();

		List<ProcessAction> lstProcessAction;

		Dossier dossier = null;

		DossierAction dossierAction = null;

		long serviceProcessId = 0;

		String stepCode;

		String actionCode = GetterUtil.getString(params.get(DossierActionTerm.ACTION_CODE));
		// TODO filter by Auto
		String auto = GetterUtil.getString(params.get(DossierActionTerm.AUTO));

		// auto != null ko check role, check dk preCondition = auto or submmit

		String referenceUid = GetterUtil.getString(params.get(DossierTerm.REFERENCE_UID));

		long dossierId = GetterUtil.getLong(params.get(DossierTerm.DOSSIER_ID));

		if (Validator.isNotNull(referenceUid)) {
			dossier = DossierLocalServiceUtil.getByRef(groupId, referenceUid);
		} else {
			dossier = DossierLocalServiceUtil.fetchDossier(dossierId);
		}

		if (dossier != null) {

			long dossierActionId = dossier.getDossierActionId();

			if (Validator.isNotNull(actionCode)) {
				dossierAction = getDossierAction(dossier.getDossierId(), actionCode);
			} else {
				dossierAction = DossierActionLocalServiceUtil.fetchDossierAction(dossierActionId);
			}

			serviceProcessId = dossierAction != null ? dossierAction.getServiceProcessId() : 0;

			stepCode = dossierAction != null ? dossierAction.getStepCode() : StringPool.BLANK;

			boolean pending = dossierAction != null ? dossierAction.getPending() : false;

			if (Validator.isNotNull(stepCode)) {

				try {
					lstProcessAction = ProcessActionLocalServiceUtil.getProcessActionByG_SPID_PRESC(groupId,
							serviceProcessId, stepCode);
					for (ProcessAction processAction : lstProcessAction) {

						String[] preConditions = StringUtil.split(processAction.getPreCondition());

						// String createDossierFiles =
						// processAction.getCreateDossierFiles();

						// String returnDossierFiles =
						// processAction.getReturnDossierFiles();

						boolean checkPreCondition = DossierMgtUtils.checkPreCondition(preConditions, dossier);

						if (!checkPreCondition) {
							continue;
						}

						JSONObject result = JSONFactoryUtil.createJSONObject();

						String postStepCode = processAction.getPostStepCode();

						if (Validator.isNull(postStepCode)) {
							continue;
						}

						ProcessStep processStep = ProcessStepLocalServiceUtil.fetchBySC_GID(postStepCode, groupId,
								processAction.getServiceProcessId());

						List<ProcessStepRole> lstProcessStepRole = ProcessStepRoleLocalServiceUtil
								.findByP_S_ID(processStep.getProcessStepId());
						List<User> lstUser = new ArrayList<User>();

						if (Validator.isNotNull(auto) && ("submit".equals(processAction.getAutoEvent())
								|| "timer".equals(processAction.getAutoEvent()))) {
							// TODO ?
						} else {
							// Check roles

							if (lstProcessStepRole == null || lstProcessStepRole.isEmpty()) {
								// Search in ServiceProcessRole
								List<ServiceProcessRole> serviceProcessRoles = ServiceProcessRoleLocalServiceUtil
										.findByS_P_ID(serviceProcessId);

								for (ServiceProcessRole serviceProcessRole : serviceProcessRoles) {

									List<User> users = UserLocalServiceUtil
											.getRoleUsers(serviceProcessRole.getRoleId());

									if (users != null) {
										for (User user : users) {
											HashMap<String, Object> moderator = new HashMap<>();
											moderator.put("moderator", serviceProcessRole.getModerator());
											user.setModelAttributes(moderator);
										}

										lstUser.addAll(users);
									}

								}
							} else {
								for (ProcessStepRole processStepRole : lstProcessStepRole) {

									List<User> users = UserLocalServiceUtil.getRoleUsers(processStepRole.getRoleId());

									if (users != null) {
										for (User user : users) {
											HashMap<String, Object> moderator = new HashMap<>();
											moderator.put("moderator", processStepRole.getModerator());
											user.setModelAttributes(moderator);
										}

										lstUser.addAll(users);
									}
								}
							}

						}

						result.put("pending", pending);
						result.put("processAction", processAction);
						result.put("lstUser", lstUser);

						results.put(result);
					}
				} catch (Exception e) {
					_log.error(e);
				}
			}
		}

		return results;
	}

	@Override
	public JSONObject getDossierActions(long dossierId, long groupId, Boolean owner, int start, int end, String sort,
			String order, ServiceContext serviceContext) throws PortalException {

		JSONObject result = JSONFactoryUtil.createJSONObject();

		Hits hits = null;

		SearchContext searchContext = new SearchContext();
		searchContext.setCompanyId(serviceContext.getCompanyId());

		try {
//			String referenceUid = null;
			if (dossierId == 0) {
//				referenceUid = String.valueOf(dossierId);
			}
			if (start == 0) {
				start = -1;
				end = -1;
			}
			LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>();

			params.put(Field.GROUP_ID, String.valueOf(groupId));
			params.put(DossierActionTerm.DOSSIER_ID, dossierId);
			if (owner != null && owner.booleanValue()) {
				params.put(Field.USER_ID, serviceContext.getUserId());
			}
			Sort[] sorts = new Sort[] {
					SortFactoryUtil.create(sort + "_sortable", Sort.STRING_TYPE, GetterUtil.getBoolean(order)) };

			hits = DossierActionLocalServiceUtil.searchLucene(params, sorts, start, end, searchContext);

			result.put("data", hits.toList());

			long total = DossierActionLocalServiceUtil.countLucene(params, searchContext);

			result.put("total", total);

		} catch (Exception e) {
			_log.error(e);
		}

		return result;
	}
	
//	private int getDuration(ServiceProcess serviceProcess) {
//		int duration = 0;
//		
//		int unit = serviceProcess.getDurationUnit();
//		
//		int count = serviceProcess.getDurationCount();
//		
//		if (unit == 0) {
//			duration = count;
//		}
//		
//		if (unit != 0) {
//			duration = count/8;
//		}
//		
//		return duration;
//	}

	protected String getDictItemName(long groupId, String collectionCode, String itemCode) {

		DictCollection dc = DictCollectionLocalServiceUtil.fetchByF_dictCollectionCode(collectionCode, groupId);

		if (Validator.isNotNull(dc)) {
			DictItem it = DictItemLocalServiceUtil.fetchByF_dictItemCode(itemCode, dc.getPrimaryKey(), groupId);
			if(Validator.isNotNull(it)){
				return it.getItemName();
			}else{
				return StringPool.BLANK;
			}
		} else {
			return StringPool.BLANK;
		}

	}
	
	private void generateCreateDossierFiles(long groupId, long userId, Dossier dossier, ProcessAction processAction) throws PortalException {
		ServiceContext context = new ServiceContext();
		context.setScopeGroupId(groupId);
		context.setCompanyId(dossier.getCompanyId());
		context.setUserId(userId);

		String createDossierFiles = processAction.getCreateDossierFiles();

		List<String> dossierFileTemplateNos = ListUtil.toList(StringUtil.split(createDossierFiles));

		DossierFileActions actions = new DossierFileActionsImpl();
		DossierFile dossierFile = null;
		String docFileReferenceUid;
		List<DossierFile> dossierFilesResult = null;
		
		if (dossierFileTemplateNos != null && !dossierFileTemplateNos.isEmpty()) {
			DossierTemplate dossierTemplate = DossierTemplateLocalServiceUtil.getByTemplateNo(groupId,
					dossier.getDossierTemplateNo());

			List<DossierPart> dossierParts = DossierPartLocalServiceUtil.getByTemplateNo(groupId,
					dossierTemplate.getTemplateNo());

			if (dossierParts != null) {
				for (DossierPart dossierPart : dossierParts) {

					String fileTemplateNo = dossierPart.getFileTemplateNo();

					if (dossierFileTemplateNos.contains(fileTemplateNo)) {
						String formData;
												
						//End add generate deliverable if has deliverable type
						if (Validator.isNull(dossierPart.getDeliverableType())) {						
							formData = AutoFillFormData.sampleDataBinding(dossierPart.getSampleData(),
									dossier.getDossierId(), context);
							try {
								dossierFile = DossierFileLocalServiceUtil
										.getDossierFileByDID_FTNO_DPT_First(dossier.getDossierId(),
												fileTemplateNo, 2, false, new DossierFileComparator(
														false, "createDate", Date.class));
							} catch (Exception e) {
								_log.debug(e);
								//_log.error(e);
							}

							if (Validator.isNull(dossierFile)
									&& dossierPart.getEForm()) {
								dossierFile = actions.addDossierFile(groupId, dossier.getDossierId(),
										StringPool.BLANK, dossier.getDossierTemplateNo(),
										dossierPart.getPartNo(), fileTemplateNo,
										dossierPart.getPartName(), StringPool.BLANK, 0L, null,
										StringPool.BLANK, String.valueOf(false), context);
								docFileReferenceUid = dossierFile.getReferenceUid();
								actions.updateDossierFileFormData(groupId, dossier.getDossierId(), docFileReferenceUid, formData, context);																													
							}
						}
						else {
							String deliverableTypeStr = dossierPart.getDeliverableType();
//							String formDataDeliverables = AutoFillFormData.sampleDataBinding(dossierPart.getSampleData(), dossier.getDossierId(), context);

							JSONObject formDataObj;
								
							DeliverableType deliverableTypeObject = DeliverableTypeLocalServiceUtil.getByCode(groupId, deliverableTypeStr);
							if (deliverableTypeObject != null) {
								String mappingData = deliverableTypeObject.getMappingData();
								JSONObject mappingDataObj = JSONFactoryUtil.createJSONObject(mappingData);
								if (mappingDataObj.has(DeliverableTypesTerm.DELIVERABLES_KEY)) {
									String deliverables = mappingDataObj.getString(DeliverableTypesTerm.DELIVERABLES_KEY);
									if (Validator.isNull(deliverables)) {
										//Add one deliverable
										dossierFilesResult = DossierFileLocalServiceUtil
														.getDossierFileByDID_FTNO_DPT(dossier.getDossierId(), fileTemplateNo, 2, false,
																QueryUtil.ALL_POS, QueryUtil.ALL_POS,
																new DossierFileComparator(false, "createDate", Date.class));
										if (dossierFilesResult != null && !dossierFilesResult.isEmpty()) {
										} else {
											formData = AutoFillFormData.sampleDataBinding(dossierPart.getSampleData(),
															dossier.getDossierId(), context);
											try {
												dossierFile = DossierFileLocalServiceUtil.getDossierFileByDID_FTNO_DPT_First(dossier.getDossierId(),
																			fileTemplateNo, 2, false, new DossierFileComparator(
																					false, "createDate", Date.class));
											} catch (Exception e) {
												_log.debug(e);
												//_log.error(e);
											}
											if (Validator.isNull(dossierFile)) {
												dossierFile = actions.addDossierFile(groupId, dossier.getDossierId(),
																	StringPool.BLANK, dossier.getDossierTemplateNo(),
																	dossierPart.getPartNo(), fileTemplateNo,
																	dossierPart.getPartName(), StringPool.BLANK, 0L, null,
																	StringPool.BLANK, String.valueOf(false), context);
											}
		
//											docFileReferenceUid = dossierFile.getReferenceUid();
		
											dossierFile.setFormData(formData);
											DossierFileLocalServiceUtil.updateDossierFile(dossierFile);
										}
									}
									else {
										formData = AutoFillFormData.sampleDataBinding(dossierPart.getSampleData(),
												dossier.getDossierId(), context);
										
										DeliverableType dlt = DeliverableTypeLocalServiceUtil.getByCode(groupId, dossierPart.getDeliverableType());
										mappingData = dlt.getMappingData();
										mappingDataObj = JSONFactoryUtil.createJSONObject(mappingData);
										formDataObj = JSONFactoryUtil.createJSONObject(formData);
										if (mappingDataObj.has(DeliverableTypesTerm.DELIVERABLES_KEY)) {
											deliverables = mappingDataObj.getString(DeliverableTypesTerm.DELIVERABLES_KEY);
											if (Validator.isNull(deliverables)) {
												try {
													dossierFile = DossierFileLocalServiceUtil
																			.getDossierFileByDID_FTNO_DPT_First(dossier.getDossierId(),
																					fileTemplateNo, 2, false, new DossierFileComparator(
																							false, "createDate", Date.class));
												} catch (Exception e) {
													_log.debug(e);
													//_log.error(e);
												}
												if (Validator.isNull(dossierFile)) {
													dossierFile = actions.addDossierFile(groupId, dossier.getDossierId(),
																			StringPool.BLANK, dossier.getDossierTemplateNo(),
																			dossierPart.getPartNo(), fileTemplateNo,
																			dossierPart.getPartName(), StringPool.BLANK, 0L, null,
																			StringPool.BLANK, String.valueOf(false), context);
												}
				
												docFileReferenceUid = dossierFile.getReferenceUid();
				
												actions.updateDossierFileFormData(groupId, dossier.getDossierId(), docFileReferenceUid, formData, context);																						
											}
											else {
												if (formDataObj.has(deliverables)) {
													formDataObj = JSONFactoryUtil.createJSONObject(formData);
													JSONArray deliverablesArr = JSONFactoryUtil.createJSONArray(formDataObj.getString(deliverables));
//													dossierFilesResult = DossierFileLocalServiceUtil
//														.getDossierFileByDID_FTNO_DPT_NOT_NULL_FID(dossier.getDossierId(), fileTemplateNo, 2,
//																					0, false);
													for (int i = 0; i < deliverablesArr.length(); i++) {
														JSONObject newFormDataObj = JSONFactoryUtil.createJSONObject();
														Iterator<?> keys = formDataObj.keys();
														while( keys.hasNext() ) {
															String key = (String)keys.next();
															if (!key.equals(deliverables)) {
																newFormDataObj.put(key, formDataObj.get(key));
															}
														}							
	
														JSONObject deliverableObj = deliverablesArr.getJSONObject(i);
														keys = deliverableObj.keys();
	
														while( keys.hasNext() ) {
															String key = (String)keys.next();
															newFormDataObj.put(key, deliverableObj.get(key));
														}	
																			
														try {
															dossierFile = DossierFileLocalServiceUtil
																	.getDossierFileByDID_FTNO_DPT_First(dossier.getDossierId(),
																			fileTemplateNo, 2, false, new DossierFileComparator(
																false, "createDate", Date.class));
														} catch (Exception e) {
															_log.debug(e);
															//_log.error(e);
														}
														if (Validator.isNull(dossierFile)) {
															dossierFile = actions.addDossierFile(groupId, dossier.getDossierId(),
																	StringPool.BLANK, dossier.getDossierTemplateNo(),
																	dossierPart.getPartNo(), fileTemplateNo,
																	dossierPart.getPartName(), StringPool.BLANK, 0L, null,
																	StringPool.BLANK, String.valueOf(false), context);
														}
	
														docFileReferenceUid = dossierFile.getReferenceUid();			
														actions.updateDossierFileFormData(groupId, dossier.getDossierId(), docFileReferenceUid, newFormDataObj.toJSONString(), context);																																							
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}	
	}
	
	private DossierAction doActionInsideProcess(long groupId, long userId, Dossier dossier, ActionConfig actionConfig, ProcessOption option, ProcessAction proAction,
			String actionCode, String actionUser, String actionNote, String payload, String assignUsers, 
			String payment,
			int syncType,
			ServiceContext context) throws PortalException {
//		_log.info("LamTV_STRART DO ACTION ==========GroupID: "+groupId + "|userId: "+userId);
		context.setUserId(userId);
		DossierAction dossierAction = null;
		JSONObject payloadObject = JSONFactoryUtil.createJSONObject(payload);
		User user = UserLocalServiceUtil.fetchUser(userId);
		Employee employee = null;
		if (user != null) {
			employee = EmployeeLocalServiceUtil.fetchByF_mappingUserId(groupId, user.getUserId());
		}		
		String type = StringPool.BLANK;
		String dossierStatus = dossier.getDossierStatus().toLowerCase();
		if (Validator.isNotNull(dossierStatus) && !"new".equals(dossierStatus)) {
			String applicantNote = _buildDossierNote(dossier, actionNote, groupId, type);
//			_log.info("applicantNote: "+applicantNote);

			dossier.setApplicantNote(applicantNote);

			dossier = DossierLocalServiceUtil.updateDossier(dossier);
		}

		long dossierId = dossier.getDossierId();
		ServiceProcess serviceProcess = null;
		String prevStatus = dossier.getDossierStatus();
		
		DossierAction previousAction = null;
		if (dossier.getDossierActionId() != 0) {
			previousAction = DossierActionLocalServiceUtil.fetchDossierAction(dossier.getDossierActionId());
		}
		
		Map<String, Boolean> flagChanged = null;
		
		if (Validator.isNotNull(payload)) {
			JSONObject pl = JSONFactoryUtil.createJSONObject(payload);
			_log.info("SONDT PAYLOAD DOSSIERACTION ======= " + pl);
			pl.put(DossierTerm.DOSSIER_NO, dossier.getDossierNo());
			dossier = DossierLocalServiceUtil.updateDossier(dossierId, pl);			
		}
				
		if (option != null && proAction != null) {
//			_log.info("In do action process action");
			long serviceProcessId = option.getServiceProcessId();
			serviceProcess = ServiceProcessLocalServiceUtil.fetchServiceProcess(serviceProcessId);
			// Add paymentFile
//			String paymentFee = proAction.getPaymentFee();
			String paymentFee = StringPool.BLANK;
//			_log.info("Payment fee: " + JSONFactoryUtil.looseSerialize(proAction.getPaymentFee()) + ", request payment: " + proAction.getRequestPayment());
			_log.info("SONDT DOSSIER ACTION RequestPayment ========= "+ JSONFactoryUtil.looseSerialize(proAction.getRequestPayment()));

			if (proAction.getRequestPayment() == ProcessActionTerm.REQUEST_PAYMENT_YEU_CAU_NOP_TAM_UNG
					|| proAction.getRequestPayment() == ProcessActionTerm.REQUEST_PAYMENT_YEU_CAU_QUYET_TOAN_PHI && Validator.isNotNull(payment)) {
				Long feeAmount = 0l, serviceAmount = 0l, shipAmount = 0l;
				String paymentNote = StringPool.BLANK;
				long advanceAmount = 0l;
				long paymentAmount = 0l;
				String epaymentProfile = StringPool.BLANK;
				String bankInfo = StringPool.BLANK;
				int paymentStatus = 0;
				String paymentMethod = StringPool.BLANK;
				NumberFormat fmt = NumberFormat.getNumberInstance(LocaleUtil.getDefault());
				DecimalFormatSymbols customSymbol = new DecimalFormatSymbols();
				customSymbol.setDecimalSeparator(',');
				customSymbol.setGroupingSeparator('.');
				((DecimalFormat)fmt).setDecimalFormatSymbols(customSymbol);
				fmt.setGroupingUsed(true);
				
				try {
					JSONObject paymentObj = JSONFactoryUtil.createJSONObject(payment);
//						_log.info("Payment object in do action: " + paymentObj);
					if (paymentObj.has("paymentFee")) {
						paymentFee = paymentObj.getString("paymentFee");
					}
					if (paymentObj.has("paymentNote")) {
						paymentNote = paymentObj.getString("paymentNote");
					}
					if (paymentObj.has("feeAmount")) {
						feeAmount = (Long)fmt.parse(paymentObj.getString("feeAmount"));
					}
					if (paymentObj.has("serviceAmount")) {
						serviceAmount = (Long)fmt.parse(paymentObj.getString("serviceAmount"));
					}
					if (paymentObj.has("shipAmount")) {
						shipAmount = (Long)fmt.parse(paymentObj.getString("shipAmount"));
					}
					if (paymentObj.has("requestPayment")) {
						paymentStatus = paymentObj.getInt("requestPayment");
					}
				}
				catch (JSONException e) {
					_log.debug(e);
					//_log.error(e);
				} catch (ParseException e) {
					_log.debug(e);
					//_log.error(e);
				}
//					_log.info("Fee amount: " + feeAmount + ", serviceAmount: " + serviceAmount + ", shipAmount: " + shipAmount);
				PaymentFile oldPaymentFile = PaymentFileLocalServiceUtil.getByDossierId(groupId, dossier.getDossierId());
				
				// _log.info("oldPaymentFile ===========================  " + JSONFactoryUtil.looseSerialize(oldPaymentFile));
				if (oldPaymentFile != null) {
					if (Validator.isNotNull(paymentNote))
						oldPaymentFile.setPaymentNote(paymentNote);
					oldPaymentFile = PaymentFileLocalServiceUtil.updatePaymentFile(oldPaymentFile);
					PaymentFileLocalServiceUtil.updateApplicantFeeAmount(oldPaymentFile.getPaymentFileId(), proAction.getRequestPayment(), feeAmount, serviceAmount, shipAmount);
				} else {
					if (proAction.getRequestPayment() == PaymentFileTerm.PAYMENT_STATUS_TAM_UNG) {
						advanceAmount = feeAmount + serviceAmount + shipAmount;
					}
					else if (proAction.getRequestPayment() == PaymentFileTerm.PAYMENT_STATUS_HOAN_THANH_PHI) {
						paymentAmount = feeAmount + serviceAmount + shipAmount - advanceAmount;
					}
					PaymentFile paymentFile = PaymentFileLocalServiceUtil.createPaymentFiles(userId, groupId,
							dossier.getDossierId(), dossier.getReferenceUid(), paymentFee, advanceAmount, feeAmount,
							serviceAmount, shipAmount, paymentAmount, paymentNote, epaymentProfile, bankInfo,
							paymentStatus, paymentMethod, context);
					
					long counterPaymentFile = CounterLocalServiceUtil.increment(PaymentFile.class.getName()+"paymentFileNo");
					
					Calendar cal = Calendar.getInstance();
					
					cal.setTime(new Date());
					
					int prefix = cal.get(Calendar.YEAR);
					
					String invoiceTemplateNo = Integer.toString(prefix) + String.format("%010d", counterPaymentFile);
					
					paymentFile.setInvoiceTemplateNo(invoiceTemplateNo);
					
					PaymentFileLocalServiceUtil.updatePaymentFile(paymentFile);
					//_log.info("==========Dossier Action SONDT START ========= ");
					//sondt create ePaymentProfile
					
					// create paymentFileActions
					PaymentFileActions actions = new PaymentFileActionsImpl();

					// get paymentConfig
					PaymentConfig paymentConfig = PaymentConfigLocalServiceUtil.getPaymentConfigByGovAgencyCode(groupId,
							dossier.getGovAgencyCode());
//					_log.info("Dossier Action SONDT groupId ========= "+ groupId + " === getGovAgencyCode ======== " + dossier.getGovAgencyCode());
//					_log.info("Dossier Action SONDT paymentConfig ========= "+ JSONFactoryUtil.looseSerialize(paymentConfig));
					// generator epaymentProfile
					JSONObject epaymentConfigJSON = paymentConfig != null ? JSONFactoryUtil.createJSONObject(paymentConfig.getEpaymentConfig()) : JSONFactoryUtil.createJSONObject();
					
					JSONObject epaymentProfileJSON = JSONFactoryUtil.createJSONObject();

					if (epaymentConfigJSON.has("paymentKeypayDomain")) {

						try {
							String generatorPayURL = PaymentUrlGenerator.generatorPayURL(groupId,
									paymentFile.getPaymentFileId(), paymentFee, dossierId);
							//_log.info("Dossier Action SONDT paymentFee ========= "+ paymentFee);
							epaymentProfileJSON.put("keypayUrl", generatorPayURL);

							// fill good_code to keypayGoodCode
							String pattern1 = "good_code=";
							String pattern2 = "&";

							String regexString = Pattern.quote(pattern1) + "(.*?)" + Pattern.quote(pattern2);

							Pattern p = Pattern.compile(regexString);
							Matcher m = p.matcher(generatorPayURL);

							if (m.find()) {
								String goodCode = m.group(1);

								epaymentProfileJSON.put("keypayGoodCode", goodCode);
							} else {
								epaymentProfileJSON.put("keypayGoodCode", StringPool.BLANK);
							}

							epaymentProfileJSON.put("keypayMerchantCode", epaymentConfigJSON.get("paymentMerchantCode"));
							epaymentProfileJSON.put("bank", "true");
							epaymentProfileJSON.put("paygate", "true");
							epaymentProfileJSON.put("feeAmount", feeAmount);
							epaymentProfileJSON.put("shipAmount", shipAmount);
							epaymentProfileJSON.put("advanceAmount", advanceAmount);
							epaymentProfileJSON.put("paymentAmount", paymentAmount);
							epaymentProfileJSON.put("serviceAmount", serviceAmount);
							
							actions.updateEProfile(dossierId, paymentFile.getReferenceUid(), epaymentProfileJSON.toJSONString(),
									context);

						} catch (IOException e) {
							// TODO Auto-generated catch block
//								e.printStackTrace();
							_log.error(e);
						}

					} else {
						actions.updateEProfile(dossierId, paymentFile.getReferenceUid(), epaymentProfileJSON.toJSONString(),
								context);
					}
					
					// end sondt
					
				}
//				try {
//					String serveNo = serviceProcess.getServerNo();
//					DossierPaymentUtils.processPaymentFile(proAction, paymentFee, groupId, dossier.getDossierId(), userId, context,
//							serveNo);
//				} catch (Exception e) {
//				}
			} else if (proAction.getRequestPayment() == ProcessActionTerm.REQUEST_PAYMENT_XAC_NHAN_HOAN_THANH_THU_PHI) {
				String CINVOICEUrl = "/postal/invoice";
				
				_log.info("SONDT payment REQUESTPAYMENT 5 ========= "+ JSONFactoryUtil.looseSerialize(payment));
				
				JSONObject resultObj = null;
				Map<String, Object> params = new HashMap<>();
				
				PaymentFile oldPaymentFile = PaymentFileLocalServiceUtil.getByDossierId(groupId, dossier.getDossierId());
				_log.info("SONDT oldPaymentFile REQUESTPAYMENT 5 ===========================  " + JSONFactoryUtil.looseSerialize(oldPaymentFile));
				
				if(Validator.isNotNull(oldPaymentFile.getEinvoice())){
					
					InvokeREST callRest = new InvokeREST();
					String baseUrl = RESTFulConfiguration.SERVER_PATH_BASE;
					HashMap<String, String> properties = new HashMap<String, String>();
					
					params.put("userName", "HA");	
					params.put("passWord", "1"); 	    	
					params.put("soid", "10"); 
					params.put("maHoadon", "01GTKT0/001"); 
					params.put("ngayHd", "01/08/2018"); 
					params.put("seri", "12314"); 
					params.put("maNthue", "01"); 
					params.put("kieuSo", "G"); 
					params.put("maKhackHang", "123133");
					params.put("ten", "Công ty TNHH ANH NAM"); 
					params.put("phone", "0123654456"); 
					params.put("tax", "2600303088"); 
					params.put("dchi", "Nam Từ Liêm, Hà Nội"); 
					params.put("maTk", "123830122233123"); 
					params.put("tenNh", "BIDV"); 
					params.put("mailH", "thaisonc89@yopmail.com");
					params.put("phoneH", "0987628930");
					params.put("tenM", "Trần Mai Trang");
					params.put("maKhL", "K");
					params.put("maNt", "VND");
					params.put("tg", "1");
					params.put("hthuc", "C");
					params.put("han", "");
					params.put("tlGgia", "0");
					params.put("ggia", "0");
					params.put("phi", "0");
					params.put("noidung", "tên thủ tục");
					params.put("tien", "220000");
					params.put("ttoan", "225000");
					params.put("maVtDetail", "AP123");
					params.put("tenDetail", "Cấp giấy phép mang vũ khí thể thao vào, ra khỏi lãnh thổ Việt Nam để luyện tập, thi đấu thể thao");
					params.put("dvtDetail", "to");
					params.put("luongDetail", "10");
					params.put("giaDetail", "20000");
					params.put("tienDetail", "200000");
					params.put("tsDetail", "10");
					params.put("thueDetail", "20000");
					params.put("ttoanDetail", "220000");
					
					resultObj = callRest.callPostAPI(groupId, HttpMethod.POST, "application/json", baseUrl,
							CINVOICEUrl, "", "", properties, params, context);
					
				}
				_log.info("SONDT resultCINVOICE REQUESTPAYMENT 5 ===========================  " + JSONFactoryUtil.looseSerialize(resultObj));
				
				if (Validator.isNotNull(oldPaymentFile) && Validator.isNotNull(resultObj)) {
					PaymentFileLocalServiceUtil.updateApplicantFeeAmount(oldPaymentFile.getPaymentFileId(),
							proAction.getRequestPayment(), oldPaymentFile.getFeeAmount(), oldPaymentFile.getServiceAmount(),
							oldPaymentFile.getShipAmount());
					
				}
				
				
			} else if (proAction.getRequestPayment() == ProcessActionTerm.REQUEST_PAYMENT_BAO_DA_NOP_PHI) {
				PaymentFile oldPaymentFile = PaymentFileLocalServiceUtil.getByDossierId(groupId, dossier.getDossierId());
				_log.info("SONDT oldPaymentFile REQUESTPAYMENT 3 ===========================  " + JSONFactoryUtil.looseSerialize(oldPaymentFile));
				if (oldPaymentFile != null) {
					PaymentFileLocalServiceUtil.updateApplicantFeeAmount(oldPaymentFile.getPaymentFileId(),
							proAction.getRequestPayment(), oldPaymentFile.getFeeAmount(), oldPaymentFile.getServiceAmount(),
							oldPaymentFile.getShipAmount());
					
				}
			}

			String postStepCode = proAction.getPostStepCode();

			ProcessStep curStep = ProcessStepLocalServiceUtil.fetchBySC_GID(postStepCode, groupId, serviceProcessId);
//			_log.info("Current step: " + curStep);
			
			int actionOverdue = getActionDueDate(groupId, dossierId, dossier.getReferenceUid(), proAction.getProcessActionId());
			Date dueDate = getDueDate(groupId, dossierId, dossier.getReferenceUid(), proAction.getProcessActionId());
			
//			_log.info("LamTV_NEXT_ACTION: " + proAction);

			String actionName = proAction.getActionName();
			if (Validator.isNotNull(proAction.getCreateDossiers())) {
				//Create new HSLT
				String GOVERNMENT_AGENCY = "GOVERNMENT_AGENCY";
				
				String govAgencyName = getDictItemName(groupId, GOVERNMENT_AGENCY, proAction.getCreateDossiers());
				String createDossiers = proAction.getCreateDossiers();
				String govAgencyCode = StringPool.BLANK;
				
				if (createDossiers.contains(StringPool.POUND)) {
					String[] splitCDs = createDossiers.split(StringPool.POUND);
					if (splitCDs.length == 2) {
						govAgencyCode = splitCDs[0];
					}
				}
				else {
					govAgencyCode = createDossiers;
				}
				
				ServiceConfig serviceConfig = ServiceConfigLocalServiceUtil.getBySICodeAndGAC(groupId, dossier.getServiceCode(), govAgencyCode);
				
				if (serviceConfig != null) {
					List<ProcessOption> lstOptions = ProcessOptionLocalServiceUtil.getByServiceProcessId(serviceConfig.getServiceConfigId());
					ProcessOption foundOption = null;
					if (createDossiers.contains(StringPool.POUND)) {
						String[] splitCDs = createDossiers.split(StringPool.POUND);
						if (splitCDs.length == 2) {
							String dossierTemplateNo = splitCDs[1];
							for (ProcessOption po : lstOptions) {
								DossierTemplate dt = DossierTemplateLocalServiceUtil.fetchDossierTemplate(po.getDossierTemplateId());
								if (dt.getTemplateNo().equals(dossierTemplateNo)) {
									foundOption = po;
									break;
								}
							}
						}
					}
					else {
						if (lstOptions.size() > 0) {
							foundOption = lstOptions.get(0);
						}
					}
					if (foundOption != null) {
						ServiceProcess ltProcess = ServiceProcessLocalServiceUtil.fetchServiceProcess(foundOption.getServiceProcessId());
						
						DossierTemplate dossierTemplate = DossierTemplateLocalServiceUtil.fetchDossierTemplate(foundOption.getDossierTemplateId());
						String delegateName = dossier.getDelegateName();
						String delegateAddress = dossier.getDelegateAddress();
						String delegateTelNo = dossier.getDelegateTelNo();
						String delegateEmail = dossier.getDelegateEmail();
						Dossier hsltDossier = DossierLocalServiceUtil.initDossier(groupId, 0l, UUID.randomUUID().toString(), 
								dossier.getCounter(), dossier.getServiceCode(),
								dossier.getServiceName(), govAgencyCode, govAgencyName, dossier.getApplicantName(), 
								dossier.getApplicantIdType(), dossier.getApplicantIdNo(), dossier.getApplicantIdDate(),
								dossier.getAddress(), dossier.getCityCode(), dossier.getCityName(), dossier.getDistrictCode(), 
								dossier.getDistrictName(), dossier.getWardCode(), dossier.getWardName(), dossier.getContactName(),
								dossier.getContactName(), dossier.getContactEmail(), dossierTemplate.getTemplateNo(), 
								dossier.getPassword(), dossier.getViaPostal(), dossier.getPostalAddress(), dossier.getPostalCityCode(),
								dossier.getPostalCityName(), dossier.getPostalTelNo(), 
								dossier.getOnline(), dossier.getNotification(), dossier.getApplicantNote(), DossierTerm.ORIGINALITY_DVCTT, context);
						if (user != null) {
							if (employee != null) {
								delegateName = employee.getFullName();
								delegateAddress = dossier.getGovAgencyName();
								delegateTelNo = employee.getTelNo();
								delegateEmail = employee.getEmail();
								
								if (hsltDossier != null) {
									hsltDossier.setDelegateName(delegateName);
									hsltDossier.setDelegateAddress(delegateAddress);
									hsltDossier.setDelegateTelNo(delegateTelNo);
									hsltDossier.setDelegateEmail(delegateEmail);
									hsltDossier = DossierLocalServiceUtil.updateDossier(hsltDossier);
								}
							}
						}

						//
						String dossierNote = StringPool.BLANK;
						if (previousAction != null) {
							dossierNote = previousAction.getActionNote();
							if (Validator.isNotNull(dossierNote)) {
								dossierNote = previousAction.getStepInstruction();
							}
						}
						if (hsltDossier != null) {
							//Set HSLT dossierId to origin dossier
							hsltDossier.setOriginDossierId(dossierId);
							hsltDossier.setServerNo(ltProcess.getServerNo());
							DossierLocalServiceUtil.updateDossier(hsltDossier);
							
							JSONObject jsonDataStatusText = getStatusText(groupId, DOSSIER_SATUS_DC_CODE, DossierTerm.DOSSIER_STATUS_NEW, StringPool.BLANK);
							DossierLocalServiceUtil.updateStatus(groupId, hsltDossier.getDossierId(), hsltDossier.getReferenceUid(),
									DossierTerm.DOSSIER_STATUS_NEW,
									jsonDataStatusText != null ? jsonDataStatusText.getString(DossierTerm.DOSSIER_STATUS_NEW) : StringPool.BLANK, StringPool.BLANK,
									StringPool.BLANK, StringPool.BLANK, dossierNote, context);
						}
						JSONObject jsonDataStatusText = getStatusText(groupId, DOSSIER_SATUS_DC_CODE, DossierTerm.DOSSIER_STATUS_INTEROPERATING, StringPool.BLANK);
						if (curStep != null) {
							dossier = DossierLocalServiceUtil.updateStatus(groupId, dossier.getDossierId(),
									dossier.getReferenceUid(), DossierTerm.DOSSIER_STATUS_INTEROPERATING,
									jsonDataStatusText != null ? jsonDataStatusText.getString(DossierTerm.DOSSIER_STATUS_INTEROPERATING) : StringPool.BLANK, StringPool.BLANK,
									StringPool.BLANK, curStep.getLockState(), dossierNote, context);
							
							
						}					
					}
				}
			}
			else {
				
			}
			if (curStep != null) {
				String curStatus = curStep.getDossierStatus();
				String curSubStatus = curStep.getDossierSubStatus();
				String stepCode = curStep.getStepCode();
				String stepName = curStep.getStepName();
				String stepInstruction = curStep.getStepInstruction();
				String sequenceNo = curStep.getSequenceNo();
				
//				_log.info("curStep.getDossierStatus(): " + curStep.getDossierStatus());
//				_log.info("curStep.getDossierSubStatus(): " + curStep.getDossierSubStatus());
				JSONObject jsonDataStatusText = getStatusText(groupId, DOSSIER_SATUS_DC_CODE, curStatus, curSubStatus);

				String fromStepCode = previousAction != null ? previousAction.getStepCode() : StringPool.BLANK;
				String fromStepName = previousAction != null ? previousAction.getStepName() : StringPool.BLANK;
				String fromSequenceNo = previousAction != null ? previousAction.getSequenceNo() : StringPool.BLANK;
				
				int state = DossierActionTerm.STATE_WAITING_PROCESSING;
				int eventStatus = (actionConfig != null ? (actionConfig.getEventType() == ActionConfigTerm.EVENT_TYPE_NOT_SENT ? DossierActionTerm.EVENT_STATUS_NOT_CREATED : DossierActionTerm.EVENT_STATUS_WAIT_SENDING) : DossierActionTerm.EVENT_STATUS_NOT_CREATED);

				dossierAction = DossierActionLocalServiceUtil.updateDossierAction(groupId, 0, dossierId,
						serviceProcessId, dossier.getDossierActionId(), 
						fromStepCode, fromStepName, fromSequenceNo,
						actionCode, actionUser, actionName, actionNote, actionOverdue,
						stepCode, stepName, 
						sequenceNo,
						dueDate, 0l, payload, stepInstruction, 
						state, eventStatus,
						context);
				
				//
				String dossierNote = StringPool.BLANK;
				if (dossierAction != null) {
					dossierNote = dossierAction.getActionNote();
					if (Validator.isNotNull(dossierNote)) {
						dossierNote = dossierAction.getStepInstruction();
					}
				}
				//Update previous action nextActionId
				if (previousAction != null && dossierAction != null) {
					previousAction = DossierActionLocalServiceUtil.updateNextActionId(previousAction.getDossierActionId(), dossierAction.getDossierActionId());					
					previousAction = DossierActionLocalServiceUtil.updateState(previousAction.getDossierActionId(), DossierActionTerm.STATE_ALREADY_PROCESSED);					
				}
				
				if (actionConfig != null) {
					if (actionConfig.getRollbackable()) {
						dossierAction = DossierActionLocalServiceUtil.updateRollbackable(dossierAction != null ? dossierAction.getDossierActionId() : 0l, true);
					}
				}
				else {
					if (proAction.isRollbackable()) {
						dossierAction = DossierActionLocalServiceUtil.updateRollbackable(dossierAction != null ? dossierAction.getDossierActionId() : 0l, true);
					}
				}
				//update dossierStatus
				dossier = DossierLocalServiceUtil.updateStatus(groupId, dossierId, dossier.getReferenceUid(), curStatus,
						jsonDataStatusText != null ? jsonDataStatusText.getString(curStatus) : StringPool.BLANK, curSubStatus,
						jsonDataStatusText != null ? jsonDataStatusText.getString(curSubStatus) : StringPool.BLANK, curStep.getLockState(), dossierNote, context);
				
				//Update dossier processing date
				flagChanged = updateProcessingDate(dossierAction, previousAction, curStep, dossier, curStatus, curSubStatus, prevStatus, context);
			}

				// update reference dossier
//				DossierAction prvAction = DossierActionLocalServiceUtil.getByNextActionId(dossierId, 0l);
				// Add DossierActionUser
				DossierActionUserImpl dossierActionUser = new DossierActionUserImpl();

				int allowAssignUser = proAction.getAllowAssignUser();
				_log.info("allowAssignUser: "+allowAssignUser);
				if (allowAssignUser != ProcessActionTerm.NOT_ASSIGNED) {
					if (Validator.isNotNull(assignUsers)) {
						_log.info("LamTV_PROCESS assignUsers != null");
						JSONArray assignedUsersArray = JSONFactoryUtil.createJSONArray(assignUsers);
					dossierActionUser.assignDossierActionUser(dossier, allowAssignUser,
							dossierAction, userId, groupId, proAction.getAssignUserId(),
							assignedUsersArray);
					} else {
						_log.info("PROCESS allowAssignUser");
						dossierActionUser.initDossierActionUser(proAction, dossier, allowAssignUser, dossierAction, userId, groupId,
								proAction.getAssignUserId());
					}
				} else {
					_log.info("PROCESS subUsers == null");
					_log.info("Dossier action: " + dossierAction);
					
					//Process role as step
					if (Validator.isNotNull(curStep.getRoleAsStep())) {
						_log.info("Copy role as step: " + curStep.getRoleAsStep());
						dossierActionUser.copyRoleAsStep(curStep, dossier);
					}	
					else {
						dossierActionUser.initDossierActionUser(proAction, dossier, allowAssignUser, dossierAction, userId, groupId,
								proAction.getAssignUserId());						
					}
				}

				dossier = DossierLocalServiceUtil.updateDossier(dossier);
				
//				PaymentFile paymentFile = PaymentFileLocalServiceUtil.getByDossierId(groupId, dossierId);
//				List<PaymentFile> syncPaymentFiles = new ArrayList<PaymentFile>();

//				for (PaymentFile pf : paymentFiles) {
//					if (pf.getIsNew()) {
//						syncPaymentFiles.add(pf);
//					}
//				}

//				for (PaymentFile spf : syncPaymentFiles) {
//				}
			
			//Update dossier document and dossier sync
			//Get next step
			ProcessStep nextStep = ProcessStepLocalServiceUtil.fetchBySC_GID(postStepCode, groupId, serviceProcessId);
			if (nextStep != null) {
			}
						
			//Check if generate dossier document
			ActionConfig ac = ActionConfigLocalServiceUtil.getByCode(groupId, actionCode);
			_log.info("ac: " + ac);
			if (ac != null) {
				//Only create dossier document if 2 && 3
				if (dossier.getOriginality() != DossierTerm.ORIGINALITY_DVCTT) {
					if (Validator.isNotNull(ac.getDocumentType()) && !ac.getActionCode().startsWith("@")) {
						//Generate document
						DocumentType dt = DocumentTypeLocalServiceUtil.getByTypeCode(groupId, ac.getDocumentType());
						if (dt != null) {
							String documentCode = DocumentTypeNumberGenerator.generateDocumentTypeNumber(groupId, ac.getCompanyId(), dt.getDocumentTypeId());
							DossierDocument dossierDocument = DossierDocumentLocalServiceUtil.addDossierDoc(groupId,
									dossierId, UUID.randomUUID().toString(), dossierAction.getDossierActionId(),
									dt.getTypeCode(), dt.getDocumentName(), documentCode, 0L, dt.getDocSync(), context);
												
							//Generate PDF
							String formData = dossierAction.getPayload();
//							formDataObj = processMergeDossierProcessRole(dossier, 1, formDataObj, dossierAction);
							JSONObject formDataObj = processMergeDossierFormData(dossier, JSONFactoryUtil.createJSONObject(formData));
							formDataObj = processMergeDossierProcessRole(dossier, 1, formDataObj, dossierAction);
							formDataObj.put("url", context.getPortalURL());
							Message message = new Message();
//							_log.info("Document script: " + dt.getDocumentScript());
							JSONObject msgData = JSONFactoryUtil.createJSONObject();
							msgData.put("className", DossierDocument.class.getName());
							msgData.put("classPK", dossierDocument.getDossierDocumentId());
							msgData.put("jrxmlTemplate", dt.getDocumentScript());
							msgData.put("formData", formDataObj.toJSONString());
							msgData.put("userId", userId);

							message.put("msgToEngine", msgData);
							MessageBusUtil.sendMessage("jasper/engine/out/destination", message);
							
							payloadObject.put("dossierDocument", dossierDocument.getDossierDocumentId());
						}
					}
				}
			}
			// sondt start sendvnpost
			String[] preConditions = StringUtil.split(proAction.getPreCondition());

			boolean checkPreCondition = DossierMgtUtils.checkPreCondition(preConditions, dossier);
			
			if (checkPreCondition) {
				
				_log.info("GO GO SEND VNPOST");
				
				vnpostEvent(dossier);
			}
			// sondt end sendvnpost
			
			//Generate output
			try {
				generateCreateDossierFiles(groupId, userId, dossier, proAction);
			}
			catch (Exception e) {
//				_log.debug(e);
				_log.error(e);
			}
		}
		else {
			
		}

		//Create notification
		createNotificationQueue(userId, groupId, dossier, actionConfig, context);
		//Create DossierSync
		String dossierRefUid = dossier.getReferenceUid();
		String syncRefUid = UUID.randomUUID().toString();
		if (syncType > 0) {
			int state = DossierActionUtils.getSyncState(syncType, dossier);
			
			//If state = 1 set pending dossier
			if (state == DossierSyncTerm.STATE_WAITING_SYNC) {
				if (dossierAction != null) {
					dossierAction.setPending(true);
					DossierActionLocalServiceUtil.updateDossierAction(dossierAction);
				}
			}
			else {
				if (dossierAction != null) {
					dossierAction.setPending(false);
					DossierActionLocalServiceUtil.updateDossierAction(dossierAction);				
				}
			}
			
			//Update payload
			
			JSONArray dossierFilesArr = JSONFactoryUtil.createJSONArray();
			if (actionConfig.getSyncType() == DossierSyncTerm.SYNCTYPE_REQUEST) {
				if (dossier.getOriginDossierId() == 0) {
					List<DossierFile> lstFiles = DossierFileLocalServiceUtil.findByDID(dossierId);
					if (lstFiles.size() > 0) {
						for (DossierFile df : lstFiles) {
							JSONObject dossierFileObj = JSONFactoryUtil.createJSONObject();
							dossierFileObj.put(DossierFileTerm.REFERENCE_UID, df.getReferenceUid());
							dossierFilesArr.put(dossierFileObj);
						}
					}					
				}
				else {
					ServiceConfig serviceConfig = ServiceConfigLocalServiceUtil.getBySICodeAndGAC(groupId, dossier.getServiceCode(), dossier.getGovAgencyCode());
					List<ProcessOption> lstOptions = ProcessOptionLocalServiceUtil.getByServiceProcessId(serviceConfig.getServiceConfigId());
					
					if (serviceConfig != null) {
						if (lstOptions.size() > 0) {
							ProcessOption processOption = lstOptions.get(0);
							
							DossierTemplate dossierTemplate = DossierTemplateLocalServiceUtil.fetchDossierTemplate(processOption.getDossierTemplateId());
							List<DossierPart> lstParts = DossierPartLocalServiceUtil.getByTemplateNo(groupId, dossierTemplate.getTemplateNo());
							
							List<DossierFile> lstFiles = DossierFileLocalServiceUtil.findByDID(dossier.getOriginDossierId());
							if (lstFiles.size() > 0) {
								for (DossierFile df : lstFiles) {
									boolean flagHslt = false;
									for (DossierPart dp : lstParts) {
										if (dp.getPartNo().equals(df.getDossierPartNo())) {
											flagHslt = true;
											break;
										}
									}
									if (flagHslt) {
										JSONObject dossierFileObj = JSONFactoryUtil.createJSONObject();
										dossierFileObj.put(DossierFileTerm.REFERENCE_UID, df.getReferenceUid());
										dossierFilesArr.put(dossierFileObj);										
									}
								}
							}										
						}
					}
					
				}
			}
			else {
				//Sync result files
				
			}
			
			payloadObject.put("dossierFiles", dossierFilesArr);
//			_log.info("Payload: " + payloadObject.toJSONString());
			
			if (Validator.isNotNull(proAction.getReturnDossierFiles())) {
				List<DossierFile> lsDossierFile = DossierFileLocalServiceUtil.findByDID(dossierId);
				dossierFilesArr = JSONFactoryUtil.createJSONArray();

				// check return file
				List<String> returnDossierFileTemplateNos = ListUtil
						.toList(StringUtil.split(proAction.getReturnDossierFiles()));

				for (DossierFile dossierFile : lsDossierFile) {
					if (returnDossierFileTemplateNos.contains(dossierFile.getFileTemplateNo())) {
						JSONObject dossierFileObj = JSONFactoryUtil.createJSONObject();
						dossierFileObj.put(DossierFileTerm.REFERENCE_UID, dossierFile.getReferenceUid());
						dossierFilesArr.put(dossierFileObj);

					}

				}
				payloadObject.put("dossierFiles", dossierFilesArr);				
			}
			
			List<DossierDocument> lstDossierDocuments = DossierDocumentLocalServiceUtil.getDossierDocumentList(dossierId, QueryUtil.ALL_POS, QueryUtil.ALL_POS);
			JSONArray dossierDocumentArr = JSONFactoryUtil.createJSONArray();

			for (DossierDocument dossierDocument : lstDossierDocuments) {
				JSONObject dossierDocumentObj = JSONFactoryUtil.createJSONObject();
				dossierDocumentObj.put(DossierDocumentTerm.REFERENCE_UID, dossierDocument.getReferenceUid());
				dossierDocumentArr.put(dossierDocumentObj);
			}
			payloadObject.put("dossierFiles", dossierFilesArr);				
			payloadObject.put("dossierDocuments", dossierDocumentArr);
			
			//Put dossier note
			payloadObject.put(DossierTerm.DOSSIER_NOTE, dossier.getDossierNote());
			
//			_log.info("Flag changed: " + flagChanged);
			payloadObject = DossierActionUtils.buildChangedPayload(payloadObject, flagChanged, dossier);
			
			DossierSyncLocalServiceUtil.updateDossierSync(groupId, userId, dossierId, dossierRefUid, syncRefUid,
					dossierAction.getPrimaryKey(), actionCode, proAction.getActionName(), actionUser, actionNote,
					syncType, actionConfig.getInfoType(), payloadObject.toJSONString(), serviceProcess.getServerNo(), state);
			
			if (state == DossierSyncTerm.STATE_NOT_SYNC
					&& actionConfig != null && actionConfig.getEventType() == ActionConfigTerm.EVENT_TYPE_SENT) {
				publishEvent(dossier);
			}
		}
		else if (actionConfig != null && actionConfig.getEventType() == ActionConfigTerm.EVENT_TYPE_SENT) {
			publishEvent(dossier);			
		}
		//Do action hslt
		if (Validator.isNotNull(actionConfig) && Validator.isNotNull(actionConfig.getMappingAction())) {
			ActionConfig mappingConfig = ActionConfigLocalServiceUtil.getByCode(groupId, actionConfig.getMappingAction());
			if (dossier.getOriginDossierId() != 0) {
				Dossier hslt = DossierLocalServiceUtil.fetchDossier(dossier.getOriginDossierId());
				ProcessOption optionHslt = getProcessOption(hslt.getServiceCode(), hslt.getGovAgencyCode(),
						hslt.getDossierTemplateNo(), groupId);
				ProcessAction actionHslt = getProcessAction(groupId, hslt.getDossierId(), hslt.getReferenceUid(), actionConfig.getMappingAction(), optionHslt.getServiceProcessId());
				
				String actionUserHslt = actionUser;
				if (employee != null) {
					actionUserHslt = actionUser;
				}
				doAction(groupId, userId, hslt, optionHslt, actionHslt, actionConfig.getMappingAction(), actionUserHslt, actionNote, payload, assignUsers, payment, mappingConfig.getSyncType(), context);
			}
			else {
				Dossier originDossier = DossierLocalServiceUtil.getByOrigin(groupId, dossierId);
				if (originDossier != null) {					
					ProcessOption optionOrigin = getProcessOption(originDossier.getServiceCode(), originDossier.getGovAgencyCode(),
							originDossier.getDossierTemplateNo(), groupId);
					ProcessAction actionOrigin = getProcessAction(groupId, originDossier.getDossierId(), originDossier.getReferenceUid(), actionConfig.getMappingAction(), optionOrigin.getServiceProcessId());
					
					doAction(groupId, userId, originDossier, optionOrigin, actionOrigin, actionConfig.getMappingAction(), actionUser, actionNote, payload, assignUsers, payment, mappingConfig.getSyncType(), context);
				}
			}
		}
		//Reindex dossier
		Indexer<Dossier> indexer = IndexerRegistryUtil
				.nullSafeGetIndexer(Dossier.class);
		indexer.reindex(dossier);
		_log.info("dossierActionFINISH: "+dossierAction);
		return dossierAction;		
	}
	
	private DossierAction doActionOutsideProcess(long groupId, long userId, Dossier dossier, ActionConfig actionConfig, ProcessOption option, ProcessAction proAction,
			String actionCode, String actionUser, String actionNote, String payload, String assignUsers, 
			String payment,
			int syncType,
			ServiceContext context) throws PortalException {
//		_log.info("LamTV_STRART DO ACTION ==========GroupID: "+groupId + "|userId: "+userId);
		context.setUserId(userId);
		DossierAction dossierAction = null;

		String type = StringPool.BLANK;
		String dossierStatus = dossier.getDossierStatus().toLowerCase();
		if (Validator.isNotNull(dossierStatus) && !"new".equals(dossierStatus)) {
			String applicantNote = _buildDossierNote(dossier, actionNote, groupId, type);
//			_log.info("applicantNote: "+applicantNote);

			dossier.setApplicantNote(applicantNote);

			dossier = DossierLocalServiceUtil.updateDossier(dossier);
		}

		ServiceProcess serviceProcess = null;
		if (option != null) {
			long serviceProcessId = option.getServiceProcessId();
			serviceProcess = ServiceProcessLocalServiceUtil.fetchServiceProcess(serviceProcessId);
		}
		
		dossierAction = DossierActionLocalServiceUtil.fetchDossierAction(dossier.getDossierActionId());
		ActionConfig ac = ActionConfigLocalServiceUtil.getByCode(groupId, actionCode);
		JSONObject payloadObject = JSONFactoryUtil.createJSONObject(payload);
		
		if (Validator.isNotNull(payload)) {
			dossier = DossierLocalServiceUtil.updateDossier(dossier.getDossierId(), payloadObject);			
		}
		
		//Create DossierSync
		String dossierRefUid = dossier.getReferenceUid();
		String syncRefUid = UUID.randomUUID().toString();
		if (syncType > 0) {
			int state = DossierActionUtils.getSyncState(syncType, dossier);
						
			//Update payload
												
			DossierSyncLocalServiceUtil.updateDossierSync(groupId, userId, dossier.getDossierId(), dossierRefUid, syncRefUid,
					dossierAction.getPrimaryKey(), actionCode, ac.getActionName(), actionUser, actionNote,
					syncType, ac.getInfoType(), payloadObject.toJSONString(), serviceProcess != null ? serviceProcess.getServerNo() : StringPool.BLANK, state);
			
		}
		
		if (ac != null && dossierAction != null) {
			//Only create dossier document if 2 && 3
			if (dossier.getOriginality() != DossierTerm.ORIGINALITY_DVCTT) {
				if (Validator.isNotNull(ac.getDocumentType()) && !ac.getActionCode().startsWith("@")) {
					//Generate document
					DocumentType dt = DocumentTypeLocalServiceUtil.getByTypeCode(groupId, ac.getDocumentType());
					String documentCode = DocumentTypeNumberGenerator.generateDocumentTypeNumber(groupId, ac.getCompanyId(), dt.getDocumentTypeId());
					
					DossierDocument dossierDocument = DossierDocumentLocalServiceUtil.addDossierDoc(groupId, dossier.getDossierId(), UUID.randomUUID().toString(), dossierAction.getDossierActionId(), dt.getTypeCode(), dt.getDocumentName(), documentCode, 0L, dt.getDocSync(), context);

					//Generate PDF
					String formData = payload;
					JSONObject formDataObj = processMergeDossierFormData(dossier, JSONFactoryUtil.createJSONObject(formData));
//					_log.info("Dossier document form data action outside: " + formDataObj.toJSONString());
					Message message = new Message();
//					_log.info("Document script: " + dt.getDocumentScript());
					JSONObject msgData = JSONFactoryUtil.createJSONObject();
					msgData.put("className", DossierDocument.class.getName());
					msgData.put("classPK", dossierDocument.getDossierDocumentId());
					msgData.put("jrxmlTemplate", dt.getDocumentScript());
					msgData.put("formData", formDataObj.toJSONString());
					msgData.put("userId", userId);

					message.put("msgToEngine", msgData);
					MessageBusUtil.sendMessage("jasper/engine/out/destination", message);
				}					
			}
		}
		
		return dossierAction;
	}

	@Override
	public DossierAction doAction(long groupId, long userId, Dossier dossier, ProcessOption option, ProcessAction proAction,
			String actionCode, String actionUser, String actionNote, String payload, String assignUsers, 
			String payment,
			int syncType,
			ServiceContext context) throws PortalException {
//		_log.info("LamTV_STRART DO ACTION ==========GroupID: "+groupId + "|userId: "+userId);
		context.setUserId(userId);
		DossierAction dossierAction = null;

		ActionConfig actionConfig = null;
		actionConfig = ActionConfigLocalServiceUtil.getByCode(groupId, actionCode);
		
		if (actionConfig != null && !actionConfig.getInsideProcess()) {
			dossierAction = doActionOutsideProcess(groupId, userId, dossier, actionConfig, option, proAction, actionCode, actionUser, actionNote, payload, assignUsers, payment, syncType, context);			
		}
		else {
			dossierAction = doActionInsideProcess(groupId, userId, dossier, actionConfig, option, proAction, actionCode, actionUser, actionNote, payload, assignUsers, payment, syncType, context);
		}
				
		return dossierAction;
	}
	
	private void publishEvent(Dossier dossier) {
		Message message = new Message();
		JSONObject msgData = JSONFactoryUtil.createJSONObject();

		message.put("msgToEngine", msgData);
		message.put("dossier", DossierMgtUtils.convertDossierToJSON(dossier));
		
		MessageBusUtil.sendMessage(DossierTerm.PUBLISH_DOSSIER_DESTINATION, message);		
	}
	
	private void vnpostEvent(Dossier dossier) {
		Message message = new Message();
		JSONObject msgData = JSONFactoryUtil.createJSONObject();

		message.put("msgToEngine", msgData);
		message.put("dossier", DossierMgtUtils.convertDossierToJSON(dossier));
		
		MessageBusUtil.sendMessage("vnpost/dossier/in/destination", message);		
	}
	
	private JSONObject processMergeDossierFormData(Dossier dossier, JSONObject jsonData) {
		jsonData.put(DossierTerm.GOV_AGENCY_NAME, dossier.getGovAgencyName());
		jsonData.put(DossierTerm.APPLICANT_ID_NO, dossier.getApplicantIdNo());
		jsonData.put(DossierTerm.APPLICANT_ID_TYPE, dossier.getApplicantIdType());
		jsonData.put(DossierTerm.APPLICANT_ID_DATE,
				APIDateTimeUtils.convertDateToString(dossier.getApplicantIdDate(), APIDateTimeUtils._NORMAL_PARTTERN));
		jsonData.put(DossierTerm.CITY_CODE, dossier.getCityCode());
		jsonData.put(DossierTerm.CITY_NAME, dossier.getCityName());
		jsonData.put(DossierTerm.DISTRICT_CODE, dossier.getDistrictCode());
		jsonData.put(DossierTerm.DISTRICT_NAME, dossier.getDistrictName());
		jsonData.put(DossierTerm.WARD_CODE, dossier.getWardCode());
		jsonData.put(DossierTerm.WARD_NAME, dossier.getWardName());
		jsonData.put(DossierTerm.DOSSIER_NO, dossier.getDossierNo());
		jsonData.put(DossierTerm.APPLICANT_NAME, dossier.getApplicantName());
		jsonData.put(DossierTerm.ADDRESS, dossier.getAddress());
		jsonData.put(DossierTerm.CONTACT_TEL_NO, dossier.getContactTelNo());
		jsonData.put(DossierTerm.CONTACT_EMAIL, dossier.getContactEmail());
		jsonData.put(DossierTerm.CONTACT_NAME, dossier.getContactName());
		jsonData.put(DossierTerm.DELEGATE_ADDRESS, dossier.getDelegateAddress());
		jsonData.put(DossierTerm.SERVICE_NAME, dossier.getServiceName());
		jsonData.put(DossierTerm.SAMPLE_COUNT, dossier.getSampleCount());
		jsonData.put(DossierTerm.DURATION_COUNT, dossier.getDurationCount());
		jsonData.put(DossierTerm.DURATION_UNIT, dossier.getDurationUnit());
		jsonData.put(DossierTerm.RECEIVE_DATE,
				APIDateTimeUtils.convertDateToString(dossier.getReceiveDate(), APIDateTimeUtils._NORMAL_PARTTERN));
		jsonData.put(DossierTerm.DUE_DATE,
				APIDateTimeUtils.convertDateToString(dossier.getDueDate(), APIDateTimeUtils._NORMAL_PARTTERN));
		if (dossier.getExtendDate() != null) {
			jsonData.put(DossierTerm.EXTEND_DATE,
					APIDateTimeUtils.convertDateToString(dossier.getExtendDate(), APIDateTimeUtils._NORMAL_PARTTERN));
		}
		jsonData.put(DossierTerm.POSTAL_ADDRESS, dossier.getPostalAddress());
		jsonData.put(DossierTerm.COUNTER, dossier.getCounter());
		jsonData.put(DossierTerm.REGISTER_BOOK_CODE, dossier.getRegisterBookCode());
		//
		long groupId = dossier.getGroupId();
		JSONArray dossierMarkArr = JSONFactoryUtil.createJSONArray();
		long dossierId = dossier.getDossierId();
		String templateNo = dossier.getDossierTemplateNo();
		List<DossierMark> dossierMarkList = DossierMarkLocalServiceUtil.getDossierMarks(groupId, dossierId);
		if (dossierMarkList != null && dossierMarkList.size() > 0) {
			JSONObject jsonMark = null;
			String partNo;
			for (DossierMark dossierMark : dossierMarkList) {
				jsonMark = JSONFactoryUtil.createJSONObject();
				partNo = dossierMark.getDossierPartNo();
				if (Validator.isNotNull(partNo)) {
					DossierPart part = DossierPartLocalServiceUtil.getByTempAndPartNo(groupId, templateNo, partNo);
					if (part != null) {
						jsonMark.put(DossierPartTerm.DOSSIERPART_ID, part.getDossierPartId());
						jsonMark.put(DossierPartTerm.PART_NAME, part.getPartName());
						jsonMark.put(DossierPartTerm.PART_TIP, part.getPartTip());
						jsonMark.put(DossierPartTerm.PART_TYPE, part.getPartType());
					}
				}
				jsonMark.put(DossierPartTerm.PART_NO, partNo);
				jsonMark.put(DossierPartTerm.FILE_MARK, dossierMark.getFileMark());
				jsonMark.put(DossierPartTerm.FILE_CHECK, dossierMark.getFileCheck());
				jsonMark.put(DossierPartTerm.FILE_COMMENT, dossierMark.getFileComment());
//				String strDossierMark = JSONFactoryUtil.looseSerialize(dossierMark);
				dossierMarkArr.put(jsonMark);
			}
		}
		jsonData.put(DossierTerm.DOSSIER_MARKS, dossierMarkArr);
		return jsonData;
	}

	//LamTV_ Mapping process dossier and formData
	private JSONObject processMergeDossierProcessRole(Dossier dossier, int length, JSONObject jsonData,
			DossierAction dAction) {
		//
		long groupId = dossier.getGroupId();
		if (dAction != null) {
			long serviceProcessId = dAction.getServiceProcessId();
			jsonData.put(DossierTerm.GOV_AGENCY_NAME, dossier.getGovAgencyName());
			jsonData.put(DossierTerm.TOTAL, length);
			jsonData.put(DossierTerm.ACTION_USER, dAction.getActionUser());
			String sequenceNo = dAction.getSequenceNo();
			if (Validator.isNotNull(sequenceNo)) {
				ProcessSequence sequence = ProcessSequenceLocalServiceUtil.findBySID_SNO(groupId, serviceProcessId,
						sequenceNo);
				if (sequence != null) {
					jsonData.put(DossierTerm.SEQUENCE_ROLE, sequence.getSequenceRole());
				} else {
					jsonData.put(DossierTerm.SEQUENCE_ROLE, StringPool.BLANK);
				}
			} else {
				jsonData.put(DossierTerm.SEQUENCE_ROLE, StringPool.BLANK);
			}
			// Process get Next sequence Role
			List<ProcessSequence> sequenceList = ProcessSequenceLocalServiceUtil.getByServiceProcess(groupId,
					serviceProcessId);
			String[] sequenceArr = null;
			if (sequenceList != null && !sequenceList.isEmpty()) {
				int lengthSeq = sequenceList.size();
				sequenceArr = new String[lengthSeq];
				for (int i = 0; i < lengthSeq; i++) {
					ProcessSequence processSequence = sequenceList.get(i);
					if (processSequence != null) {
						sequenceArr[i] = processSequence.getSequenceNo();
					}
				}
			}

			if (sequenceArr != null && sequenceArr.length > 0) {
				for (int i = 0; i < sequenceArr.length; i++) {
//						_log.info("sequenceArr[i]: "+sequenceArr[i]);
				}
				Arrays.sort(sequenceArr);
				for (int i = 0; i < sequenceArr.length - 1; i++) {
					String seq = sequenceArr[i];
					if (sequenceNo.equals(seq)) {
						String nextSequenceNo = sequenceArr[i + 1];
						if (Validator.isNotNull(nextSequenceNo)) {
							ProcessSequence sequence = ProcessSequenceLocalServiceUtil.findBySID_SNO(groupId,
									serviceProcessId, nextSequenceNo);
							if (sequence != null) {
								jsonData.put(DossierTerm.NEXT_SEQUENCE_ROLE, sequence.getSequenceRole());
							} else {
								jsonData.put(DossierTerm.NEXT_SEQUENCE_ROLE, StringPool.BLANK);
							}
						} else {
							jsonData.put(DossierTerm.NEXT_SEQUENCE_ROLE, StringPool.BLANK);
						}
					}
				}
			} else {
				jsonData.put(DossierTerm.NEXT_SEQUENCE_ROLE, StringPool.BLANK);
			}
		}

		return jsonData;
	}
	
	private void createNotificationQueue(long userId, long groupId, Dossier dossier, ActionConfig actionConfig, ServiceContext context) {
		if (actionConfig != null && Validator.isNotNull(actionConfig.getNotificationType())) {
			DossierAction dossierAction = DossierActionLocalServiceUtil.fetchDossierAction(dossier.getDossierActionId());
			User u = UserLocalServiceUtil.fetchUser(userId);
			
			Notificationtemplate notiTemplate = NotificationtemplateLocalServiceUtil.fetchByF_NotificationtemplateByType(groupId, actionConfig.getNotificationType());
			Date now = new Date();
	        Calendar cal = Calendar.getInstance();
	        cal.setTime(now);
	        cal.add(Calendar.DATE, 5);
	        
			Date expired = cal.getTime();
			if (notiTemplate != null) {
				if (actionConfig.getDocumentType().startsWith("APLC")) {
					if (dossier.getOriginality() == DossierTerm.ORIGINALITY_MOTCUA
							|| dossier.getOriginality() == DossierTerm.ORIGINALITY_LIENTHONG) {
						try {
							NotificationQueueLocalServiceUtil.addNotificationQueue(
									userId, groupId, 
									actionConfig.getNotificationType(), 
									Dossier.class.getName(), 
									String.valueOf(dossier.getDossierId()), 
									dossierAction.getPayload(), 
									u.getFullName(), 
									dossier.getApplicantName(), 
									0L, 
									dossier.getContactEmail(), 
									dossier.getContactTelNo(), 
									now, 
									expired, 
									context);
						} catch (NoSuchUserException e) {
							_log.debug(e);
							//_log.error(e);
//							e.printStackTrace();
						}
					}
				}
				else if (actionConfig.getDocumentType().startsWith("EMPL")) {
					if (dossier.getOriginality() == DossierTerm.ORIGINALITY_MOTCUA
							|| dossier.getOriginality() == DossierTerm.ORIGINALITY_LIENTHONG) {
						try {
							StepConfig stepConfig = StepConfigLocalServiceUtil.getByCode(groupId, dossierAction.getFromStepCode());
							if (stepConfig != null && stepConfig.getStepType() == StepConfigTerm.STEP_TYPE_DISPLAY_MENU_BY_PROCESSED) {
								List<DossierActionUser> lstDaus = DossierActionUserLocalServiceUtil.getByDossierAndStepCode(dossier.getDossierId(), dossierAction.getFromStepCode());
								for (DossierActionUser dau : lstDaus) {
									if (dau.getAssigned() == DossierActionUserTerm.ASSIGNED_TH || dau.getAssigned() == DossierActionUserTerm.ASSIGNED_PH) {
										Employee employee = EmployeeLocalServiceUtil.fetchByF_mappingUserId(groupId, u.getUserId());
										String telNo = employee != null ? employee.getTelNo() : StringPool.BLANK;
										String fullName = employee != null ? employee.getFullName() : u.getFullName();
										NotificationQueueLocalServiceUtil.addNotificationQueue(
												userId, groupId, 
												actionConfig.getNotificationType(), 
												Dossier.class.getName(), 
												String.valueOf(dossier.getDossierId()), 
												dossierAction.getPayload(), 
												fullName, 
												fullName, 
												0L, 
												u.getEmailAddress(), 
												telNo, 
												now, 
												expired, 
												context);																		
									}
								}
							}
						} catch (NoSuchUserException e) {
							_log.debug(e);
							//_log.error(e);
//							e.printStackTrace();
						}
					}					
				}
				else if (actionConfig.getDocumentType().startsWith("USER")) {
					
				}
			}
		}		
	}
	
	private Map<String, Boolean> updateProcessingDate(DossierAction dossierAction, DossierAction prevAction, ProcessStep processStep, Dossier dossier, String curStatus, String curSubStatus, String prevStatus, ServiceContext context) {
		Date now = new Date();
		Map<String, Boolean> bResult = new HashMap<>();
		String serviceCode = dossier.getServiceCode();
		String govAgencyCode = dossier.getGovAgencyCode();
		String dossierTemplateNo = dossier.getDossierTemplateNo();
		ProcessOption option = null;
		LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>();
		params.put(DossierTerm.GOV_AGENCY_CODE, dossier.getGovAgencyCode());
		params.put(DossierTerm.SERVICE_CODE, dossier.getServiceCode());
		params.put(DossierTerm.DOSSIER_TEMPLATE_NO, dossier.getDossierTemplateNo());
		params.put(DossierTerm.DOSSIER_STATUS, StringPool.BLANK);
		ServiceProcess serviceProcess =  null;
		
		try {
			option = getProcessOption(serviceCode, govAgencyCode, dossierTemplateNo, dossier.getGroupId());
			long serviceProcessId = option.getServiceProcessId();
			serviceProcess = ServiceProcessLocalServiceUtil.fetchServiceProcess(serviceProcessId);
			
		} catch (PortalException e) {
			_log.debug(e);
			//_log.error(e);
//			e.printStackTrace();
		}		
		
		if ((Validator.isNull(prevStatus) && DossierTerm.DOSSIER_STATUS_NEW.equals(curStatus)
				&& (dossier.getOriginality() == DossierTerm.ORIGINALITY_MOTCUA))
				|| (DossierTerm.DOSSIER_STATUS_RECEIVING.equals(curStatus) && dossier.getOriginality() == DossierTerm.ORIGINALITY_LIENTHONG)) {
			
			try {
				if (Validator.isNotNull(option)) {
					String dossierRef = DossierNumberGenerator.generateDossierNumber(dossier.getGroupId(), dossier.getCompanyId(),
							dossier.getDossierId(), option.getProcessOptionId(), serviceProcess.getDossierNoPattern(), params);

					dossier.setDossierNo(dossierRef.trim());
					
//					DossierLocalServiceUtil.updateDossier(dossier);
					
					bResult.put(DossierTerm.DOSSIER_NO, true);					
				}
			} catch (PortalException e) {
				_log.debug(e);
				//_log.error(e);
//				e.printStackTrace();
			}		
		}
		
		if (DossierTerm.DOSSIER_STATUS_NEW.equals(prevStatus)
				&& DossierTerm.DOSSIER_STATUS_RECEIVING.equals(curStatus)) {
//			try {
//				DossierLocalServiceUtil.updateSubmittingDate(dossier.getGroupId(), dossier.getDossierId(), dossier.getReferenceUid(), now, context);
				dossier.setSubmitDate(now);
				bResult.put(DossierTerm.SUBMIT_DATE, true);
//			} catch (PortalException e) {
//				_log.error(e);
//				e.printStackTrace();
//			}
		}
		if (dossier.getOriginality() != DossierTerm.ORIGINALITY_DVCTT &&
				(DossierTerm.DOSSIER_STATUS_PROCESSING.equals(curStatus)
				|| DossierTerm.DOSSIER_STATUS_NEW.equals(curStatus))
				&& dossier.getReceiveDate() == null) {
//			try {
//				DossierLocalServiceUtil.updateReceivingDate(dossier.getGroupId(), dossier.getDossierId(), dossier.getReferenceUid(), now, context);
				dossier.setReceiveDate(now);
				bResult.put(DossierTerm.RECEIVE_DATE, true);

				Double durationCount = serviceProcess.getDurationCount();
				int durationUnit = serviceProcess.getDurationUnit();
				Date dueDate = null;
				if (Validator.isNotNull(durationCount) && durationCount > 0) {
					dueDate = HolidayUtils.getDueDate(now, durationCount, durationUnit, dossier.getGroupId());
				}
				
				if (Validator.isNotNull(dueDate)) {
					dossier.setDueDate(dueDate);
//					DossierLocalServiceUtil.updateDueDate(dossier.getGroupId(), dossier.getDossierId(), dossier.getReferenceUid(), dueDate, context);					
					bResult.put(DossierTerm.DUE_DATE, true);
				}
				
				dossier.setDurationCount(durationCount);
				dossier.setDurationUnit(durationUnit);
				
//				dossier = DossierLocalServiceUtil.updateDossier(dossier);
//			} catch (PortalException e) {
//				_log.error(e);
//				e.printStackTrace();
//			}
		}

		if (DossierTerm.DOSSIER_STATUS_RECEIVING.equals(prevStatus) && DossierTerm.DOSSIER_STATUS_PROCESSING.equals(curStatus)) {	
//			try {
//				DossierLocalServiceUtil.updateProcessDate(dossier.getGroupId(), dossier.getDossierId(), dossier.getReferenceUid(), now, context);
				dossier.setProcessDate(now);
				bResult.put(DossierTerm.PROCESS_DATE, true);
//			} catch (PortalException e) {
//				_log.error(e);
//				e.printStackTrace();
//			}
		}
		if (DossierTerm.DOSSIER_STATUS_RELEASING.equals(curStatus)
				|| DossierTerm.DOSSIER_STATUS_DENIED.equals(curStatus)
				|| DossierTerm.DOSSIER_STATUS_UNRESOLVED.equals(curStatus)
				|| DossierTerm.DOSSIER_STATUS_CANCELLED.equals(curStatus)
				|| DossierTerm.DOSSIER_STATUS_DONE.equals(curStatus)) {
			if (Validator.isNull(dossier.getReleaseDate())) {
//				try {
//					DossierLocalServiceUtil.updateReleaseDate(dossier.getGroupId(), dossier.getDossierId(), dossier.getReferenceUid(), now, context);
					dossier.setReleaseDate(now);
					bResult.put(DossierTerm.RELEASE_DATE, true);
					dossierAction = DossierActionLocalServiceUtil.updateState(dossierAction.getDossierActionId(), DossierActionTerm.STATE_ALREADY_PROCESSED);					
//				} catch (PortalException e) {
//					_log.error(e);
//					e.printStackTrace();
//				}				
			}
		}
		if (DossierTerm.DOSSIER_STATUS_DENIED.equals(curStatus)
				|| DossierTerm.DOSSIER_STATUS_UNRESOLVED.equals(curStatus)
				|| DossierTerm.DOSSIER_STATUS_CANCELLED.equals(curStatus)
				|| DossierTerm.DOSSIER_STATUS_DONE.equals(curStatus)) {
			if (Validator.isNull(dossier.getFinishDate())) {
//				try {
//					DossierLocalServiceUtil.updateFinishDate(dossier.getGroupId(), dossier.getDossierId(), dossier.getReferenceUid(), now, context);
					dossier.setFinishDate(now);
					bResult.put(DossierTerm.FINISH_DATE, true);
					dossierAction = DossierActionLocalServiceUtil.updateState(dossierAction.getDossierActionId(), DossierActionTerm.STATE_ALREADY_PROCESSED);					
//				} catch (PortalException e) {
//					_log.error(e);
//					e.printStackTrace();
//				}				
			}
		}
		
		if (DossierTerm.DOSSIER_STATUS_RECEIVING.equals(prevStatus)
				&& DossierTerm.DOSSIER_STATUS_PROCESSING.equals(curStatus)) {
			bResult.put(DossierTerm.DOSSIER_NO, true);
			bResult.put(DossierTerm.RECEIVE_DATE, true);
			bResult.put(DossierTerm.PROCESS_DATE, true);
			bResult.put(DossierTerm.RELEASE_DATE, true);
			bResult.put(DossierTerm.FINISH_DATE, true);
		}
		
		//Calculate step due date
//		DossierAction dossierAction = DossierActionLocalServiceUtil.fetchDossierAction(dossier.getDossierActionId());
//		_log.info("dossierAction: "+dossierAction);
		Date rootDate = now;
		Date dueDate = null;

//		if (prevAction != null) {
//			if (prevAction.getDueDate() != null) {
//				if (rootDate.getTime() < prevAction.getDueDate().getTime()) {
//					rootDate = prevAction.getDueDate();
//				}
//			}
//		}

		Double durationCount = processStep.getDurationCount();
		_log.info("durationCountStep: "+durationCount);
		int durationUnit = serviceProcess.getDurationUnit();
		
		_log.info("Calculate do action duration count: " + durationCount);
		if (Validator.isNotNull(durationCount) && durationCount > 0) {
			dueDate = HolidayUtils.getDueDate(rootDate, durationCount, durationUnit, dossier.getGroupId());
			_log.info("dueDateAction: "+dueDate);
		}		
	
		DossierLocalServiceUtil.updateDossier(dossier);
		
//		_log.info("Due date in do action: " + dueDate);
		if (dossierAction != null) {
			if (dueDate != null) {
				long dateNowTimeStamp = now.getTime();
				Long dueDateTimeStamp = dueDate.getTime();
				int overdue = 0;
//				_log.info("dueDateTEST: "+dueDate);
//				_log.info("Due date timestamp: " + dueDateTimeStamp);
				if (dueDateTimeStamp != null && dueDateTimeStamp > 0) {
					long subTimeStamp = dueDateTimeStamp - dateNowTimeStamp;
					if (subTimeStamp > 0) {
						overdue = calculatorOverDue(durationUnit, subTimeStamp);
//						overdue = calculatorOverDue(durationUnit, subTimeStamp, dateNowTimeStamp, dueDateTimeStamp,
//								dossierAction.getGroupId(), true);
					} else {
						overdue = -calculatorOverDue(durationUnit, subTimeStamp);
////						calculatorOverDue(int durationUnit, long subTimeStamp, long releaseDateTimeStamp,
////								long dueDateTimeStamp, long groupId, true);
//						overdue = -calculatorOverDue(durationUnit, subTimeStamp, dateNowTimeStamp, dueDateTimeStamp,
//								dossierAction.getGroupId(), true);
					}
				} else {
				}
				_log.info("dueDateTEST111: "+dueDate);
				dossierAction.setActionOverdue(overdue);
				dossierAction.setDueDate(dueDate);
				
				DossierAction dActTest = DossierActionLocalServiceUtil.updateDossierAction(dossierAction);
				_log.info("dActTest: "+dActTest);
			}
		}
	
		return bResult;
	}
	
	private static int calculatorOverDue(int durationUnit, long subTimeStamp) {
		if (subTimeStamp < 0) {
			subTimeStamp = Math.abs(subTimeStamp);
		}
		double dueCount;
		double overDue;
		int retval = Double.compare(durationUnit, 1.0);
		if (retval < 0) {
			dueCount = (double) subTimeStamp / VALUE_CONVERT_DATE_TIMESTAMP;
			double subDueCount = (double) Math.round(dueCount * 100) / 100;
			overDue = (double) Math.ceil(subDueCount * 4) / 4;
			return (int)overDue;
		} else {
			dueCount = (double) subTimeStamp / VALUE_CONVERT_HOUR_TIMESTAMP;
			overDue = (double) Math.round(dueCount);
		}

		return (int)overDue;
	}

//	private static int calculatorOverDue(int durationUnit, long subTimeStamp, long releaseDateTimeStamp,
//			long dueDateTimeStamp, long groupId, boolean flagStepDue) {
//
//		//Process count day off work
//		long dueCountLong = (long) subTimeStamp / VALUE_CONVERT_DATE_TIMESTAMP;
//		int dueCountInt = (int) dueCountLong;
//		double countDayHoliday = 0;
//		if (dueCountInt > 0) {
//			_log.info("START 1");
//			countDayHoliday = HolidayUtils.getCountDateByHoliday(dueDateTimeStamp, releaseDateTimeStamp,
//					dueCountInt, groupId);
//		} else if (dueCountInt < 0) {
//			_log.info("START 2");
//			countDayHoliday = HolidayUtils.getCountDateByHoliday(releaseDateTimeStamp, dueDateTimeStamp,
//					Math.abs(dueCountInt), groupId);
//		}
//		_log.info("countDayHoliday: "+countDayHoliday);
//
//		double countWork = 0;
//		if (flagStepDue) {
//			//Process balance
//			double balance = (double) (subTimeStamp % VALUE_CONVERT_DATE_TIMESTAMP) / VALUE_CONVERT_HOUR_TIMESTAMP;
//			if (balance > 1) {
//				int countHours = HolidayUtils.getCountHoursByHoliday(dueDateTimeStamp, releaseDateTimeStamp, groupId);
//				_log.info("countHours: "+countHours);
//				countWork += (double) countHours / 8;
//			} else if (balance < -1){
//				int countHours = HolidayUtils.getCountHoursByHoliday(releaseDateTimeStamp, dueDateTimeStamp, groupId);
//				_log.info("countHours: "+countHours);
//				countWork += (double) countHours / 8;
//			}
//		}
//		
//		_log.info("countDayHoliday: "+countDayHoliday);
//
//		if (subTimeStamp < 0) {
//			subTimeStamp = Math.abs(subTimeStamp);
//		}
//
//		String strOverDue;
//		double dueCount;
//		double overDue;
//		int retval = Double.compare(durationUnit, 1.0);
//		if (retval < 0) {
//			strOverDue = " ngày";
//			if (flagStepDue) {
//				dueCount = (int) (subTimeStamp / VALUE_CONVERT_DATE_TIMESTAMP);
//			} else {
//				dueCount = (double) subTimeStamp / VALUE_CONVERT_DATE_TIMESTAMP;
//			}
//			_log.info("dueCount: "+dueCount);
//			_log.info("countDayHoliday: "+countDayHoliday);
//			double dueCountReal = dueCount - countDayHoliday + countWork;
//			double subDueCount = (double) Math.round(dueCountReal * 100) / 100;
//			overDue = (double) Math.ceil(subDueCount * 4) / 4;
//			_log.info("overDue: "+overDue);
//			//TODO: Process a.0 = a
//			boolean flagCeil = false;
//			String strOverDueConvert = String.valueOf(overDue);
//			if (Validator.isNotNull(strOverDueConvert)) {
//				if (strOverDueConvert.contains(EXTEND_ONE_VALUE) || strOverDueConvert.contains(EXTEND_TWO_VALUE)) {
//					flagCeil = true;
//				}
//			}
//			if (!flagCeil) {
////				_log.info("flagCeil: "+flagCeil);
////				_log.info("overDue: "+overDue);
////				_log.info("strOverDue: "+strOverDue);
//				return overDue + strOverDue;
//			}
//		} else {
//			strOverDue = " giờ";
//			if (flagStepDue) {
//				dueCount = (int) (subTimeStamp / VALUE_CONVERT_HOUR_TIMESTAMP);
//			} else {
//				dueCount = (double) subTimeStamp / VALUE_CONVERT_HOUR_TIMESTAMP;
//			}
//			double dueCountReal = dueCount - countDayHoliday * 8 + countWork * 8;
//			overDue = (double) Math.round(dueCountReal);
//		}
//
//		_log.info("overDue: "+overDue);
//		_log.info("strOverDue: "+strOverDue);
//		return (int)overDue + strOverDue;
//	}
	
	@Override
	public DossierAction doAction(long groupId, long dossierId, String referenceUid, String actionCode,
			long processActionId, String actionUser, String actionNote, long assignUserId, long userId,
			String subUsers, ServiceContext context) throws PortalException {

		_log.info("STRART DO ACTION ==========:GroupID: "+groupId);
		context.setUserId(userId);

		DossierAction dossierAction = null;

		Dossier dossier = getDossier(groupId, dossierId, referenceUid);
		// _log.info("dossier: " + dossier);
		
//		String type = StringPool.BLANK;

//		if (dossier != null) {
//			String dossierStatus = dossier.getDossierStatus().toLowerCase();
//			if (Validator.isNotNull(dossierStatus) && !dossierStatus.equals("new")) {
//				String applicantNote = _buildDossierNote(dossier, actionNote, groupId, type);
//				_log.info("applicantNote: "+applicantNote);
//
//				dossier.setApplicantNote(applicantNote);
//
//				DossierLocalServiceUtil.updateDossier(dossier);
//			}
//		}

		if (Validator.isNull(dossier)) {
			throw new NotFoundException("DossierNotFoundException");
		}

		ProcessOption option = null;
		ActionConfig actionConfig = null;
		
		try {
			_log.info("dossier.getServiceCode(): " + dossier.getServiceCode());
			_log.info("dossier.getGovAgencyCode(): " + dossier.getGovAgencyCode());
			_log.info("dossier.getDossierTemplateNo(): " + dossier.getDossierTemplateNo());
			option = getProcessOption(dossier.getServiceCode(), dossier.getGovAgencyCode(),
					dossier.getDossierTemplateNo(), groupId);
		} catch (Exception e) {
			_log.debug(e);
			//_log.error(e);
			throw new NotFoundException("ServiceProcessNotFoundException");
		}

		long serviceProcessId = option.getServiceProcessId();
		_log.info("serviceProcessId: " + serviceProcessId);

		ServiceProcess serviceProcess = ServiceProcessLocalServiceUtil.fetchServiceProcess(serviceProcessId);

		ProcessAction processAction = null;

		if (processActionId != 0) {
			_log.info("PROCESS_ACTION");
			processAction = ProcessActionLocalServiceUtil.getProcessAction(processActionId);
			if (processAction != null) {
				_log.info("SYNC action: " + processAction.getSyncActionCode());
			}
		} else {
			_log.info("PROCESS_ACTION_BY_CODE");

			processAction = getProcessAction(groupId, dossierId, referenceUid, actionCode, serviceProcessId);
			if (processAction != null) {
				_log.info("processAction: " + processAction.getActionCode());
			}
		}

		// Add paymentFile
		if (processAction != null && Validator.isNotNull(processAction.getPaymentFee())) {
			try {
				DossierPaymentUtils.processPaymentFile(processAction, processAction.getPaymentFee(), groupId, dossierId, userId,
						context, serviceProcess.getServerNo());
			} catch (Exception e) {

				_log.debug(e);
				//_log.error(e);
				_log.info("Can not create PaymentFile with pattern \"" + processAction.getPaymentFee() + "\"");
			}

		}

//		boolean isSubmitType = isSubmitType(processAction);

		boolean hasDossierSync = false;

		if (processActionId != 0) {
			hasDossierSync = hasDossierSync(groupId, dossierId, referenceUid, processAction);
		}

		// TODO take a look later

//		boolean hasForedDossierSync = forcedDossierSync(groupId, dossierId, referenceUid, processAction, isSubmitType);
//
//		boolean isCreateDossier = hasCreateDossier(groupId, dossierId, referenceUid, actionCode, serviceProcessId,
//				hasDossierSync);

		// TODO Hard fix for test
		List<String> types = new ArrayList<>();
		types.add(OCPSUserUtils.APPLICANT_01);
		types.add(OCPSUserUtils.APPLICANT_02);
		types.add(OCPSUserUtils.EMPLOYEE_01);
		types.add(OCPSUserUtils.EMPLOYEE_02);

		String postStepCode = processAction.getPostStepCode();

		ProcessStep curStep = ProcessStepLocalServiceUtil.fetchBySC_GID(postStepCode, groupId, serviceProcessId);

		int actionOverdue = getActionDueDate(groupId, dossierId, referenceUid, processActionId);

		Date dueDate = getDueDate(groupId, dossierId, referenceUid, processActionId);

		String payload = buildPayload(groupId, dossierId, referenceUid, processActionId);
		actionConfig = ActionConfigLocalServiceUtil.getByCode(groupId, actionCode);
	
		DossierAction previousAction = null;
		if (dossier.getDossierActionId() != 0) {
			previousAction = DossierActionLocalServiceUtil.fetchDossierAction(dossier.getDossierActionId());
		}
		
		// In the special action (actionCode = 1100, save DOSSIER in SERVER)
		if (actionCode.contentEquals(SPECIAL_ACTION)
				&& (types.contains(OCPSUserUtils.APPLICANT_01) || types.contains(OCPSUserUtils.APPLICANT_02))) {
			// TODO check DossierStatus
			_log.info("DO_SPECIAL_ACTION");

			// Set dossierStatus is NEW
			JSONObject jsStatus = JSONFactoryUtil.createJSONObject();

			getDossierStatus(jsStatus, groupId, DOSSIER_SATUS_DC_CODE, DossierStatusConstants.NEW);

			if (curStep != null && actionConfig != null) {
				String fromStepCode = previousAction != null ? previousAction.getStepCode() : StringPool.BLANK;
				String fromStepName = previousAction != null ? previousAction.getStepName() : StringPool.BLANK;
				String fromSequenceNo = previousAction != null ? previousAction.getSequenceNo() : StringPool.BLANK;
				
				dossierAction = DossierActionLocalServiceUtil.updateDossierAction(groupId, 0, dossierId, serviceProcessId,
						0l, 
						fromStepCode, fromStepName, fromSequenceNo,
						actionCode, actionUser, processAction.getActionName(), actionNote, actionOverdue,
						processAction.getSyncActionCode(), false, processAction.getRollbackable(), curStep.getStepCode(),
						curStep.getStepName(), 
						curStep.getSequenceNo(),
						dueDate, 0l, payload, curStep.getStepInstruction(), 
						DossierActionTerm.STATE_ALREADY_PROCESSED, (actionConfig.getEventType() == ActionConfigTerm.EVENT_TYPE_NOT_SENT ? DossierActionTerm.EVENT_STATUS_NOT_CREATED : DossierActionTerm.EVENT_STATUS_WAIT_SENDING), 
						context);				
				if (actionConfig.getRollbackable()) {
					DossierActionLocalServiceUtil.updateRollbackable(dossierAction.getDossierActionId(), true);
				}
			}
			else {
				dossierAction = DossierActionLocalServiceUtil.updateDossierAction(groupId, 0, dossierId, serviceProcessId,
						0l, 
						StringPool.BLANK, StringPool.BLANK, StringPool.BLANK,
						actionCode, actionUser, processAction.getActionName(), actionNote, actionOverdue,
						processAction.getSyncActionCode(), false, processAction.getRollbackable(), curStep != null ? curStep.getStepCode() : StringPool.BLANK,
						curStep != null ? curStep.getStepName() : StringPool.BLANK, 
						curStep != null ? curStep.getSequenceNo() : StringPool.BLANK,
						dueDate, 0l, payload, curStep != null ? curStep.getStepInstruction() : StringPool.BLANK, 
						DossierActionTerm.STATE_ALREADY_PROCESSED, DossierActionTerm.EVENT_STATUS_NOT_CREATED, 						
						context);				
			}

			// Add DossierActionUser

			DossierActionUserImpl dossierActionUser = new DossierActionUserImpl();

			_log.debug("subUsers***" + subUsers);

			if (Validator.isNotNull(subUsers)) {
				JSONArray subUsersArray = JSONFactoryUtil.createJSONArray(subUsers);
				dossierActionUser.assignDossierActionUser(dossier, processAction.getAllowAssignUser(), dossierAction, userId, groupId,
						assignUserId, subUsersArray);
			} else {
				dossierActionUser.initDossierActionUser(processAction, dossier, processAction.getAllowAssignUser(), dossierAction, userId, groupId,
						assignUserId);
			}

			dossier = DossierLocalServiceUtil.updateStatus(groupId, dossierId, referenceUid, DossierStatusConstants.NEW,
					jsStatus.getString(DossierStatusConstants.NEW), StringPool.BLANK, StringPool.BLANK,
					StringPool.BLANK, dossierAction.getStepInstruction(), context);

		} else {

			_log.info("NEXT_ACTION");

			JSONObject jsStatus = JSONFactoryUtil.createJSONObject();

			JSONObject jsSubStatus = JSONFactoryUtil.createJSONObject();

			// String syncActionCode = processAction.getSyncActionCode();

			_log.info("curStep.getDossierStatus(): " + curStep.getDossierStatus());
			getDossierStatus(jsStatus, groupId, DOSSIER_SATUS_DC_CODE, curStep.getDossierStatus());

			// update reference dossier
			DossierAction prvAction = DossierActionLocalServiceUtil.getByNextActionId(dossierId, 0l);
			
			String fromStepCode = previousAction != null ? previousAction.getStepCode() : StringPool.BLANK;
			String fromStepName = previousAction != null ? previousAction.getStepName() : StringPool.BLANK;
			String fromSequenceNo = previousAction != null ? previousAction.getSequenceNo() : StringPool.BLANK;

			int state = DossierActionTerm.STATE_ALREADY_PROCESSED;
			int eventStatus = (actionConfig != null ? (actionConfig.getEventType() == ActionConfigTerm.EVENT_TYPE_NOT_SENT ? DossierActionTerm.EVENT_STATUS_NOT_CREATED : DossierActionTerm.EVENT_STATUS_WAIT_SENDING) : DossierActionTerm.EVENT_STATUS_NOT_CREATED);
			dossierAction = DossierActionLocalServiceUtil.updateDossierAction(groupId, 0, dossierId, serviceProcessId,
					0l, 
					fromStepCode, fromStepName, fromSequenceNo,
					actionCode, actionUser, processAction.getActionName(), actionNote, actionOverdue,
					processAction.getSyncActionCode(), hasDossierSync, processAction.getRollbackable(),
					curStep.getStepCode(), curStep.getStepName(), 
					curStep.getSequenceNo(),
					dueDate, 0l, payload, curStep.getStepInstruction(),
					state, eventStatus,
					context);

			if (actionConfig != null) {
				if (actionConfig.getRollbackable()) {
					DossierActionLocalServiceUtil.updateRollbackable(dossierAction.getDossierActionId(), true);
				}
			}
			else {
				if (processAction.isRollbackable()) {
					DossierActionLocalServiceUtil.updateRollbackable(dossierAction.getDossierActionId(), true);
				}
			}
			
			// Add DossierActionUser

			DossierActionUserImpl dossierActionUser = new DossierActionUserImpl();

			if (Validator.isNotNull(subUsers)) {
				_log.info("PROCESS subUsers != null");
				JSONArray subUsersArray = JSONFactoryUtil.createJSONArray(subUsers);
				dossierActionUser.assignDossierActionUser(dossier, processAction.getAllowAssignUser(), dossierAction, userId, groupId,
						assignUserId, subUsersArray);
			} else {
				_log.info("PROCESS subUsers == null");
				dossierActionUser.initDossierActionUser(processAction, dossier, processAction.getAllowAssignUser(), dossierAction, userId, groupId,
						assignUserId);
			}
			//_log.info("UPDATE DOSSIER STATUS************");
			//_log.info(curStep.getDossierStatus());
			//_log.info(curStep.getDossierSubStatus());
			//_log.info("*********************************");
			// Set dossierStatus by CUR_STEP
			// LamTV: Update lockState when Sync
			dossier = DossierLocalServiceUtil.updateStatus(groupId, dossierId, referenceUid, curStep.getDossierStatus(),
					jsStatus.getString(curStep.getDossierStatus()), curStep.getDossierSubStatus(),
					jsSubStatus.getString(curStep.getDossierSubStatus()), curStep.getLockState(),
					dossierAction.getStepInstruction(), context);
			
			//_log.info(jsStatus.toJSONString());
			//_log.info(jsSubStatus.toJSONString());

			//_log.info("dossier_" + dossier.getDossierStatus());

			//_log.info("*********************************");

			if (Validator.isNull(dossier.getDossierNo())
					&& (curStep.getDossierStatus().contentEquals(DossierStatusConstants.PAYING)
							|| (curStep.getDossierStatus().contentEquals(DossierStatusConstants.PROCESSING)))) {

				_log.info("PROCESS getDossierStatus == PAYING or PROCESSING");
				LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>();
				params.put(DossierTerm.GOV_AGENCY_CODE, dossier.getGovAgencyCode());
				params.put(DossierTerm.SERVICE_CODE, dossier.getServiceCode());
				params.put(DossierTerm.DOSSIER_TEMPLATE_NO, dossier.getDossierTemplateNo());
				params.put(DossierTerm.DOSSIER_STATUS, StringPool.BLANK);

				String dossierRef = DossierNumberGenerator.generateDossierNumber(groupId, dossier.getCompanyId(),
						dossierId, option.getProcessOptionId(), serviceProcess.getDossierNoPattern(), params);

				// Cap nhat ngay tiep nhan khi duoc cap so

				dossier.setReceiveDate(new Date());

				dossier.setDossierNo(dossierRef.trim());
				// To index
				DossierLocalServiceUtil.syncDossier(dossier);
			}

			// update nextActionId
			_log.info("prvAction:" + prvAction);
			if (Validator.isNotNull(prvAction)) {
				DossierActionLocalServiceUtil.updateNextActionId(prvAction.getDossierActionId(),
						dossierAction.getDossierActionId());
			}

			_log.info("hasDossierSync:" + hasDossierSync);
			if (hasDossierSync) {
				// SyncAction
//				int method = 0;
				_log.info("PROCESS update Dossier Sync:" + hasDossierSync);
//				DossierSyncLocalServiceUtil.updateDossierSync(groupId, userId, dossierId, dossier.getReferenceUid(),
//						isCreateDossier, method, dossierAction.getPrimaryKey(), StringPool.BLANK,
//						serviceProcess.getServerNo());

				// TODO add SYNC for DossierFile and PaymentFile here

				//TODO: process SyncDossierFile
				processSyncDossierFile(dossierId, dossier.getReferenceUid(), processAction, groupId, userId, serviceProcess.getServerNo());
				// SyncDossierFile
				//List<DossierFile> lsDossierFile = DossierFileLocalServiceUtil.getByDossierIdAndIsNew(dossierId, true);

				// check return file
				//List<String> returnDossierFileTemplateNos = ListUtil
				//		.toList(StringUtil.split(processAction.getReturnDossierFiles()));

				//_log.info("__return dossierFiles" + processAction.getReturnDossierFiles());

				//for (DossierFile dosserFile : lsDossierFile) {

				//	_log.info("&&&StartUpdateDossierFile" + new Date());

				//	dosserFile.setIsNew(false);

				//	DossierFileLocalServiceUtil.updateDossierFile(dosserFile);

				//	_log.info("&&&EndUpdateDossierFile" + new Date());

				//	_log.info("__dossierPart" + processAction.getReturnDossierFiles());
				//	_log.info("__dossierPart" + dosserFile.getFileTemplateNo());

				//	if (returnDossierFileTemplateNos.contains(dosserFile.getFileTemplateNo())) {
				//		_log.info("START SYNC DOSSIER FILE");
				//		DossierSyncLocalServiceUtil.updateDossierSync(groupId, userId, dossierId,
				//				dossier.getReferenceUid(), false, 1, dosserFile.getDossierFileId(),
				//				dosserFile.getReferenceUid(), serviceProcess.getServerNo());

				//	}

				//}

			}

			String preCondition = processAction.getPreCondition();
			String autoEvent = processAction.getAutoEvent();
			_log.info("preCondition: "+preCondition);

			//LamTV: Process case auto Event
			boolean flagEvent = false;
			if (Validator.isNotNull(autoEvent) && autoEvent.toLowerCase().contentEquals("timmer")) {
				flagEvent = true;
			}

			if (Validator.isNotNull(preCondition)) {
				// case reject_cancelling
				_log.info("REJECT_CANCELLING....");

				if (preCondition.toLowerCase().contentEquals("reject_cancelling")) {
					// flag-off
					_log.info("DO REJECT_CANCELLING....");

					Dossier sourceDossier = DossierLocalServiceUtil.getByRef(55217, dossier.getReferenceUid());
					_log.info("DO REJECT_CANCELLING.... FIND RESOURCE");

					sourceDossier.setCancellingDate(null);

					DossierLocalServiceUtil.updateDossier(sourceDossier);

					dossier.setCancellingDate(null);

					// To index
					DossierLocalServiceUtil.syncDossier(dossier);

					// add dossierLog

					// in CLIENT

					String refUid = PortalUUIDUtil.generate();
					int status = 2;

					DossierRequestUDLocalServiceUtil.updateDossierRequest(0, dossierId, refUid, "reject_cancelling",
							actionNote, 0, status, context);

					// in SERVER
					
					context.setScopeGroupId(sourceDossier.getGroupId());
					DossierRequestUDLocalServiceUtil.updateDossierRequest(0, sourceDossier.getDossierId(), refUid, "reject_cancelling",
							actionNote, 0, status, context);
					
					context.setScopeGroupId(dossier.getGroupId());
				}

				//LamTV: Update status when approved canceling
				if (preCondition.toLowerCase().contentEquals("cancelling")) {
					// flag-off
					_log.info("START CANCELLING....");

					// in CLIENT

					String refUid = PortalUUIDUtil.generate();
					int status = 1;

					DossierRequestUDLocalServiceUtil.updateDossierRequest(0, dossierId, refUid, "cancelling",
							actionNote, 0, status, context);

					// in SERVER

					Dossier sourceDossier = DossierLocalServiceUtil.getByRef(55217, dossier.getReferenceUid());
					if (sourceDossier != null) {
						context.setScopeGroupId(sourceDossier.getGroupId());
						DossierRequestUDLocalServiceUtil.updateDossierRequest(0, sourceDossier.getDossierId(), refUid, "cancelling",
								actionNote, 0, status, context);
					}

					context.setScopeGroupId(dossier.getGroupId());

				}

				_log.info("REJECT_SUBMIT....");
				if (preCondition.toLowerCase().contentEquals("reject_submitting")) {
					// flag-off
					_log.info("DO REJECT_SUBMIT....");

					Dossier sourceDossier = DossierLocalServiceUtil.getByRef(55217, dossier.getReferenceUid());
					_log.info("DO REJECT_SUBMIT.... FIND RESOURCE");

					sourceDossier.setEndorsementDate(null);

					DossierLocalServiceUtil.updateDossier(sourceDossier);

					dossier.setEndorsementDate(null);

					// To index
					DossierLocalServiceUtil.syncDossier(dossier);
					
					
					String refUid = PortalUUIDUtil.generate();
					int status = 2;

					DossierRequestUDLocalServiceUtil.updateDossierRequest(0, dossierId, refUid, "reject_submitting",
							actionNote, 0, status, context);

					// in SERVER
					
					context.setScopeGroupId(sourceDossier.getGroupId());
					DossierRequestUDLocalServiceUtil.updateDossierRequest(0, sourceDossier.getDossierId(), refUid, "reject_submitting",
							actionNote, 0, status, context);
					
					context.setScopeGroupId(dossier.getGroupId());

				}

				//LamTV: Update process approved endorsement
				if (preCondition.toLowerCase().contentEquals("submitting")) {
					if (flagEvent) {
						// flag-off
						_log.info("START APPROVED SUBMIT....");

						
						String refUid = PortalUUIDUtil.generate();
						int status = 3;

						DossierRequestUDLocalServiceUtil.updateDossierRequest(0, dossierId, refUid, "submitting",
								actionNote, 0, status, context);

						// in SERVER
						
						Dossier sourceDossier = DossierLocalServiceUtil.getByRef(55217, dossier.getReferenceUid());
						if (sourceDossier != null) {
							context.setScopeGroupId(sourceDossier.getGroupId());
							DossierRequestUDLocalServiceUtil.updateDossierRequest(0, sourceDossier.getDossierId(), refUid,
									"submitting", actionNote, 0, status, context);
						}

						context.setScopeGroupId(dossier.getGroupId());
					} else {
						// flag-off
						_log.info("START APPROVED SUBMIT....");

						
						String refUid = PortalUUIDUtil.generate();
						int status = 1;

						DossierRequestUDLocalServiceUtil.updateDossierRequest(0, dossierId, refUid, "submitting",
								actionNote, 0, status, context);

						// in SERVER
						
						Dossier sourceDossier = DossierLocalServiceUtil.getByRef(55217, dossier.getReferenceUid());
						if (sourceDossier != null) {
							context.setScopeGroupId(sourceDossier.getGroupId());
							DossierRequestUDLocalServiceUtil.updateDossierRequest(0, sourceDossier.getDossierId(), refUid,
									"submitting", actionNote, 0, status, context);
						}

						context.setScopeGroupId(dossier.getGroupId());
					}

				}

				_log.info("REJECT_CORRECTING....");
				if (preCondition.toLowerCase().contentEquals("reject_correcting")) {
					// flag-off
					_log.info("DO REJECT_CORRECTING....");

					Dossier sourceDossier = DossierLocalServiceUtil.getByRef(55217, dossier.getReferenceUid());
					_log.info("DO REJECT_CORRECTING.... FIND RESOURCE");

					sourceDossier.setCorrecttingDate(null);

					DossierLocalServiceUtil.updateDossier(sourceDossier);

					dossier.setCorrecttingDate(null);

					// To index
					DossierLocalServiceUtil.syncDossier(dossier);

					String refUid = PortalUUIDUtil.generate();
					int status = 2;

					DossierRequestUDLocalServiceUtil.updateDossierRequest(0, dossierId, refUid, "reject_correcting",
							actionNote, 0, status, context);

					// in SERVER

					context.setScopeGroupId(sourceDossier.getGroupId());
					DossierRequestUDLocalServiceUtil.updateDossierRequest(0, sourceDossier.getDossierId(), refUid, "reject_correcting",
							actionNote, 0, status, context);
					
					context.setScopeGroupId(dossier.getGroupId());

				}

				//LamTV: Update process approved correcting
				if (preCondition.toLowerCase().contentEquals("correcting")) {
					if (flagEvent) {
						// flag-off
						_log.info("START APPROVED CORRECTING....");

						String refUid = PortalUUIDUtil.generate();
						int status = 3;

						// IN CLIENT
						DossierRequestUDLocalServiceUtil.updateDossierRequest(0, dossierId, refUid, "correcting",
								actionNote, 0, status, context);

						// IN SERVER
						Dossier sourceDossier = DossierLocalServiceUtil.getByRef(55217, dossier.getReferenceUid());
						if (sourceDossier != null) {
							context.setScopeGroupId(sourceDossier.getGroupId());
							DossierRequestUDLocalServiceUtil.updateDossierRequest(0, sourceDossier.getDossierId(), refUid,
									"correcting", actionNote, 0, status, context);
						}
						context.setScopeGroupId(dossier.getGroupId());
					}else {
						// flag-off
						_log.info("START APPROVED CORRECTING....");

						String refUid = PortalUUIDUtil.generate();
						int status = 1;

						// IN CLIENT
						DossierRequestUDLocalServiceUtil.updateDossierRequest(0, dossierId, refUid, "correcting",
								actionNote, 0, status, context);

						// IN SERVER
						Dossier sourceDossier = DossierLocalServiceUtil.getByRef(55217, dossier.getReferenceUid());
						if (sourceDossier != null) {
							context.setScopeGroupId(sourceDossier.getGroupId());
							DossierRequestUDLocalServiceUtil.updateDossierRequest(0, sourceDossier.getDossierId(), refUid,
									"correcting", actionNote, 0, status, context);
						}
						context.setScopeGroupId(dossier.getGroupId());
					}
				}

			}

			// Add PaymentSync

//			PaymentFile paymentFile = PaymentFileLocalServiceUtil.getByDossierId(groupId, dossierId);
//			List<PaymentFile> syncPaymentFiles = new ArrayList<PaymentFile>();

//			for (PaymentFile pf : paymentFiles) {
//				if (pf.getIsNew()) {
//					syncPaymentFiles.add(pf);
//				}
//			}

//			for (PaymentFile spf : syncPaymentFiles) {
				// Hard-code
//				_log.info("PROCESS PaymentFile START");
//				if (groupId != 55217) {
//					DossierSyncLocalServiceUtil.updateDossierSync(groupId, userId, dossierId, dossier.getReferenceUid(),
//							false, 3, spf.getPrimaryKey(), spf.getReferenceUid(), serviceProcess.getServerNo());
//				}
//
//			}
		}

		ProcessStep postStep = ProcessStepLocalServiceUtil.fetchBySC_GID(postStepCode, groupId, serviceProcessId);

		String dossierBriefNote = DossierContentGenerator.getBriefNote(groupId, dossierId, postStep != null ? postStep.getBriefNote() : StringPool.BLANK);

		if (Validator.isNotNull(dossierBriefNote)) {
			DossierLocalServiceUtil.updateDossierBriefNote(dossierId, dossierBriefNote);
		}

		// do plugin auto

		// 1. get current Step
		// 2. get all plugins of this step
		// 3. get plugin has autoRun
		// 4. Create update formData

//		_log.info("IN_CURRENT_STEP:" + curStep.getStepCode() + curStep.getStepName());

		List<ProcessPlugin> plugins = ProcessPluginLocalServiceUtil.getProcessPlugins(serviceProcessId,
				curStep != null ? curStep.getStepCode() : StringPool.BLANK);

//		_log.info("WE_HAVE_PLUGINS:" + plugins.size());

		List<ProcessPlugin> autoPlugins = new ArrayList<ProcessPlugin>();

		for (ProcessPlugin plg : plugins) {
			if (plg.getAutoRun()) {
				autoPlugins.add(plg);
			}
		}

		_log.info("AND_HAVE_AUTO_RUN_PLUGINS:" + autoPlugins.size());

		for (ProcessPlugin plg : autoPlugins) {
			// do create file
			String fileTemplateNo = plg.getSampleData();

			fileTemplateNo = StringUtil.replaceFirst(fileTemplateNo, "#", StringPool.BLANK);

			_doAutoRun(groupId, fileTemplateNo, dossierId, dossier.getDossierTemplateNo(), context);
		}

		_log.info("END DO ACTION ==========");
		return dossierAction;
	}

	private void _doAutoRun(long groupId, String fileTemplateNo, long dossierId, String dossierTemplateNo,
			ServiceContext context) {

		String formData = StringPool.BLANK;

		fileTemplateNo = StringUtil.replaceFirst(fileTemplateNo, "#", StringPool.BLANK);

		try {
			// Dossier dossier = DossierLocalServiceUtil.getDossier(dossierId);

			DossierFile dossierFile = DossierFileLocalServiceUtil.getDossierFileByDID_FTNO_First(dossierId,
					fileTemplateNo, false, new DossierFileComparator(false, "createDate", Date.class));

			DossierPart dossierPart = DossierPartLocalServiceUtil.getByFileTemplateNo(groupId, fileTemplateNo);

			formData = AutoFillFormData.sampleDataBinding(dossierPart.getSampleData(), dossierId, context);

			if (Validator.isNull(dossierFile)) {

				DossierFileActions actions = new DossierFileActionsImpl();

				dossierFile = actions.addDossierFile(groupId, dossierId, PortalUUIDUtil.generate(), dossierTemplateNo,
						dossierPart.getPartNo(), fileTemplateNo, dossierPart.getPartName(), StringPool.BLANK, 0L, null,
						StringPool.BLANK, String.valueOf(false), context);
			}

			DossierFileActions actions = new DossierFileActionsImpl();

			actions.updateDossierFileFormData(groupId, dossierId, dossierFile.getReferenceUid(), formData, context);

		} catch (Exception e) {
			_log.debug(e);
			//_log.error(e);
			_log.info("Cant get formdata with fileTemplateNo_" + fileTemplateNo);
		}

	}

	@Override
	public Dossier markerVisited(long groupId, long dossierId, String referenceUid) throws PortalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DossierAction doRollback(long groupId, long dossierId, String referenceUid, long userId)
			throws PortalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONObject getContacts(long groupId, long dossierId, String referenceUid) throws PortalException {
		// TODO Auto-generated method stub
		JSONObject result = JSONFactoryUtil.createJSONObject();
		// get DossierAction
		DossierAction dossierAction = DossierActionLocalServiceUtil.getByNextActionId(dossierId, 0);
		long dossierActionId = dossierAction.getDossierActionId();
		List<DossierActionUser> listUser = DossierActionUserLocalServiceUtil.getListUser(dossierActionId);
		result.put("listUser", listUser);
		return result;
	}

	protected long getNextActionId(long groupId, long dossierId, String refId, ProcessAction currentAction) {
		// TODO get the recently DOSSIER_ACTION, set nextActionId for the
		// recently DOSSIER_ACTION by currentActionId

		return 0l;
	}

	protected JSONObject getDossierStatus(JSONObject ob, long groupId, String collectionCode, String itemCode) {

		DictCollection dc = DictCollectionLocalServiceUtil.fetchByF_dictCollectionCode(collectionCode, groupId);

		if (Validator.isNotNull(dc)) {
			DictItem it = DictItemLocalServiceUtil.fetchByF_dictItemCode(itemCode, dc.getPrimaryKey(), groupId);

			if (Validator.isNotNull(it)) {
				ob.put(itemCode, it.getItemName());
			}
		}
		
		return null;
	}

	//LamTV_Process get dossierStatus and dossierSubStatus text
	private JSONObject getStatusText(long groupId, String collectionCode, String curStatus, String curSubStatus) {

		JSONObject jsonData = null;
		DictCollection dc = DictCollectionLocalServiceUtil.fetchByF_dictCollectionCode(collectionCode, groupId);

		if (Validator.isNotNull(dc) && Validator.isNotNull(curStatus)) {
			jsonData = JSONFactoryUtil.createJSONObject();
			DictItem it = DictItemLocalServiceUtil.fetchByF_dictItemCode(curStatus, dc.getPrimaryKey(), groupId);
			if (Validator.isNotNull(it)) {
				jsonData.put(curStatus, it.getItemName());
				if (Validator.isNotNull(curSubStatus)) {
					DictItem dItem = DictItemLocalServiceUtil.fetchByF_dictItemCode(curSubStatus, dc.getPrimaryKey(),
							groupId);
					if (Validator.isNotNull(dItem)) {
						jsonData.put(curSubStatus, dItem.getItemName());
					}
				}
			}
		}

		return jsonData;
	}

	protected boolean isSubmitType(ProcessAction processAction) {

		// TODO add more logics here
		if (processAction.getAutoEvent().contentEquals(AUTO_EVENT_SUBMIT)) {
			return true;
		} else {
			return false;
		}

	}

	protected Date getDueDate(long groupId, long dossierId, String refId, long processActionId) {
		// TODO add implement here

		return new Date();
	}

	protected String buildPayload(long groupId, long dossierId, String refId, long processActionId) {
		// TODO add implement here

		return StringPool.BLANK;
	}

	protected int getActionDueDate(long groupId, long dossierId, String refId, long processActionId) {
		// TODO add implement here

		return 0;
	}

	protected ProcessOption getProcessOption(String serviceInfoCode, String govAgencyCode, String dossierTemplateNo,
			long groupId) throws PortalException {

		ServiceConfig config = ServiceConfigLocalServiceUtil.getBySICodeAndGAC(groupId, serviceInfoCode, govAgencyCode);

		return ProcessOptionLocalServiceUtil.getByDTPLNoAndServiceCF(groupId, dossierTemplateNo,
				config.getServiceConfigId());
	}

	protected Dossier getDossier(long groupId, long dossierId, String refId) throws PortalException {

		Dossier dossier = null;

		if (dossierId != 0) {
			dossier = DossierLocalServiceUtil.fetchDossier(dossierId);
		} else {
			dossier = DossierLocalServiceUtil.getByRef(groupId, refId);
		}

		return dossier;
	}

	protected boolean hasCreateDossier(long groupId, long dossierId, String refId, String actionCode,
			long serviceProcessId, boolean hasDossierSync) throws PortalException {

		boolean isCreate = false;

		Dossier dossier = getDossier(groupId, dossierId, refId);

		// ProcessAction action = getProcessAction(groupId, dossierId, refId,
		// actionCode, serviceProcessId);

		// String postStepCode = action.getPostStepCode();

		// ProcessStep step =
		// ProcessStepLocalServiceUtil.fetchBySC_GID(postStepCode, groupId,
		// serviceProcessId);

		// DOSSIER has nextAction form NEW to PROCESSING that it was created on
		// client and it wasn't created ONLINE
		if (hasDossierSync && dossier.getDossierStatus().contentEquals(DossierStatusConstants.NEW)
				&& !dossier.isOnline()) {
			isCreate = true;
		}

		return isCreate;
	}

	protected boolean hasDossierSync(long groupId, long dossierId, String refId, ProcessAction action)
			throws PortalException {

		Dossier dossier = getDossier(groupId, dossierId, refId);

		// TODO add more logic here
		boolean isSync = false;

		if (dossier.getOnline() && action.getSyncActionCode().length() != 0) {
			isSync = true;
		}

		//Hot fix
//		if (action.getSyncActionCode().length() != 0) {
//			isSync = true;
//		}

		//Hot fix
		//if (action.getSyncActionCode().length() != 0) {
		//	isSync = true;
		//}

		_log.info("GROUPID_" + groupId);
		_log.info("ISSYNC_" + isSync);

		return isSync;

	}

	protected boolean forcedDossierSync(long groupId, long dossierId, String refId, ProcessAction action,
			boolean isSubmit) throws PortalException {

		Dossier dossier = getDossier(groupId, dossierId, refId);

		// TODO add more logic here
		boolean isSync = false;

		if (!isSubmit && dossier.getOnline() && (action.getSyncActionCode().length() != 0
				&& action.getSyncActionCode().contains(StringPool.EXCLAMATION))) {
			isSync = true;
		}

		return isSync;

	}

	protected ProcessAction getProcessAction(long groupId, long dossierId, String refId, String actionCode,
			long serviceProcessId) throws PortalException {

		ProcessAction action = null;

		try {
			List<ProcessAction> actions = ProcessActionLocalServiceUtil.getByActionCode(groupId, actionCode,
					serviceProcessId);
			Dossier dossier = getDossier(groupId, dossierId, refId);

			String dossierStatus = dossier.getDossierStatus();

			String dossierSubStatus = Validator.isNull(dossier.getDossierSubStatus()) ? StringPool.BLANK
					: dossier.getDossierSubStatus();

			for (ProcessAction act : actions) {

				String preStepCode = act.getPreStepCode();

				ProcessStep step = ProcessStepLocalServiceUtil.fetchBySC_GID(preStepCode, groupId, serviceProcessId);

				String subStepStatus = StringPool.BLANK;

				if (Validator.isNotNull(step)) {
					subStepStatus = Validator.isNull(step.getDossierSubStatus()) ? StringPool.BLANK
							: step.getDossierSubStatus();
				}

				if (Validator.isNull(step)) {
					action = act;
					break;
				} else {

					if (step.getDossierStatus().contentEquals(dossierStatus)
							&& subStepStatus.contentEquals(dossierSubStatus)) {

						action = act;
						break;
					}
				}
			}

		} catch (Exception e) {

			_log.debug(e);
			//_log.error(e);

			throw new NotFoundException("ProcessActionNotFoundException with actionCode= " + actionCode
					+ "|serviceProcessId= " + serviceProcessId + "|referenceUid= " + refId + "|groupId= " + groupId);
		}

		return action;
	}

	@Override
	public Dossier initDossier(long groupId, long dossierId, String referenceUid, int counter, String serviceCode,
			String serviceName, String govAgencyCode, String govAgencyName, String applicantName,
			String applicantIdType, String applicantIdNo, String applicantIdDate, String address, String cityCode,
			String cityName, String districtCode, String districtName, String wardCode, String wardName,
			String contactName, String contactTelNo, String contactEmail, String dossierTemplateNo, String password,
			int viaPostal, String postalAddress, String postalCityCode, String postalCityName, String postalTelNo,
			boolean online, boolean notification, String applicantNote, int originality, ServiceContext context) throws PortalException {

		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
		Date appIdDate = null;

		try {
			appIdDate = sdf.parse(applicantIdDate);

		} catch (Exception e) {
			// TODO: handle exception
			_log.debug(e);
			//_log.error(e);
		}

		Dossier dossier = null;

		try {

			//Process
			dossier = DossierLocalServiceUtil.initDossier(groupId, dossierId, referenceUid, counter, serviceCode,
					serviceName, govAgencyCode, govAgencyName, applicantName, applicantIdType, applicantIdNo, appIdDate,
					address, cityCode, cityName, districtCode, districtName, wardCode, wardName, contactName,
					contactTelNo, contactEmail, dossierTemplateNo, password, viaPostal, postalAddress, postalCityCode,
					postalCityName, postalTelNo, online, notification, applicantNote, originality, context);

		} catch (Exception e) {
			_log.error(e);
		}

		return dossier;
	}

	@Override
	public Dossier assignDossierToProcess(long dossierId, String dossierNote, String submissionNote, String briefNote,
			String dossierNo, long folderId, long dossierActionId, String serverNo, ServiceContext context)
			throws PortalException {

		return DossierLocalServiceUtil.assignToProcess(dossierId, dossierNote, submissionNote, briefNote, dossierNo,
				folderId, dossierActionId, serverNo, context);
	}

	protected DossierAction getDossierAction(long dossierId, String actionCode) {

		DossierAction dossierAction = DossierActionLocalServiceUtil.getDossierActionbyDossierIdandActionCode(dossierId,
				actionCode);

		return dossierAction;
	}

	protected List<ProcessAction> getListProcessAtion(long serviceProcessId) throws PortalException {

		List<ProcessAction> lstprocessAction = ProcessActionLocalServiceUtil
				.getProcessActionbyServiceProcessId(serviceProcessId);

		return lstprocessAction;

	}

	protected List<ProcessStep> getListProcessStep(long serviceProcessId) throws PortalException {

		List<ProcessStep> lstprocessStep = ProcessStepLocalServiceUtil
				.getProcessStepbyServiceProcessId(serviceProcessId);

		return lstprocessStep;

	}

	protected static Log _log = LogFactoryUtil.getLog(DossierActionsImpl.class);

	@Override
	public Dossier cloneDossier(long groupId, long dossierId, ServiceContext context) throws PortalException {

		Dossier srcDossier = DossierLocalServiceUtil.fetchDossier(dossierId);

		Dossier desDossier = DossierLocalServiceUtil.cloneDossier(srcDossier);

		DossierFileLocalServiceUtil.cloneDossierFilesByDossierId(groupId, srcDossier.getPrimaryKey(), dossierId, 1,
				context);

		return desDossier;
	}

	@Override
	public JSONObject getDossierTodo(long userId, long companyId, long groupId, LinkedHashMap<String, Object> params,
			Sort[] sorts, ServiceContext serviceContext) {

		JSONObject result = JSONFactoryUtil.createJSONObject();

		SearchContext searchContext = new SearchContext();

		searchContext.setCompanyId(companyId);

		String statusCode = StringPool.BLANK;

		String subStatusCode = StringPool.BLANK;

		JSONArray statistics = JSONFactoryUtil.createJSONArray();

		long total = 0;

		try {
			DictCollection dictCollection = DictCollectionLocalServiceUtil.fetchByF_dictCollectionCode("DOSSIER_STATUS",
					groupId);
			statusCode = GetterUtil.getString(params.get(DossierTerm.STATUS));

			subStatusCode = GetterUtil.getString(params.get(DossierTerm.SUBSTATUS));

			if (Validator.isNotNull(statusCode) || Validator.isNotNull(subStatusCode)) {
				DictItem dictItem = null;
				if (Validator.isNotNull(statusCode)) {
					dictItem = DictItemLocalServiceUtil.fetchByF_dictItemCode(statusCode,
							dictCollection.getDictCollectionId(), groupId);
				} else {
					dictItem = DictItemLocalServiceUtil.fetchByF_dictItemCode(subStatusCode,
							dictCollection.getDictCollectionId(), groupId);
				}

				if (dictItem != null) {
					long count = DossierLocalServiceUtil.countLucene(params, searchContext);

					JSONObject statistic = JSONFactoryUtil.createJSONObject();
					statistic.put("dossierStatus", statusCode);
					statistic.put("dossierSubStatus", subStatusCode);
					statistic.put("level", dictItem.getLevel());
					statistic.put("statusName", dictItem.getItemName());
					statistic.put("count", count);

					statistics.put(statistic);

					total = count;
				}

			} else {

				// DictItem: treeIndex chua luu dung nen chua dung duoc ham nay
				/*
				 * List<DictItem> dictItems =
				 * DictItemLocalServiceUtil.findByF_dictCollectionId(
				 * dictCollection.getDictCollectionId(), QueryUtil.ALL_POS,
				 * QueryUtil.ALL_POS, new DictItemComparator(false, "treeIndex",
				 * String.class));
				 * 
				 * for (DictItem dictItem : dictItems) { statusCode =
				 * dictItem.getLevel() == 0 ? dictItem.getItemCode() :
				 * StringPool.BLANK; subStatusCode = dictItem.getLevel() == 1 ?
				 * dictItem.getItemCode() : StringPool.BLANK;
				 * params.put(DossierTerm.DOSSIER_STATUS, statusCode);
				 * params.put(DossierTerm.DOSSIER_SUB_STATUS, subStatusCode);
				 * long count = DossierLocalServiceUtil.countLucene(params,
				 * searchContext);
				 * 
				 * JSONObject statistic = JSONFactoryUtil.createJSONObject();
				 * 
				 * statistic.put("dossierStatus", statusCode);
				 * statistic.put("dossierSubStatus", subStatusCode);
				 * statistic.put("level", dictItem.getLevel());
				 * statistic.put("statusName", dictItem.getItemName());
				 * statistic.put("count", count); total += count;
				 * statistics.put(statistic); }
				 */

				/*
				 * List<DictItem> dictItems = DictItemLocalServiceUtil
				 * .findByF_dictCollectionId_parentItemId(dictCollection.
				 * getDictCollectionId(), 0);
				 * 
				 * 
				 * for (DictItem dictItem : dictItems) {
				 * 
				 * statusCode = dictItem.getItemCode(); subStatusCode =
				 * StringPool.BLANK;
				 * 
				 * params.put(DossierTerm.STATUS, statusCode);
				 * params.put(DossierTerm.SUBSTATUS, subStatusCode);
				 * 
				 * long count = DossierLocalServiceUtil.countLucene(params,
				 * searchContext);
				 * 
				 * JSONObject statistic = JSONFactoryUtil.createJSONObject();
				 * 
				 * statistic.put("dossierStatus", statusCode);
				 * statistic.put("dossierSubStatus", subStatusCode);
				 * statistic.put("level", dictItem.getLevel());
				 * statistic.put("statusName", dictItem.getItemName());
				 * statistic.put("count", count); total += count;
				 * statistics.put(statistic);
				 * 
				 * List<DictItem> childDictItems = DictItemLocalServiceUtil.
				 * findByF_dictCollectionId_parentItemId(
				 * dictCollection.getDictCollectionId(),
				 * dictItem.getDictItemId());
				 * 
				 * if (childDictItems != null) { for (DictItem childDictItem :
				 * childDictItems) {
				 * 
				 * subStatusCode = childDictItem.getItemCode();
				 * 
				 * statusCode = StringPool.BLANK;
				 * 
				 * params.put(DossierTerm.STATUS, statusCode);
				 * 
				 * params.put(DossierTerm.SUBSTATUS, subStatusCode);
				 * 
				 * long childCount = DossierLocalServiceUtil.countLucene(params,
				 * searchContext);
				 * 
				 * JSONObject childStatistic =
				 * JSONFactoryUtil.createJSONObject();
				 * 
				 * childStatistic.put("dossierStatus", dictItem.getItemCode());
				 * childStatistic.put("dossierSubStatus", subStatusCode);
				 * childStatistic.put("level", childDictItem.getLevel());
				 * childStatistic.put("statusName",
				 * childDictItem.getItemName()); childStatistic.put("count",
				 * childCount);
				 * 
				 * // Khong tinh tong so cua con do da tinh tong cua // cha //
				 * total += childCount; statistics.put(childStatistic); } } }
				 */

				List<DictItem> dictItems = DictItemLocalServiceUtil
						.findByF_dictCollectionId(dictCollection.getDictCollectionId());

				for (DictItem dictItem : dictItems) {

//					statusCode = StringPool.BLANK;
					subStatusCode = StringPool.BLANK;

					if (dictItem.getParentItemId() != 0) {
						subStatusCode = dictItem.getItemCode();
						DictItem parentDictItem = DictItemLocalServiceUtil.getDictItem(dictItem.getParentItemId());
						statusCode = parentDictItem.getItemCode();
					} else {
						statusCode = dictItem.getItemCode();
					}
					params.put(DossierTerm.STATUS, statusCode);
					params.put(DossierTerm.SUBSTATUS, subStatusCode);

					long count = DossierLocalServiceUtil.countLucene(params, searchContext);

					JSONObject statistic = JSONFactoryUtil.createJSONObject();

					statistic.put("dossierStatus", statusCode);
					statistic.put("dossierSubStatus", subStatusCode);
					statistic.put("level", dictItem.getLevel());
					statistic.put("statusName", dictItem.getItemName());
					statistic.put("count", count);
					if (dictItem.getParentItemId() == 0) {
						total += count;
					}

					statistics.put(statistic);
				}
			}

			result.put("data", statistics);

			result.put("total", total);

		} catch (Exception e) {
			_log.error(e);
		}

		return result;
	}

private String _buildDossierNote(Dossier dossier, String actionNote, long groupId, String type) {

		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		String defaultTimezone = TimeZone.getDefault().getID();
		sdf.setTimeZone(TimeZone.getTimeZone(defaultTimezone));
		Date date = new Date();

		StringBuilder sb = new StringBuilder();

		String oldNote = dossier.getApplicantNote();
		_log.info("oldNote: "+oldNote);
		_log.info("actionNote: "+actionNote);

		if (Validator.isNotNull(oldNote) && oldNote.contains("<br>")) {
			if (Validator.isNotNull(actionNote)) {
				if (groupId != 55217) {
					sb.append("<br>");
					sb.append("[" + sdf.format(date) + "]");
					sb.append(": ");
					sb.append(actionNote);
					sb.append(oldNote);
				} else {
					sb.append("<br>");
					sb.append("[" + sdf.format(date) + "]");
					sb.append(": ");
					sb.append(actionNote);
				}
			} else {
				if (groupId != 55217) {
					sb.append(oldNote);
				}
			}
		} else if (Validator.isNotNull(actionNote)) {
			sb.append("<br>");
			sb.append("[" + sdf.format(date) + "]");
			sb.append(": ");
			sb.append(actionNote);
		}

		return sb.toString();

	}

	private boolean checkPermission(String status, String subStatus, long groupId, long userId) {
		boolean isPermission = false;

		List<ProcessStep> processSteps;

		processSteps = ProcessStepLocalServiceUtil.getByStatusAnsSubStatus(status, subStatus, groupId);

		List<Role> roles = new ArrayList<Role>();

		for (ProcessStep step : processSteps) {
			List<ProcessStepRole> processStepRoles;

			processStepRoles = ProcessStepRoleLocalServiceUtil.findByP_S_ID(step.getPrimaryKey());

			for (ProcessStepRole stepRole : processStepRoles) {
				Role role = null;

				try {
					role = RoleLocalServiceUtil.getRole(stepRole.getRoleId());

					roles.add(role);
				} catch (Exception e) {
					// TODO: handle exception
					_log.info(e);
				}
			}
		}
		// List<User> users = new ArrayList<>();

		for (Role role : roles) {

			long[] elmUsers = RoleLocalServiceUtil.getUserPrimaryKeys(role.getRoleId());

			for (long elmUserId : elmUsers) {
				if (elmUserId == userId) {
					isPermission = true;

					break;
				}
			}
		}

		return isPermission;
	}

	@Override
	public JSONObject getDossierTodoPermission(long userId, long companyId, long groupId,
			LinkedHashMap<String, Object> params, Sort[] sorts, ServiceContext serviceContext) {

		JSONObject result = JSONFactoryUtil.createJSONObject();

		SearchContext searchContext = new SearchContext();

		searchContext.setCompanyId(companyId);

		String statusCode = StringPool.BLANK;

		String subStatusCode = StringPool.BLANK;

		JSONArray statistics = JSONFactoryUtil.createJSONArray();

		long total = 0;

		try {
			DictCollection dictCollection = DictCollectionLocalServiceUtil.fetchByF_dictCollectionCode("DOSSIER_STATUS",
					groupId);
			statusCode = GetterUtil.getString(params.get(DossierTerm.STATUS));

			subStatusCode = GetterUtil.getString(params.get(DossierTerm.SUBSTATUS));

			if (Validator.isNotNull(statusCode) || Validator.isNotNull(subStatusCode)) {
				DictItem dictItem = null;
				if (Validator.isNotNull(statusCode)) {
					dictItem = DictItemLocalServiceUtil.fetchByF_dictItemCode(statusCode,
							dictCollection.getDictCollectionId(), groupId);
				} else {
					dictItem = DictItemLocalServiceUtil.fetchByF_dictItemCode(subStatusCode,
							dictCollection.getDictCollectionId(), groupId);
				}

				if (dictItem != null) {
					long count = DossierLocalServiceUtil.countLucene(params, searchContext);

					JSONObject statistic = JSONFactoryUtil.createJSONObject();
					statistic.put("dossierStatus", statusCode);
					statistic.put("dossierSubStatus", subStatusCode);
					statistic.put("level", dictItem.getLevel());
					statistic.put("statusName", dictItem.getItemName());
					statistic.put("count", count);

					statistics.put(statistic);

					total = count;
				}

			} else {
				List<DictItem> dictItems = DictItemLocalServiceUtil
						.findByF_dictCollectionId(dictCollection.getDictCollectionId());

				// Get list dossierActionId
//				List<DossierActionUser> dauList = DossierActionUserLocalServiceUtil.getListUserByUserId(userId);
//				long dossierActionId = 0;
//				StringBuilder sb = null;
//				if (dauList != null && dauList.size() > 0) {
//					sb = new StringBuilder();
//					int length = dauList.size();
//					DossierActionUser dau = null;
//					for (int i = 0; i < length; i++) {
//						dau = dauList.get(i);
//						dossierActionId = dau.getDossierActionId();
//						// StringBuilder sb = new StringBuilder();
//						if (dossierActionId > 0) {
//
//							if (i == 0) {
//								sb.append(dossierActionId);
//							} else {
//								sb.append(StringPool.COMMA);
//								sb.append(dossierActionId);
//							}
//						}
//					}
//				}

				for (DictItem dictItem : dictItems) {
					String metaData = dictItem.getMetaData();
					String specialStatus = StringPool.BLANK;
					if (Validator.isNotNull(metaData)) {
						// _log.info("metaData: " +metaData);
						try {
							JSONObject metaJson = JSONFactoryUtil.createJSONObject(metaData);
							specialStatus = metaJson.getString("specialStatus");
							// _log.info("specialStatus: " +specialStatus);

						} catch (Exception e) {
							// TODO: handle exception
							_log.info(e);
						}
					}

//					statusCode = StringPool.BLANK;
					subStatusCode = StringPool.BLANK;
					// Get info status of dossier
					if (dictItem.getParentItemId() != 0) {
						subStatusCode = dictItem.getItemCode();
						// _log.info("subStatusCode: " +subStatusCode);
						DictItem parentDictItem = DictItemLocalServiceUtil.getDictItem(dictItem.getParentItemId());
						statusCode = parentDictItem.getItemCode();
						// _log.info("statusCode: " +statusCode);
					} else {
						statusCode = dictItem.getItemCode();
						// _log.info("statusCode: " +statusCode);
					}
					// Check permission user login
					boolean isPermission = checkPermission(statusCode, subStatusCode, groupId, userId);

					if (isPermission) {
						// _log.info("isPermission: " +isPermission);
						// _log.info("userId: " +userId);
						// _log.info("strdossierActionId: " +sb.toString());

						JSONObject statistic = JSONFactoryUtil.createJSONObject();

						if (Validator.isNotNull(specialStatus) && Boolean.parseBoolean(specialStatus)) {
							// Add params
							params.put(DossierTerm.STATUS, statusCode);
							params.put(DossierTerm.SUBSTATUS, subStatusCode);
//							params.put(DossierTerm.DOSSIER_ACTION_ID, sb.toString());
							params.put(DossierTerm.FOLLOW, String.valueOf(true));

							long count = DossierLocalServiceUtil.countLucene(params, searchContext);
							// _log.info("count: " + count);

							statistic.put("dossierStatus", statusCode);
							statistic.put("dossierSubStatus", subStatusCode);
							statistic.put("level", dictItem.getLevel());
							statistic.put("statusName", dictItem.getItemName());
							statistic.put("count", count);
							if (dictItem.getParentItemId() != 0) {
								total += count;
							}
							statistics.put(statistic);
						} else {
							params.put(DossierTerm.STATUS, statusCode);
							params.put(DossierTerm.SUBSTATUS, subStatusCode);
							params.put(DossierTerm.FOLLOW, String.valueOf(false));

							long count = DossierLocalServiceUtil.countLucene(params, searchContext);

							statistic.put("dossierStatus", statusCode);
							statistic.put("dossierSubStatus", subStatusCode);
							statistic.put("level", dictItem.getLevel());
							statistic.put("statusName", dictItem.getItemName());
							statistic.put("count", count);
							if (dictItem.getParentItemId() != 0) {
								total += count;
							}
							statistics.put(statistic);
						}
					}

				}
			}

			result.put("data", statistics);

			result.put("total", total);

		} catch (Exception e) {
			_log.error(e);
		}

		return result;
	}

	@Override
	public JSONObject getDossierCountTodoPermission(long userId, long companyId, long groupId,
			LinkedHashMap<String, Object> params, Object object, ServiceContext serviceContext) {
		JSONObject result = JSONFactoryUtil.createJSONObject();

		SearchContext searchContext = new SearchContext();

		searchContext.setCompanyId(companyId);

		String statusCode = StringPool.BLANK;

		String subStatusCode = StringPool.BLANK;

		JSONArray statistics = JSONFactoryUtil.createJSONArray();

		long total = 0;

		try {
			statusCode = GetterUtil.getString(params.get(DossierTerm.STATUS));

			if (Validator.isNotNull(statusCode) ) {

				String[] statusCodeArr = statusCode.split(StringPool.COMMA);
				if (statusCodeArr != null && statusCodeArr.length > 0) {
					for (String strStatus : statusCodeArr) {
						if (Validator.isNotNull(strStatus)) {
//							_log.info("strStatus: "+strStatus);
							params.put(DossierTerm.STATUS, strStatus);
							params.put(DossierTerm.SUBSTATUS, subStatusCode);
							params.put(DossierTerm.OWNER, String.valueOf(true));
							if (!"done".equals(strStatus) && !"cancelled".equals(strStatus)) {
								params.put(DossierTerm.NOT_STATE, "cancelling");
							}
							if("cancelled".equals(strStatus)) {
								params.put(DossierTerm.NOT_STATE, StringPool.BLANK);
								params.put(DossierTerm.NOT_STATUS_REG, null);
							}

							long count = DossierLocalServiceUtil.countLucene(params, searchContext);

							JSONObject statistic = JSONFactoryUtil.createJSONObject();
							statistic.put("dossierStatus", strStatus);
							statistic.put("dossierSubStatus", subStatusCode);
							statistic.put("count", count);

							statistics.put(statistic);

							total += count;
						}
					}
				}
			}

			result.put("data", statistics);
//			_log.info("statistics: "+statistics);

			result.put("total", total);
//			_log.info("total: "+total);

		} catch (Exception e) {
			_log.error(e);
		}

		return result;
	}

	@Override
	public Dossier createDossier(long groupId,String serviceCode, String govAgencyCode, String applicantName,
			String applicantIdType, String applicantIdNo, Date applicantIdDate, String address, String cityCode,
			String districtCode, String wardCode, String contactName, String contactTelNo, String contactEmail,
			boolean isSameAsApplicant, String delegateName, String delegateIdNo, String delegateTelNo,
			String delegateEmail, String delegateAddress, String delegateCityCode, String delegateDistrictCode,
			String delegateWardCode, String applicantNote, String briefNote,
			String dossierNo, String dossierTemplateNo, int viaPostal, String postalServiceCode,
			String postalServiceName, String postalAddress, String postalCityCode, String postalDistrictCode,
			String postalWardCode, String postalTelNo, int originality, ServiceContext context) throws PortalException {

		return DossierLocalServiceUtil.createDossier(groupId, serviceCode, govAgencyCode, applicantName,
				applicantIdType, applicantIdNo, applicantIdDate, address, cityCode, districtCode, wardCode, contactName,
				contactTelNo, contactEmail, isSameAsApplicant, delegateName, delegateIdNo, delegateTelNo, delegateEmail,
				delegateAddress, delegateCityCode, delegateDistrictCode, delegateWardCode, applicantNote, briefNote,
				dossierNo, dossierTemplateNo, viaPostal, postalServiceCode, postalServiceName, postalAddress, postalCityCode, postalDistrictCode, postalWardCode, postalTelNo, originality, context);
				
	}

	private void processSyncDossierFile(long dossierId, String referenceUid, ProcessAction processAction, long groupId,
			long userId, String serverNo) {

		// check return file
		List<String> returnDossierFileTemplateNos = ListUtil
				.toList(StringUtil.split(processAction.getReturnDossierFiles()));
		_log.info("__return dossierFiles" + processAction.getReturnDossierFiles());
		_log.info("__return dossierId" + dossierId);

		List<DossierFile> dossierFileList = null;
		if (returnDossierFileTemplateNos != null && returnDossierFileTemplateNos.size() > 0) {
			dossierFileList = DossierFileLocalServiceUtil.getDossierFilesByD_DP(dossierId, 2);
		} else {
			dossierFileList = DossierFileLocalServiceUtil.getByDossierIdAndIsNew(dossierId, true);
		}

		// Update and sync dossierFile
		if (returnDossierFileTemplateNos != null && returnDossierFileTemplateNos.size() > 0) {
			_log.info("__return dossierFiles" + processAction.getReturnDossierFiles());
			if (dossierFileList != null && dossierFileList.size() > 0) {
				_log.info("__return size" + dossierFileList.size());
				String fileTemplateNo;
				for (DossierFile dossierFile : dossierFileList) {

					// Update record dossierFile
					if (dossierFile.getIsNew()) {
						updateIsNewDossierFile(dossierFile);
					}

					_log.info("__dossierPart" + processAction.getReturnDossierFiles());
					_log.info("__dossierPart" + dossierFile.getFileTemplateNo());
					fileTemplateNo = dossierFile.getFileTemplateNo();

					if (returnDossierFileTemplateNos.contains(fileTemplateNo)) {
						_log.info("START SYNC DOSSIER FILE");
//						DossierSyncLocalServiceUtil.updateDossierSync(groupId, userId, dossierId, referenceUid, false,
//								1, dossierFile.getDossierFileId(), dossierFile.getReferenceUid(), serverNo);

					}
				}
			}
		} else {// only update dossierFile
			if (dossierFileList != null && dossierFileList.size() > 0) {
				for (DossierFile dossierFile : dossierFileList) {
					updateIsNewDossierFile(dossierFile);
				}
			}
		}
	}

	//Update DossierFile when Sync
	private void updateIsNewDossierFile(DossierFile dossierFile) {
		_log.info("&&&StartUpdateDossierFile" + new Date());
		dossierFile.setIsNew(false);
		DossierFileLocalServiceUtil.updateDossierFile(dossierFile);
		_log.info("&&&EndUpdateDossierFile" + new Date());
	}

	//LamTV_Process check permission action
	private boolean processCheckEnable(String preCondition, String autoEvent, Dossier dossier, String actionCode,
			long groupId) {
		if (AUTO_EVENT_SUBMIT.equals(autoEvent) || AUTO_EVENT_TIMMER.equals(autoEvent)
				|| AUTO_EVENT_LISTENER.equals(autoEvent) || AUTO_EVENT_SPECIAL.equals(autoEvent)) {
			return false;
		}
		String[] preConditionArr = StringUtil.split(preCondition);
		if (preConditionArr != null && preConditionArr.length > 0) {
			return DossierMgtUtils.checkPreCondition(preConditionArr, dossier);
		}

//		int originality = dossier.getOriginality();
//		ActionConfig actConfig = ActionConfigLocalServiceUtil.getByCode(groupId, actionCode);
//		if (actConfig != null) {
//			int syncType = actConfig.getSyncType();
//			if (originality == 1 && syncType == 2) {
//				result = true;
//				return result;
//			} else if (syncType == 1) {
//				if (originality == 2 || originality == 3) {
//					result = true;
//					return result;
//				}
//			}
//		}

		return true;
	}

	// LamTV_Process role list user
	private List<User> processRoleListUser(List<ProcessStepRole> processStepRoleList, long serviceProcessId) {
		List<User> lstUser = null;
		// Check roles
		_log.info("processStepRoleList: "+processStepRoleList);
		if (processStepRoleList != null && processStepRoleList.size() > 0) {
			_log.info("processStepRoleList.size(): "+processStepRoleList.size());
			lstUser = new ArrayList<User>();
			for (ProcessStepRole processStepRole : processStepRoleList) {
				List<User> users = UserLocalServiceUtil.getRoleUsers(processStepRole.getRoleId());
				if (users != null && users.size() > 0) {
					_log.info("users.size(): "+users.size());
					HashMap<String, Object> assigned = new HashMap<>();
					assigned.put(ProcessStepRoleTerm.ASSIGNED, 0);
					for (User user : users) {
						HashMap<String, Object> moderator = new HashMap<>();
						moderator.put(ProcessStepRoleTerm.MODERATOR, processStepRole.getModerator());
						user.setModelAttributes(moderator);
						user.setModelAttributes(assigned);
					}
					lstUser.addAll(users);
				}
			}
		} else {
			// Search in ServiceProcessRole
			List<ServiceProcessRole> serviceProcessRoleList = ServiceProcessRoleLocalServiceUtil
					.findByS_P_ID(serviceProcessId);
			if (serviceProcessRoleList != null && serviceProcessRoleList.size() > 0) {
				lstUser = new ArrayList<User>();
				for (ServiceProcessRole serviceProcessRole : serviceProcessRoleList) {
					List<User> users = UserLocalServiceUtil
							.getRoleUsers(serviceProcessRole.getRoleId());
					if (users != null && users.size() > 0) {
						for (User user : users) {
							HashMap<String, Object> moderator = new HashMap<>();
							moderator.put("moderator", serviceProcessRole.getModerator());
							user.setModelAttributes(moderator);
						}
						lstUser.addAll(users);
					}
				}
			}
		}

		return lstUser;
	}

	private List<User> processRoleAsStepListUser(Dossier dossier, String stepCode, long serviceProcessId, ProcessStep processStep) {
		List<User> lstUser = null;
		// Check roles		
		lstUser = new ArrayList<User>();

		List<ProcessStepRole> processStepRoleList = ProcessStepRoleLocalServiceUtil
				.findByP_S_ID(processStep.getProcessStepId());
		for (ProcessStepRole role : processStepRoleList) {
			List<User> lstUsers = UserLocalServiceUtil.getRoleUsers(role.getRoleId());
			for (User u : lstUsers) {
				HashMap<String, Object> assigned = new HashMap<>();
				assigned.put(ProcessStepRoleTerm.ASSIGNED, 0);	
				HashMap<String, Object> moderator = new HashMap<>();
				moderator.put(ProcessStepRoleTerm.MODERATOR, role.getModerator());
				u.setModelAttributes(moderator);
				u.setModelAttributes(assigned);
				
				lstUser.add(u);
			}
		}
		
		return lstUser;
	}
	
	private List<User> processRoleAsStepDonedListUser(Dossier dossier, String stepCode, long serviceProcessId, ProcessStep processStep) {
		List<User> lstUser = null;
		// Check roles
		List<DossierActionUser> lstDaus = DossierActionUserLocalServiceUtil.getByDossierAndStepCode(dossier.getDossierId(), stepCode);
		
		lstUser = new ArrayList<User>();
		List<ProcessStepRole> processStepRoleList = ProcessStepRoleLocalServiceUtil
				.findByP_S_ID(processStep.getProcessStepId());
		
		for (ProcessStepRole role : processStepRoleList) {
			List<User> lstUsers = UserLocalServiceUtil.getRoleUsers(role.getRoleId());
			for (User u : lstUsers) {
				for (DossierActionUser dau : lstDaus) {
					if (dau.getUserId() == u.getUserId()) {
						User user = UserLocalServiceUtil.fetchUser(dau.getUserId());
						
						HashMap<String, Object> assigned = new HashMap<>();
						assigned.put(ProcessStepRoleTerm.ASSIGNED, dau.getAssigned());
						HashMap<String, Object> moderator = new HashMap<>();
						moderator.put(ProcessStepRoleTerm.MODERATOR, dau.getModerator());
						user.setModelAttributes(moderator);
						user.setModelAttributes(assigned);
								
						lstUser.add(user);
						
						break;
					}
				}
			}
		}

		return lstUser;
	}

	//LamTV_Process list file result
	private JSONObject processFileResult(List<DossierFile> dossierFilesResult, JSONObject createFile, String partNo) {
		boolean eForm = false;
		String formData ;
		String formScript;
		String docFileReferenceUid;
		long fileEntryId = 0;
		long dossierFileId = 0;
		for (DossierFile dossierFile : dossierFilesResult) {
			if (dossierFile.getDossierPartNo().equals(partNo)) {
				eForm = dossierFile.getEForm();
				formData = dossierFile.getFormData();
				formScript = dossierFile.getFormScript();
				docFileReferenceUid = dossierFile.getReferenceUid();
				fileEntryId = dossierFile.getFileEntryId();
				dossierFileId = dossierFile.getDossierFileId();
				//
				createFile.put(DossierFileTerm.E_FORM, eForm);
				createFile.put(DossierFileTerm.DOSSIER_FILE_ID, dossierFileId);
				createFile.put(DossierFileTerm.FORM_DATA, formData);
				createFile.put(DossierFileTerm.FORM_SCRIPT, formScript);
				createFile.put(DossierFileTerm.REFERENCE_UID, docFileReferenceUid);
				createFile.put(DossierFileTerm.FILE_ENTRY_ID, fileEntryId);
				break;
			}
		}
		return createFile;
	}

	//LamTV_Process update Dossier
	@Override
	public Dossier initUpdateDossier(long groupId, long id, String applicantName, String applicantIdType,
			String applicantIdNo, String applicantIdDate, String address, String cityCode, String cityName,
			String districtCode, String districtName, String wardCode, String wardName, String contactName,
			String contactTelNo, String contactEmail, String dossierTemplateNo, int viaPostal, String postalAddress,
			String postalCityCode, String postalCityName, String postalTelNo, String applicantNote, boolean isSameAsApplicant,
			String delegateName, String delegateIdNo, String delegateTelNo, String delegateEmail,
			String delegateAddress, String delegateCityCode, String delegateDistrictCode, String delegateWardCode,
			Long sampleCount, ServiceContext serviceContext) {

		try {
			return DossierLocalServiceUtil.initUpdateDossier(groupId, id, applicantName, applicantIdType, applicantIdNo,
					applicantIdDate, address, cityCode, cityName, districtCode, districtName, wardCode, wardName,
					contactName, contactTelNo, contactEmail, dossierTemplateNo, viaPostal, postalAddress,
					postalCityCode, postalCityName, postalTelNo, applicantNote, isSameAsApplicant, delegateName,
					delegateIdNo, delegateTelNo, delegateEmail, delegateAddress, delegateCityCode, delegateDistrictCode,
					delegateWardCode, sampleCount, serviceContext);

		} catch (Exception e) {
			_log.debug(e);
			//_log.error(e);
			return null;
		}
	}

	@Override
	public Dossier publishDossier(long groupId, long dossierId, String referenceUid, int counter, String serviceCode,
			String serviceName, String govAgencyCode, String govAgencyName, String applicantName,
			String applicantIdType, String applicantIdNo, String applicantIdDate, String address, String cityCode,
			String cityName, String districtCode, String districtName, String wardCode, String wardName,
			String contactName, String contactTelNo, String contactEmail, String dossierTemplateNo, String password,
			int viaPostal, String postalAddress, String postalCityCode, String postalCityName, String postalTelNo,
			boolean online, boolean notification, String applicantNote, int originality, 
			Date createDate, Date modifiedDate, Date submitDate, Date receiveDate, Date dueDate,
			Date releaseDate, Date finishDate, Date cancellingDate, Date correctingDate, 
			Date endorsementDate, Date extendDate,
			Date processDate, ServiceContext context)
			throws PortalException {

//		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
		Date appIdDate = null;
//		try {
//			appIdDate = sdf.parse(applicantIdDate);
//		} catch (Exception e) {
//			// TODO: handle exception
//			_log.debug(e);
//			//_log.error(e);
//		}

		Dossier dossier = null;

		try {

			//Process
			dossier = DossierLocalServiceUtil.publishDossier(groupId, dossierId, referenceUid, counter, serviceCode,
					serviceName, govAgencyCode, govAgencyName, applicantName, applicantIdType, applicantIdNo, appIdDate,
					address, cityCode, cityName, districtCode, districtName, wardCode, wardName, contactName,
					contactTelNo, contactEmail, dossierTemplateNo, password, viaPostal, postalAddress, postalCityCode,
					postalCityName, postalTelNo, online, notification, applicantNote, originality, createDate, modifiedDate, submitDate, receiveDate, dueDate,
					releaseDate, finishDate, cancellingDate, correctingDate, 
					endorsementDate, extendDate,
					processDate, context);

		} catch (Exception e) {
			_log.debug(e);
			//_log.error(e);
		}

		return dossier;	
	}

}
