package com.siurbinfo.srs;

import com.siurbinfo.srs.service.cognito.group.CognitoGroupService;
import com.siurbinfo.srs.service.cognito.user.CognitoUserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class SrsApplicationTests {

	@MockitoBean
	CognitoUserService cognitoUserService;

	@MockitoBean
	CognitoGroupService cognitoGroupService;

	@Test
	void contextLoads() {
	}

}
