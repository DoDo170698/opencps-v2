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

package org.opencps.datamgt.model;

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
 * The base model interface for the Holiday service. Represents a row in the &quot;opencps_holiday&quot; database table, with each column mapped to a property of this class.
 *
 * <p>
 * This interface and its corresponding implementation {@link org.opencps.datamgt.model.impl.HolidayModelImpl} exist only as a container for the default property accessors generated by ServiceBuilder. Helper methods and all application logic should be put in {@link org.opencps.datamgt.model.impl.HolidayImpl}.
 * </p>
 *
 * @author khoavu
 * @see Holiday
 * @see org.opencps.datamgt.model.impl.HolidayImpl
 * @see org.opencps.datamgt.model.impl.HolidayModelImpl
 * @generated
 */
@ProviderType
public interface HolidayModel extends BaseModel<Holiday>, GroupedModel,
	ShardedModel, StagedAuditedModel {
	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this interface directly. All methods that expect a holiday model instance should use the {@link Holiday} interface instead.
	 */

	/**
	 * Returns the primary key of this holiday.
	 *
	 * @return the primary key of this holiday
	 */
	public long getPrimaryKey();

	/**
	 * Sets the primary key of this holiday.
	 *
	 * @param primaryKey the primary key of this holiday
	 */
	public void setPrimaryKey(long primaryKey);

	/**
	 * Returns the uuid of this holiday.
	 *
	 * @return the uuid of this holiday
	 */
	@AutoEscape
	@Override
	public String getUuid();

	/**
	 * Sets the uuid of this holiday.
	 *
	 * @param uuid the uuid of this holiday
	 */
	@Override
	public void setUuid(String uuid);

	/**
	 * Returns the holiday ID of this holiday.
	 *
	 * @return the holiday ID of this holiday
	 */
	public long getHolidayId();

	/**
	 * Sets the holiday ID of this holiday.
	 *
	 * @param holidayId the holiday ID of this holiday
	 */
	public void setHolidayId(long holidayId);

	/**
	 * Returns the company ID of this holiday.
	 *
	 * @return the company ID of this holiday
	 */
	@Override
	public long getCompanyId();

	/**
	 * Sets the company ID of this holiday.
	 *
	 * @param companyId the company ID of this holiday
	 */
	@Override
	public void setCompanyId(long companyId);

	/**
	 * Returns the group ID of this holiday.
	 *
	 * @return the group ID of this holiday
	 */
	@Override
	public long getGroupId();

	/**
	 * Sets the group ID of this holiday.
	 *
	 * @param groupId the group ID of this holiday
	 */
	@Override
	public void setGroupId(long groupId);

	/**
	 * Returns the user ID of this holiday.
	 *
	 * @return the user ID of this holiday
	 */
	@Override
	public long getUserId();

	/**
	 * Sets the user ID of this holiday.
	 *
	 * @param userId the user ID of this holiday
	 */
	@Override
	public void setUserId(long userId);

	/**
	 * Returns the user uuid of this holiday.
	 *
	 * @return the user uuid of this holiday
	 */
	@Override
	public String getUserUuid();

	/**
	 * Sets the user uuid of this holiday.
	 *
	 * @param userUuid the user uuid of this holiday
	 */
	@Override
	public void setUserUuid(String userUuid);

	/**
	 * Returns the user name of this holiday.
	 *
	 * @return the user name of this holiday
	 */
	@AutoEscape
	@Override
	public String getUserName();

	/**
	 * Sets the user name of this holiday.
	 *
	 * @param userName the user name of this holiday
	 */
	@Override
	public void setUserName(String userName);

	/**
	 * Returns the create date of this holiday.
	 *
	 * @return the create date of this holiday
	 */
	@Override
	public Date getCreateDate();

	/**
	 * Sets the create date of this holiday.
	 *
	 * @param createDate the create date of this holiday
	 */
	@Override
	public void setCreateDate(Date createDate);

	/**
	 * Returns the modified date of this holiday.
	 *
	 * @return the modified date of this holiday
	 */
	@Override
	public Date getModifiedDate();

	/**
	 * Sets the modified date of this holiday.
	 *
	 * @param modifiedDate the modified date of this holiday
	 */
	@Override
	public void setModifiedDate(Date modifiedDate);

	/**
	 * Returns the holiday date of this holiday.
	 *
	 * @return the holiday date of this holiday
	 */
	public Date getHolidayDate();

	/**
	 * Sets the holiday date of this holiday.
	 *
	 * @param holidayDate the holiday date of this holiday
	 */
	public void setHolidayDate(Date holidayDate);

	/**
	 * Returns the description of this holiday.
	 *
	 * @return the description of this holiday
	 */
	@AutoEscape
	public String getDescription();

	/**
	 * Sets the description of this holiday.
	 *
	 * @param description the description of this holiday
	 */
	public void setDescription(String description);

	/**
	 * Returns the holiday type of this holiday.
	 *
	 * @return the holiday type of this holiday
	 */
	public int getHolidayType();

	/**
	 * Sets the holiday type of this holiday.
	 *
	 * @param holidayType the holiday type of this holiday
	 */
	public void setHolidayType(int holidayType);

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
	public int compareTo(Holiday holiday);

	@Override
	public int hashCode();

	@Override
	public CacheModel<Holiday> toCacheModel();

	@Override
	public Holiday toEscapedModel();

	@Override
	public Holiday toUnescapedModel();

	@Override
	public String toString();

	@Override
	public String toXmlString();
}