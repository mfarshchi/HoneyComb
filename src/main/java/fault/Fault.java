package fault;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;

/**
 * Created by wilsoncao on 6/9/16.
 */
public interface Fault {
    public void start() throws AmazonServiceException, AmazonClientException, HoneyCombException;
}
