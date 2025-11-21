/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.mw.totp_2fa.key.model;

import com.liferay.portal.kernel.annotation.ImplementationClassName;
import com.liferay.portal.kernel.model.PersistedModel;
import com.liferay.portal.kernel.util.Accessor;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The extended model interface for the SecretKey service. Represents a row in the &quot;totp_SecretKey&quot; database table, with each column mapped to a property of this class.
 *
 * @author Brian Wing Shun Chan
 * @see SecretKeyModel
 * @generated
 */
@ImplementationClassName("com.mw.totp_2fa.key.model.impl.SecretKeyImpl")
@ProviderType
public interface SecretKey extends PersistedModel, SecretKeyModel {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify this interface directly. Add methods to <code>com.mw.totp_2fa.key.model.impl.SecretKeyImpl</code> and rerun ServiceBuilder to automatically copy the method declarations to this interface.
	 */
	public static final Accessor<SecretKey, Long> SECRET_KEY_ID_ACCESSOR =
		new Accessor<SecretKey, Long>() {

			@Override
			public Long get(SecretKey secretKey) {
				return secretKey.getSecretKeyId();
			}

			@Override
			public Class<Long> getAttributeClass() {
				return Long.class;
			}

			@Override
			public Class<SecretKey> getTypeClass() {
				return SecretKey.class;
			}

		};

}