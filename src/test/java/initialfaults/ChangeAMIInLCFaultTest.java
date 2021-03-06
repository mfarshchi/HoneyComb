package initialfaults;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model
    .CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.ec2.model.Instance;
import fault.HoneyCombException;
import lib.AsgService;
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

import static org.mockito.Mockito.*;

/**
 * Created by wilsoncao on 6/14/16.
 */
public class ChangeAMIInLCFaultTest {
  private static Ec2Service ec2Service;
  private static AsgService asgService;
  private static LaunchConfiguration lc;
  private static CreateLaunchConfigurationRequest req;
  private static Instance instance;
  private static String asgName;
  private static String faultyAmiId;
  private static AutoScalingGroup asg;
  private static HashMap<String, String> params;

  LogChanger log = new LogChanger();

  @Rule
  public ExpectedException thrown = ExpectedException.none();


  @Before
  public void setUp() throws IOException {
    ec2Service = mockLib.Ec2Service.getEc2Service();
    asgService = mockLib.AsgService.getAsgService();
    asg = mockAws.AutoScalingGroup.getAsg();
    asgName = "asg";

    faultyAmiId = "faultyAmiId";
    params = new HashMap<String, String>();
    log.setupLogForTest();

  }

  @After
  public void tearDown() throws Exception {
    log.resetLogAfterTest();
  }


  @Test
  public void faultTest() throws Exception {
    HashMap<String, String> params = new HashMap<>();
    params.put("asgName", asgName);
    params.put("faultyAmiId", faultyAmiId);
    params.put("faultInstanceId", "asdfjasldfkjasdf;");
    ChangeAmiInLcFault fault = new ChangeAmiInLcFault(params);
    fault.ec2ServiceSetter(ec2Service);
    fault.asgServiceSetter(asgService);
    fault.asgSetter(asg);
    fault.start();

    InOrder inOrder = inOrder(asgService, ec2Service);
    inOrder.verify(asgService).getLaunchConfigurationForAutoScalingGroup
        (asgName);
    inOrder.verify(asgService).createLaunchConfiguration(any());
    inOrder.verify(asgService).updateLaunchConfigurationInAutoScalingGroup
        (asgName, "faulty-lc");
    inOrder.verify(ec2Service).terminateInstance(anyString());


  }

  @Test
  public void faultTestNull() throws Exception {
    HashMap<String, String> params = new HashMap<>();
    params.put("asgName", asgName);
    params.put("faultyAmiId", faultyAmiId);
    params.put("faultInstanceId", "asdfjasldfkjasdf;");
    ChangeAmiInLcFault fault = new ChangeAmiInLcFault(params);
    thrown.expect(HoneyCombException.class);
    fault.start();


  }


}
