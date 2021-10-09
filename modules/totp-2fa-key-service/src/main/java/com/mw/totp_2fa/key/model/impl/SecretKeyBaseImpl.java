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

package com.mw.totp_2fa.key.model.impl;

import com.mw.totp_2fa.key.model.SecretKey;
import com.mw.totp_2fa.key.service.SecretKeyLocalServiceUtil;

/**
 * The extended model base implementation for the SecretKey service. Represents a row in the &quot;totp_SecretKey&quot; database table, with each column mapped to a property of this class.
 *
 * <p>
 * This class exists only as a container for the default extended model level methods generated by ServiceBuilder. Helper methods and all application logic should be put in {@link SecretKeyImpl}.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see SecretKeyImpl
 * @see SecretKey
 * @generated
 */
public abstract class SecretKeyBaseImpl
	extends SecretKeyModelImpl implements SecretKey {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this class directly. All methods that expect a secret key model instance should use the <code>SecretKey</code> interface instead.
	 */
	@Override
	public void persist() {
		if (this.isNew()) {
			SecretKeyLocalServiceUtil.addSecretKey(this);
		}
		else {
			SecretKeyLocalServiceUtil.updateSecretKey(this);
		}
	}

}