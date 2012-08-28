package mtest;

import mapp.App;
import mapp.PseudoBooleanExample;
import org.testng.annotations.Test;
import org.testng.Assert;

public class PseudoBooleanExampleTest {
    @Test
    public void test() throws Exception {
       Assert.assertTrue(true);
       
       PseudoBooleanExample.main();
    }    
}