/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.opencps.dossiermgt.model;

import aQute.bnd.annotation.ProviderType;

import com.liferay.expando.kernel.model.ExpandoBridge;

import com.liferay.portal.kernel.bean.AutoEscape;
import com.liferay.portal.kernel.model.BaseModel;
import com.liferay.portal.kernel.model.CacheModel;
import com.liferay.portal.kernel.model.GroupedModel;
import com.liferay.portal.kernel.model.ShardedModel;
import com.liferay.portal.kernel.model.StagedAuditedModel;
import com.liferay.portal.kernel.service.ServiceContext;

import java.io.Serializable;

import java.util.Date;

/**
 * The base model interface for the PaymentConfig service. Represents a row in the &quot;opencps_paymentconfig&quot; database table, with each column mapped to a property of this class.
 *
 * <p>
 * This interface and its corresponding implementation {@link org.opencps.dossiermgt.model.impl.PaymentConfigModelImpl} exist only as a container for the default property accessors generated by ServiceBuilder. Helper methods and all application logic should be put in {@link org.opencps.dossiermgt.model.impl.PaymentConfigImpl}.
 * </p>
 *
 * @author huymq
 * @see PaymentConfig
 * @see org.opencps.dossiermgt.model.impl.PaymentConfigImpl
 * @see org.opencps.dossiermgt.model.impl.PaymentConfigModelImpl
 * @generated
 */
@ProviderType
public interface PaymentConfigModel extends BaseModel<PaymentConfig>,
	GroupedModel, ShardedModel, StagedAuditedModel {
	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this interface directly. All methods that expect a payment config model instance should use the {@link PaymentConfig} interface instead.
	 */

	/**
	 * Returns the primary key of this payment config.
	 *
	 * @return the primary key of this payment config
	 */
	public long getPrimaryKey();

	/**
	 * Sets the primary key of this payment config.
	 *
	 * @param primaryKey the primary key of this payment config
	 */
	public void setPrimaryKey(long primaryKey);

	/**
	 * Returns the uuid of this payment config.
	 *
	 * @return the uuid of this payment config
	 */
	@AutoEscape
	@Override
	public String getUuid();

	/**
	 * Sets the uuid of this payment config.
	 *
	 * @param uuid the uuid of this payment config
	 */
	@Override
	public void setUuid(String uuid);

	/**
	 * Returns the payment config ID of this payment config.
	 *
	 * @return the payment config ID of this payment config
	 */
	public long getPaymentConfigId();

	/**
	 * Sets the payment config ID of this payment config.
	 *
	 * @param paymentConfigId the payment config ID of this payment config
	 */
	public void setPaymentConfigId(long paymentConfigId);

	/**
	 * Returns the group ID of this payment config.
	 *
	 * @return the group ID of this payment config
	 */
	@Override
	public long getGroupId();

	/**
	 * Sets the group ID of this payment config.
	 *
	 * @param groupId the group ID of this payment config
	 */
	@Override
	public void setGroupId(long groupId);

	/**
	 * Returns the company ID of this payment config.
	 *
	 * @return the company ID of this payment config
	 */
	@Override
	public long getCompanyId();

	/**
	 * Sets the company ID of this payment config.
	 *
	 * @param companyId the company ID of this payment config
	 */
	@Override
	public void setCompanyId(long companyId);

	/**
	 * Returns the user ID of this payment config.
	 *
	 * @return the user ID of this payment config
	 */
	@Override
	public long getUserId();

	/**
	 * Sets the user ID of this payment config.
	 *
	 * @param userId the user ID of this payment config
	 */
	@Override
	public void setUserId(long userId);

	/**
	 * Returns the user uuid of this payment config.
	 *
	 * @return the user uuid of this payment config
	 */
	@Override
	public String getUserUuid();

	/**
	 * Sets the user uuid of this payment config.
	 *
	 * @param userUuid the user uuid of this payment config
	 */
	@Override
	public void setUserUuid(String userUuid);

	/**
	 * Returns the user name of this payment config.
	 *
	 * @return the user name of this payment config
	 */
	@AutoEscape
	@Override
	public String getUserName();

	/**
	 * Sets the user name of this payment config.
	 *
	 * @param userName the user name of this payment config
	 */
	@Override
	public void setUserName(String userName);

	/**
	 * Returns the create date of this payment config.
	 *
	 * @return the create date of this payment config
	 */
	@Override
	public Date getCreateDate();

	/**
	 * Sets the create date of this payment config.
	 *
	 * @param createDate the create date of this payment config
	 */
	@Override
	public void setCreateDate(Date createDate);

	/**
	 * Returns the modified date of this payment config.
	 *
	 * @return the modified date of this payment config
	 */
	@Override
	public Date getModifiedDate();

	/**
	 * Sets the modified date of this payment config.
	 *
	 * @param modifiedDate the modified date of this payment config
	 */
	@Override
	public void setModifiedDate(Date modifiedDate);

	/**
	 * Returns the gov agency code of this payment config.
	 *
	 * @return the gov agency code of this payment config
	 */
	@AutoEscape
	public String getGovAgencyCode();

	/**
	 * Sets the gov agency code of this payment config.
	 *
	 * @param govAgencyCode the gov agency code of this payment config
	 */
	public void setGovAgencyCode(String govAgencyCode);

	/**
	 * Returns the gov agency name of this payment config.
	 *
	 * @return the gov agency name of this payment config
	 */
	@AutoEscape
	public String getGovAgencyName();

	/**
	 * Sets the gov agency name of this payment config.
	 *
	 * @param govAgencyName the gov agency name of this payment config
	 */
	public void setGovAgencyName(String govAgencyName);

	/**
	 * Returns the gov agency tax no of this payment config.
	 *
	 * @return the gov agency tax no of this payment config
	 */
	@AutoEscape
	public String getGovAgencyTaxNo();

	/**
	 * Sets the gov agency tax no of this payment config.
	 *
	 * @param govAgencyTaxNo the gov agency tax no of this payment config
	 */
	public void setGovAgencyTaxNo(String govAgencyTaxNo);

	/**
	 * Returns the invoice template no of this payment config.
	 *
	 * @return the invoice template no of this payment config
	 */
	@AutoEscape
	public String getInvoiceTemplateNo();

	/**
	 * Sets the invoice template no of this payment config.
	 *
	 * @param invoiceTemplateNo the invoice template no of this payment config
	 */
	public void setInvoiceTemplateNo(String invoiceTemplateNo);

	/**
	 * Returns the invoice issue no of this payment config.
	 *
	 * @return the invoice issue no of this payment config
	 */
	@AutoEscape
	public String getInvoiceIssueNo();

	/**
	 * Sets the invoice issue no of this payment config.
	 *
	 * @param invoiceIssueNo the invoice issue no of this payment config
	 */
	public void setInvoiceIssueNo(String invoiceIssueNo);

	/**
	 * Returns the invoice last no of this payment config.
	 *
	 * @return the invoice last no of this payment config
	 */
	@AutoEscape
	public String getInvoiceLastNo();

	/**
	 * Sets the invoice last no of this payment config.
	 *
	 * @param invoiceLastNo the invoice last no of this payment config
	 */
	public void setInvoiceLastNo(String invoiceLastNo);

	/**
	 * Returns the invoice form of this payment config.
	 *
	 * @return the invoice form of this payment config
	 */
	@AutoEscape
	public String getInvoiceForm();

	/**
	 * Sets the invoice form of this payment config.
	 *
	 * @param invoiceForm the invoice form of this payment config
	 */
	public void setInvoiceForm(String invoiceForm);

	/**
	 * Returns the bank info of this payment config.
	 *
	 * @return the bank info of this payment config
	 */
	@AutoEscape
	public String getBankInfo();

	/**
	 * Sets the bank info of this payment config.
	 *
	 * @param bankInfo the bank info of this payment config
	 */
	public void setBankInfo(String bankInfo);

	/**
	 * Returns the epayment config of this payment config.
	 *
	 * @return the epayment config of this payment config
	 */
	@AutoEscape
	public String getEpaymentConfig();

	/**
	 * Sets the epayment config of this payment config.
	 *
	 * @param epaymentConfig the epayment config of this payment config
	 */
	public void setEpaymentConfig(String epaymentConfig);

	@Override
	public boolean isNew();

	@Override
	public void setNew(boolean n);

	@Override
	public boolean isCachedModel();

	@Override
	public void setCachedModel(boolean cachedModel);

	@Override
	public boolean isEscapedModel();

	@Override
	public Serializable getPrimaryKeyObj();

	@Override
	public void setPrimaryKeyObj(Serializable primaryKeyObj);

	@Override
	public ExpandoBridge getExpandoBridge();

	@Override
	public void setExpandoBridgeAttributes(BaseModel<?> baseModel);

	@Override
	public void setExpandoBridgeAttributes(ExpandoBridge expandoBridge);

	@Override
	public void setExpandoBridgeAttributes(ServiceContext serviceContext);

	@Override
	public Object clone();

	@Override
	public int compareTo(PaymentConfig paymentConfig);

	@Override
	public int hashCode();

	@Override
	public CacheModel<PaymentConfig> toCacheModel();

	@Override
	public PaymentConfig toEscapedModel();

	@Override
	public PaymentConfig toUnescapedModel();

	@Override
	public String toString();

	@Override
	public String toXmlString();
}