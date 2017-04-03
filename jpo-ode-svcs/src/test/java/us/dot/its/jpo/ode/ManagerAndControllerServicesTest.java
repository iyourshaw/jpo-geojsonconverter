package us.dot.its.jpo.ode;

import org.junit.Test;
import mockit.Mocked;
import mockit.Verifications;
import us.dot.its.jpo.ode.eventlog.EventLogger;

public class ManagerAndControllerServicesTest {

	@Test
	public void testLog1(@Mocked Throwable t) {
		ManagerAndControllerServices.log(true, "test", t);
		new Verifications(){{
			EventLogger.logger.info(anyString);
		}};
	}
	
	@Test
	public void testLog2() {
		ManagerAndControllerServices.log(false, "test", null);
		new Verifications(){{
			EventLogger.logger.error(anyString);
		}};
	}
	
	@Test
	public void testLog3(@Mocked Throwable t) {
		ManagerAndControllerServices.log(false, "test", t);
		new Verifications(){{
			EventLogger.logger.error(anyString, t);
		}};
	}

}
