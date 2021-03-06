package initialfaults;

import lib.Ec2Service;
import logmodifier.LogChanger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

import java.io.IOException;
import java.util.HashMap;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;

/**
 * Created by wilsoncao on 6/17/16.
 */
public class InstanceUnavailableFaultTest {
  private String instanceName;
  private HashMap<String, String> params;
  private Ec2Service ec2Service;
  LogChanger log = new LogChanger();

  @After
  public void tearDown() throws Exception {
    log.resetLogAfterTest();
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws IOException {
    instanceName = "hello";
    params = new HashMap<String, String>();
    ec2Service = mockLib.Ec2Service.getEc2Service();
    log.setupLogForTest();
  }


  @Test
  public void faultTest() throws Exception {
    HashMap<String, String> params = new HashMap<>();
    params.put("instanceId", instanceName);
    params.put("faultInstanceId", "asdfjasldfkjasdf;");
    InstanceUnavailableFault fault = new InstanceUnavailableFault(params);
    fault.ec2ServiceSetter(ec2Service);
    fault.start();

    InOrder inOrder = inOrder(ec2Service);

    inOrder.verify(ec2Service).terminateInstance(anyString());

  }

  @Test
  public void faultTestNull() throws Exception {
    HashMap<String, String> params = new HashMap<>();
    params.put("instanceId", instanceName);
    params.put("faultInstanceId", "asdfjasldfkjasdf;");
    InstanceUnavailableFault fault = new InstanceUnavailableFault(params);
    thrown.expect(Exception.class);
    fault.start();


  }
}
