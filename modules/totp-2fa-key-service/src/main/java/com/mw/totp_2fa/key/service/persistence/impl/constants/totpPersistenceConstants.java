/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.mw.totp_2fa.key.service.persistence.impl.constants;

/**
 * @author Brian Wing Shun Chan
 * @generated
 */
public class totpPersistenceConstants {

	public static final String BUNDLE_SYMBOLIC_NAME =
		"com.mw.totp_2fa.key.service";

	public static final String ORIGIN_BUNDLE_SYMBOLIC_NAME_FILTER =
		"(origin.bundle.symbolic.name=" + BUNDLE_SYMBOLIC_NAME + ")";

	// The Service Builder Gradle plugin version this project was generated
	// with emits "(name=service)" here, but on this Liferay release the Spring
	// Extender only ever registers per-module Configuration services with
	// name=portlet (verified: every one of Liferay's own ~90 Service Builder
	// modules registers "name=portlet"; none register "name=service"). Left
	// as "service" this reference is permanently UNSATISFIED.
	public static final String SERVICE_CONFIGURATION_FILTER =
		"(&" + ORIGIN_BUNDLE_SYMBOLIC_NAME_FILTER + "(name=portlet))";

}