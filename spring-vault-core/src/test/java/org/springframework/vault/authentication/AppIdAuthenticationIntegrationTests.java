/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.vault.authentication;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.vault.client.VaultClient;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.client.VaultException;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.VaultToken;
import org.springframework.vault.util.IntegrationTestSupport;
import org.springframework.vault.util.Settings;
import org.springframework.vault.util.TestRestTemplateFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AppIdAuthentication}.
 * 
 * @author Mark Paluch
 */
public class AppIdAuthenticationIntegrationTests extends IntegrationTestSupport {

	@Before
	public void before() throws Exception {

		if (!prepare().hasAuth("app-id")) {
			prepare().mountAuth("app-id");
		}

		prepare().getVaultOperations().doWithVault(
				new VaultOperations.SessionCallback<Object>() {

					@Override
					public Object doWithVault(VaultOperations.VaultSession session) {

						Map<String, String> appIdData = new HashMap<String, String>();
						appIdData.put("value", "dummy"); // policy
						appIdData.put("display_name", "this is my test application");

						session.postForEntity("auth/app-id/map/app-id/myapp", appIdData,
								Map.class);

						Map<String, String> userIdData = new HashMap<String, String>();
						userIdData.put("value", "myapp"); // name of the app-id
						userIdData.put("cidr_block", "0.0.0.0/0");

						session.postForEntity(
								"auth/app-id/map/user-id/static-userid-value",
								userIdData, Map.class);

						return null;
					}
				});
	}

	@Test
	public void shouldLoginSuccessfully() throws Exception {

		AppIdAuthenticationOptions options = AppIdAuthenticationOptions.builder()
				.appId("myapp") //
				.userIdMechanism(new StaticUserId("static-userid-value")) //
				.build();

		VaultClient vaultClient = new VaultClient(TestRestTemplateFactory.create(Settings
				.createSslConfiguration()), new VaultEndpoint());

		AppIdAuthentication authentication = new AppIdAuthentication(options, vaultClient);
		VaultToken login = authentication.login();

		assertThat(login.getToken()).isNotEmpty();
	}

	@Test(expected = VaultException.class)
	public void loginShouldFail() throws Exception {

		AppIdAuthenticationOptions options = AppIdAuthenticationOptions.builder()
				.appId("wrong") //
				.userIdMechanism(new StaticUserId("wrong")) //
				.build();

		VaultClient vaultClient = new VaultClient(TestRestTemplateFactory.create(Settings
				.createSslConfiguration()), new VaultEndpoint());

		new AppIdAuthentication(options, vaultClient).login();
	}
}
